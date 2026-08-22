package com.evolux.tv.data

import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.io.SequenceInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class PlaylistCatalog(
    val canais: List<Canal>,
    val filmes: List<Midia>,
    val series: List<Midia>,
    val truncado: Boolean = false
)

class PlaylistRepository {
    private data class DadosSerie(
        val serieNome: String,
        val temporadaNumero: Int,
        val episodioNumero: Int?
    )

    suspend fun carregar(
        urlPlaylist: String,
        aoAtualizarParcial: suspend (PlaylistCatalog, Int) -> Unit = { _, _ -> }
    ): PlaylistCatalog = withContext(Dispatchers.IO) {
        val conexao = (URL(urlPlaylist).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            useCaches = false
            setRequestProperty(
                "Accept",
                "audio/x-mpegurl, application/vnd.apple.mpegurl, application/json, text/plain"
            )
        }

        try {
            val codigo = conexao.responseCode
            val contentType = conexao.contentType.orEmpty().lowercase(Locale.ROOT)
            if (codigo !in 200..299) throw IOException("Playlist respondeu HTTP $codigo")
            if ("text/html" in contentType) throw IOException("Playlist respondeu HTML")

            val entrada = conexao.inputStream ?: throw IOException("Playlist sem conteúdo")
            entrada.use { fluxo ->
                val prefixo = lerPrefixo(fluxo)
                val prefixoTrimmed = prefixo.toString(StandardCharsets.UTF_8).trimStart()
                if (prefixoTrimmed.isBlank()) throw IOException("Playlist vazia")
                if (prefixoTrimmed.startsWith("<")) throw IOException("Playlist respondeu HTML no corpo")

                if (prefixoTrimmed.startsWith("{") || prefixoTrimmed.startsWith("[")) {
                    parseJson(lerJsonLimitado(prefixo, fluxo))
                } else {
                    val fluxoCompleto = SequenceInputStream(ByteArrayInputStream(prefixo), fluxo)
                    fluxoCompleto.bufferedReader(StandardCharsets.UTF_8).use {
                        parseM3u(it, aoAtualizarParcial)
                    }
                }
            }
        } finally {
            conexao.disconnect()
        }
    }

    private fun lerPrefixo(fluxo: InputStream): ByteArray {
        val saida = ByteArrayOutputStream(PREFIX_BYTES)
        val buffer = ByteArray(BUFFER_BYTES)
        while (saida.size() < PREFIX_BYTES) {
            val quantidade = fluxo.read(buffer, 0, minOf(buffer.size, PREFIX_BYTES - saida.size()))
            if (quantidade <= 0) break
            saida.write(buffer, 0, quantidade)
        }
        return saida.toByteArray()
    }

    private fun lerJsonLimitado(prefixo: ByteArray, fluxo: InputStream): String {
        val saida = ByteArrayOutputStream(minOf(prefixo.size, MAX_PLAYLIST_BYTES.toInt()))
        var total = 0L
        if (prefixo.isNotEmpty()) {
            saida.write(prefixo)
            total += prefixo.size
        }
        val buffer = ByteArray(BUFFER_BYTES)
        while (true) {
            val quantidade = fluxo.read(buffer)
            if (quantidade < 0) break
            total += quantidade
            if (total > MAX_PLAYLIST_BYTES) {
                throw IOException("JSON da playlist excede o limite seguro de ${MAX_PLAYLIST_BYTES / (1024 * 1024)} MB")
            }
            saida.write(buffer, 0, quantidade)
        }
        return saida.toString(StandardCharsets.UTF_8.name()).trim()
    }

