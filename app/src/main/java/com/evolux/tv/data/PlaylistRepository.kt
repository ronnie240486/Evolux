package com.evolux.tv.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
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
            val corpo = (if (codigo in 200..299) conexao.inputStream else conexao.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
                .trim()

            if (codigo !in 200..299 || corpo.isBlank() || corpo.startsWith("<") || "text/html" in contentType) {
                throw IOException("Lista indisponível ou credenciais inválidas")
            }

            if (corpo.startsWith("{") || corpo.startsWith("[")) {
                parseJson(corpo)
            } else {
                parseM3u(corpo)
            }
        } finally {
            conexao.disconnect()
        }
    }

    private fun parseM3u(corpo: String): PlaylistCatalog {
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

        corpo.lineSequence().forEach { linhaOriginal ->
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
            throw IOException("Lista indisponível ou credenciais inválidas")
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
                ?: throw IOException("Lista indisponível ou credenciais inválidas")
        }

        val canais = mutableListOf<Canal>()
        val filmes = mutableListOf<Midia>()
        val series = mutableListOf<Midia>()
        for (indice in 0 until itens.length()) {
            val item = itens.optJSONObject(indice) ?: continue
            val url = primeiraString(item, "stream_url", "url", "direct_source") ?: continue
            val titulo = primeiraString(item, "name", "title", "stream_display_name") ?: "Sem título"
            val grupo = primeiraString(item, "category_name", "group-title", "category") ?: ""
            val logo = primeiraString(item, "stream_icon", "logo", "tvg-logo") ?: ""
            adicionarEntrada(indice, titulo, grupo, logo, url, canais, filmes, series)
        }

        if (canais.isEmpty() && filmes.isEmpty() && series.isEmpty()) {
            throw IOException("Lista indisponível ou credenciais inválidas")
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
}
