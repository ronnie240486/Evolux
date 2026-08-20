package com.evolux.tv.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.io.SequenceInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class PlaylistCatalog(
    val canais: List<Canal>,
    val filmes: List<Midia>,
    val series: List<Midia>
)

class PlaylistRepository {
    suspend fun carregar(urlPlaylist: String): PlaylistCatalog = withContext(Dispatchers.IO) {
        val conexao = (URL(urlPlaylist).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            useCaches = false
            setRequestProperty("Accept", "audio/x-mpegurl, application/vnd.apple.mpegurl, application/json, text/plain")
        }

        try {
            val codigo = conexao.responseCode
            val contentType = conexao.contentType.orEmpty().lowercase(Locale.ROOT)
            if (codigo !in 200..299) {
                throw IOException("Playlist respondeu HTTP $codigo")
            }
            if ("text/html" in contentType) {
                throw IOException("Playlist respondeu HTML")
            }

            val entrada = conexao.inputStream ?: throw IOException("Playlist sem conteúdo")
            entrada.use { fluxo ->
                val prefixo = lerPrefixo(fluxo)
                val prefixoTrimmed = prefixo.toString(StandardCharsets.UTF_8).trimStart()
                if (prefixoTrimmed.isBlank()) {
                    throw IOException("Playlist vazia")
                }
                if (prefixoTrimmed.startsWith("<")) {
                    throw IOException("Playlist respondeu HTML no corpo")
                }

                if (prefixoTrimmed.startsWith("{") || prefixoTrimmed.startsWith("[")) {
                    parseJson(lerJsonLimitado(prefixo, fluxo))
                } else {
                    val fluxoCompleto = SequenceInputStream(ByteArrayInputStream(prefixo), fluxo)
                    fluxoCompleto.bufferedReader(StandardCharsets.UTF_8).use { leitor ->
                        parseM3u(leitor)
                    }
                }
            }
        } finally {
            conexao.disconnect()
        }
    }

    private fun lerPrefixo(fluxo: InputStream): ByteArray {
        val prefixo = ByteArray(PREFIX_BYTES)
        val quantidade = fluxo.read(prefixo)
        return if (quantidade <= 0) ByteArray(0) else prefixo.copyOf(quantidade)
    }

    private fun lerJsonLimitado(prefixo: ByteArray, fluxo: InputStream): String {
        val saida = ByteArrayOutputStream(minOf(prefixo.size, MAX_PLAYLIST_BYTES.toInt()))
        var total = 0L
        if (prefixo.isNotEmpty()) {
            saida.write(prefixo)
            total += prefixo.size
        }

        val buffer = ByteArray(BUFFER_BYTES)
        while (true) {
            val quantidade = fluxo.read(buffer)
            if (quantidade < 0) break
            total += quantidade
            if (total > MAX_PLAYLIST_BYTES) {
                throw IOException("Playlist excede o limite seguro de ${MAX_PLAYLIST_BYTES / (1024 * 1024)} MB")
            }
            saida.write(buffer, 0, quantidade)
        }
        return saida.toString(StandardCharsets.UTF_8.name()).trim()
    }

    private fun parseM3u(leitor: Reader): PlaylistCatalog {
        data class Entrada(
            val titulo: String,
            val grupo: String,
            val logo: String
        )

        val canais = mutableListOf<Canal>()
        val filmes = mutableListOf<Midia>()
        val series = mutableListOf<Midia>()
        var pendente: Entrada? = null
        var indice = 0

        leitor.forEachLine { linhaOriginal ->
            val linha = linhaOriginal.trim()
            when {
                linha.startsWith("#EXTINF", ignoreCase = true) -> {
                    val atributos = atributosExtinf(linha)
                    val titulo = linha.substringAfter(',', "Sem título").trim().ifBlank { "Sem título" }
                    pendente = Entrada(
                        titulo = titulo,
                        grupo = atributos["group-title"].orEmpty(),
                        logo = atributos["tvg-logo"].orEmpty()
                    )
                }

                linha.isNotEmpty() && !linha.startsWith("#") -> {
                    if (indice >= MAX_ITEMS) {
                        throw IOException("Playlist excede o limite seguro de $MAX_ITEMS itens")
                    }
                    val entrada = pendente ?: Entrada("Canal ${indice + 1}", "", "")
                    adicionarEntrada(
                        indice = indice++,
                        titulo = entrada.titulo,
                        grupo = entrada.grupo,
                        logo = entrada.logo,
                        url = linha,
                        canais = canais,
                        filmes = filmes,
                        series = series
                    )
                    pendente = null
                }
            }
        }

        if (canais.isEmpty() && filmes.isEmpty() && series.isEmpty()) {
            throw IOException("Playlist sem itens reconhecíveis")
        }
        return PlaylistCatalog(canais, filmes, series)
    }