    private suspend fun parseM3u(
        leitor: Reader,
        aoAtualizarParcial: suspend (PlaylistCatalog, Int) -> Unit
    ): PlaylistCatalog {
        data class Entrada(
            val titulo: String,
            val grupo: String,
            val logo: String,
            val tipoHint: String?,
            val serieNome: String?,
            val temporadaNumero: Int?,
            val episodioNumero: Int?
        )

        val canais = mutableListOf<Canal>()
        val filmes = mutableListOf<Midia>()
        val series = mutableListOf<Midia>()
        var pendente: Entrada? = null
        var totalItens = 0
        var truncado = false
        var proximoLote = PRIMEIRO_LOTE_ITENS
        val leitorBuffer = leitor as? BufferedReader ?: leitor.buffered()

        while (true) {
            val linha = leitorBuffer.readLine()?.trim() ?: break
            when {
                linha.startsWith("#EXTINF", ignoreCase = true) -> {
                    val atributos = atributosExtinf(linha)
                    val titulo = extrairTituloExtinf(linha)
                    val grupo = listOfNotNull(
                            atributos["group-title"],
                            atributos["group"],
                            atributos["category"]
                        ).joinToString(" | ")
                    val grupoNormalizado = normalizarTexto(grupo)
                    val grupoPodeConterSerie = grupoNormalizado.startsWith("series") ||
                        grupoNormalizado.containsAny("serie", "show", "novela", "anime", "season", "temporada")
                    val dadosSerie = if (grupoPodeConterSerie) extrairDadosSerie(titulo, grupo) else null
                    pendente = Entrada(
                        titulo = titulo,
                        grupo = grupo,
                        logo = normalizarImagemUrl(
                            listOfNotNull(
                                atributos["tvg-logo"],
                                atributos["poster"],
                                atributos["cover"],
                                atributos["movie_image"],
                                atributos["series_image"],
                                atributos["stream-icon"],
                                atributos["icon"]
                            ).firstOrNull { it.isNotBlank() }.orEmpty()
                        ),
                        tipoHint = atributos["stream_type"]
                            ?: atributos["tvg-type"]
                            ?: atributos["type"]
                            ?: atributos["kind"],
                        serieNome = dadosSerie?.serieNome,
                        temporadaNumero = dadosSerie?.temporadaNumero,
                        episodioNumero = dadosSerie?.episodioNumero
                    )
                }

                linha.isNotEmpty() && !linha.startsWith("#") -> {
                    totalItens++
                    val entrada = pendente ?: Entrada("Item $totalItens", "", "", null, null, null, null)
                    if (!adicionarEntrada(
                        indice = totalItens - 1,
                            titulo = entrada.titulo,
                            grupo = entrada.grupo,
                            logo = entrada.logo,
                            url = linha,
                            canais = canais,
                            filmes = filmes,
                            series = series,
                            tipoHint = entrada.tipoHint,
                            serieNome = entrada.serieNome,
                            temporadaNumero = entrada.temporadaNumero,
                            episodioNumero = entrada.episodioNumero,
                            aplicarFamiliaM3u = true
                        )
                    ) {
                        truncado = true
                    }
                    pendente = null

                    // Libera a Home em lotes, sem esperar o M3U inteiro terminar.
                    // O primeiro lote aparece cedo e os seguintes atualizam as telas gradualmente.
                    if (totalItens >= proximoLote) {
                        // O catálogo completo permanece nas listas finais. O callback recebe
                        // somente uma amostra fixa para a UI; copiar todas as listas acumuladas
                        // a cada lote gerava custo O(n²) e prendia TV Boxes fracas em 97%.
                        aoAtualizarParcial(
                            PlaylistCatalog(
                                canais = canais.take(48).toList(),
                                filmes = filmes.take(200).toList(),
                                series = series.take(200).toList(),
                                truncado = false
                            ),
                            totalItens
                        )
                        proximoLote += ITENS_POR_LOTE
                    }
                }
            }
        }

        if (canais.isEmpty() && filmes.isEmpty() && series.isEmpty()) {
            throw IOException("Playlist sem itens reconhecíveis")
        }
        return PlaylistCatalog(canais, filmes, series, truncado)
    }

    private data class JsonItem(val objeto: JSONObject, val dicaTipo: String?)

