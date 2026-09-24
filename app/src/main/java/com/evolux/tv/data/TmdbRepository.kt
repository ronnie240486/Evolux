package com.evolux.tv.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Sinopse de filmes e séries vinda do TMDB (The Movie Database). Listas
 * M3U/Xtream quase nunca trazem sinopse de verdade (só nome/categoria/
 * logo/link) -- pra mostrar uma sinopse real pro usuário a única fonte
 * possível é buscar por nome numa base externa, e o TMDB é a mesma fonte
 * já usada no Rencia/Supreme (mesma chave de API, ver TmdbRepository.kt
 * de lá) pra manter o padrão entre os apps da família.
 */
class TmdbRepository {

    companion object {
        // Mesma chave já usada no Rencia/Supreme.
        private const val API_KEY = "aad81d5ba22644702893f3a88f6a08c1"
        // Cache compartilhado entre todas as telas/instâncias -- uma vez
        // buscada (ou confirmado que não existe) a sinopse de um título,
        // não busca de novo à toa.
        private val cacheFilme = mutableMapOf<String, String?>()
        private val cacheSerie = mutableMapOf<String, String?>()
    }

    /** Sinopse de um filme, buscada pelo nome. */
    suspend fun buscarSinopseFilme(nomeBruto: String): String? =
        buscarSinopse(nomeBruto, "movie", cacheFilme)

    /** Sinopse de uma série, buscada pelo nome. */
    suspend fun buscarSinopseSerie(nomeBruto: String): String? =
        buscarSinopse(nomeBruto, "tv", cacheSerie)

    private suspend fun buscarSinopse(
        nomeBruto: String,
        tipoBusca: String,
        cache: MutableMap<String, String?>
    ): String? = withContext(Dispatchers.IO) {
        val nome = limparNome(nomeBruto)
        if (nome.isBlank()) return@withContext null
        val chave = nome.lowercase()
        if (cache.containsKey(chave)) return@withContext cache[chave]

        val sinopse = runCatching { buscarNoTmdb(nome, tipoBusca) }.getOrNull()
        cache[chave] = sinopse
        sinopse
    }

    /** Remove sufixos comuns (qualidade, idioma, tags entre colchetes) que
     * atrapalham a busca por nome no TMDB. */
    private fun limparNome(nome: String): String =
        nome
            .replace(Regex("(?i)\\b(dublado|legendado|dub|leg)\\b"), "")
            .replace(Regex("(?i)\\b(4k|fhd|hd|sd|h265|h264)\\b"), "")
            .replace(Regex("[\\[({].*?[\\])}]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun buscarNoTmdb(nome: String, tipoBusca: String): String? {
        val consulta = URLEncoder.encode(nome, StandardCharsets.UTF_8.name())
        val url = URL("https://api.themoviedb.org/3/search/$tipoBusca?api_key=$API_KEY&language=pt-BR&query=$consulta")
        val conexao = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Evolux/1.0 (Android)")
            setRequestProperty("Connection", "close")
        }
        val corpo = try {
            if (conexao.responseCode !in 200..299) throw IOException("TMDB HTTP ${conexao.responseCode}")
            conexao.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } finally {
            conexao.disconnect()
        }
        if (corpo.isBlank()) return null
        val resultados = JSONObject(corpo).optJSONArray("results") ?: return null
        val primeiro = resultados.optJSONObject(0) ?: return null
        return primeiro.optString("overview").takeIf { it.isNotBlank() }
    }
}