    private fun parseJson(corpo: String): PlaylistCatalog {
        val raiz = if (corpo.startsWith("[")) null else JSONObject(corpo)
        val itens = if (raiz == null) {
            JSONArray(corpo)
        } else {
            listOf("streams", "channels", "items", "data")
                .asSequence()
                .mapNotNull { raiz.optJSONArray(it) }
                .firstOrNull()
                ?: throw IOException("JSON sem uma lista de streams reconhecível")
        }

        val canais = mutableListOf<Canal>()
        val filmes = mutableListOf<Midia>()
        val series = mutableListOf<Midia>()
        for (indice in 0 until itens.length()) {
            if (indice >= MAX_ITEMS) {
                throw IOException("JSON excede o limite seguro de $MAX_ITEMS itens")
            }
            val item = itens.optJSONObject(indice) ?: continue
            val url = primeiraString(item, "stream_url", "url", "direct_source") ?: continue
            val titulo = primeiraString(item, "name", "title", "stream_display_name") ?: "Sem título"
            val grupo = primeiraString(item, "category_name", "group-title", "category") ?: ""
            val logo = primeiraString(item, "stream_icon", "logo", "tvg-logo") ?: ""
            adicionarEntrada(indice, titulo, grupo, logo, url, canais, filmes, series)
        }

        if (canais.isEmpty() && filmes.isEmpty() && series.isEmpty()) {
            throw IOException("JSON sem itens reconhecíveis")
        }
        return PlaylistCatalog(canais, filmes, series)
    }

    private fun adicionarEntrada(
        indice: Int,
        titulo: String,
        grupo: String,
        logo: String,
        url: String,
        canais: MutableList<Canal>,
        filmes: MutableList<Midia>,
        series: MutableList<Midia>
    ) {
        val grupoNormalizado = grupo.lowercase(Locale.ROOT)
        val id = "playlist_${indice}_${titulo.hashCode().toUInt()}"
        when {
            grupoNormalizado.contains("filme") || grupoNormalizado.contains("movie") ||
                grupoNormalizado.contains("vod") -> filmes += Midia(
                id = id,
                titulo = titulo,
                imagemUrl = logo,
                tipo = TipoMidia.FILME,
                streamUrl = url
            )

            grupoNormalizado.contains("serie") || grupoNormalizado.contains("série") ||
                grupoNormalizado.contains("show") -> series += Midia(
                id = id,
                titulo = titulo,
                imagemUrl = logo,
                tipo = TipoMidia.SERIE,
                streamUrl = url
            )

            else -> canais += Canal(
                id = id,
                nome = titulo,
                logoUrl = logo,
                streamUrl = url,
                categoria = grupo.ifBlank { "TV ao vivo" }
            )
        }
    }

    private fun atributosExtinf(linha: String): Map<String, String> {
        val regex = Regex("([A-Za-z0-9_-]+)=\\\"([^\\\"]*)\\\"")
        return regex.findAll(linha).associate { match ->
            match.groupValues[1].lowercase(Locale.ROOT) to match.groupValues[2]
        }
    }

    private fun primeiraString(objeto: JSONObject, vararg chaves: String): String? =
        chaves.asSequence()
            .mapNotNull { chave ->
                val valor = objeto.opt(chave)
                if (valor == null || valor == JSONObject.NULL) null else valor.toString().trim().ifBlank { null }
            }
            .firstOrNull()

    private companion object {
        const val PREFIX_BYTES = 4 * 1024
        const val BUFFER_BYTES = 8 * 1024
        const val MAX_PLAYLIST_BYTES = 8L * 1024 * 1024
        const val MAX_ITEMS = 10_000
    }
}
