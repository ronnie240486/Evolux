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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
import com.evolux.tv.data.normalizarConsulta
import com.evolux.tv.data.gerarDestaques
import com.evolux.tv.data.gerarFileirasEspeciais
import com.evolux.tv.ui.components.Tela
import com.evolux.tv.ui.components.TopNavBar
import com.evolux.tv.ui.screens.*
import com.evolux.tv.ui.theme.FundoEscuro
import com.evolux.tv.ui.theme.EvoluxTheme

private const val CHAVE_FAVORITOS = "favoritos_ids"
private const val CHAVE_CANAIS_FAVORITOS = "favoritos_canais_ids"
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

private fun chaveNomeSerie(midia: Midia): String {
    val nome = midia.serieNome?.takeIf { it.isNotBlank() } ?: midia.titulo
    return normalizarConsulta(nome)
        .replace("(?i)\\b(s|t|season|temporada)\\s*\\d{1,2}.*$".toRegex(), "")
        .replace("[^a-z0-9]+".toRegex(), "")
}

private fun criarPreviewHome(catalogo: PlaylistCatalog): PlaylistCatalog {
    return PlaylistCatalog(
        canais = catalogo.canais.take(48).toList(),
        filmes = catalogo.filmes.take(200).toList(),
        series = catalogo.series.take(200).toList(),
        truncado = catalogo.truncado
    )
}

class MainActivity : ComponentActivity() {
    private val atividadeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EvoluxTheme {
                EvoluxApp(atividadeScope)
            }
        }
    }

    override fun onDestroy() {
        atividadeScope.cancel()
        super.onDestroy()
    }
}

