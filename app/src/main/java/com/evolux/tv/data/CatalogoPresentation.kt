package com.evolux.tv.data

/** Uma fileira de Home construída a partir de um grupo/categoria real do catálogo. */
data class FileiraCatalogo(
    val titulo: String,
    val itens: List<Midia>
)

/**
 * Constrói os cards de sugestão a partir do catálogo autorizado.
 * Quando a fonte fornece popularidade ou nota, esses campos definem a ordem
 * de “mais em alta”; sem esses campos, o app não inventa ranking.
 */
private fun grupoDestaque(midia: Midia): String {
    if (midia.tipo == TipoMidia.FILME) return "filme"
    val cat = midia.categoria.lowercase()
    return when {
        "anime" in cat -> "anime"
        "dorama" in cat || "kdrama" in cat || "k-drama" in cat || "k drama" in cat -> "dorama"
        "novela" in cat -> "novela"
        else -> "serie"
    }
}

fun gerarDestaques(catalogo: PlaylistCatalog, limite: Int = 8): List<Destaque> {
    val candidatos = (catalogo.filmes + catalogo.series)
        .asSequence()
        .filter { it.titulo.isNotBlank() && it.streamUrl.isNotBlank() && it.imagemUrl.isNotBlank() }
        // Uma novela/série tem muitos capítulos/episódios com o mesmo nome base;
        // sem isso, ela sozinha lotava o pool de destaques com "ela mesma".
        .distinctBy { midia ->
            if (midia.tipo == TipoMidia.FILME) {
                midia.id
            } else {
                midia.serieNome?.takeIf { it.isNotBlank() }?.lowercase() ?: midia.titulo.lowercase()
            }
        }
        .sortedWith(
            compareByDescending<Midia> { it.popularidade ?: Long.MIN_VALUE }
                .thenByDescending { it.nota ?: -1.0 }
                .thenBy { it.titulo.lowercase() }
        )
        .toList()

    val porGrupo = candidatos.groupBy(::grupoDestaque)
    val poolPorGrupo = porGrupo.mapValues { (_, itens) -> itens.take(20).shuffled() }.toMutableMap()

    // Round-robin entre os grupos (filme, serie, anime, dorama, novela) pra
    // garantir mistura, em vez de deixar o grupo com mais itens dominar tudo.
    val ordemGrupos = listOf("filme", "serie", "anime", "dorama", "novela").filter { it in poolPorGrupo }
    val selecionados = mutableListOf<Midia>()
    var indiceGrupo = 0
    while (selecionados.size < limite && ordemGrupos.isNotEmpty()) {
        val grupo = ordemGrupos[indiceGrupo % ordemGrupos.size]
        val fila = poolPorGrupo[grupo]
        if (!fila.isNullOrEmpty()) {
            selecionados.add(fila.first())
            poolPorGrupo[grupo] = fila.drop(1)
        }
        indiceGrupo++
        if (indiceGrupo > ordemGrupos.size * 30) break // segurança contra loop infinito
    }

    return selecionados
        .shuffled()
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
                midiaId = midia.id
            )
        }
        .toList()
}

/**
 * Seleciona somente grupos reais cujo nome indica uma fileira editorial.
 * Se a M3U não tiver determinado grupo, nenhuma fileira artificial é criada.
 */
fun gerarFileirasEspeciais(catalogo: PlaylistCatalog): List<FileiraCatalogo> {
    val grupos = (catalogo.filmes + catalogo.series)
        .filter { it.categoria.isNotBlank() && it.imagemUrl.isNotBlank() && it.streamUrl.isNotBlank() }
        .groupBy { it.categoria }

    val regras = listOf(
        listOf("alta", "popular", "trending", "top") to "FILMES EM ALTA",
        listOf("lancamento", "lancamentos", "novidade", "premiere", "new") to "LANÇAMENTOS",
        listOf("disney") to "SÉRIES DA DISNEY",
        listOf("asterisco", "estrela", "*") to "DESTAQUES"
    )

    return regras.mapNotNull { (termos, tituloPadrao) ->
        val itens = grupos
            .filter { (grupo, _) ->
                val grupoNormalizado = normalizarGrupo(grupo)
                termos.any { termo ->
                    if (termo == "*") grupo.contains('*') || grupo.contains('★')
                    else grupoNormalizado.contains(normalizarGrupo(termo))
                }
            }
            .flatMap { (_, itensDoGrupo) -> itensDoGrupo }
            .distinctBy { it.id }
            .take(40)
        itens.takeIf { it.isNotEmpty() }?.let {
            FileiraCatalogo(titulo = tituloPadrao, itens = it)
        }
    }
}

private fun normalizarGrupo(valor: String): String {
    return java.text.Normalizer.normalize(valor, java.text.Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase()
}
