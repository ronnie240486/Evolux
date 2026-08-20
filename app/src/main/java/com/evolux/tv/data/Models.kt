package com.evolux.tv.data

/**
 * Um item de destaque (o banner grande da Home, tipo "Legado Real 2026").
 * A lista de destaques é derivada do catálogo autorizado carregado pelo MAC.
 */
data class Destaque(
    val id: String,
    val titulo: String,
    val subtitulo: String,
    val ano: String,
    val sinopse: String,
    val imagemUrl: String,
    val streamUrl: String,
    val trailerUrl: String? = null
)

enum class TipoMidia { FILME, SERIE }

data class Midia(
    val id: String,
    val titulo: String,
    val imagemUrl: String,
    val tipo: TipoMidia,
    val streamUrl: String,
    val progresso: Float? = null // 0f..1f, usado em "Continue assistindo"
)

data class Canal(
    val id: String,
    val nome: String,
    val logoUrl: String,
    val streamUrl: String,
    val categoria: String
)

data class Jogo(
    val id: String,
    val timeCasaSigla: String,
    val timeCasaLogoUrl: String,
    val timeVisitanteSigla: String,
    val timeVisitanteLogoUrl: String,
    val horario: String,
    val campeonato: String,
    val streamUrl: String
)
