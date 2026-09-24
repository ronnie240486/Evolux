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
    val trailerUrl: String? = null,
    val tipo: TipoMidia = TipoMidia.FILME,
    val midiaId: String? = null
)

enum class TipoMidia { FILME, SERIE }

data class Midia(
    val id: String,
    val titulo: String,
    val imagemUrl: String,
    val tipo: TipoMidia,
    val streamUrl: String,
    val progresso: Float? = null, // 0f..1f, usado em "Continue assistindo"
    val categoria: String = "",
    val nota: Double? = null,
    val popularidade: Long? = null,
    val sinopse: String = "",
    val serieId: String? = null,
    val serieNome: String? = null,
    val episodioNome: String? = null,
    val temporadaNumero: Int? = null,
    val episodioNumero: Int? = null
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
    val streamUrl: String,
    val placarCasa: Int? = null,
    val placarVisitante: Int? = null,
    val aoVivo: Boolean = false,
    val encerrado: Boolean = false,
    val timeCasaNomeCompleto: String = "",
    val timeVisitanteNomeCompleto: String = "",
    // Instante do jogo em milissegundos (epoch) -- usado só pra ordenar/
    // agrupar por data na tela de Jogos; "horario" continua sendo o texto
    // já formatado ("dd/MM HH:mm") mostrado pro usuário.
    val horarioMillis: Long = 0L
)
