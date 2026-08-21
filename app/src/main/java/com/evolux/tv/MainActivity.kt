package com.evolux.tv

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.evolux.tv.R
import com.evolux.tv.data.EvoluxRepository
import com.evolux.tv.data.Destaque
import com.evolux.tv.data.EvoluxConfig
import com.evolux.tv.data.CatalogoCache
import com.evolux.tv.data.MacAddressUtils
import com.evolux.tv.data.PlaylistCatalog
import com.evolux.tv.data.PlaylistRepository
import com.evolux.tv.data.Midia
import com.evolux.tv.data.TipoMidia
import com.evolux.tv.data.Jogo
import com.evolux.tv.data.JogosDoDiaRepository
import com.evolux.tv.data.OrdemCatalogo
import com.evolux.tv.data.ResultadoConfiguracao
import com.evolux.tv.data.XtreamRepository
import com.evolux.tv.data.gerarDestaques
import com.evolux.tv.data.gerarFileirasEspeciais
import com.evolux.tv.ui.components.Tela
import com.evolux.tv.ui.components.TopNavBar
import com.evolux.tv.ui.screens.*
import com.evolux.tv.ui.theme.FundoEscuro
import com.evolux.tv.ui.theme.EvoluxTheme

private const val CHAVE_FAVORITOS = "favoritos_ids"
private const val CHAVE_MAC_LOGICO = "mac_logico_evolux"
private const val CHAVE_MAC_AUTORIZADO = "mac_autorizado_confirmado"
private const val CHAVE_PLAYLIST_ATIVA = "playlist_ativa"
private const val CHAVE_CATEGORIAS_OCULTAS = "categorias_ocultas"
private const val CHAVE_ORDEM_CANAIS = "ordem_canais"
private const val CHAVE_ORDEM_FILMES = "ordem_filmes"
private const val CHAVE_ORDEM_SERIES = "ordem_series"

private data class Reproducao(
    val titulo: String,
    val streamUrl: String
)

private data class HomePresentation(
    val destaques: List<Destaque>,
    val fileirasEspeciais: List<com.evolux.tv.data.FileiraCatalogo>
)

private fun lerOrdem(valor: String?): OrdemCatalogo = runCatching {
    OrdemCatalogo.valueOf(valor.orEmpty())
}.getOrDefault(OrdemCatalogo.PADRAO)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EvoluxTheme {
                EvoluxApp()
            }
        }
    }
}

