package com.evolux.tv.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Busca os jogos de futebol do dia na TheSportsDB (https://www.thesportsdb.com),
 * que tem uma chave de teste pública ("3") liberada pra uso básico sem cadastro.
 * Retorna só informação (placar/horário/times) — não inclui link de transmissão,
 * já que isso depende da lista de canais de cada usuário.
 */
class EsporteRepository {

    suspend fun buscarJogosDoDia(): List<Jogo> = withContext(Dispatchers.IO) {
        val data = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(java.util.Date())
        val url = URL("https://www.thesportsdb.com/api/v1/json/3/eventsday.php?d=$data&s=Soccer")
        val conexao = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            val codigo = conexao.responseCode
            if (codigo !in 200..299) return@withContext emptyList()
            val corpo = conexao.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val raiz = runCatching { JSONObject(corpo) }.getOrNull() ?: return@withContext emptyList()
            val eventos = raiz.optJSONArray("events") ?: return@withContext emptyList()
            buildList {
                for (indice in 0 until eventos.length()) {
                    val evento = eventos.optJSONObject(indice) ?: continue
                    val casa = evento.optString("strHomeTeam").ifBlank { continue }
                    val visitante = evento.optString("strAwayTeam").ifBlank { continue }
                    val horarioBruto = evento.optString("strTime")
                    add(
                        Jogo(
                            id = evento.optString("idEvent").ifBlank { "jogo_$indice" },
                            timeCasaSigla = siglaTime(casa),
                            timeCasaLogoUrl = "",
                            timeVisitanteSigla = siglaTime(visitante),
                            timeVisitanteLogoUrl = "",
                            horario = formatarHorario(horarioBruto),
                            campeonato = evento.optString("strLeague").ifBlank { "Futebol" },
                            streamUrl = ""
                        )
                    )
                }
            }
        } catch (_: SocketTimeoutException) {
            emptyList()
        } catch (_: IOException) {
            emptyList()
        } catch (_: Exception) {
            emptyList()
        } finally {
            conexao.disconnect()
        }
    }

    /** Usa até 3 letras do nome do time quando não há sigla oficial disponível na API. */
    private fun siglaTime(nomeCompleto: String): String {
        val palavras = nomeCompleto.trim().split(" ").filter { it.isNotBlank() }
        return if (palavras.size == 1) {
            nomeCompleto.take(3).uppercase(Locale.ROOT)
        } else {
            nomeCompleto
        }
    }

    private fun formatarHorario(horaBruta: String): String {
        if (horaBruta.isBlank()) return "Horário a definir"
        return runCatching {
            val entrada = SimpleDateFormat("HH:mm:ss", Locale.ROOT).parse(horaBruta)
            SimpleDateFormat("HH:mm", Locale.ROOT).format(entrada!!)
        }.getOrDefault(horaBruta)
    }
}
