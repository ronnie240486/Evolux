package com.evolux.tv.data

import org.json.JSONArray
import org.json.JSONObject

/** Configuração remota retornada pelo endpoint por MAC. */
data class EvoluxConfig(
    val registered: Boolean,
    val allowed: Boolean,
    val mac: String,
    val appId: String,
    val appName: String,
    val playlistUrls: List<String>,
    val logoUrl: String? = null,
    val bannerUrl: String? = null,
    val backgroundUrl: String? = null,
    val iconUrl: String? = null,
    val status: String? = null,
    val messageTitle: String? = null,
    val messageText: String? = null,
    val messageImageUrl: String? = null,
    val blockTitle: String? = null,
    val blockMessage: String? = null,
    val renewButtonText: String? = null,
    val renewButtonUrl: String? = null,
    val iconLiveTvUrl: String? = null,
    val iconMoviesUrl: String? = null,
    val iconSeriesUrl: String? = null,
    val playlistSyncRequired: Boolean = false
) {
    val primeiraPlaylistValida: String?
        get() = playlistUrls.firstOrNull { it.startsWith("https://") || it.startsWith("http://") }
}

object EvoluxConfigParser {
    fun parse(json: String): EvoluxConfig? {
        return runCatching {
            val objeto = JSONObject(json)
            val icones = objeto.optJSONObject("icons")
            EvoluxConfig(
                registered = objeto.optBoolean("registered", false),
                allowed = objeto.optBoolean("allowed", false),
                mac = objeto.optNullableString("mac").orEmpty(),
                appId = objeto.optNullableString("app_id") ?: "evolux",
                appName = objeto.optNullableString("app_name") ?: "Evolux",
                playlistUrls = objeto.extrairPlaylistUrls(),
                logoUrl = objeto.optNullableString("logo_url")
                    ?: objeto.optNullableString("logo"),
                bannerUrl = objeto.optNullableString("banner_url")
                    ?: objeto.optNullableString("banner"),
                backgroundUrl = objeto.optNullableString("background_url")
                    ?: objeto.optNullableString("background"),
                iconUrl = objeto.optNullableString("icon_url")
                    ?: objeto.optNullableString("icon"),
                status = objeto.optNullableString("status"),
                messageTitle = objeto.optNullableString("message_title"),
                messageText = objeto.optNullableString("message_text"),
                messageImageUrl = objeto.optNullableString("message_image_url"),
                blockTitle = objeto.optNullableString("block_title"),
                blockMessage = objeto.optNullableString("block_message"),
                renewButtonText = objeto.optNullableString("renew_button_text"),
                renewButtonUrl = objeto.optNullableString("renew_button_url"),
                iconLiveTvUrl = icones?.optNullableString("live_tv"),
                iconMoviesUrl = icones?.optNullableString("movies"),
                iconSeriesUrl = icones?.optNullableString("series"),
                playlistSyncRequired = objeto.optBoolean("playlist_sync_required", false)
            )
        }.getOrNull()
    }

    private fun JSONObject.extrairPlaylistUrls(): List<String> {
        val resultado = linkedSetOf<String>()
        val chaves = listOf(
            "playlist_urls",
            "playlist_url",
            "playlist",
            "playlists",
            "lists",
            "m3u_url",
            "url"
        )
        chaves.forEach { chave -> adicionarValor(opt(chave), resultado) }
        return resultado.toList()
    }

    private fun adicionarValor(valor: Any?, resultado: MutableSet<String>) {
        when (valor) {
            is String -> valor.trim().takeIf { it.startsWith("http://") || it.startsWith("https://") }?.let(resultado::add)
            is JSONArray -> {
                for (indice in 0 until valor.length()) {
                    adicionarValor(valor.opt(indice), resultado)
                }
            }
            is JSONObject -> {
                listOf("playlist_url", "url", "m3u_url", "stream_url").forEach { chave ->
                    adicionarValor(valor.opt(chave), resultado)
                }
            }
        }
    }

    private fun JSONObject.optNullableString(chave: String): String? {
        val bruto = opt(chave)
        if (bruto == null || bruto == JSONObject.NULL) return null
        return bruto.toString().trim().ifBlank { null }
    }
}
