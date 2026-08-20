package com.evolux.tv.data

/**
 * Constrói o conteúdo do destaque a partir do catálogo autorizado.
 * Nenhuma sugestão é inventada: cada card mantém o streamUrl e a imagem
 * do item que veio da playlist.
 */
fun gerarDestaques(catalogo: PlaylistCatalog, limite: Int = 8): List<Destaque> {
    return (catalogo.filmes + catalogo.series)
        .asSequence()
        .filter { it.titulo.isNotBlank() && it.streamUrl.isNotBlank() }
        .distinctBy { it.id }
        .take(limite)
        .mapIndexed { indice, midia ->
            Destaque(
                id = "destaque_${midia.id}",
                titulo = midia.titulo,
                subtitulo = if (midia.tipo == TipoMidia.FILME) "FILME DO SEU CATÁLOGO" else "SÉRIE DO SEU CATÁLOGO",
                ano = "Evolux",
                sinopse = "Disponível na sua lista autorizada.",
                imagemUrl = midia.imagemUrl,
                streamUrl = midia.streamUrl
            )
        }
        .toList()
}