@Composable
fun EvoluxApp(atividadeScope: CoroutineScope) {
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
    // O catálogo precisa sobreviver às recomposições e trocas de tela.
    // O cancelamento ocorre somente quando a Activity é destruída.
    val escopo = atividadeScope
    var macAutorizado by remember { mutableStateOf("") }
    var catalogo by remember { mutableStateOf<PlaylistCatalog?>(null) }
    var catalogoPreview by remember { mutableStateOf<PlaylistCatalog?>(null) }
    var catalogoPronto by remember { mutableStateOf(false) }
    var homePronta by remember { mutableStateOf(false) }
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
    var restaurandoCatalogoCompleto by remember { mutableStateOf(false) }
    var tentativaRestauracaoCompleta by remember { mutableStateOf(false) }
    var progressoCatalogo by remember { mutableStateOf(1) }
    var segundosCatalogo by remember { mutableStateOf(0) }
    var reproducao by remember { mutableStateOf<Reproducao?>(null) }
    var jogosDoDia by remember { mutableStateOf<List<Jogo>>(emptyList()) }
    var canaisFavoritosIds by remember {
        mutableStateOf(preferencias.getStringSet(CHAVE_CANAIS_FAVORITOS, emptySet()).orEmpty())
    }
    var categoriaInicialSeries by remember { mutableStateOf<String?>(null) }
    var servicoInicialSeries by remember { mutableStateOf<String?>(null) }

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
        if (catalogoBase.series.isNotEmpty() && catalogoBase.series.all { it.id.startsWith("xtream_series_") }) return
        escopo.launch {
            val seriesXtream = withTimeoutOrNull(15_000L) {
                xtreamRepository.carregarSeries(urlPlaylist)
            }.orEmpty()
            if (seriesXtream.isEmpty() || playlistUrlAtual != urlPlaylist) return@launch

            // A M3U continua sendo a fonte dos episódios e das categorias reais.
            // Apenas a imagem/sinopse da série é enriquecida com o registro oficial Xtream.
            val oficiaisPorNome = seriesXtream
                .filter { it.imagemUrl.isNotBlank() }
                .associateBy { chaveNomeSerie(it) }
            val seriesEnriquecidas = catalogoBase.series.map { item ->
                val oficial = oficiaisPorNome[chaveNomeSerie(item)]
                if (oficial == null) item else item.copy(
                    imagemUrl = oficial.imagemUrl,
                    nota = item.nota ?: oficial.nota,
                    popularidade = item.popularidade ?: oficial.popularidade,
                    sinopse = item.sinopse.ifBlank { oficial.sinopse },
                    serieId = item.serieId ?: oficial.serieId,
                    serieNome = item.serieNome ?: oficial.serieNome
                )
            }
            val atualizado = catalogoBase.copy(series = seriesEnriquecidas)
            if (playlistUrlAtual == urlPlaylist) {
                catalogo = atualizado
                catalogoPreview = criarPreviewHome(atualizado)
                catalogoPronto = true
                homePronta = true
            }
            CatalogoCache.salvar(contexto, fingerprint, atualizado)
            escopo.launch(Dispatchers.IO) {
                CatalogoCache.salvarIndice(contexto, fingerprint, atualizado)
            }
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
        var previewPersistido = false
        try {
            if (!forcar) {
                // A abertura normal exige o cache detalhado completo. O índice leve é usado
                // apenas como apoio, nunca para liberar uma tela com conteúdo parcial.
                val cacheCompleto = CatalogoCache.carregar(contexto, fingerprint)
                if (cacheCompleto != null) {
                    progressoCatalogo = 100
                    catalogo = cacheCompleto
                    catalogoPreview = criarPreviewHome(cacheCompleto)
                    catalogoPronto = true
                    tentativaRestauracaoCompleta = true
                    homePronta = true
                    playlistAtiva = indice
                    carregarSeriesXtreamEmSegundoPlano(urlPlaylist, fingerprint, cacheCompleto)
                    return null
                }
                val cachePreview = CatalogoCache.carregarPreview(contexto, fingerprint)
                if (cachePreview != null) {
                    // Cache válido libera a Home sem reconstruir dezenas de milhares de objetos.
                    progressoCatalogo = 98
                    catalogo = null
                    catalogoPreview = criarPreviewHome(cachePreview)
                    catalogoPronto = false
                    tentativaRestauracaoCompleta = false
                    homePronta = cachePreview.canais.size >= 24 && cachePreview.filmes.size >= 24 && cachePreview.series.size >= 24
                    playlistAtiva = indice
                    return null
                }
            }
            progressoCatalogo = 15
            val catalogoM3u = playlistRepository.carregar(urlPlaylist) { parcial, itensLidos ->
                withContext(Dispatchers.Main.immediate) {
                    if (playlistUrlAtual == urlPlaylist) {
                        // Depois que a Home visual já está completa, não troque seus cards a cada lote.
                        // O catálogo integral continua sendo montado apenas para as telas internas/cache.
                        if (!homePronta) {
                            val previewAtual = criarPreviewHome(parcial)
                            val prontaAgora = parcial.canais.size >= 24 && parcial.filmes.size >= 24 && parcial.series.size >= 24
                            catalogoPreview = previewAtual
                            homePronta = prontaAgora
                            if (prontaAgora && !previewPersistido) {
                                previewPersistido = true
                                escopo.launch(Dispatchers.IO) {
                                    CatalogoCache.salvarPreview(contexto, fingerprint, previewAtual)
                                }
                            }
                        }
                        progressoCatalogo = if (homePronta) {
                            minOf(96, maxOf(35, 15 + itensLidos / 1_000))
                        } else {
                            minOf(30, maxOf(5, 15 + itensLidos / 1_000))
                        }
                    }
                }
            }
            progressoCatalogo = 98
            // Mantém o catálogo integral somente no arquivo; a Home continua leve.
            if (!homePronta || catalogoPreview == null) {
                catalogoPreview = criarPreviewHome(catalogoM3u)
            }
            if (!previewPersistido) {
                previewPersistido = true
                val previewFinal = catalogoPreview ?: criarPreviewHome(catalogoM3u)
                escopo.launch(Dispatchers.IO) {
                    CatalogoCache.salvarPreview(contexto, fingerprint, previewFinal)
                }
            }
            // Mantém o catálogo integral na sessão para as abas abrirem sem uma segunda leitura.
            // A Home continua usando somente catalogoPreview e não compõe estas listas completas.
            catalogo = catalogoM3u
            catalogoPronto = true
            tentativaRestauracaoCompleta = true
            homePronta = true
            playlistAtiva = indice
            preferencias.edit().putInt(CHAVE_PLAYLIST_ATIVA, indice).apply()
            CatalogoCache.salvar(contexto, fingerprint, catalogoM3u)
            carregarSeriesXtreamEmSegundoPlano(urlPlaylist, fingerprint, catalogoM3u)
            escopo.launch(Dispatchers.IO) {
                CatalogoCache.salvarIndice(contexto, fingerprint, catalogoM3u)
            }
            return null
        } catch (erro: Exception) {
            return erro.message?.takeIf { it.isNotBlank() } ?: "Não foi possível interpretar o catálogo."
        } finally {
            carregandoCatalogo = false
        }
    }

    suspend fun restaurarCatalogoCompletoDoCache() {
        if (catalogoPronto || restaurandoCatalogoCompleto || tentativaRestauracaoCompleta) return
        val configuracao = configuracaoAtual ?: return
        val urlPlaylist = playlistUrlAtual ?: return
        val fingerprint = CatalogoCache.fingerprint(configuracao, urlPlaylist)
        restaurandoCatalogoCompleto = true
        carregandoCatalogo = true
        progressoCatalogo = 98
        var precisaRecarregar = false
        try {
            val completo = CatalogoCache.carregar(contexto, fingerprint)
            if (completo != null && playlistUrlAtual == urlPlaylist) {
                catalogo = completo
                catalogoPronto = true
                tentativaRestauracaoCompleta = true
                if (completo.series.none { it.id.startsWith("xtream_series_") }) {
                    carregarSeriesXtreamEmSegundoPlano(urlPlaylist, fingerprint, completo)
                }
                escopo.launch(Dispatchers.IO) {
                    CatalogoCache.salvarIndice(contexto, fingerprint, completo)
                }
            } else if (playlistUrlAtual == urlPlaylist) {
                // Evita repetir infinitamente uma restauração que falhou.
                tentativaRestauracaoCompleta = true
                precisaRecarregar = true
            }
        } finally {
            restaurandoCatalogoCompleto = false
            carregandoCatalogo = false
        }
        if (precisaRecarregar && playlistUrlAtual == urlPlaylist) {
            Toast.makeText(contexto, "Não foi possível abrir o cache completo. Recarregando a lista uma vez.", Toast.LENGTH_LONG).show()
            val erro = carregarCatalogo(configuracao, playlistAtiva, forcar = true)
            if (erro != null) Toast.makeText(contexto, erro, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(catalogoPronto, catalogoPreview, playlistUrlAtual, carregandoCatalogo, tentativaRestauracaoCompleta) {
        if (catalogoPreview != null && !catalogoPronto && !carregandoCatalogo && !tentativaRestauracaoCompleta) {
            restaurarCatalogoCompletoDoCache()
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
                    val precisaCarregar = (catalogoAtual == null && catalogoPreview == null) ||
                        (catalogoAtual != null && catalogoAtual.canais.isEmpty() && catalogoAtual.filmes.isEmpty() && catalogoAtual.series.isEmpty()) ||
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
                        catalogoPreview = null
                        catalogoPronto = false
                        tentativaRestauracaoCompleta = false
                        homePronta = false
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

    LaunchedEffect(telaAtual, catalogo, carregandoCatalogo, homePronta) {
        if (telaAtual == Tela.INICIO) {
            delay(5_000L)
            while (isActive) {
                if (!homePronta || carregandoCatalogo) {
                    delay(1_500)
                    continue
                }
                val jogosDaLista = withContext(Dispatchers.Default) {
                    jogosDaPlaylist(catalogo?.canais.orEmpty())
                }
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
            servicoInicialSeries = null
            telaAtual = Tela.SERIES
        } else {
            abrirConteudo(midia.titulo, midia.streamUrl)
        }
    }
    val abrirDestaqueDaHome: (Destaque) -> Unit = { destaque ->
        if (destaque.tipo == TipoMidia.SERIE) {
            categoriaInicialSeries = destaque.categoria
            servicoInicialSeries = null
            telaAtual = Tela.SERIES
        } else {
            abrirConteudo(destaque.titulo, destaque.streamUrl)
        }
    }
    fun chaveServicoAtalho(nome: String): String {
        return com.evolux.tv.data.normalizarConsulta(nome)
            .replace("plus", "")
            .replace("mais", "")
            .replace("+", "")
            .replace("[^a-z0-9]".toRegex(), "")
    }
    fun tokensServicoAtalho(nome: String): List<String> {
        val chave = chaveServicoAtalho(nome)
        return when {
            "max" in chave || "hbo" in chave -> listOf("max", "hbo")
            "prime" in chave || "amazon" in chave -> listOf("prime", "amazon")
            "star" in chave -> listOf("star")
            else -> listOf(chave)
        }
    }
    fun categoriaRealDoServico(nomeServico: String): String? {
        val fonte = catalogo ?: catalogoPreview ?: return null
        val tokens = tokensServicoAtalho(nomeServico)
        return fonte.series.asSequence()
            .map { it.categoria.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .firstOrNull { categoria ->
                val chave = chaveServicoAtalho(categoria)
                tokens.any { token -> token.isNotBlank() && chave.contains(token) }
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

    val previewDisponivel = homePronta && catalogoPreview != null
    // A Home só é liberada depois que o catálogo detalhado completo está em memória.
    // O preview existe apenas durante a carga e nunca libera a interface principal.
    if (catalogo == null || !catalogoPronto || !previewDisponivel) {
        CatalogoLoadingScreen(
            estado = estadoLogin,
            carregandoCatalogo = carregandoCatalogo,
            progressoCatalogo = progressoCatalogo,
            segundosCatalogo = segundosCatalogo
        )
        return
    }

    // A Home e as telas internas usam fontes separadas para evitar que a Home observe o catálogo integral.
    val catalogoHome: PlaylistCatalog = catalogoPreview ?: return
    // A Home permanece leve; apenas as telas internas recebem o catálogo completo.
    val catalogoAtual: PlaylistCatalog = if (telaAtual == Tela.INICIO) {
        catalogoHome
    } else {
        catalogo ?: catalogoHome
    }
    // Chave O(1): nunca comparar estruturalmente as listas completas em cada recomposição.
    val catalogoChave = System.identityHashCode(catalogoAtual)
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
    val homePresentation by produceState<HomePresentation?>(initialValue = null, catalogoHome) {
        value = withContext(Dispatchers.Default) {
            HomePresentation(
                destaques = gerarDestaques(catalogoHome),
                fileirasEspeciais = gerarFileirasEspeciais(catalogoHome)
            )
        }
    }
    val destaques = homePresentation?.destaques.orEmpty()
    val fileirasEspeciais = homePresentation?.fileirasEspeciais.orEmpty()
    val filmesDaHome = remember(catalogoHome) { catalogoHome.filmes.take(24).toList() }
    val seriesDaHome = remember(catalogoHome) { catalogoHome.series.take(24).toList() }
    val favoritos = remember { mutableStateListOf<Midia>() }

    LaunchedEffect(preferencias, catalogoChave) {
        val idsSalvos = withContext(Dispatchers.IO) {
            preferencias.getStringSet(CHAVE_FAVORITOS, emptySet()).orEmpty()
        }
        val favoritosEncontrados = withContext(Dispatchers.Default) {
            (catalogoAtual.filmes.asSequence() + catalogoAtual.series.asSequence())
                .filter { it.id in idsSalvos }
                .toList()
        }
        favoritos.clear()
        favoritos.addAll(favoritosEncontrados)
    }

    val canaisFavoritos = remember(catalogoChave, canaisFavoritosIds) {
        catalogoAtual.canais.filter { it.id in canaisFavoritosIds }
    }
    val aoAlternarFavoritoCanal: (com.evolux.tv.data.Canal) -> Unit = { canal ->
        val novosIds = if (canal.id in canaisFavoritosIds) {
            canaisFavoritosIds - canal.id
        } else {
            canaisFavoritosIds + canal.id
        }
        canaisFavoritosIds = novosIds
        preferencias.edit().putStringSet(CHAVE_CANAIS_FAVORITOS, novosIds).apply()
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
    val categorias by produceState<Triple<List<String>, List<String>, List<String>>>(
        initialValue = Triple(emptyList(), emptyList(), emptyList()),
        key1 = catalogoChave
    ) {
        value = withContext(Dispatchers.Default) {
            Triple(
                catalogoAtual.canais.map { it.categoria.ifBlank { "TV ao vivo" } }.distinct().sorted(),
                catalogoAtual.filmes.map { it.categoria.ifBlank { "Sem categoria" } }.distinct().sorted(),
                catalogoAtual.series.map { it.categoria.ifBlank { "Séries" } }.distinct().sorted()
            )
        }
    }
    val categoriasCanais = categorias.first
    val categoriasFilmes = categorias.second
    val categoriasSeries = categorias.third
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
                    if (it == Tela.SERIES) {
                        categoriaInicialSeries = null
                        servicoInicialSeries = null
                    }
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
                aoAbrirSeries = {
                    categoriaInicialSeries = null
                    servicoInicialSeries = null
                    telaAtual = Tela.SERIES
                },
                aoAbrirServico = { servico ->
                    val categoriaReal = categoriaRealDoServico(servico)
                    if (categoriaReal != null) {
                        categoriaInicialSeries = categoriaReal
                        servicoInicialSeries = null
                        telaAtual = Tela.SERIES
                    } else {
                        Toast.makeText(contexto, "Categoria de $servico não encontrada na lista.", Toast.LENGTH_SHORT).show()
                    }
                },
                jogosDoDia = jogosDoDia,
                aoAbrirJogos = { telaAtual = Tela.JOGOS },
                aoAbrirJogo = { jogo -> abrirConteudo("${jogo.timeCasaSigla} x ${jogo.timeVisitanteSigla}", jogo.streamUrl) },
                ehFavorito = ehFavorito,
                aoAlternarFavorito = aoAlternarFavorito
            )

            Tela.TV_AO_VIVO -> LiveTvScreen(
                canais = catalogoAtual.canais,
                canaisFavoritos = canaisFavoritos,
                aoAbrirCanal = { abrirConteudo(it.nome, it.streamUrl) },
                aoAlternarFavorito = aoAlternarFavoritoCanal,
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
                servicoInicial = servicoInicialSeries,
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
