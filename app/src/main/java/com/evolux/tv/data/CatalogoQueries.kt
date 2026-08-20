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

fun normalizarConsulta(valor: String): String = Normalizer
    .normalize(valor, Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "")
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
