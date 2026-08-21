package com.evolux.tv.data

import java.text.Normalizer
import java.util.Locale

enum class OrdemCatalogo(val rotulo: String) {
    PADRAO("Padrão"),
    NOME_AZ("Nome A–Z"),
    NOME_ZA("Nome Z–A"),
    NOTA("Nota"),
    POPULARIDADE("Popularidade")
}

val ORDEM_FAMILIAS_CANAIS = listOf(
    "Canais Abertos",
    "Filmes e Séries",
    "Documentários",
    "Esportes",
    "Notícias",
    "Infantil",
    "Variedades",
    "Música e Rádio",
    "Religiosos",
    "24/7 e Temáticos",
    "Outros Canais"
)

fun familiaDoCanal(canal: Canal): String {
    val texto = normalizarConsulta("${canal.categoria} ${canal.nome}")
    return when {
        texto.containsAny("globo", "sbt", "record", "band", "cultura", "gazeta", "tv brasil", "rede tv", "abertos") -> "Canais Abertos"
        texto.containsAny("hbo", "telecine", "paramount", "amc", "cine sky", "cine filmes", "filmes e series", "24/7 filmes", "cinema", "tnt") -> "Filmes e Séries"
        texto.containsAny("document", "history", "historia", "animal planet", "nat geo", "national geographic", "discovery") -> "Documentários"
        texto.containsAny("premiere", "espn", "sportv", "sportynet", "ppv", "esporte", "esportes", "dazn", "ufc", "nba", "eleven", "caze tv", "jogos do dia") -> "Esportes"
        texto.containsAny("noticia", "news", "cnn", "globonews", "bandnews", "record news", "jovem pan") -> "Notícias"
        texto.containsAny("infantil", "cartoon", "nick", "gloob", "kids", "disney", "baby") -> "Infantil"
        texto.containsAny("variedade", "lifestyle", "entretenimento", "reality", "culinaria", "comedia") -> "Variedades"
        texto.containsAny("radio", "radios", "musica", "music", "mtv", "multishow", "bis") -> "Música e Rádio"
        texto.containsAny("religios", "gospel", "aparecida", "cançao nova", "cancao nova") -> "Religiosos"
        texto.containsAny("24/7", "exclusivos", "4k", "reels", "shorts", "anime", "dorama", "seriado", "novela", "turca") -> "24/7 e Temáticos"
        else -> "Outros Canais"
    }
}

fun familiasDeCanais(canais: List<Canal>): List<String> {
    val presentes = canais.asSequence()
        .map(::familiaDoCanal)
        .toSet()
    return ORDEM_FAMILIAS_CANAIS.filter { it in presentes }
}

/** Categorias originais do group-title, ordenadas por família sem perder o nome real. */
fun categoriasReaisDeCanais(canais: List<Canal>, categoriasOcultas: Set<String> = emptySet()): List<String> {
    val ocultas = categoriasOcultas.map(::categoriaChave).toSet()
    val categorias = canais.asSequence()
        .map { it.categoria.ifBlank { "TV ao vivo" }.trim() }
        .filter { categoriaChave(it) !in ocultas }
        .distinctBy(::categoriaChave)
        .toList()
    // A M3U pode ter grupos diferentes e em ordens diferentes; preserve a ordem em que o arquivo fornece cada grupo.
    return categorias
}

fun filtrarEOrdenarMidias(
    itens: List<Midia>,
    busca: String,
    categoria: String?,
    ordem: OrdemCatalogo,
    categoriasOcultas: Set<String> = emptySet()
): List<Midia> {
    val buscaNormalizada = normalizarConsulta(busca)
    val categoriaNormalizada = categoria?.takeUnless { it == "Todos" }
        ?.let(::normalizarConsulta)
    val ocultas = categoriasOcultas.map(::categoriaChave).toSet()
    val filtradas = itens.asSequence()
        .filter { midia -> categoriaChave(midia.categoria.ifBlank { "Sem categoria" }) !in ocultas }
        .filter { midia -> categoriaNormalizada == null || categoriaChave(midia.categoria.ifBlank { "Sem categoria" }) == categoriaChave(categoriaNormalizada) }
        .filter { midia ->
            buscaNormalizada.isBlank() || listOf(midia.titulo, midia.categoria, midia.sinopse)
                .any { normalizarConsulta(it).contains(buscaNormalizada) }
        }
    return ordenarMidias(filtradas.toList(), ordem)
}

fun filtrarEOrdenarCanaisPorFamilia(
    itens: List<Canal>,
    busca: String,
    familia: String?,
    ordem: OrdemCatalogo,
    categoriasOcultas: Set<String> = emptySet()
): List<Canal> {
    val buscaNormalizada = normalizarConsulta(busca)
    val familiaNormalizada = familia?.takeUnless { it == "Todos" }
        ?.let(::normalizarConsulta)
    val ocultas = categoriasOcultas.map(::categoriaChave).toSet()
    val filtradas = itens.asSequence()
        .filter { canal -> categoriaChave(canal.categoria.ifBlank { "TV ao vivo" }) !in ocultas }
        .filter { canal -> familiaNormalizada == null || normalizarConsulta(familiaDoCanal(canal)) == familiaNormalizada }
        .filter { canal ->
            buscaNormalizada.isBlank() || listOf(canal.nome, canal.categoria)
                .any { normalizarConsulta(it).contains(buscaNormalizada) }
        }
        .toList()
    return when (ordem) {
        OrdemCatalogo.NOME_ZA -> filtradas.sortedByDescending { normalizarConsulta(it.nome) }
        else -> filtradas.sortedBy { normalizarConsulta(it.nome) }
    }
}