    private fun parseJson(corpo: String): PlaylistCatalog {
        val itens = extrairItensJson(corpo)
        if (itens.isEmpty()) throw IOException("JSON sem uma lista de streams reconhecível")

        val canais = mutableListOf<Canal>()
        val filmes = mutableListOf<Midia>()
        val series = mutableListOf<Midia>()
        val urlsVistas = mutableSetOf<String>()
        var truncado = false

        for ((indice, entrada) in itens.withIndex()) {
            val item = entrada.objeto
            val url = primeiraString(
                item,
                "stream_url",
                "stream_link",
                "direct_source",
                "url",
                "link"
            ) ?: continue
            if (!urlsVistas.add(url)) continue

            val titulo = primeiraString(item, "name", "title", "stream_display_name") ?: "Item ${indice + 1}"
            val tipoHint = entrada.dicaTipo
                ?: primeiraString(item, "stream_type", "type", "kind")
            val grupo = primeiraString(
                item,
                "category_name",
                "group-title",
                "group",
                "category",
                "category_name_en"
            ).orEmpty()
            val logo = normalizarImagemUrl(
                primeiraString(
                    item,
                    "stream_icon",
                    "stream_icon_url",
                    "logo",
                    "tvg-logo",
                    "icon",
                    "poster",
                    "cover",
                    "cover_big",
                    "movie_image",
                    "series_image",
                    "backdrop_path"
                ).orEmpty()
            )
            val nota = primeiraDouble(item, "rating", "vote_average", "rating_imdb", "imdb_rating")
            val popularidade = primeiraLong(item, "popularity", "vote_count", "views", "view_count")
            val sinopse = primeiraString(item, "plot", "description", "overview", "synopsis", "short_description").orEmpty()
            val dadosSerie = extrairDadosSerie(titulo, grupo)
            if (!adicionarEntrada(
                    indice,
                    titulo,
                    grupo,
                    logo,
                    url,
                    canais,
                    filmes,
                    series,
                    nota,
                    popularidade,
                    tipoHint,
                    sinopse,
                    dadosSerie?.serieNome,
                    dadosSerie?.temporadaNumero,
                    dadosSerie?.episodioNumero,
                    aplicarFamiliaM3u = false
                )
            ) {
                truncado = true
            }
        }

        if (canais.isEmpty() && filmes.isEmpty() && series.isEmpty()) {
            throw IOException("JSON sem itens reconhecíveis")
        }
        return PlaylistCatalog(canais, filmes, series, truncado)
    }

    private fun extrairItensJson(corpo: String): List<JsonItem> {
        val itens = mutableListOf<JsonItem>()
        val raizArray = corpo.trimStart().startsWith("[")
        if (raizArray) {
            adicionarArray(JSONArray(corpo), null, itens)
            return itens
        }

        val raiz = JSONObject(corpo)
        val chaves = listOf(
            "streams" to null,
            "channels" to "live",
            "live" to "live",
            "live_streams" to "live",
            "movies" to "movie",
            "filmes" to "movie",
            "series" to "series",
            "shows" to "series",
            "items" to null,
            "data" to null,
            "results" to null
        )
        chaves.forEach { (chave, dica) ->
            raiz.optJSONArray(chave)?.let { adicionarArray(it, dica, itens) }
        }
        if (itens.isEmpty() && raiz.has("url")) itens += JsonItem(raiz, null)
        return itens
    }

    private fun adicionarArray(array: JSONArray, dicaTipo: String?, destino: MutableList<JsonItem>) {
        for (indice in 0 until array.length()) {
            array.optJSONObject(indice)?.let { destino += JsonItem(it, dicaTipo) }
        }
    }

