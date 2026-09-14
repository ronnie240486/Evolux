package com.evolux.tv.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Busca os jogos do dia na API pública gratuita e sem chave do ESPN
 * (site.api.espn.com), cobrindo competições brasileiras e as principais
 * internacionais. Não inclui link de transmissão — isso depende da lista
 * de canais de cada usuário, a API só dá times/horário/campeonato.
 */
class EsporteRepository {

    private data class Liga(val slug: String, val nome: String)

    private val ligas = listOf(
        Liga("bra.1", "Brasileirão Série A"),
        Liga("bra.2", "Brasileirão Série B"),
        Liga("bra.copa_do_brazil", "Copa do Brasil"),
        Liga("conmebol.libertadores", "Libertadores"),
        Liga("conmebol.sudamericana", "Sul-Americana"),
        Liga("conmebol.america", "Copa América"),
        Liga("uefa.champions", "Champions League"),
        Liga("uefa.europa", "Europa League"),
        Liga("fifa.world", "Copa do Mundo"),
        Liga("eng.1", "Premier League"),
        Liga("esp.1", "La Liga"),
        Liga("ita.1", "Serie A (Itália)"),
        Liga("ger.1", "Bundesliga"),
        Liga("fra.1", "Ligue 1"),
        Liga("arg.1", "Liga Argentina")
    )

    suspend fun buscarJogosDoDia(): List<Jogo> = withContext(Dispatchers.IO) {
        ligas.flatMap { liga -> buscarLiga(liga) }
    }

    private fun buscarLiga(liga: Liga): List<Jogo> {
        val url = URL("https://site.api.espn.com/apis/site/v2/sports/soccer/${liga.slug}/scoreboard")
        val conexao = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            val codigo = conexao.responseCode
            if (codigo !in 200..299) return emptyList<Jogo>()
            val corpo = conexao.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val raiz = runCatching { JSONObject(corpo) }.getOrNull() ?: return emptyList<Jogo>()
            val eventos = raiz.optJSONArray("events") ?: return emptyList<Jogo>()
            val resultado = mutableListOf<Jogo>()
            for (indice in 0 until eventos.length()) {
                val evento = eventos.optJSONObject(indice) ?: continue
                val competicoes = evento.optJSONArray("competitions") ?: continue
                val competicao = competicoes.optJSONObject(0) ?: continue
                val competidores = competicao.optJSONArray("competitors") ?: continue
                if (competidores.length() < 2) continue

                var siglaCasa = ""
                var siglaFora = ""
                for (indiceCompetidor in 0 until competidores.length()) {
                    val competidor = competidores.optJSONObject(indiceCompetidor) ?: continue
                    val time = competidor.optJSONObject("team")
                    val nome = time?.optString("shortDisplayName").orEmpty()
                        .ifBlank { time?.optString("displayName").orEmpty() }
                        .ifBlank { time?.optString("abbreviation").orEmpty() }
                    if (nome.isBlank()) continue
                    if (competidor.optString("homeAway") == "home") siglaCasa = nome else siglaFora = nome
                }
                if (siglaCasa.isBlank() || siglaFora.isBlank()) continue

                resultado.add(
                    Jogo(
                        id = evento.optString("id").ifBlank { "${liga.slug}_$indice" },
                        timeCasaSigla = siglaCasa,
                        timeCasaLogoUrl = "",
                        timeVisitanteSigla = siglaFora,
                        timeVisitanteLogoUrl = "",
                        horario = formatarHorario(evento.optString("date")),
                        campeonato = liga.nome,
                        streamUrl = ""
                    )
                )
            }
            return resultado
        } catch (_: SocketTimeoutException) {
            return emptyList<Jogo>()
        } catch (_: IOException) {
            return emptyList<Jogo>()
        } catch (_: Exception) {
            return emptyList<Jogo>()
        } finally {
            conexao.disconnect()
        }
    }

    /** A API devolve o horário em UTC (ex: 2026-09-14T23:00Z); converte pro fuso do aparelho. */
    private fun formatarHorario(dataIso: String): String {
        if (dataIso.isBlank()) return "Horário a definir"
        return runCatching {
            val formatoEntrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.ROOT).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val data = formatoEntrada.parse(dataIso) ?: return@runCatching "Horário a definir"
            SimpleDateFormat("dd/MM HH:mm", Locale.ROOT).format(data)
        }.getOrDefault("Horário a definir")
    }
}
