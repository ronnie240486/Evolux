package com.evolux.tv.data

import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.io.SequenceInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class PlaylistCatalog(
    val canais: List<Canal>,
    val filmes: List<Midia>,
    val series: List<Midia>,
    val truncado: Boolean = false
)

class PlaylistRepository {
    suspend fun carregar(urlPlaylist: String): PlaylistCatalog = withContext(Dispatchers.IO) {
        val conexao = (URL(urlPlaylist).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            useCaches = false
            setRequestProperty(
                "Accept",
                "audio/x-mpegurl, application/vnd.apple.mpegurl, application/json, text/plain"
            )
        }

        try {
            val codigo = conexao.responseCode
            val contentType = conexao.contentType.orEmpty().lowercase(Locale.ROOT)
            if (codigo !in 200..299) throw IOException("Playlist respondeu HTTP $codigo")
            if ("text/html" in contentType) throw IOException("Playlist respondeu HTML")

            val entrada = conexao.inputStream ?: throw IOException("Playlist sem conteúdo")
            entrada.use { fluxo ->
                val prefixo = lerPrefixo(fluxo)
                val prefixoTrimmed = prefixo.toString(StandardCharsets.UTF_8).trimStart()
                if (prefixoTrimmed.isBlank()) throw IOException("Playlist vazia")
                if (prefixoTrimmed.startsWith("<")) throw IOException("Playlist respondeu HTML no corpo")

                if (prefixoTrimmed.startsWith("{") || prefixoTrimmed.startsWith("[")) {
                    parseJson(lerJsonLimitado(prefixo, fluxo))
                } else {
                    val fluxoCompleto = SequenceInputStream(ByteArrayInputStream(prefixo), fluxo)
                    fluxoCompleto.bufferedReader(StandardCharsets.UTF_8).use(::parseM3u)
                }
            }
        } finally {
            conexao.disconnect()
        }
    }

