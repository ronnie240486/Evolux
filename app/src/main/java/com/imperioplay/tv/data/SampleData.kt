package com.imperioplay.tv.data

/**
 * NOTA IMPORTANTE:
 * Estes dados são só placeholders para o app compilar e a tela ficar
 * navegável. Troque por uma chamada real (API própria, um catálogo
 * licenciado, um serviço de EPG, etc.) em um repositório/ViewModel.
 * As URLs de imagem abaixo são só exemplos (picsum.photos) e as
 * streamUrl são vazias — é aí que entra a URL real de cada conteúdo.
 */
object SampleData {

    // Lista que alimenta o banner de destaque da Home.
    // O FeaturedBanner troca automaticamente entre os itens desta lista.
    val destaques = listOf(
        Destaque(
            id = "destaque_1",
            titulo = "LEGADO REAL",
            subtitulo = "DESTAQUE DA SEMANA",
            ano = "2026",
            sinopse = "Em um reino dividido pela ambição, um herdeiro inesperado precisa " +
                "enfrentar traições e batalhas épicas para reconquistar o que é seu por direito.",
            imagemUrl = "https://picsum.photos/id/1043/1600/900",
            streamUrl = ""
        ),
        Destaque(
            id = "destaque_2",
            titulo = "GUERRA SOMBRIA",
            subtitulo = "LANÇAMENTO",
            ano = "2026",
            sinopse = "Três soldados perdidos atrás das linhas inimigas precisam decidir " +
                "entre a lealdade e a sobrevivência.",
            imagemUrl = "https://picsum.photos/id/1067/1600/900",
            streamUrl = ""
        ),
        Destaque(
            id = "destaque_3",
            titulo = "A COROA DE FERRO",
            subtitulo = "SÉRIE ORIGINAL",
            ano = "2026",
            sinopse = "O trono está vazio e sete famílias disputam o direito de governar.",
            imagemUrl = "https://picsum.photos/id/1050/1600/900",
            streamUrl = ""
        )
    )

    val lancamentosFilmes = listOf(
        Midia("f1", "Guerra Sombria", "https://picsum.photos/id/1067/400/600", TipoMidia.FILME, ""),
        Midia("f2", "O Código Eterno", "https://picsum.photos/id/1015/400/600", TipoMidia.FILME, ""),
        Midia("f3", "Fronteira Final", "https://picsum.photos/id/1011/400/600", TipoMidia.FILME, ""),
        Midia("f4", "O Último Guerreiro", "https://picsum.photos/id/1005/400/600", TipoMidia.FILME, ""),
        Midia("f5", "Sobreviventes do Amanhã", "https://picsum.photos/id/1035/400/600", TipoMidia.FILME, ""),
        Midia("f6", "Reino em Chamas", "https://picsum.photos/id/1041/400/600", TipoMidia.FILME, "")
    )

    val lancamentosSeries = listOf(
        Midia("s1", "A Coroa de Ferro", "https://picsum.photos/id/1050/400/600", TipoMidia.SERIE, ""),
        Midia("s2", "Sombras do Poder", "https://picsum.photos/id/1024/400/600", TipoMidia.SERIE, ""),
        Midia("s3", "Herdeiros do Norte", "https://picsum.photos/id/1039/400/600", TipoMidia.SERIE, ""),
        Midia("s4", "O Julgamento", "https://picsum.photos/id/1074/400/600", TipoMidia.SERIE, ""),
        Midia("s5", "Código Sombra", "https://picsum.photos/id/1084/400/600", TipoMidia.SERIE, ""),
        Midia("s6", "Império Oculto", "https://picsum.photos/id/1062/400/600", TipoMidia.SERIE, "")
    )

    val continuarAssistindo = listOf(
        Midia("c1", "Interestelar", "https://picsum.photos/id/1018/400/600", TipoMidia.FILME, "", 0.6f),
        Midia("c2", "Vingança Silenciosa", "https://picsum.photos/id/1027/400/600", TipoMidia.FILME, "", 0.35f),
        Midia("c3", "A Grande Mentira", "https://picsum.photos/id/1031/400/600", TipoMidia.SERIE, "", 0.8f),
        Midia("c4", "Missão Proibida", "https://picsum.photos/id/1040/400/600", TipoMidia.FILME, "", 0.2f),
        Midia("c5", "O Rei Esquecido", "https://picsum.photos/id/1055/400/600", TipoMidia.SERIE, "", 0.5f),
        Midia("c6", "Planeta Selvagem", "https://picsum.photos/id/1080/400/600", TipoMidia.FILME, "", 0.15f)
    )

    val canaisAoVivo = listOf(
        Canal("ch1", "Esporte HD", "https://picsum.photos/id/1069/200/200", "", "Esportes"),
        Canal("ch2", "Cinema Premium", "https://picsum.photos/id/1070/200/200", "", "Filmes"),
        Canal("ch3", "Notícias 24h", "https://picsum.photos/id/1071/200/200", "", "Notícias"),
        Canal("ch4", "Kids Play", "https://picsum.photos/id/1072/200/200", "", "Infantil"),
        Canal("ch5", "Séries HD", "https://picsum.photos/id/1073/200/200", "", "Séries"),
        Canal("ch6", "Documentário+", "https://picsum.photos/id/1076/200/200", "", "Documentários")
    )

    val jogosDoDia = listOf(
        Jogo(
            id = "j1",
            timeCasaSigla = "FLA",
            timeCasaLogoUrl = "https://picsum.photos/id/1080/100/100",
            timeVisitanteSigla = "PAL",
            timeVisitanteLogoUrl = "https://picsum.photos/id/1081/100/100",
            horario = "19:00",
            campeonato = "Campeonato Brasileiro",
            streamUrl = ""
        ),
        Jogo(
            id = "j2",
            timeCasaSigla = "COR",
            timeCasaLogoUrl = "https://picsum.photos/id/1082/100/100",
            timeVisitanteSigla = "SAO",
            timeVisitanteLogoUrl = "https://picsum.photos/id/1083/100/100",
            horario = "21:30",
            campeonato = "Campeonato Brasileiro",
            streamUrl = ""
        ),
        Jogo(
            id = "j3",
            timeCasaSigla = "MCI",
            timeCasaLogoUrl = "https://picsum.photos/id/1084/100/100",
            timeVisitanteSigla = "RMA",
            timeVisitanteLogoUrl = "https://picsum.photos/id/1025/100/100",
            horario = "16:00",
            campeonato = "UEFA Champions League",
            streamUrl = ""
        )
    )
}