    private fun adicionarEntrada(
        indice: Int,
        titulo: String,
        grupo: String,
        logo: String,
        url: String,
        canais: MutableList<Canal>,
        filmes: MutableList<Midia>,
        series: MutableList<Midia>,
        nota: Double? = null,
        popularidade: Long? = null,
        tipoHint: String? = null,
        sinopse: String = "",
        serieNome: String? = null,
        temporadaNumero: Int? = null,
        episodioNumero: Int? = null,
        aplicarFamiliaM3u: Boolean = false
    ): Boolean {
        if (indice >= MAX_TOTAL_ITEMS) return false
        val tipoNormalizado = normalizarTexto(tipoHint.orEmpty())
        val grupoNormalizado = normalizarTexto(grupo)
        val familiaGrupo = classificarFamiliaGrupo(grupoNormalizado, aplicarFamiliaM3u)
        val id = "playlist_${indice}_${titulo.hashCode().toUInt()}"
        val tipoPorUrl = detectarTipoPorUrl(url)
        val tipoExplicitoSerie = tipoNormalizado.containsAny("series", "serie", "show")
        val tipoExplicitoFilme = tipoNormalizado.containsAny("movie", "movies", "filme", "filmes", "vod")
        val tipoExplicitoCanal = tipoNormalizado.containsAny("live", "canal", "channel", "tv")
        val ehSerie = when {
            familiaGrupo == FamiliaGrupo.SERIE -> true
            familiaGrupo == FamiliaGrupo.FILME || familiaGrupo == FamiliaGrupo.CANAL -> false
            tipoExplicitoSerie -> true
            tipoExplicitoFilme || tipoExplicitoCanal -> false
            tipoPorUrl == TipoUrl.SERIE -> true
            tipoPorUrl == TipoUrl.FILME || tipoPorUrl == TipoUrl.CANAL -> false
            else -> grupoNormalizado.containsAny("series", "serie", "show", "novela", "anime") &&
                !grupoNormalizado.containsAny("filme", "filmes", "movie", "movies", "vod")
        }
        val ehFilme = when {
            familiaGrupo == FamiliaGrupo.FILME -> true
            familiaGrupo == FamiliaGrupo.SERIE || familiaGrupo == FamiliaGrupo.CANAL -> false
            tipoExplicitoFilme -> true
            tipoExplicitoSerie || tipoExplicitoCanal -> false
            tipoPorUrl == TipoUrl.FILME -> true
            tipoPorUrl == TipoUrl.SERIE || tipoPorUrl == TipoUrl.CANAL -> false
            else -> grupoNormalizado.containsAny("filme", "filmes", "movie", "movies", "vod", "cinema")
        }
        return when {
            ehSerie -> {
                run {
                    series += Midia(
                        id = id,
                        titulo = titulo,
                        imagemUrl = logo,
                        tipo = TipoMidia.SERIE,
                        streamUrl = url,
                        categoria = grupo.ifBlank { "Sem categoria" },
                        nota = nota,
                        popularidade = popularidade,
                        sinopse = sinopse,
                        serieId = serieNome?.let { normalizarTexto(it) },
                        serieNome = serieNome,
                        episodioNome = titulo,
                        temporadaNumero = temporadaNumero,
                        episodioNumero = episodioNumero
                    )
                    true
                }
            }
            ehFilme -> {
                run {
                    filmes += Midia(
                        id = id,
                        titulo = titulo,
                        imagemUrl = logo,
                        tipo = TipoMidia.FILME,
                        streamUrl = url,
                        categoria = grupo.ifBlank { "Sem categoria" },
                        nota = nota,
                        popularidade = popularidade,
                        sinopse = sinopse
                    )
                    true
                }
            }
            else -> {
                // Sem tipo/grupo explícito, só um stream claramente ao vivo cai em canais.
                // Itens ambíguos não são promovidos a séries por causa do título.
                run {
                    canais += Canal(id, titulo, logo, url, grupo.ifBlank { "TV ao vivo" })
                    true
                }
            }
        }
    }

    private fun normalizarTexto(valor: String): String = Normalizer
        .normalize(valor, Normalizer.Form.NFD)
        .replace(DIACRITICS_REGEX, "")
        .lowercase(Locale.ROOT)

    private fun String.containsAny(vararg termos: String): Boolean = termos.any(::contains)

    private fun atributosExtinf(linha: String): Map<String, String> {
        return EXTINF_ATTRIBUTE_REGEX.findAll(linha).associate { match ->
            match.groupValues[1].lowercase(Locale.ROOT) to match.groupValues[2]
        }
    }

    private fun extrairTituloExtinf(linha: String): String {
        var dentroDeAspas = false
        linha.forEachIndexed { indice, caractere ->
            when {
                caractere == '\"' -> dentroDeAspas = !dentroDeAspas
                caractere == ',' && !dentroDeAspas -> {
                    return linha.substring(indice + 1).trim().ifBlank { "Sem título" }
                }
            }
        }
        return "Sem título"
    }

    private fun extrairDadosSerie(titulo: String, grupo: String): DadosSerie? {
        for (padrao in SERIE_PATTERNS) {
            val encontro = padrao.find(titulo) ?: continue
            val nome = encontro.groupValues[1].trim().trim('-', '.', '|', '_')
            if (nome.isNotBlank()) {
                val numeros = encontro.groupValues.drop(2).mapNotNull { it.toIntOrNull() }
                return DadosSerie(
                    serieNome = nome,
                    temporadaNumero = numeros.firstOrNull() ?: 1,
                    episodioNumero = numeros.getOrNull(1)
                )
            }
        }

        val temporadaDoGrupo = SEASON_GROUP_REGEX.find(grupo)
        if (temporadaDoGrupo != null && normalizarTexto(grupo).containsAny("series", "serie", "show", "novela", "anime")) {
            return DadosSerie(
                serieNome = titulo.substringBefore(" - Temporada", titulo).trim(),
                temporadaNumero = temporadaDoGrupo.groupValues[1].toIntOrNull() ?: 1,
                episodioNumero = null
            )
        }
        return null
    }

    private enum class TipoUrl { CANAL, FILME, SERIE }
    private enum class FamiliaGrupo { DESCONHECIDA, CANAL, FILME, SERIE }

