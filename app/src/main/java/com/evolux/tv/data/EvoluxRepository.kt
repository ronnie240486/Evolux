package com.evolux.tv.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface ResultadoConfiguracao {
    data class Sucesso(val configuracao: EvoluxConfig) : ResultadoConfiguracao
    data class Erro(val mensagem: String, val detalhe: String? = null) : ResultadoConfiguracao
}

private class RespostaConfiguracaoException(val detalheSeguro: String) : IOException()

private data class ResultadoPlaylist(
    val valida: Boolean,
    val detalhe: String
)

class EvoluxRepository(
    private val baseUrl: String = "https://renciaapp.manus.space/api/v5/apps/evolux/config"
) {
    suspend fun buscarConfiguracao(mac: String): ResultadoConfiguracao = withContext(Dispatchers.IO) {
        val macNormalizado = MacAddressUtils.normalizar(mac)
        if (macNormalizado == null) {
            return@withContext ResultadoConfiguracao.Erro(
                mensagem = "MAC inválido",
                detalhe = "Use o formato AA:BB:CC:DD:EE:FF."
            )
        }

        try {
            val json = requisitarConfiguracao(macNormalizado)
            val configuracao = EvoluxConfigParser.parse(json)
                ?: return@withContext ResultadoConfiguracao.Erro(
                    mensagem = "Resposta de configuração inválida",
                    detalhe = "O endpoint respondeu, mas o corpo não é um JSON válido."
                )

            if (!configuracao.registered || !configuracao.allowed) {
                return@withContext ResultadoConfiguracao.Erro(
                    mensagem = "Aparelho não autorizado para usar o Evolux",
                    detalhe = "registered=${configuracao.registered}; allowed=${configuracao.allowed}. Cadastre este MAC no painel."
                )
            }

            val playlistUrl = configuracao.primeiraPlaylistValida
                ?: return@withContext ResultadoConfiguracao.Erro(
                    mensagem = "Lista indisponível ou credenciais inválidas",
                    detalhe = "Nenhuma URL HTTP/HTTPS foi encontrada em playlist_urls."
                )

            val resultadoPlaylist = playlistValida(playlistUrl)
            if (!resultadoPlaylist.valida) {
                return@withContext ResultadoConfiguracao.Erro(
                    mensagem = "Lista indisponível ou credenciais inválidas",
                    detalhe = resultadoPlaylist.detalhe
                )
            }

            ResultadoConfiguracao.Sucesso(configuracao.copy(mac = macNormalizado))
        } catch (erro: RespostaConfiguracaoException) {
            ResultadoConfiguracao.Erro(
                mensagem = "Falha na resposta do servidor",
                detalhe = erro.detalheSeguro
            )
        } catch (_: SocketTimeoutException) {
            ResultadoConfiguracao.Erro(
                mensagem = "Tempo limite excedido",
                detalhe = "O servidor demorou para responder. Tente novamente."
            )
        } catch (_: IOException) {
            ResultadoConfiguracao.Erro(
                mensagem = "Não foi possível conectar ao servidor",
                detalhe = "Verifique a internet do aparelho e tente novamente."
            )
        } catch (erro: Exception) {
            ResultadoConfiguracao.Erro(
                mensagem = "Não foi possível validar o aparelho",
                detalhe = "Falha interna: ${erro::class.simpleName ?: "erro desconhecido"}."
            )
        }
    }

    private fun playlistValida(urlPlaylist: String): ResultadoPlaylist {
        val conexao = try {
            (URL(urlPlaylist).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                useCaches = false
                setRequestProperty(
                    "Accept",
                    "audio/x-mpegurl, application/vnd.apple.mpegurl, application/json, text/plain"
                )
            }
        } catch (_: Exception) {
            return ResultadoPlaylist(false, "Não foi possível abrir a URL da playlist.")
        }

        return try {
            val codigo = conexao.responseCode
            val contentType = conexao.contentType.orEmpty().lowercase(Locale.ROOT)
            val inicio = (if (codigo in 200..299) conexao.inputStream else conexao.errorStream)
                ?.bufferedReader()
                ?.use { leitor ->
                    val buffer = CharArray(4_096)
                    val quantidade = leitor.read(buffer)
                    if (quantidade > 0) String(buffer, 0, quantidade) else ""
                }
                .orEmpty()
                .trimStart()

            when {
                codigo !in 200..299 -> ResultadoPlaylist(false, "A playlist respondeu HTTP $codigo.")
                "text/html" in contentType -> ResultadoPlaylist(false, "A playlist respondeu HTML (Content-Type: text/html).")
                inicio.startsWith("<") -> ResultadoPlaylist(false, "A playlist respondeu HTML no corpo da resposta.")
                inicio.isBlank() -> ResultadoPlaylist(false, "A playlist respondeu vazia.")
                else -> ResultadoPlaylist(true, "Playlist aceita.")
            }
        } catch (_: SocketTimeoutException) {
            ResultadoPlaylist(false, "Tempo limite ao consultar a playlist.")
        } catch (_: IOException) {
            ResultadoPlaylist(false, "Falha de rede ao consultar a playlist.")
        } catch (_: Exception) {
            ResultadoPlaylist(false, "Falha inesperada ao consultar a playlist.")
        } finally {
            conexao.disconnect()
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
            val contentType = conexao.contentType.orEmpty().lowercase(Locale.ROOT)

            when {
                codigo !in 200..299 -> throw RespostaConfiguracaoException(
                    "O endpoint de configuração respondeu HTTP $codigo."
                )
                "text/html" in contentType || corpo.trimStart().startsWith("<") ->
                    throw RespostaConfiguracaoException("O endpoint respondeu HTML em vez de JSON.")
                corpo.isBlank() -> throw RespostaConfiguracaoException("O endpoint respondeu vazio.")
                else -> corpo
            }
        } finally {
            conexao.disconnect()
        }
    }
}
