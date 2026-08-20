package com.evolux.tv.data

/** Uma fileira de Home construída a partir de um grupo/categoria real do catálogo. */
data class FileiraCatalogo(
    val titulo: String,
    val itens: List<Midia>,
    val servico: String? = null
)

/**
 * Constrói os cards de sugestão a partir do catálogo autorizado.
 * Quando a fonte fornece popularidade ou nota, esses campos definem a ordem
 * de “mais em alta”; sem esses campos, o app não inventa ranking.
 */
fun gerarDestaques(catalogo: PlaylistCatalog, limite: Int = 8): List<Destaque> {
    return (catalogo.filmes + catalogo.series)
        .asSequence()
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
 * Seleciona somente grupos reais cujo nome indica uma fileira editorial.
 * Se a M3U não tiver determinado grupo, nenhuma fileira artificial é criada.
 */
fun gerarFileirasEspeciais(catalogo: PlaylistCatalog): List<FileiraCatalogo> {
    val seriesElegiveis = catalogo.series.asSequence()
        .filter { it.categoria.isNotBlank() && it.imagemUrl.isNotBlank() && it.streamUrl.isNotBlank() }

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
        listOf("star plus") to "Star+"
    )

    val fileirasDeServico = servicos.mapNotNull { (termos, nomeServico) ->
        val itens = seriesElegiveis
            .filter { item ->
                val normalizado = normalizarGrupo(item.categoria)
                termos.any { normalizado.contains(normalizarGrupo(it)) }
            }
            .distinctBy { it.id }
            .take(40)
            .toList()
        itens.takeIf { it.isNotEmpty() }?.let {
            FileiraCatalogo(titulo = nomeServico, itens = it, servico = nomeServico)
        }
    }

    val midiasElegiveis = (catalogo.filmes.asSequence() + catalogo.series.asSequence())
        .filter { it.categoria.isNotBlank() && it.imagemUrl.isNotBlank() && it.streamUrl.isNotBlank() }
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
            .take(40)
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
