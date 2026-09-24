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

class EvoluxRepository(
    // BUG CRÍTICO corrigido: o painel migrou do Manus pro Railway -- o
    // domínio antigo (renciaapp.manus.space) não fala mais a API de
    // verdade pra apps novos, devolvendo resposta inválida (às vezes nem
    // JSON) em vez de um erro claro (mesma migração já feita no
    // Rencia/Supreme e no Fusion -- ver RenciaRepository.kt deles).
    // Railway é o domínio PRIMÁRIO agora; Manus fica só como reserva,
    // pra MAC que por algum motivo ainda só esteja cadastrado no painel
    // antigo.
    private val baseUrl: String = "https://renciaapp-production.up.railway.app/api/v5/apps/evolux/config",
    private val baseUrlFallback: String = "https://renciaapp.manus.space/api/v5/apps/evolux/config"
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

            // BUG DE VELOCIDADE corrigido: antes, TODA ativação por MAC fazia
            // uma checagem de rede extra aqui (playlistValida, uma requisição
            // A MAIS pra playlist só pra ler o prefixo e conferir content-type)
            // ANTES sequer de tentar baixar/usar a lista de verdade -- ou seja,
            // duas idas na rede sequenciais (uma só de "sondagem", outra o
          // download de verdade) pra todo login, mesmo quando já existe cache
            // válido local. É exatamente esse tipo de checagem redundante que
            // faz o Evolux abrir mais devagar que o Maximus/Ouro Pro, que vão
            // direto pro cache/download real sem essa sondagem prévia. Se a
            // playlist realmente estiver com problema, isso já aparece do
            // mesmo jeito (com a mesma mensagem) na etapa de carregarCatalogo
            // logo em seguida -- não perde nenhum tratamento de erro, só para
            // de pagar a rodada de rede extra no caminho feliz (que é a
            // maioria dos casos).
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

    /** Tenta o Railway (painel atual) primeiro; só cai pro Manus (painel
     * antigo) se o Railway não responder nada aproveitável -- mesma ordem
     * usada no Rencia/Supreme e no Fusion. Se os dois falharem, propaga o
     * erro do PRIMÁRIO (Railway), que é o diagnóstico mais relevante hoje. */
    private fun requisitarConfiguracao(mac: String): String =
        runCatching { requisitarConfiguracaoDe(baseUrl, mac) }
            .getOrElse { erroPrimario ->
                runCatching { requisitarConfiguracaoDe(baseUrlFallback, mac) }
                    .getOrElse { throw erroPrimario }
            }

    private fun requisitarConfiguracaoDe(base: String, mac: String): String {
        val url = URL("$base?mac=${URLEncoder.encode(mac, StandardCharsets.UTF_8.name())}")
        val conexao = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Evolux/1.0 (Android)")
            setRequestProperty("Connection", "close")
            setRequestProperty("Cache-Control", "no-cache")
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
