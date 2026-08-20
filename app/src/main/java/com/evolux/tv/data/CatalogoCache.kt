package com.evolux.tv.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

object CatalogoCache {
    private const val NOME_ARQUIVO = "evolux-catalogo-cache.json"

    suspend fun carregar(contexto: Context, fingerprint: String): PlaylistCatalog? = withContext(Dispatchers.IO) {
        val arquivo = File(contexto.filesDir, NOME_ARQUIVO)
        if (!arquivo.exists()) return@withContext null
        runCatching {
            val raiz = JSONObject(arquivo.readText())
            if (raiz.optString("fingerprint") != fingerprint) return@runCatching null
            val canais = raiz.optJSONArray("canais")?.toCanais().orEmpty()
            val filmes = raiz.optJSONArray("filmes")?.toMidias().orEmpty()
            val series = raiz.optJSONArray("series")?.toMidias().orEmpty()
            if (canais.isEmpty() && filmes.isEmpty() && series.isEmpty()) null
            else PlaylistCatalog(canais, filmes, series, truncado = raiz.optBoolean("truncado", false))
        }.getOrNull()
    }

    suspend fun salvar(contexto: Context, fingerprint: String, catalogo: PlaylistCatalog) = withContext(Dispatchers.IO) {
        val raiz = JSONObject()
            .put("fingerprint", fingerprint)
            .put("truncado", catalogo.truncado)
            .put("canais", catalogo.canais.toJsonCanais())
            .put("filmes", catalogo.filmes.toJsonMidias())
            .put("series", catalogo.series.toJsonMidias())
        File(contexto.filesDir, NOME_ARQUIVO).writeText(raiz.toString())
    }

    fun fingerprint(configuracao: EvoluxConfig): String {
        val entrada = configuracao.playlistUrls.joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256").digest(entrada.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun List<Canal>.toJsonCanais(): JSONArray = JSONArray().also { array ->
        forEach { canal ->
            array.put(
                JSONObject()
                    .put("id", canal.id)
                    .put("nome", canal.nome)
                    .put("logoUrl", canal.logoUrl)
                    .put("streamUrl", canal.streamUrl)
                    .put("categoria", canal.categoria)
            )
        }
    }

    private fun List<Midia>.toJsonMidias(): JSONArray = JSONArray().also { array ->
        forEach { midia ->
            array.put(
                JSONObject()
                    .put("id", midia.id)
                    .put("titulo", midia.titulo)
                    .put("imagemUrl", midia.imagemUrl)
                    .put("tipo", midia.tipo.name)
                    .put("streamUrl", midia.streamUrl)
                    .put("progresso", midia.progresso ?: JSONObject.NULL)
                    .put("categoria", midia.categoria)
                    .put("nota", midia.nota ?: JSONObject.NULL)
                    .put("popularidade", midia.popularidade ?: JSONObject.NULL)
                    .put("sinopse", midia.sinopse)
                    .put("serieId", midia.serieId ?: JSONObject.NULL)
                    .put("serieNome", midia.serieNome ?: JSONObject.NULL)
                    .put("episodioNome", midia.episodioNome ?: JSONObject.NULL)
                    .put("temporadaNumero", midia.temporadaNumero ?: JSONObject.NULL)
                    .put("episodioNumero", midia.episodioNumero ?: JSONObject.NULL)
            )
        }
    }

    private fun JSONArray.toCanais(): List<Canal> = buildList {
        for (indice in 0 until length()) {
            val item = optJSONObject(indice) ?: continue
            add(
                Canal(
                    id = item.optString("id"),
                    nome = item.optString("nome"),
                    logoUrl = item.optString("logoUrl"),
                    streamUrl = item.optString("streamUrl"),
                    categoria = item.optString("categoria")
                )
            )
        }
    }

    private fun JSONArray.toMidias(): List<Midia> = buildList {
        for (indice in 0 until length()) {
            val item = optJSONObject(indice) ?: continue
            val tipo = runCatching { TipoMidia.valueOf(item.optString("tipo")) }.getOrDefault(TipoMidia.FILME)
            add(
                Midia(
                    id = item.optString("id"),
                    titulo = item.optString("titulo"),
                    imagemUrl = item.optString("imagemUrl"),
                    tipo = tipo,
                    streamUrl = item.optString("streamUrl"),
                    progresso = item.optNullableFloat("progresso"),
                    categoria = item.optString("categoria"),
                    nota = item.optNullableDouble("nota"),
                    popularidade = item.optNullableLong("popularidade"),
                    sinopse = item.optString("sinopse"),
                    serieId = item.optNullableString("serieId"),
                    serieNome = item.optNullableString("serieNome"),
                    episodioNome = item.optNullableString("episodioNome"),
                    temporadaNumero = item.optNullableInt("temporadaNumero"),
                    episodioNumero = item.optNullableInt("episodioNumero")
                )
            )
        }
    }

    private fun JSONObject.optNullableString(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
    private fun JSONObject.optNullableInt(key: String): Int? = if (isNull(key)) null else optInt(key)
    private fun JSONObject.optNullableLong(key: String): Long? = if (isNull(key)) null else optLong(key)
    private fun JSONObject.optNullableDouble(key: String): Double? = if (isNull(key)) null else optDouble(key)
    private fun JSONObject.optNullableFloat(key: String): Float? = if (isNull(key)) null else optDouble(key).toFloat()
}
