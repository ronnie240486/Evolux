package com.evolux.tv.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Cliente para as rotas do backend do Rencia App descritas no documento
 * "Integração Universal de APKs" (base: https://renciaapp.manus.space).
 * Cobre heartbeat, avisos/sincronização de lista, falha de reprodução,
 * confirmação de avisos, comandos remotos e checagem de atualização.
 */
class RenciaApiClient(
    private val appId: String = "evolux",
    private val base: String = "https://renciaapp.manus.space"
) {
    // ---------- 2.2 Atualização do aplicativo ----------

    data class InfoAtualizacao(
        val version: String?,
        val url: String?,
        val apkLink: String?,
        val forceUpdate: Boolean,
        val updateAvailable: Boolean,
        val releaseNotes: String?
    )

    suspend fun checarAtualizacao(mac: String): InfoAtualizacao? = withContext(Dispatchers.IO) {
        val json = getJson("/api/v5/apps/$appId/update?mac=${enc(mac)}") ?: return@withContext null
        runCatching {
            InfoAtualizacao(
                version = json.optNullableString("version"),
                url = json.optNullableString("url"),
                apkLink = json.optNullableString("apk_link"),
                forceUpdate = json.optBoolean("force_update", false),
                updateAvailable = json.optBoolean("update_available", false),
                releaseNotes = json.optNullableString("release_notes")
            )
        }.getOrNull()
    }

    // ---------- 2.3 Heartbeat ----------

    data class RespostaHeartbeat(
        val success: Boolean,
        val contentUpdated: Boolean,
        val command: String?
    )

    suspend fun heartbeat(mac: String, conteudoAtual: String?): RespostaHeartbeat? = withContext(Dispatchers.IO) {
        val rota = buildString {
            append("/api/v5/heartbeat?mac=${enc(mac)}")
            if (!conteudoAtual.isNullOrBlank()) {
                append("&current_content=${enc(conteudoAtual)}")
            }
        }
        val json = getJson(rota) ?: return@withContext null
        runCatching {
            RespostaHeartbeat(
                success = json.optBoolean("success", false),
                contentUpdated = json.optBoolean("contentUpdated", false),
                command = json.optNullableString("command")
            )
        }.getOrNull()
    }

    // ---------- 2.4 Avisos, vencimento e sincronização de lista ----------

    data class Aviso(
        val id: String,
        val status: String?,
        val severity: String?,
        val title: String?,
        val message: String?,
        val createdAt: String?,
        val acknowledged: Boolean
    )

    data class Vencimento(
        val diasRestantes: Int?,
        val estado: String?,
        val modalKey: String?,
        val modalTitle: String?,
        val modalMessage: String?,
        val showModal: Boolean
    )

    data class RespostaListNotifications(
        val notifications: List<Aviso>,
        val expiration: Vencimento?,
        val failoverActive: Boolean,
        val failoverState: String?,
        val activeListName: String?,
        val activeListNumber: Int?,
        val playlistSyncRequired: Boolean,
        val playlistSyncMode: String?,
        val playlistSyncMessage: String?,
        val failoverTransitionId: String?
    )

    suspend fun listNotifications(mac: String): RespostaListNotifications? = withContext(Dispatchers.IO) {
        val json = getJson("/api/v5/list-notifications?mac=${enc(mac)}") ?: return@withContext null
        runCatching {
            val avisos = mutableListOf<Aviso>()
            json.optJSONArray("notifications")?.let { array ->
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    avisos.add(
                        Aviso(
                            id = item.optNullableString("id") ?: continue,
                            status = item.optNullableString("status"),
                            severity = item.optNullableString("severity"),
                            title = item.optNullableString("title"),
                            message = item.optNullableString("message"),
                            createdAt = item.optNullableString("created_at"),
                            acknowledged = item.optBoolean("acknowledged", false)
                        )
                    )
                }
            }
            val expiracaoJson = json.optJSONObject("expiration")
            val expiracao = expiracaoJson?.let {
                Vencimento(
                    diasRestantes = it.optNullableInt("dias_restantes") ?: it.optNullableInt("days_remaining"),
                    estado = it.optNullableString("estado") ?: it.optNullableString("state"),
                    modalKey = it.optNullableString("expiration_modal_key"),
                    modalTitle = it.optNullableString("expiration_modal_title"),
                    modalMessage = it.optNullableString("expiration_modal_message"),
                    showModal = it.optBoolean("expiration_show_modal", false)
                )
            } ?: run {
                // Alguns retornos trazem os campos de expiração direto na raiz.
                if (json.optBoolean("expiration_show_modal", false)) {
                    Vencimento(
                        diasRestantes = json.optNullableInt("dias_restantes"),
                        estado = json.optNullableString("status"),
                        modalKey = json.optNullableString("expiration_modal_key"),
                        modalTitle = json.optNullableString("expiration_modal_title"),
                        modalMessage = json.optNullableString("expiration_modal_message"),
                        showModal = true
                    )
                } else null
            }

            RespostaListNotifications(
                notifications = avisos,
                expiration = expiracao,
                failoverActive = json.optBoolean("failover_active", false),
                failoverState = json.optNullableString("failover_state"),
                activeListName = json.optNullableString("active_list_name"),
                activeListNumber = json.optNullableInt("active_list_number"),
                playlistSyncRequired = json.optBoolean("playlist_sync_required", false),
                playlistSyncMode = json.optNullableString("playlist_sync_mode"),
                playlistSyncMessage = json.optNullableString("playlist_sync_message"),
                failoverTransitionId = json.optNullableString("failover_transition_id")
            )
        }.getOrNull()
    }

    // ---------- 5.1 Confirmação de aviso de lista ----------

    suspend fun ackNotification(mac: String, alertId: String): Boolean = withContext(Dispatchers.IO) {
        val corpo = JSONObject().apply {
            put("mac", mac)
            put("alert_id", alertId)
        }
        val resposta = postJson("/api/v5/list-notifications/ack", corpo) ?: return@withContext false
        resposta.optBoolean("success", false)
    }

    // ---------- 2.5 Falha de reprodução e troca automática de lista ----------

    data class RespostaFalhaReproducao(
        val switchApplied: Boolean,
        val message: String?,
        val failoverActive: Boolean,
        val activeListNumber: Int?,
        val playlistSyncRequired: Boolean,
        val failoverTransitionId: String?
    )

    suspend fun reportarFalhaReproducao(mac: String, listaAtivaNumero: Int): RespostaFalhaReproducao? =
        withContext(Dispatchers.IO) {
            val corpo = JSONObject().apply {
                put("mac", mac)
                put("active_list_number", listaAtivaNumero)
            }
            val json = postJson("/api/v5/playback-failure", corpo) ?: return@withContext null
            runCatching {
                RespostaFalhaReproducao(
                    switchApplied = json.optBoolean("switch_applied", false),
                    message = json.optNullableString("message"),
                    failoverActive = json.optBoolean("failover_active", false),
                    activeListNumber = json.optNullableInt("active_list_number"),
                    playlistSyncRequired = json.optBoolean("playlist_sync_required", false),
                    failoverTransitionId = json.optNullableString("failover_transition_id")
                )
            }.getOrNull()
        }

    // ---------- 5.2 Comandos remotos ----------

    data class ComandoRemoto(val id: String, val command: String, val payload: JSONObject?)

    suspend fun buscarComandosRemotos(mac: String): List<ComandoRemoto> = withContext(Dispatchers.IO) {
        val json = getJson("/api/v5/remote-commands?mac=${enc(mac)}") ?: return@withContext emptyList()
        val resultado = mutableListOf<ComandoRemoto>()
        val array = json.optJSONArray("commands") ?: json.optJSONArray("remote_commands")
        if (array != null) {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val id = item.optNullableString("command_id") ?: item.optNullableString("id") ?: continue
                val comando = item.optNullableString("command") ?: continue
                resultado.add(ComandoRemoto(id, comando, item.optJSONObject("payload")))
            }
        }
        resultado
    }

    /** status deve ser "executed" ou "failed", conforme o contrato do backend. */
    suspend fun confirmarComandoRemoto(
        mac: String,
        commandId: String,
        status: String,
        resultMessage: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val corpo = JSONObject().apply {
            put("mac", mac)
            put("command_id", commandId)
            put("status", status)
            if (!resultMessage.isNullOrBlank()) put("result_message", resultMessage)
        }
        val resposta = postJson("/api/v5/remote-commands/ack", corpo) ?: return@withContext false
        resposta.optBoolean("success", false)
    }

    // ---------- Infra HTTP comum ----------

    private fun enc(valor: String) = URLEncoder.encode(valor, StandardCharsets.UTF_8.name())

    private fun getJson(caminho: String): JSONObject? {
        val conexao = (URL("$base$caminho").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Evolux/1.0 (Android)")
        }
        return try {
            val codigo = conexao.responseCode
            val corpo = (if (codigo in 200..299) conexao.inputStream else conexao.errorStream)
                ?.bufferedReader(StandardCharsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
            if (codigo !in 200..299 || corpo.isBlank()) return null
            runCatching { JSONObject(corpo) }.getOrNull()
        } catch (_: SocketTimeoutException) {
            null
        } catch (_: IOException) {
            null
        } catch (_: Exception) {
            null
        } finally {
            conexao.disconnect()
        }
    }

    private fun postJson(caminho: String, corpo: JSONObject): JSONObject? {
        val conexao = (URL("$base$caminho").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            useCaches = false
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Evolux/1.0 (Android)")
        }
        return try {
            conexao.outputStream.use { it.write(corpo.toString().toByteArray(StandardCharsets.UTF_8)) }
            val codigo = conexao.responseCode
            val resposta = (if (codigo in 200..299) conexao.inputStream else conexao.errorStream)
                ?.bufferedReader(StandardCharsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
            if (codigo !in 200..299 || resposta.isBlank()) return null
            runCatching { JSONObject(resposta) }.getOrNull()
        } catch (_: SocketTimeoutException) {
            null
        } catch (_: IOException) {
            null
        } catch (_: Exception) {
            null
        } finally {
            conexao.disconnect()
        }
    }
}

private fun JSONObject.optNullableString(chave: String): String? {
    val bruto = opt(chave)
    if (bruto == null || bruto == JSONObject.NULL) return null
    return bruto.toString().trim().ifBlank { null }
}

private fun JSONObject.optNullableInt(chave: String): Int? {
    if (!has(chave) || isNull(chave)) return null
    return runCatching { getInt(chave) }.getOrNull()
}
