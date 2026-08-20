package com.evolux.tv.data

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed interface ResultadoConfiguracao {
    data class Sucesso(val configuracao: EvoluxConfig) : ResultadoConfiguracao
    data class Erro(val mensagem: String) : ResultadoConfiguracao
}

class EvoluxRepository(
    private val baseUrl: String = "https://renciaapp.manus.space/api/v5/apps/evolux/config"
) {
    suspend fun buscarConfiguracao(mac: String): ResultadoConfiguracao = withContext(Dispatchers.IO) {
        val macNormalizado = MacAddressUtils.normalizar(mac)
        if (macNormalizado == null) {
            ResultadoConfiguracao.Erro("Informe um MAC válido no formato AA:BB:CC:DD:EE:FF")
        } else {
            try {
                val json = requisitarConfiguracao(macNormalizado)
                val configuracao = EvoluxConfigParser.parse(json)
                if (configuracao == null) {
                    ResultadoConfiguracao.Erro("Resposta de configuração inválida")
                } else {
                    val playlistUrl = configuracao.primeiraPlaylistValida
                    when {
                        !configuracao.registered || !configuracao.allowed -> {
                            ResultadoConfiguracao.Erro("Aparelho não autorizado para usar o Evolux")
                        }
                        playlistUrl == null || !playlistValida(playlistUrl) -> {
                            ResultadoConfiguracao.Erro("Lista indisponível ou credenciais inválidas")
                        }
                        else -> ResultadoConfiguracao.Sucesso(
                            configuracao.copy(mac = macNormalizado)
                        )
                    }
                }
            } catch (erro: IOException) {
                ResultadoConfiguracao.Erro("Não foi possível conectar ao servidor")
            } catch (erro: Exception) {
                ResultadoConfiguracao.Erro("Não foi possível validar o aparelho")
            }
        }
    }

    private fun playlistValida(urlPlaylist: String): Boolean {
        return try {
            val conexao = (URL(urlPlaylist).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                useCaches = false
                setRequestProperty(
                    "Accept",
                    "audio/x-mpegurl, application/vnd.apple.mpegurl, application/json, text/plain"
                )
            }

            try {
                val codigo = conexao.responseCode
                val contentType = conexao.contentType.orEmpty().lowercase()
                val inicio = (if (codigo in 200..299) conexao.inputStream else conexao.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText().take(4_096) }
                    .orEmpty()
                    .trimStart()

                codigo in 200..299 && inicio.isNotEmpty() && !inicio.startsWith("<") &&
                    "text/html" !in contentType
            } finally {
                conexao.disconnect()
            }
        } catch (erro: IOException) {
            false
        } catch (erro: Exception) {
            false
        }
    }

    private fun requisitarConfiguracao(mac: String): String {
        val url = URL("$baseUrl?mac=${URLEncoder.encode(mac, StandardCharsets.UTF_8.name())}")
        val conexao = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            useCaches = false
            setRequestProperty("Accept", "application/json")
        }

        return try {
            val codigo = conexao.responseCode
            val corpo = (if (codigo in 200..299) conexao.inputStream else conexao.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            val contentType = conexao.contentType.orEmpty().lowercase()

            if (codigo !in 200..299 || corpo.trimStart().startsWith("<") || "text/html" in contentType) {
                throw IOException("Resposta da configuração indisponível")
            }
            corpo
        } finally {
            conexao.disconnect()
        }
    }
}