fun filtrarEOrdenarCanais(
    itens: List<Canal>,
    busca: String,
    categoria: String?,
    ordem: OrdemCatalogo,
    categoriasOcultas: Set<String> = emptySet()
): List<Canal> {
    val buscaNormalizada = normalizarConsulta(busca)
    val categoriaNormalizada = categoria?.takeUnless { it == "Todos" }
        ?.let(::normalizarConsulta)
    val ocultas = categoriasOcultas.map(::categoriaChave).toSet()
    val filtradas = itens.asSequence()
        .filter { canal -> categoriaChave(canal.categoria.ifBlank { "TV ao vivo" }) !in ocultas }
        .filter { canal -> categoriaNormalizada == null || categoriaChave(canal.categoria.ifBlank { "TV ao vivo" }) == categoriaChave(categoriaNormalizada) }
        .filter { canal -> buscaNormalizada.isBlank() || listOf(canal.nome, canal.categoria).any { normalizarConsulta(it).contains(buscaNormalizada) } }
        .toList()
    return when (ordem) {
        OrdemCatalogo.NOME_ZA -> filtradas.sortedByDescending { normalizarConsulta(it.nome) }
        else -> filtradas.sortedBy { normalizarConsulta(it.nome) }
    }
}

fun normalizarConsulta(valor: String): String = Normalizer
    .normalize(valor, Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "")
    .lowercase(Locale.ROOT)
    .trim()

private fun String.containsAny(vararg termos: String): Boolean = termos.any { contains(normalizarConsulta(it)) }

private fun ordenarMidias(itens: List<Midia>, ordem: OrdemCatalogo): List<Midia> {
    return when (ordem) {
        OrdemCatalogo.PADRAO -> itens
        OrdemCatalogo.NOME_AZ -> itens.sortedBy { normalizarConsulta(it.titulo) }
        OrdemCatalogo.NOME_ZA -> itens.sortedByDescending { normalizarConsulta(it.titulo) }
        OrdemCatalogo.NOTA -> itens.sortedWith(compareByDescending<Midia> { it.nota ?: Double.NEGATIVE_INFINITY }.thenBy { normalizarConsulta(it.titulo) })
        OrdemCatalogo.POPULARIDADE -> itens.sortedWith(compareByDescending<Midia> { it.popularidade ?: Long.MIN_VALUE }.thenBy { normalizarConsulta(it.titulo) })
    }
}

fun prioridadeCategoriaReal(categoria: String): Int {
    val texto = normalizarConsulta(categoria)
    val chave = categoriaChave(categoria)
    return when {
        texto.contains("24/7") || chave.startsWith("247") -> 900
        chave.contains("globo") -> 10
        chave == "sbt" || chave.startsWith("sbt") -> 20
        chave.contains("abert") -> 30
        chave.contains("record") -> 40
        chave.contains("band") -> 50
        chave.contains("filmeseseries") || chave.contains("cinesky") || chave.contains("cinefilmes") || chave == "4k" -> 100
        chave.contains("hbo") -> 110
        chave.contains("telecine") -> 120
        chave.contains("paramount") -> 130
        chave == "max" || chave.contains("max") -> 140
        chave.contains("primevideo") -> 150
        chave.contains("disney") || chave.contains("appletv") -> 160
        chave.contains("document") || chave.contains("discovery") || chave.contains("history") || chave.contains("natgeo") -> 200
        chave.contains("infantil") || chave.contains("cartoon") || chave.contains("gloob") || chave.contains("nick") || chave.contains("anime") || chave.contains("desenho") -> 220
        chave.contains("premiere") || chave.contains("sportv") || chave.contains("espn") || chave.contains("sportynet") || chave.contains("esporte") || chave.contains("ppv") || chave.contains("dazn") || chave.contains("ufc") || chave.contains("nba") || chave.contains("eleven") || chave.contains("cazetv") || chave.contains("jogosdodia") -> 300
        chave.contains("noticia") || chave.contains("news") -> 400
        chave.contains("radio") || chave.contains("music") || chave.contains("mtv") || chave.contains("multishow") -> 500
        chave.contains("relig") || chave.contains("gospel") -> 520
        else -> 700
    }
}

fun categoriaChave(valor: String): String = normalizarConsulta(valor).replace(Regex("[^a-z0-9]+"), "")

private fun normalizarTexto(valor: String): String = normalizarConsulta(valor)

@Suppress("UNUSED_VARIABLE")
private val manterCompatibilidadeNormalizacao = ::normalizarTexto
