package com.evolux.tv.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Integração Xtream compatível com as respostas observadas no OuroPro.
 * A URL cadastrada continua sendo a fonte de autenticação; usuário e senha
 * são extraídos somente em tempo de execução e nunca ficam no código.
 */
class XtreamRepository {
    private data class Conexao(
        val base: String,
        val usuario: String,
        val senha: String
    )

    suspend fun carregarSeries(urlPlaylist: String): List<Midia> = withContext(Dispatchers.IO) {
        val conexao = extrairConexao(urlPlaylist) ?: return@withContext emptyList()
        runCatching {
            val categorias = getJsonArray(conexao, "get_series_categories")
                .mapNotNull { objeto ->
                    objeto.optString("category_id").takeIf { it.isNotBlank() }?.let { id ->
                        id to objeto.optString("category_name").ifBlank { "Séries" }
                    }
                }
                .toMap()
            val seriesJson = getJsonArray(conexao, "get_series")
            val vistos = mutableSetOf<String>()
            seriesJson.mapNotNull { item ->
                val id = item.optString("series_id").takeIf { it.isNotBlank() && it != "null" }
                    ?: return@mapNotNull null
                if (!vistos.add(id)) return@mapNotNull null
                val titulo = item.optString("title").ifBlank { item.optString("name") }.ifBlank { "Série $id" }
                val categoriaId = item.optString("category_id")
                    .ifBlank { item.optJSONArray("category_ids")?.optString(0).orEmpty() }
                val categoria = categorias[categoriaId] ?: "Séries"
                val capa = primeiraImagem(
                    item.optString("cover"),
                    item.optJSONArray("backdrop_path")?.optString(0).orEmpty()
                )
                val nota = item.optString("rating").toDoubleOrNull()
                    ?: item.optDouble("rating_5based", Double.NaN).takeIf { !it.isNaN() }?.times(2.0)
                Midia(
                    id = "xtream_series_$id",
                    titulo = titulo,
                    imagemUrl = capa,
                    tipo = TipoMidia.SERIE,
                    streamUrl = "xtream://series/$id",
                    categoria = categoria,
                    nota = nota,
                    popularidade = item.optLong("last_modified", 0L).takeIf { it > 0L },
                    sinopse = item.optString("plot"),
                    serieId = id,
                    serieNome = titulo
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun carregarEpisodios(urlPlaylist: String, serie: Midia): List<Midia> = withContext(Dispatchers.IO) {
        val serieId = serie.serieId?.takeIf { it.isNotBlank() } ?: return@withContext emptyList()
        val conexao = extrairConexao(urlPlaylist) ?: return@withContext emptyList()
        runCatching {
            val raiz = getJsonObject(conexao, "get_series_info", "series_id" to serieId)
            val episodios = raiz.opt("episodes")
            when (episodios) {
                is JSONObject -> parseEpisodiosPorTemporada(episodios, conexao, serie)
                is JSONArray -> parseArrayDeEpisodios(episodios, "1", conexao, serie)
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
    }

    private fun parseEpisodiosPorTemporada(
        temporadas: JSONObject,
        conexao: Conexao,
        serie: Midia
    ): List<Midia> {
        val resultado = mutableListOf<Midia>()
        val chaves = temporadas.keys()
        while (chaves.hasNext()) {
            val chave = chaves.next()
            val valor = temporadas.opt(chave)
            if (valor is JSONArray) resultado += parseArrayDeEpisodios(valor, chave, conexao, serie)
        }
        return resultado
    }

    private fun parseArrayDeEpisodios(
        array: JSONArray,
        temporadaChave: String,
        conexao: Conexao,
        serie: Midia
    ): List<Midia> {
        val temporadaPadrao = temporadaChave.filter { it.isDigit() }.toIntOrNull() ?: 1
        return buildList {
            for (indice in 0 until array.length()) {
                val item = array.optJSONObject(indice) ?: continue
                val id = item.optString("id").takeIf { it.isNotBlank() && it != "null" } ?: continue
                val temporada = item.optInt("season", temporadaPadrao)
                val episodio = item.optInt("episode_num", indice + 1)
                val info = item.optJSONObject("info")
                val titulo = item.optString("title")
                    .ifBlank { "${serie.titulo} S${temporada.toString().padStart(2, '0')}E${episodio.toString().padStart(2, '0')}" }
                val extensao = item.optString("container_extension")
                    .ifBlank { info?.optString("container_extension").orEmpty() }
                    .ifBlank { "mp4" }
                    .removePrefix(".")
                val imagem = primeiraImagem(
                    item.optString("movie_image"),
                    item.optString("cover"),
                    item.optString("cover_big"),
                    info?.optString("movie_image").orEmpty(),
                    info?.optString("cover_big").orEmpty(),
                    serie.imagemUrl
                )
                val sinopse = item.optString("plot").ifBlank { info?.optString("plot").orEmpty() }
                add(
                    Midia(
                        id = "xtream_episode_${serie.serieId}_$id",
                        titulo = titulo,
                        imagemUrl = imagem,
                        tipo = TipoMidia.SERIE,
                        streamUrl = "${conexao.base}/series/${conexao.usuario}/${conexao.senha}/$id.$extensao",
                        categoria = serie.categoria,
                        nota = item.optString("rating").toDoubleOrNull()
                            ?: info?.optString("rating")?.toDoubleOrNull(),
                        sinopse = sinopse,
                        serieId = serie.serieId,
                        serieNome = serie.titulo,
                        episodioNome = titulo,
                        temporadaNumero = temporada,
                        episodioNumero = episodio
                    )
                )
            }
        }
    }

    private fun getJsonArray(conexao: Conexao, action: String): List<JSONObject> {
        val corpo = requisitar(conexao, action)
        val array = JSONArray(corpo)
        return buildList {
            for (indice in 0 until array.length()) {
                array.optJSONObject(indice)?.let(::add)
            }
        }
    }

    private fun getJsonObject(conexao: Conexao, action: String, vararg parametros: Pair<String, String>): JSONObject {
        return JSONObject(requisitar(conexao, action, *parametros))
    }

    private fun requisitar(conexao: Conexao, action: String, vararg parametros: Pair<String, String>): String {
        val query = buildString {
            append("username=").append(encode(conexao.usuario))
            append("&password=").append(encode(conexao.senha))
            append("&action=").append(encode(action))
            parametros.forEach { (chave, valor) ->
                append('&').append(encode(chave)).append('=').append(encode(valor))
            }
        }
        val conexaoHttp = (URL("${conexao.base}/player_api.php?$query").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 30_000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Evolux/1.0 (Android)")
            setRequestProperty("Connection", "close")
        }
        try {
            if (conexaoHttp.responseCode !in 200..299) throw IOException("Xtream HTTP ${conexaoHttp.responseCode}")
            val texto = conexaoHttp.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }.trim()
            if (texto.isBlank() || texto.startsWith("<")) throw IOException("Xtream respondeu conteúdo inválido")
            return texto
        } finally {
            conexaoHttp.disconnect()
        }
    }

    private fun extrairConexao(urlPlaylist: String): Conexao? {
        val url = runCatching { URL(urlPlaylist) }.getOrNull() ?: return null
        val parametros = url.query.orEmpty().split('&')
            .mapNotNull { parte ->
                val separador = parte.indexOf('=')
                if (separador <= 0) return@mapNotNull null
                val chave = URLDecoder.decode(parte.substring(0, separador), StandardCharsets.UTF_8.name())
                val valor = URLDecoder.decode(parte.substring(separador + 1), StandardCharsets.UTF_8.name())
                chave.lowercase(Locale.ROOT) to valor
            }
            .toMap()
        val usuario = parametros["username"]?.takeIf { it.isNotBlank() } ?: return null
        val senha = parametros["password"]?.takeIf { it.isNotBlank() } ?: return null
        val porta = when {
            url.port > 0 -> ":${url.port}"
            url.protocol.equals("https", ignoreCase = true) && url.defaultPort != 443 -> ":${url.defaultPort}"
            url.protocol.equals("http", ignoreCase = true) && url.defaultPort != 80 -> ":${url.defaultPort}"
            else -> ""
        }
        return Conexao("${url.protocol}://${url.host}$porta", usuario, senha)
    }

    private fun primeiraImagem(vararg valores: String): String = valores.firstOrNull {
        it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://"))
    }.orEmpty()

    private fun encode(valor: String): String = URLEncoder.encode(valor, StandardCharsets.UTF_8.name())

    companion object {
        fun pareceXtream(url: String): Boolean {
            val analisada = runCatching { URL(url) }.getOrNull() ?: return false
            val caminho = analisada.path.lowercase(Locale.ROOT)
            val consulta = analisada.query.orEmpty().lowercase(Locale.ROOT)
            val endpoint = caminho.endsWith("/get.php") || caminho.endsWith("/player_api.php")
            return endpoint && consulta.contains("username=") && consulta.contains("password=")
        }
    }
}
