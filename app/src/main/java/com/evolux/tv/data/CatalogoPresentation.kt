package com.evolux.tv.data

/** Uma fileira de Home construída a partir de um grupo/categoria real do catálogo. */
data class FileiraCatalogo(
    val titulo: String,
    val itens: List<Midia>,
    val servico: String? = null
)

private const val LIMITE_AMOSTRA_HOME = 1_200
private const val LIMITE_CARDS_HOME = 40

/**
 * Constrói os destaques usando uma amostra limitada para não ordenar dezenas de
 * milhares de itens na thread da interface. As grades completas continuam usando
 * o catálogo integral em suas próprias telas.
 */
fun gerarDestaques(catalogo: PlaylistCatalog, limite: Int = 8): List<Destaque> {
    val candidatos = sequence {
        yieldAll(catalogo.filmes.asSequence().take(LIMITE_AMOSTRA_HOME))
        yieldAll(catalogo.series.asSequence().take(LIMITE_AMOSTRA_HOME))
    }
    return candidatos
        .filter { it.titulo.isNotBlank() && it.streamUrl.isNotBlank() && it.imagemUrl.isNotBlank() }
        .distinctBy { it.id }
        .sortedWith(
            compareByDescending<Midia> { it.popularidade ?: Long.MIN_VALUE }
                .thenByDescending { it.nota ?: -1.0 }
                .thenBy { it.titulo.lowercase() }
        )
        .take(limite)
        .map { midia ->
            val metrica = when {
                midia.nota != null -> "Nota ${"%.1f".format(midia.nota)}"
                midia.popularidade != null -> "Mais assistido"
                else -> "Do seu catálogo"
            }
            Destaque(
                id = "destaque_${midia.id}",
                titulo = midia.titulo,
                subtitulo = if (midia.tipo == TipoMidia.FILME) {
                    "FILME EM ALTA • $metrica"
                } else {
                    "SÉRIE EM ALTA • $metrica"
                },
                ano = midia.categoria.ifBlank { "Evolux" },
                sinopse = "Disponível na sua lista autorizada.",
                imagemUrl = midia.imagemUrl,
                streamUrl = midia.streamUrl,
                tipo = midia.tipo,
                categoria = midia.categoria
            )
        }
        .toList()
}

/**
 * Seleciona grupos reais para a Home sem criar categorias artificiais. Cada
 * serviço guarda apenas um item representativo, pois a Home usa o emblema como
 * atalho; a grade completa permanece disponível em Séries.
 */
fun gerarFileirasEspeciais(catalogo: PlaylistCatalog): List<FileiraCatalogo> {
    val servicos = listOf(
        listOf("disney") to "Disney+",
        listOf("netflix") to "Netflix",
        listOf("pluto") to "Pluto TV",
        listOf("prime") to "Prime Video",
        listOf("max", "hbo") to "Max",
        listOf("globoplay") to "Globoplay",
        listOf("paramount") to "Paramount+",
        listOf("crunchyroll") to "Crunchyroll",
        listOf("funimation") to "Funimation",
        listOf("apple") to "Apple TV+",
        listOf("discovery") to "Discovery+",
        listOf("star plus", "star+") to "Star+"
    )
    val buckets = servicos.associate { it.second to ArrayList<Midia>(1) }
    val idsPorServico = servicos.associate { it.second to HashSet<String>() }
    val servicosPendentes = buckets.keys.toMutableSet()

    for (item in catalogo.series) {
        if (servicosPendentes.isEmpty()) break
        if (item.categoria.isBlank() || item.imagemUrl.isBlank() || item.streamUrl.isBlank()) continue
        val grupo = normalizarGrupo(item.categoria)
        for ((termos, nomeServico) in servicos) {
            if (buckets.getValue(nomeServico).size >= 1) continue
            if (!termos.any { grupo.contains(normalizarGrupo(it)) }) continue
            if (idsPorServico.getValue(nomeServico).add(item.id)) {
                buckets.getValue(nomeServico).add(item)
                servicosPendentes.remove(nomeServico)
            }
        }
    }

    val fileirasDeServico = servicos.mapNotNull { (_, nomeServico) ->
        buckets.getValue(nomeServico).takeIf { it.isNotEmpty() }?.let {
            FileiraCatalogo(titulo = nomeServico, itens = it, servico = nomeServico)
        }
    }

    val midiasElegiveis = sequence {
        yieldAll(catalogo.filmes.asSequence().take(LIMITE_AMOSTRA_HOME))
        yieldAll(catalogo.series.asSequence().take(LIMITE_AMOSTRA_HOME))
    }.filter {
        it.categoria.isNotBlank() && it.imagemUrl.isNotBlank() && it.streamUrl.isNotBlank()
    }
    val regrasGerais = listOf(
        listOf("alta", "popular", "trending", "top") to "FILMES EM ALTA",
        listOf("lancamento", "lancamentos", "novidade", "premiere", "new") to "LANÇAMENTOS",
        listOf("asterisco", "estrela", "*") to "DESTAQUES"
    )
    val fileirasGerais = regrasGerais.mapNotNull { (termos, tituloPadrao) ->
        val itens = midiasElegiveis
            .filter { item ->
                val grupoNormalizado = normalizarGrupo(item.categoria)
                termos.any { termo ->
                    if (termo == "*") item.categoria.contains('*') || item.categoria.contains('★')
                    else grupoNormalizado.contains(normalizarGrupo(termo))
                }
            }
            .distinctBy { it.id }
            .take(LIMITE_CARDS_HOME)
            .toList()
        itens.takeIf { it.isNotEmpty() }?.let {
            FileiraCatalogo(titulo = tituloPadrao, itens = it)
        }
    }
    return fileirasDeServico + fileirasGerais
}

private fun normalizarGrupo(valor: String): String {
    return java.text.Normalizer.normalize(valor, java.text.Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase()
}