    private fun classificarFamiliaGrupo(grupoNormalizado: String, aplicarFamiliaM3u: Boolean): FamiliaGrupo {
        return when {
            grupoNormalizado.startsWith("series |") || grupoNormalizado == "series" || grupoNormalizado.startsWith("series -") -> FamiliaGrupo.SERIE
            grupoNormalizado.startsWith("filmes |") || grupoNormalizado == "filmes" || grupoNormalizado.startsWith("filmes -") -> FamiliaGrupo.FILME
            grupoNormalizado.startsWith("24/7") || grupoNormalizado.contains("filmes e series") -> FamiliaGrupo.CANAL
            aplicarFamiliaM3u -> FamiliaGrupo.CANAL
            else -> FamiliaGrupo.DESCONHECIDA
        }
    }

    private fun detectarTipoPorUrl(url: String): TipoUrl? {
        val normalizada = url.lowercase(Locale.ROOT)
        val caminho = normalizada.substringBefore('?')
        return when {
            caminho.contains("/series/") || normalizada.contains("series/") -> TipoUrl.SERIE
            caminho.contains("/movie/") || caminho.contains("/movies/") ||
                caminho.contains("/vod/") || caminho.contains("/video/") ||
                normalizada.contains("=movie") || normalizada.contains("==movie") -> TipoUrl.FILME
            caminho.contains("/live/") || caminho.contains("/channel/") -> TipoUrl.CANAL
            else -> null
        }
    }

    private fun normalizarImagemUrl(valor: String): String {
        val limpo = valor.trim()
        return when {
            limpo.startsWith("//") -> "https:$limpo"
            limpo.startsWith("http://") || limpo.startsWith("https://") -> limpo
            else -> limpo
        }
    }

    private fun primeiraDouble(objeto: JSONObject, vararg chaves: String): Double? {
        return chaves.asSequence()
            .mapNotNull { chave ->
                when (val valor = objeto.opt(chave)) {
                    is Number -> valor.toDouble()
                    is String -> valor.toDoubleOrNull()
                    else -> null
                }
            }
            .firstOrNull { it.isFinite() }
    }

    private fun primeiraLong(objeto: JSONObject, vararg chaves: String): Long? {
        return chaves.asSequence()
            .mapNotNull { chave ->
                when (val valor = objeto.opt(chave)) {
                    is Number -> valor.toLong()
                    is String -> valor.toLongOrNull()
                    else -> null
                }
            }
            .firstOrNull()
    }

    private fun primeiraString(objeto: JSONObject, vararg chaves: String): String? {
        return chaves.asSequence()
            .mapNotNull { chave ->
                val valor = objeto.opt(chave)
                if (valor == null || valor == JSONObject.NULL) null
                else valor.toString().trim().ifBlank { null }
            }
            .firstOrNull()
    }

    private companion object {
        const val PREFIX_BYTES = 4 * 1024
        const val BUFFER_BYTES = 8 * 1024
        const val MAX_PLAYLIST_BYTES = 8L * 1024 * 1024
        // Limites de proteção contra uma resposta malformada, não um corte normal do catálogo.
        // A playlist do usuário com cerca de 12 mil itens passa inteira.
        const val MAX_TOTAL_ITEMS = 100_000
        const val PRIMEIRO_LOTE_ITENS = 1_000
        const val ITENS_POR_LOTE = 5_000
        val DIACRITICS_REGEX = "\\p{M}+".toRegex()
        val EXTINF_ATTRIBUTE_REGEX = Regex("([A-Za-z0-9_-]+)=\\\"([^\\\"]*)\\\"")
        val SERIE_PATTERNS = listOf(
            Regex("(?i)^(.*?)[\\s._|:-]+s(?:eason)?\\s*0*(\\d{1,2})[\\s._|:-]*e(?:p(?:is[oó]dio)?)?\\s*0*(\\d{1,3}).*$"),
            Regex("(?i)^(.*?)[\\s._|:-]+0*(\\d{1,2})x0*(\\d{1,3}).*$"),
            Regex("(?i)^(.*?)(?:\\s*[-_.| ]*\\s*(?:s|t|season|temporada)\\s*0*(\\d{1,2})(?:\\s*[-_.| ]*\\s*(?:e|ep|epis[oó]dio)?\\s*0*(\\d{1,3}))?.*)$")
        )
        val SEASON_GROUP_REGEX = Regex("(?i)(?:temporada|season|s|t)\\s*0*(\\d{1,2})")
    }
}
