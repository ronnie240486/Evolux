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
    val ocultas = categoriasOcultas.map(::normalizarConsulta).toSet()
    val filtradas = itens.asSequence()
        .filter { midia -> normalizarConsulta(midia.categoria.ifBlank { "Sem categoria" }) !in ocultas }
        .filter { midia -> categoriaNormalizada == null || normalizarConsulta(midia.categoria.ifBlank { "Sem categoria" }) == categoriaNormalizada }
        .filter { midia ->
            buscaNormalizada.isBlank() || listOf(midia.titulo, midia.categoria, midia.sinopse)
                .any { normalizarConsulta(it).contains(buscaNormalizada) }
        }
    return ordenarMidias(filtradas.toList(), ordem)
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
    val ocultas = categoriasOcultas.map(::normalizarConsulta).toSet()
    val filtradas = itens.asSequence()
        .filter { canal -> normalizarConsulta(canal.categoria.ifBlank { "TV ao vivo" }) !in ocultas }
        .filter { canal -> categoriaNormalizada == null || normalizarConsulta(canal.categoria.ifBlank { "TV ao vivo" }) == categoriaNormalizada }
        .filter { canal -> buscaNormalizada.isBlank() || listOf(canal.nome, canal.categoria).any { normalizarConsulta(it).contains(buscaNormalizada) } }
        .toList()
    return when (ordem) {
        OrdemCatalogo.NOME_ZA -> filtradas.sortedByDescending { normalizarConsulta(it.nome) }
        else -> filtradas.sortedBy { normalizarConsulta(it.nome) }
    }
}

private val PALAVRAS_ADULTO = listOf("adult", "adulto", "xxx", "+18", "18+", " porn", "pornô", "porno")

fun ehCategoriaAdulto(categoria: String): Boolean {
    val normalizada = " ${normalizarConsulta(categoria)} "
    return PALAVRAS_ADULTO.any { normalizada.contains(it) }
}

/**
 * Ordena categorias respeitando uma ordem customizada salva pelo usuário
 * (arrastar/mover em Configurações). Categorias novas que ainda não estão na
 * ordem customizada aparecem depois, em ordem alfabética. Categorias adultas
 * sempre vão para o final da lista, independente da ordem customizada.
 */
fun ordenarCategorias(categoriasBrutas: List<String>, ordemCustom: List<String>): List<String> {
    val (adultas, normais) = categoriasBrutas.partition(::ehCategoriaAdulto)
    val posicao = ordemCustom.withIndex().associate { (indice, nome) -> normalizarConsulta(nome) to indice }
    val normaisOrdenadas = normais.sortedWith(
        compareBy<String> { posicao[normalizarConsulta(it)] ?: Int.MAX_VALUE }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it }
    )
    val adultasOrdenadas = adultas.sortedWith(String.CASE_INSENSITIVE_ORDER)
    return normaisOrdenadas + adultasOrdenadas
}

private val REGEX_MARCAS_DIACRITICAS = "\\p{M}+".toRegex()

fun normalizarConsulta(valor: String): String = Normalizer
    .normalize(valor, Normalizer.Form.NFD)
    .replace(REGEX_MARCAS_DIACRITICAS, "")
    .lowercase(Locale.ROOT)
    .trim()

private fun ordenarMidias(itens: List<Midia>, ordem: OrdemCatalogo): List<Midia> {
    return when (ordem) {
        OrdemCatalogo.PADRAO -> itens
        OrdemCatalogo.NOME_AZ -> itens.sortedBy { normalizarConsulta(it.titulo) }
        OrdemCatalogo.NOME_ZA -> itens.sortedByDescending { normalizarConsulta(it.titulo) }
        OrdemCatalogo.NOTA -> itens.sortedWith(compareByDescending<Midia> { it.nota ?: Double.NEGATIVE_INFINITY }.thenBy { normalizarConsulta(it.titulo) })
        OrdemCatalogo.POPULARIDADE -> itens.sortedWith(compareByDescending<Midia> { it.popularidade ?: Long.MIN_VALUE }.thenBy { normalizarConsulta(it.titulo) })
    }
}
