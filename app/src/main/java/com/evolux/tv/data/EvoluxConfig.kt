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
    val iconUrl: String? = null
) {
    val primeiraPlaylistValida: String?
        get() = playlistUrls.firstOrNull { it.startsWith("https://") || it.startsWith("http://") }
}

object EvoluxConfigParser {
    fun parse(json: String): EvoluxConfig? {
        return runCatching {
            val objeto = JSONObject(json)
            val urls = objeto.optJSONArray("playlist_urls").toStringList()

            EvoluxConfig(
                registered = objeto.optBoolean("registered", false),
                allowed = objeto.optBoolean("allowed", false),
                mac = objeto.optNullableString("mac").orEmpty(),
                appId = objeto.optNullableString("app_id") ?: "evolux",
                appName = objeto.optNullableString("app_name") ?: "Evolux",
                playlistUrls = urls,
                logoUrl = objeto.optNullableString("logo_url")
                    ?: objeto.optNullableString("logo"),
                bannerUrl = objeto.optNullableString("banner_url")
                    ?: objeto.optNullableString("banner"),
                backgroundUrl = objeto.optNullableString("background_url")
                    ?: objeto.optNullableString("background"),
                iconUrl = objeto.optNullableString("icon_url")
                    ?: objeto.optNullableString("icon")
            )
        }.getOrNull()
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (indice in 0 until length()) {
                val url = optString(indice, "").trim()
                if (url.isNotEmpty()) add(url)
            }
        }
    }

    private fun JSONObject.optNullableString(chave: String): String? {
        val bruto = opt(chave)
        if (bruto == null || bruto == JSONObject.NULL) return null
        return bruto.toString().trim().ifBlank { null }
    }
}