    private fun lerPrefixo(fluxo: InputStream): ByteArray {
        val saida = ByteArrayOutputStream(PREFIX_BYTES)
        val buffer = ByteArray(BUFFER_BYTES)
        while (saida.size() < PREFIX_BYTES) {
            val quantidade = fluxo.read(buffer, 0, minOf(buffer.size, PREFIX_BYTES - saida.size()))
            if (quantidade <= 0) break
            saida.write(buffer, 0, quantidade)
        }
        return saida.toByteArray()
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
                throw IOException("JSON da playlist excede o limite seguro de ${MAX_PLAYLIST_BYTES / (1024 * 1024)} MB")
            }
            saida.write(buffer, 0, quantidade)
        }
        return saida.toString(StandardCharsets.UTF_8.name()).trim()
    }

    private fun parseM3u(leitor: Reader): PlaylistCatalog {
        data class Entrada(val titulo: String, val grupo: String, val logo: String)

        val canais = mutableListOf<Canal>()
        val filmes = mutableListOf<Midia>()
        val series = mutableListOf<Midia>()
        var pendente: Entrada? = null
        var totalItens = 0
        var truncado = false
        val leitorBuffer = leitor as? BufferedReader ?: leitor.buffered()

        while (true) {
            val linha = leitorBuffer.readLine()?.trim() ?: break
            when {
                linha.startsWith("#EXTINF", ignoreCase = true) -> {
                    val atributos = atributosExtinf(linha)
                    val titulo = linha.substringAfter(',', "Sem título").trim().ifBlank { "Sem título" }
                    pendente = Entrada(
                        titulo = titulo,
                        grupo = listOfNotNull(
                            atributos["group-title"],
                            atributos["group"],
                            atributos["category"]
                        ).joinToString(" | "),
                        logo = atributos["tvg-logo"].orEmpty()
                    )
                }

                linha.isNotEmpty() && !linha.startsWith("#") -> {
                    totalItens++
                    if (totalItens > MAX_TOTAL_ITEMS) {
                        truncado = true
                        break
                    }
                    val entrada = pendente ?: Entrada("Item $totalItens", "", "")
                    if (!adicionarEntrada(
                            indice = totalItens - 1,
                            titulo = entrada.titulo,
                            grupo = entrada.grupo,
                            logo = entrada.logo,
                            url = linha,
                            canais = canais,
                            filmes = filmes,
                            series = series
                        )
                    ) {
                        truncado = true
                    }
                    pendente = null
                }
            }
        }

        if (canais.isEmpty() && filmes.isEmpty() && series.isEmpty()) {
            throw IOException("Playlist sem itens reconhecíveis")
        }
        return PlaylistCatalog(canais, filmes, series, truncado)
    }

    private data class JsonItem(val objeto: JSONObject, val dicaTipo: String?)

    private fun parseJson(corpo: String): PlaylistCatalog {
        val itens = extrairItensJson(corpo)
        if (itens.isEmpty()) throw IOException("JSON sem uma lista de streams reconhecível")

        val canais = mutableListOf<Canal>()
        val filmes = mutableListOf<Midia>()
        val series = mutableListOf<Midia>()
        val urlsVistas = mutableSetOf<String>()
        var truncado = itens.size > MAX_TOTAL_ITEMS

        for ((indice, entrada) in itens.take(MAX_TOTAL_ITEMS).withIndex()) {
            val item = entrada.objeto
            val url = primeiraString(
                item,
                "stream_url",
                "stream_link",
                "direct_source",
                "url",
                "link"
            ) ?: continue
            if (!urlsVistas.add(url)) continue

            val titulo = primeiraString(item, "name", "title", "stream_display_name") ?: "Item ${indice + 1}"
            val grupo = listOfNotNull(
                entrada.dicaTipo,
                primeiraString(item, "stream_type", "type", "kind"),
                primeiraString(item, "category_name", "group-title", "group", "category")
            ).joinToString(" | ")
            val logo = primeiraString(item, "stream_icon", "logo", "tvg-logo", "icon") ?: ""
            if (!adicionarEntrada(indice, titulo, grupo, logo, url, canais, filmes, series)) {
                truncado = true
            }
        }

        if (canais.isEmpty() && filmes.isEmpty() && series.isEmpty()) {
            throw IOException("JSON sem itens reconhecíveis")
        }
        return PlaylistCatalog(canais, filmes, series, truncado)
    }

    private fun extrairItensJson(corpo: String): List<JsonItem> {
        val itens = mutableListOf<JsonItem>()
        val raizArray = corpo.trimStart().startsWith("[")
        if (raizArray) {
            adicionarArray(JSONArray(corpo), null, itens)
            return itens
        }

        val raiz = JSONObject(corpo)
        val chaves = listOf(
            "streams" to null,
            "channels" to "live",
            "live" to "live",
            "live_streams" to "live",
            "movies" to "movie",
            "filmes" to "movie",
            "series" to "series",
            "shows" to "series",
            "items" to null,
            "data" to null,
            "results" to null
        )
        chaves.forEach { (chave, dica) ->
            raiz.optJSONArray(chave)?.let { adicionarArray(it, dica, itens) }
        }
        if (itens.isEmpty() && raiz.has("url")) itens += JsonItem(raiz, null)
        return itens
    }

    private fun adicionarArray(array: JSONArray, dicaTipo: String?, destino: MutableList<JsonItem>) {
        for (indice in 0 until array.length()) {
            array.optJSONObject(indice)?.let { destino += JsonItem(it, dicaTipo) }
        }
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
    ): Boolean {
        val grupoNormalizado = normalizarTexto("$grupo $titulo")
        val id = "playlist_${indice}_${titulo.hashCode().toUInt()}"
        return when {
            grupoNormalizado.containsAny("series", "serie", "show", "novela", "anime", "desenho") -> {
                if (series.size >= MAX_ITEMS_PER_CATEGORY) false else {
                    series += Midia(
                        id = id,
                        titulo = titulo,
                        imagemUrl = logo,
                        tipo = TipoMidia.SERIE,
                        streamUrl = url,
                        categoria = grupo.ifBlank { "Sem categoria" }
                    )
                    true
                }
            }
            grupoNormalizado.containsAny("filme", "filmes", "movie", "movies", "vod", "cinema") -> {
                if (filmes.size >= MAX_ITEMS_PER_CATEGORY) false else {
                    filmes += Midia(
                        id = id,
                        titulo = titulo,
                        imagemUrl = logo,
                        tipo = TipoMidia.FILME,
                        streamUrl = url,
                        categoria = grupo.ifBlank { "Sem categoria" }
                    )
                    true
                }
            }
            else -> {
                if (canais.size >= MAX_ITEMS_PER_CATEGORY) false else {
                    canais += Canal(id, titulo, logo, url, grupo.ifBlank { "TV ao vivo" })
                    true
                }
            }
        }
    }

    private fun normalizarTexto(valor: String): String = Normalizer
        .normalize(valor, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase(Locale.ROOT)

    private fun String.containsAny(vararg termos: String): Boolean = termos.any(::contains)

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
                if (valor == null || valor == JSONObject.NULL) null
                else valor.toString().trim().ifBlank { null }
            }
            .firstOrNull()

    private companion object {
        const val PREFIX_BYTES = 4 * 1024
        const val BUFFER_BYTES = 8 * 1024
        const val MAX_PLAYLIST_BYTES = 8L * 1024 * 1024
        // Limites de proteção contra uma resposta malformada, não um corte normal do catálogo.
        // A playlist do usuário com cerca de 12 mil itens passa inteira.
        const val MAX_TOTAL_ITEMS = 100_000
        const val MAX_ITEMS_PER_CATEGORY = 50_000
    }
}
