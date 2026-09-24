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
 * de canais de cada usuário, a API só dá times/horário/campeonato/placar.
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
        // Cada liga vem numa chamada separada, então a lista concatenada sai
        // agrupada por liga (todos os jogos do Brasileirão, depois todos da
        // Libertadores, etc.) -- não por data/horário. Ordena pelo instante
        // real do jogo (horarioMillis) pra ficar do mais cedo pro mais
        // tarde, misturando as ligas na ordem certa.
        // Jogos sem horário reconhecido (horarioMillis == 0L) vão pro final,
        // em vez de aparecer misturados no topo como se fossem "os mais cedo".
        ligas.flatMap { liga -> buscarLiga(liga) }
            .sortedBy { if (it.horarioMillis > 0L) it.horarioMillis else Long.MAX_VALUE }
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
                var nomeCasa = ""
                var nomeFora = ""
                var logoCasa = ""
                var logoFora = ""
                var placarCasa: Int? = null
                var placarFora: Int? = null
                for (indiceCompetidor in 0 until competidores.length()) {
                    val competidor = competidores.optJSONObject(indiceCompetidor) ?: continue
                    val time = competidor.optJSONObject("team")
                    val nomeCompleto = time?.optString("displayName").orEmpty()
                    val nomeCurto = time?.optString("shortDisplayName").orEmpty()
                        .ifBlank { nomeCompleto }
                        .ifBlank { time?.optString("abbreviation").orEmpty() }
                    if (nomeCurto.isBlank()) continue
                    val logo = time?.optString("logo").orEmpty()
                    val placar = competidor.optString("score").toIntOrNull()
                    if (competidor.optString("homeAway") == "home") {
                        siglaCasa = nomeCurto; nomeCasa = nomeCompleto; logoCasa = logo; placarCasa = placar
                    } else {
                        siglaFora = nomeCurto; nomeFora = nomeCompleto; logoFora = logo; placarFora = placar
                    }
                }
                if (siglaCasa.isBlank() || siglaFora.isBlank()) continue

                val statusObjeto = competicao.optJSONObject("status")?.optJSONObject("type")
                val estado = statusObjeto?.optString("state").orEmpty()
                val encerrado = statusObjeto?.optBoolean("completed", false) ?: false
                val aoVivo = estado == "in"
                val instanteMillis = parseInstanteMillis(evento.optString("date"))

                resultado.add(
                    Jogo(
                        id = evento.optString("id").ifBlank { "${liga.slug}_$indice" },
                        timeCasaSigla = siglaCasa,
                        timeCasaLogoUrl = logoCasa,
                        timeVisitanteSigla = siglaFora,
                        timeVisitanteLogoUrl = logoFora,
                        horario = formatarHorario(instanteMillis),
                        campeonato = liga.nome,
                        streamUrl = "",
                        placarCasa = if (aoVivo || encerrado) placarCasa else null,
                        placarVisitante = if (aoVivo || encerrado) placarFora else null,
                        aoVivo = aoVivo,
                        encerrado = encerrado,
                        timeCasaNomeCompleto = nomeCasa,
                        timeVisitanteNomeCompleto = nomeFora,
                        horarioMillis = instanteMillis
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

    /** A API devolve o horário em UTC (ex: 2026-09-14T23:00Z); converte pra
     * um instante absoluto (epoch millis), usado tanto pra ordenar/agrupar
     * por data quanto pra formatar o texto exibido. 0L quando não dá pra
     * interpretar a data (jogo sem horário definido). */
    private fun parseInstanteMillis(dataIso: String): Long {
        if (dataIso.isBlank()) return 0L
        return runCatching {
            val formatoEntrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.ROOT).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            formatoEntrada.parse(dataIso)?.time ?: 0L
        }.getOrDefault(0L)
    }

    /** Formata o instante pro fuso do aparelho -- "dd/MM HH:mm". */
    private fun formatarHorario(instanteMillis: Long): String {
        if (instanteMillis <= 0L) return "Horário a definir"
        return runCatching {
            SimpleDateFormat("dd/MM HH:mm", Locale.ROOT).format(java.util.Date(instanteMillis))
        }.getOrDefault("Horário a definir")
    }
}