@Composable
fun EvoluxApp() {
    var telaAtual by remember { mutableStateOf(Tela.INICIO) }
    val contexto = LocalContext.current
    val preferencias = remember(contexto) {
        contexto.getSharedPreferences("evolux_preferencias", Context.MODE_PRIVATE)
    }
    val macLogico = remember(preferencias) {
        preferencias.getString(CHAVE_MAC_LOGICO, null) ?: MacAddressUtils.gerarMacLogico().also { novoMac ->
            preferencias.edit().putString(CHAVE_MAC_LOGICO, novoMac).apply()
        }
    }
    val macInicial = macLogico
    val macJaAutorizado = preferencias.getBoolean(CHAVE_MAC_AUTORIZADO, false)
    val repository = remember { EvoluxRepository() }
    val playlistRepository = remember { PlaylistRepository() }
    val xtreamRepository = remember { XtreamRepository() }
    val jogosDoDiaRepository = remember { JogosDoDiaRepository() }
    val escopo = rememberCoroutineScope()
    var macAutorizado by remember { mutableStateOf("") }
    var catalogo by remember { mutableStateOf<PlaylistCatalog?>(null) }
    var configuracaoAtual by remember { mutableStateOf<EvoluxConfig?>(null) }
    var fontesConfiguradas by remember { mutableStateOf<List<String>>(emptyList()) }
    var playlistAtiva by remember { mutableStateOf(preferencias.getInt(CHAVE_PLAYLIST_ATIVA, 0)) }
    var playlistUrlAtual by remember { mutableStateOf<String?>(null) }
    var categoriasOcultas by remember { mutableStateOf(preferencias.getStringSet(CHAVE_CATEGORIAS_OCULTAS, emptySet()).orEmpty()) }
    var ordens by remember {
        mutableStateOf(
            mapOf(
                "canais" to lerOrdem(preferencias.getString(CHAVE_ORDEM_CANAIS, null)),
                "filmes" to lerOrdem(preferencias.getString(CHAVE_ORDEM_FILMES, null)),
                "series" to lerOrdem(preferencias.getString(CHAVE_ORDEM_SERIES, null))
            )
        )
    }
    var estadoLogin by remember { mutableStateOf<EstadoLoginMac>(EstadoLoginMac.Ocioso) }
    var validacaoEmAndamento by remember { mutableStateOf(false) }
    var carregandoCatalogo by remember { mutableStateOf(false) }
    var progressoCatalogo by remember { mutableStateOf(1) }
    var segundosCatalogo by remember { mutableStateOf(0) }
    var reproducao by remember { mutableStateOf<Reproducao?>(null) }
    var jogosDoDia by remember { mutableStateOf<List<Jogo>>(emptyList()) }
    var categoriaInicialSeries by remember { mutableStateOf<String?>(null) }

    fun abreviarEquipe(nome: String): String {
        val limpo = nome.replace(Regex("[^A-Za-zÀ-ÿ0-9 ]"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (limpo.isEmpty()) return "---"
        if (limpo.size == 1) return limpo.first().take(4).uppercase()
        return limpo.take(3).joinToString("") { it.first().uppercase() }.take(4)
    }

    fun extrairHorarioJogo(nome: String): String {
        return Regex("\\b([01]?\\d|2[0-3]):[0-5]\\d\\b").find(nome)?.value ?: "Hoje"
    }

    fun jogosDaPlaylist(canais: List<com.evolux.tv.data.Canal>): List<Jogo> {
        return canais.asSequence()
            .filter { canal ->
                val categoria = canal.categoria.trim().lowercase()
                categoria == "jogos do dia" || categoria.contains("jogos do dia")
            }
            .take(8)
            .mapIndexed { indice, canal ->
                val nomeSemHorario = canal.nome
                    .replace("⚽", "")
                    .replace(Regex("\\b([01]?\\d|2[0-3]):[0-5]\\d\\b"), "")
                    .trim()
                val partes = nomeSemHorario
                    .split(Regex("\\s+x\\s+|\\s+vs\\s+|\\s+v\\s+", RegexOption.IGNORE_CASE))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                val casa = partes.getOrNull(0) ?: canal.nome
                val visitante = partes.getOrNull(1) ?: "Jogo"
                Jogo(
                    id = "playlist_jogo_${canal.id.ifBlank { indice.toString() }}",
                    timeCasaSigla = abreviarEquipe(casa),
                    timeCasaLogoUrl = canal.logoUrl,
                    timeVisitanteSigla = abreviarEquipe(visitante),
                    timeVisitanteLogoUrl = canal.logoUrl,
                    horario = extrairHorarioJogo(canal.nome),
                    campeonato = "Jogos do Dia",
                    streamUrl = canal.streamUrl,
                    status = "PROGRAMADO",
                    dataHoraUtc = ""
                )
            }
            .toList()
    }

    BackHandler(enabled = reproducao != null || telaAtual != Tela.INICIO) {
        if (reproducao != null) {
            reproducao = null
        } else {
            telaAtual = Tela.INICIO
        }
    }

    fun carregarSeriesXtreamEmSegundoPlano(
        urlPlaylist: String,
        fingerprint: String,
        catalogoBase: PlaylistCatalog
    ) {
        if (!XtreamRepository.pareceXtream(urlPlaylist)) return
        escopo.launch {
            val seriesXtream = withTimeoutOrNull(15_000L) {
                xtreamRepository.carregarSeries(urlPlaylist)
            }.orEmpty()
            if (seriesXtream.isEmpty() || playlistUrlAtual != urlPlaylist) return@launch
            val atualizado = (catalogo ?: catalogoBase).copy(series = seriesXtream)
            catalogo = atualizado
            CatalogoCache.salvar(contexto, fingerprint, atualizado)
        }
    }

    suspend fun carregarCatalogo(configuracao: EvoluxConfig, indiceSolicitado: Int = playlistAtiva, forcar: Boolean = false): String? {
        if (carregandoCatalogo) return null
        val fontes = configuracao.playlistUrls.filter { it.startsWith("http://") || it.startsWith("https://") }
        if (fontes.isEmpty()) return "Nenhuma URL de playlist foi encontrada."
        val indice = indiceSolicitado.coerceIn(0, fontes.lastIndex)
        val urlPlaylist = fontes[indice]
        playlistUrlAtual = urlPlaylist
        val fingerprint = CatalogoCache.fingerprint(configuracao, urlPlaylist)
        carregandoCatalogo = true
        progressoCatalogo = 2
        segundosCatalogo = 0
        try {
            if (!forcar) {
                val cache = CatalogoCache.carregar(contexto, fingerprint)
                if (cache != null) {
                    progressoCatalogo = 98
                    catalogo = cache
                    playlistAtiva = indice
                    if (cache.series.none { it.id.startsWith("xtream_series_") }) {
                        carregarSeriesXtreamEmSegundoPlano(urlPlaylist, fingerprint, cache)
                    }
                    return null
                }
            }
            progressoCatalogo = 15
            val catalogoM3u = playlistRepository.carregar(urlPlaylist) { parcial, itensLidos ->
                withContext(Dispatchers.Main.immediate) {
                    if (playlistUrlAtual == urlPlaylist) {
                        // A Home abre com o primeiro lote; o restante da M3U continua em segundo plano.
                        catalogo = parcial
                        progressoCatalogo = minOf(96, maxOf(20, 15 + itensLidos / 1_000))
                    }
                }
            }
            progressoCatalogo = 98
            catalogo = catalogoM3u
            playlistAtiva = indice
            preferencias.edit().putInt(CHAVE_PLAYLIST_ATIVA, indice).apply()
            CatalogoCache.salvar(contexto, fingerprint, catalogoM3u)
            carregarSeriesXtreamEmSegundoPlano(urlPlaylist, fingerprint, catalogoM3u)
            return null
        } catch (erro: Exception) {
            return erro.message?.takeIf { it.isNotBlank() } ?: "Não foi possível interpretar o catálogo."
        } finally {
            carregandoCatalogo = false
        }
    }

    LaunchedEffect(carregandoCatalogo) {
        if (carregandoCatalogo) {
            while (isActive) {
                delay(1_000)
                segundosCatalogo++
                progressoCatalogo = minOf(97, maxOf(progressoCatalogo, 2 + segundosCatalogo))
            }
        }
    }

    suspend fun validarAcesso(macInformado: String, mostrarCarregando: Boolean = true) {
        if (validacaoEmAndamento) return
        validacaoEmAndamento = true
        if (mostrarCarregando || macAutorizado.isBlank()) estadoLogin = EstadoLoginMac.Carregando()
        try {
            when (val resultado = repository.buscarConfiguracao(macInformado)) {
                is ResultadoConfiguracao.Sucesso -> {
                    val novasFontes = resultado.configuracao.playlistUrls.filter {
                        it.startsWith("http://") || it.startsWith("https://")
                    }
                    val catalogoAtual = catalogo
                    val precisaCarregar = catalogoAtual == null ||
                        (catalogoAtual.canais.isEmpty() && catalogoAtual.filmes.isEmpty() && catalogoAtual.series.isEmpty()) ||
                        macAutorizado != resultado.configuracao.mac ||
                        playlistUrlAtual !in novasFontes
                    configuracaoAtual = resultado.configuracao
                    fontesConfiguradas = novasFontes
                    playlistAtiva = playlistAtiva.coerceIn(0, (novasFontes.size - 1).coerceAtLeast(0))
                    macAutorizado = resultado.configuracao.mac
                    preferencias.edit()
                        .putString(CHAVE_MAC_LOGICO, resultado.configuracao.mac)
                        .putBoolean(CHAVE_MAC_AUTORIZADO, true)
                        .apply()
                    if (novasFontes.isEmpty()) {
                        Toast.makeText(
                            contexto,
                            "MAC autorizado, mas nenhuma playlist foi encontrada no painel.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else if (precisaCarregar) {
                        // Não mostrar a Home com contadores zerados enquanto a playlist é baixada.
                        catalogo = null
                        escopo.launch {
                            val erroCatalogo = carregarCatalogo(resultado.configuracao, playlistAtiva)
                            if (erroCatalogo != null) {
                                Toast.makeText(contexto, erroCatalogo, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    estadoLogin = EstadoLoginMac.Ocioso
                }
                is ResultadoConfiguracao.Erro -> {
                    preferencias.edit().putBoolean(CHAVE_MAC_AUTORIZADO, false).apply()
                    estadoLogin = EstadoLoginMac.Erro(resultado.mensagem, resultado.detalhe)
                }
            }
        } finally {
            validacaoEmAndamento = false
        }
    }

    LaunchedEffect(macJaAutorizado, macLogico) {
        if (macJaAutorizado && macLogico.isNotBlank() && macAutorizado.isBlank()) {
            validarAcesso(macLogico)
        }
    }

    LaunchedEffect(macLogico) {
        while (isActive) {
            if (macAutorizado.isBlank()) {
                validarAcesso(macLogico, mostrarCarregando = true)
            } else {
                // Revalida a configuração e a playlist para reconhecer troca no painel.
                validarAcesso(macLogico, mostrarCarregando = false)
            }
            delay(5_000)
        }
    }

    LaunchedEffect(telaAtual, catalogo, carregandoCatalogo) {
        if (telaAtual == Tela.INICIO) {
            while (isActive) {
                if (carregandoCatalogo) {
                    delay(1_500)
                    continue
                }
                val jogosDaLista = jogosDaPlaylist(catalogo?.canais.orEmpty())
                if (jogosDaLista.isNotEmpty()) {
                    jogosDoDia = withTimeoutOrNull(4_000L) {
                        jogosDoDiaRepository.enriquecerEscudos(jogosDaLista)
                    } ?: jogosDaLista
                } else {
                    val jogosApi = jogosDoDiaRepository.carregarProximosJogos()
                    if (jogosDaPlaylist(catalogo?.canais.orEmpty()).isEmpty()) {
                        jogosDoDia = jogosApi
                    }
                }
                delay(30 * 60 * 1_000L)
            }
        }
    }

    LaunchedEffect(validacaoEmAndamento) {
        if (validacaoEmAndamento) {
            var segundos = 0
            while (isActive && validacaoEmAndamento) {
                delay(1_000)
                segundos++
                if (estadoLogin is EstadoLoginMac.Carregando) {
                    estadoLogin = EstadoLoginMac.Carregando(
                        porcentagem = minOf(99, maxOf(1, segundos * 5)),
                        segundos = segundos
                    )
                }
            }
        }
    }

    val aoTentarLogin: (String) -> Unit = { macInformado ->
        escopo.launch {
            validarAcesso(macInformado, mostrarCarregando = true)
        }
    }

    val abrirConteudo: (String, String) -> Unit = { titulo, url ->
        if (url.isBlank()) {
            Toast.makeText(contexto, "$titulo ainda não possui stream configurado", Toast.LENGTH_SHORT).show()
        } else {
            reproducao = Reproducao(titulo = titulo, streamUrl = url)
        }
    }
    val abrirMidiaDaHome: (Midia) -> Unit = { midia ->
        if (midia.tipo == TipoMidia.SERIE) {
            categoriaInicialSeries = midia.categoria
            telaAtual = Tela.SERIES
        } else {
            abrirConteudo(midia.titulo, midia.streamUrl)
        }
    }
    val abrirDestaqueDaHome: (Destaque) -> Unit = { destaque ->
        if (destaque.tipo == TipoMidia.SERIE) {
            categoriaInicialSeries = destaque.categoria
            telaAtual = Tela.SERIES
        } else {
            abrirConteudo(destaque.titulo, destaque.streamUrl)
        }
    }
    if (macAutorizado.isBlank() && estadoLogin !is EstadoLoginMac.Carregando) {
        MacLoginScreen(
            estado = estadoLogin,
            macInicial = macInicial,
            aoCopiarMac = { macParaCopiar ->
                val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("MAC do Evolux", macParaCopiar))
                Toast.makeText(contexto, "MAC copiado", Toast.LENGTH_SHORT).show()
            },
            aoTentarLogin = aoTentarLogin
        )
        return
    }

    if (catalogo == null) {
        CatalogoLoadingScreen(
            estado = estadoLogin,
            carregandoCatalogo = carregandoCatalogo,
            progressoCatalogo = progressoCatalogo,
            segundosCatalogo = segundosCatalogo
        )
        return
    }

    val catalogoAtual = catalogo ?: return
    reproducao?.let { atual ->
        PlayerScreen(
            titulo = atual.titulo,
            streamUrl = atual.streamUrl,
            aoFechar = { reproducao = null }
        )
        return
    }
    // O parser já entrega filmes e séries separados por group-title.
    // A preparação dos destaques pode percorrer milhares de itens; por isso fica fora da UI.
    val homePresentation by produceState<HomePresentation?>(initialValue = null, catalogoAtual) {
        value = withContext(Dispatchers.Default) {
            HomePresentation(
                destaques = gerarDestaques(catalogoAtual),
                fileirasEspeciais = gerarFileirasEspeciais(catalogoAtual)
            )
        }
    }
    val destaques = homePresentation?.destaques.orEmpty()
    val fileirasEspeciais = homePresentation?.fileirasEspeciais.orEmpty()
    val filmesDaHome = remember(catalogoAtual) { catalogoAtual.filmes.take(24).toList() }
    val seriesDaHome = remember(catalogoAtual) { catalogoAtual.series.take(24).toList() }
    val favoritos = remember { mutableStateListOf<Midia>() }

    LaunchedEffect(preferencias) {
        val idsSalvos = preferencias
            .getStringSet(CHAVE_FAVORITOS, emptySet())
            .orEmpty()
        favoritos.addAll(
            (catalogoAtual.filmes.asSequence() + catalogoAtual.series.asSequence())
                .filter { it.id in idsSalvos }
                .toList()
        )
    }

    val ehFavorito: (Midia) -> Boolean = { midia ->
        favoritos.any { it.id == midia.id }
    }
    val aoAlternarFavorito: (Midia) -> Unit = { midia ->
        val indice = favoritos.indexOfFirst { it.id == midia.id }
        if (indice >= 0) favoritos.removeAt(indice) else favoritos.add(midia)
        preferencias.edit()
            .putStringSet(CHAVE_FAVORITOS, favoritos.map { it.id }.toSet())
            .apply()
    }
    val ocultasLive = categoriasOcultas.filter { it.startsWith("live|") }.map { it.substringAfter('|') }.toSet()
    val ocultasFilmes = categoriasOcultas.filter { it.startsWith("filmes|") }.map { it.substringAfter('|') }.toSet()
    val ocultasSeries = categoriasOcultas.filter { it.startsWith("series|") }.map { it.substringAfter('|') }.toSet()
    val (categoriasCanais, categoriasFilmes, categoriasSeries) = remember(catalogoAtual) {
        Triple(
            catalogoAtual.canais.map { it.categoria.ifBlank { "TV ao vivo" } }.distinct().sorted(),
            catalogoAtual.filmes.map { it.categoria.ifBlank { "Sem categoria" } }.distinct().sorted(),
            catalogoAtual.series.map { it.categoria.ifBlank { "Séries" } }.distinct().sorted()
        )
    }
    val aoAlternarCategoriaOculta: (String, String) -> Unit = { secao, categoria ->
        val chave = "$secao|$categoria"
        categoriasOcultas = if (chave in categoriasOcultas) categoriasOcultas - chave else categoriasOcultas + chave
        preferencias.edit().putStringSet(CHAVE_CATEGORIAS_OCULTAS, categoriasOcultas).apply()
    }
    val aoMudarOrdem: (String, OrdemCatalogo) -> Unit = { secao, ordem ->
        ordens = ordens + (secao to ordem)
        val chave = when (secao) {
            "canais" -> CHAVE_ORDEM_CANAIS
            "filmes" -> CHAVE_ORDEM_FILMES
            else -> CHAVE_ORDEM_SERIES
        }
        preferencias.edit().putString(chave, ordem.name).apply()
    }
    val aoSelecionarPlaylist: (Int) -> Unit = { indice ->
        configuracaoAtual?.let { configuracao ->
            escopo.launch {
                val erro = carregarCatalogo(configuracao, indice, forcar = true)
                if (erro != null) Toast.makeText(contexto, erro, Toast.LENGTH_LONG).show()
            }
        }
    }
    val aoRecarregarCatalogo: () -> Unit = {
        configuracaoAtual?.let { configuracao ->
            escopo.launch {
                val erro = carregarCatalogo(configuracao, playlistAtiva, forcar = true)
                if (erro != null) Toast.makeText(contexto, erro, Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.evolux_background_futurista),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.82f)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xC90A0E1A))
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            TopNavBar(
                telaSelecionada = telaAtual,
                aoSelecionar = {
                    if (it == Tela.SERIES) categoriaInicialSeries = null
                    telaAtual = it
                }
            )

            when (telaAtual) {
            Tela.INICIO -> HomeScreen(
                destaques = destaques,
                canaisCount = catalogoAtual.canais.size,
                filmesCount = catalogoAtual.filmes.size,
                seriesCount = catalogoAtual.series.size,
                filmes = filmesDaHome,
                series = seriesDaHome,
                fileirasEspeciais = fileirasEspeciais,
                aoAbrirMidia = abrirMidiaDaHome,
                aoAssistirDestaque = abrirDestaqueDaHome,
                aoAbrirCanais = { telaAtual = Tela.TV_AO_VIVO },
                aoAbrirFilmes = { telaAtual = Tela.FILMES },
                aoAbrirSeries = { telaAtual = Tela.SERIES },
                jogosDoDia = jogosDoDia,
                aoAbrirJogos = { telaAtual = Tela.JOGOS },
                aoAbrirJogo = { jogo -> abrirConteudo("${jogo.timeCasaSigla} x ${jogo.timeVisitanteSigla}", jogo.streamUrl) },
                ehFavorito = ehFavorito,
                aoAlternarFavorito = aoAlternarFavorito
            )

            Tela.TV_AO_VIVO -> LiveTvScreen(
                canais = catalogoAtual.canais,
                aoAbrirCanal = { abrirConteudo(it.nome, it.streamUrl) },
                categoriasOcultas = ocultasLive,
                ordemInicial = ordens["canais"] ?: OrdemCatalogo.PADRAO,
                aoMudarOrdem = { aoMudarOrdem("canais", it) }
            )

            Tela.FILMES -> GradeMidiaScreen(
                titulo = "Filmes",
                itens = catalogoAtual.filmes,
                aoSelecionar = { abrirConteudo(it.titulo, it.streamUrl) },
                ehFavorito = ehFavorito,
                aoAlternarFavorito = aoAlternarFavorito,
                categoriasOcultas = ocultasFilmes,
                ordemInicial = ordens["filmes"] ?: OrdemCatalogo.PADRAO,
                aoMudarOrdem = { aoMudarOrdem("filmes", it) }
            )

            Tela.SERIES -> SeriesBrowserScreen(
                itens = catalogoAtual.series,
                categoriaInicial = categoriaInicialSeries,
                aoAssistir = { abrirConteudo(it.episodioNome ?: it.titulo, it.streamUrl) },
                categoriasOcultas = ocultasSeries,
                ordemInicial = ordens["series"] ?: OrdemCatalogo.PADRAO,
                aoMudarOrdem = { aoMudarOrdem("series", it) },
                carregarEpisodios = { serie ->
                    val url = playlistUrlAtual
                    if (url != null && XtreamRepository.pareceXtream(url)) {
                        xtreamRepository.carregarEpisodios(url, serie)
                    } else {
                        emptyList()
                    }
                }
            )

            Tela.JOGOS -> GamesScreen(
                jogos = jogosDoDia,
                aoAbrirJogo = { abrirConteudo("${it.timeCasaSigla} x ${it.timeVisitanteSigla}", it.streamUrl) }
            )

            Tela.FAVORITOS -> GradeMidiaScreen(
                titulo = "Favoritos",
                itens = favoritos,
                aoSelecionar = { abrirConteudo(it.titulo, it.streamUrl) },
                ehFavorito = ehFavorito,
                aoAlternarFavorito = aoAlternarFavorito,
                mensagemVazio = "Você ainda não adicionou nada aos favoritos."
            )

            Tela.CONFIGURACOES -> SettingsScreen(
                playlistUrls = fontesConfiguradas,
                playlistAtiva = playlistAtiva,
                aoSelecionarPlaylist = aoSelecionarPlaylist,
                aoRecarregarCatalogo = aoRecarregarCatalogo,
                categoriasCanais = categoriasCanais,
                categoriasFilmes = categoriasFilmes,
                categoriasSeries = categoriasSeries,
                categoriasOcultas = categoriasOcultas,
                aoAlternarCategoriaOculta = aoAlternarCategoriaOculta,
                ordens = ordens,
                aoMudarOrdem = aoMudarOrdem,
                aoTrocarMac = {
                    preferencias.edit().putBoolean(CHAVE_MAC_AUTORIZADO, false).apply()
                    macAutorizado = ""
                    estadoLogin = EstadoLoginMac.Ocioso
                    telaAtual = Tela.INICIO
                }
            )
        }
    }
}
}

private fun pertenceAFamiliaFilmes(categoria: String): Boolean {
    val normalizada = categoria.lowercase()
    return normalizada == "filmes" || normalizada.startsWith("filmes |") || normalizada.startsWith("filmes -")
}

private fun pertenceAFamiliaSeries(categoria: String): Boolean {
    val normalizada = categoria.lowercase()
    return normalizada == "series" || normalizada.startsWith("series |") || normalizada.startsWith("series -")
}
