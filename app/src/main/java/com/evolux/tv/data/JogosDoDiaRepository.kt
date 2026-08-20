package com.evolux.tv.data

import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class JogosDoDiaRepository {
    suspend fun carregarProximosJogos(limite: Int = 5): List<Jogo> = withContext(Dispatchers.IO) {
        runCatching {
            val hoje = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val url = URL("https://www.thesportsdb.com/api/v1/json/123/eventsday.php?d=$hoje&s=Soccer")
            val conexao = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 10_000
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Evolux/1.0 (Android TV)")
                setRequestProperty("Connection", "close")
            }
            try {
                if (conexao.responseCode !in 200..299) return@runCatching emptyList()
                val corpo = conexao.inputStream.bufferedReader().use { it.readText() }
                val eventos = JSONObject(corpo).optJSONArray("events") ?: return@runCatching emptyList()
                val agora = System.currentTimeMillis()
                buildList {
                    for (indice in 0 until eventos.length()) {
                        val evento = eventos.optJSONObject(indice) ?: continue
                        val status = evento.optString("strStatus").uppercase(Locale.ROOT)
                        if (status in STATUS_ENCERRADO) continue
                        val timestamp = evento.optString("strTimestamp")
                            .ifBlank { "${evento.optString("dateEvent")}T${evento.optString("strTime")}" }
                        val instante = parseUtc(timestamp) ?: continue
                        if (instante < agora) continue
                        val casa = evento.optString("strHomeTeam").ifBlank { "Casa" }
                        val visitante = evento.optString("strAwayTeam").ifBlank { "Fora" }
                        add(
                            Jogo(
                                id = evento.optString("idEvent").ifBlank { "event_$indice" },
                                timeCasaSigla = abreviarTime(casa),
                                timeCasaLogoUrl = evento.optString("strHomeTeamBadge"),
                                timeVisitanteSigla = abreviarTime(visitante),
                                timeVisitanteLogoUrl = evento.optString("strAwayTeamBadge"),
                                horario = evento.optString("strTimeLocal")
                                    .takeIf { it.isNotBlank() }
                                    ?.take(5)
                                    ?: formatarHorario(instante),
                                campeonato = evento.optString("strLeague").ifBlank { "Futebol" },
                                streamUrl = "",
                                status = status,
                                dataHoraUtc = timestamp
                            )
                        )
                    }
                }.sortedBy { parseUtc(it.dataHoraUtc) ?: Long.MAX_VALUE }.take(limite)
            } finally {
                conexao.disconnect()
            }
        }.getOrDefault(emptyList())
    }

    private fun parseUtc(valor: String): Long? {
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(valor)?.time
        }.getOrNull()
    }

    private fun formatarHorario(instante: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(instante))
    }

    private fun abreviarTime(nome: String): String {
        val partes = nome.uppercase(Locale.ROOT)
            .replace(Regex("[^A-ZÀ-Ý0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in PALAVRAS_IGNORADAS }
        if (partes.isEmpty()) return "---"
        if (partes.size == 1) return partes.first().take(3)
        val iniciais = partes.take(3).joinToString(separator = "") { it.first().toString() }
        return iniciais.take(4)
    }

    companion object {
        private val STATUS_ENCERRADO = setOf("FT", "AET", "PEN", "CANC", "POST", "ABD")
        private val PALAVRAS_IGNORADAS = setOf("FC", "SC", "CF", "AC", "EC", "DE", "DO", "DA", "THE")
    }
}
