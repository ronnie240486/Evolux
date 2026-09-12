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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.evolux.tv.R
import com.evolux.tv.data.EvoluxRepository
import com.evolux.tv.data.EvoluxConfig
import com.evolux.tv.data.CatalogoCache
import com.evolux.tv.data.Canal
import com.evolux.tv.data.Destaque
import com.evolux.tv.data.MacAddressUtils
import com.evolux.tv.data.PlaylistCatalog
import com.evolux.tv.data.PlaylistRepository
import com.evolux.tv.data.Midia
import com.evolux.tv.data.OrdemCatalogo
import com.evolux.tv.data.RenciaApiClient
import com.evolux.tv.data.ResultadoConfiguracao
import com.evolux.tv.data.TipoMidia
import com.evolux.tv.data.XtreamRepository
import com.evolux.tv.data.gerarDestaques
import com.evolux.tv.data.gerarFileirasEspeciais
import com.evolux.tv.ui.components.Tela
import com.evolux.tv.ui.components.TopNavBar
import com.evolux.tv.ui.screens.*
import com.evolux.tv.ui.theme.FundoEscuro
import com.evolux.tv.ui.theme.EvoluxTheme
import com.evolux.tv.ui.theme.Dourado

private const val CHAVE_FAVORITOS = "favoritos_ids"
private const val CHAVE_MAC_LOGICO = "mac_logico_evolux"
private const val CHAVE_MAC_AUTORIZADO = "mac_autorizado_confirmado"
private const val CHAVE_PLAYLIST_ATIVA = "playlist_ativa"
private const val CHAVE_CATEGORIAS_OCULTAS = "categorias_ocultas"
private const val CHAVE_ORDEM_CANAIS = "ordem_canais"
private const val CHAVE_ORDEM_FILMES = "ordem_filmes"
private const val CHAVE_ORDEM_SERIES = "ordem_series"
private const val CHAVE_PIN_ADULTO = "pin_adulto"
private const val CHAVE_ORDEM_CAT_CANAIS = "ordem_cat_canais"
private const val CHAVE_ORDEM_CAT_FILMES = "ordem_cat_filmes"
private const val CHAVE_ORDEM_CAT_SERIES = "ordem_cat_series"

private data class Reproducao(
    val titulo: String,
    val streamUrl: String,
    val canal: Canal? = null
)

private fun lerOrdem(valor: String?): OrdemCatalogo = runCatching {
    OrdemCatalogo.valueOf(valor.orEmpty())
}.getOrDefault(OrdemCatalogo.PADRAO)

private fun lerListaOrdenada(valor: String?): List<String> =
    valor.orEmpty().split('|').map { it.trim() }.filter { it.isNotBlank() }

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
    val renciaApi = remember { RenciaApiClient(appId = "evolux") }
    val xtreamRepository = remember { XtreamRepository() }
    val escopo = rememberCoroutineScope()
    var macAutorizado by remember { mutableStateOf("") }
    var catalogo by remember { mutableStateOf<PlaylistCatalog?>(null) }
    var configuracaoAtual by remember { mutableStateOf<EvoluxConfig?>(null) }
    var fontesConfiguradas by remember { mutableStateOf<List<String>>(emptyList()) }
    var playlistAtiva by remember { mutableStateOf(preferencias.getInt(CHAVE_PLAYLIST_ATIVA, 0)) }
    var playlistUrlAtual by remember { mutableStateOf<String?>(null) }
    var categoriasOcultas by remember { mutableStateOf(preferencias.getStringSet(CHAVE_CATEGORIAS_OCULTAS, emptySet()).orEmpty()) }
    var pinAdulto by remember { mutableStateOf(preferencias.getString(CHAVE_PIN_ADULTO, null)) }
    var ordemCategoriasCanais by remember { mutableStateOf(lerListaOrdenada(preferencias.getString(CHAVE_ORDEM_CAT_CANAIS, null))) }
    var ordemCategoriasFilmes by remember { mutableStateOf(lerListaOrdenada(preferencias.getString(CHAVE_ORDEM_CAT_FILMES, null))) }
    var ordemCategoriasSeries by remember { mutableStateOf(lerListaOrdenada(preferencias.getString(CHAVE_ORDEM_CAT_SERIES, null))) }
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
    var reproducao by remember { mutableStateOf<Reproducao?>(null) }

    BackHandler(enabled = reproducao != null || telaAtual != Tela.INICIO) {
        if (reproducao != null) {
            reproducao = null
        } else {
            telaAtual = Tela.INICIO
        }
    }

    suspend fun carregarCatalogo(configuracao: EvoluxConfig, indiceSolicitado: Int = playlistAtiva, forcar: Boolean = false): String? {
        val fontes = configuracao.playlistUrls.filter { it.startsWith("http://") || it.startsWith("https://") }
        if (fontes.isEmpty()) return "Nenhuma URL de playlist foi encontrada."
        val indice = indiceSolicitado.coerceIn(0, fontes.lastIndex)
        val urlPlaylist = fontes[indice]
        playlistUrlAtual = urlPlaylist
        val fingerprint = CatalogoCache.fingerprint(configuracao, urlPlaylist)
        carregandoCatalogo = true
        if (estadoLogin is EstadoLoginMac.Carregando) {
            val atual = estadoLogin as EstadoLoginMac.Carregando
            estadoLogin = atual.copy(etapa = "Baixando lista de canais...")
        }
        try {
            if (!forcar) {
                val cache = CatalogoCache.carregar(contexto, fingerprint)
                if (cache != null) {
                    catalogo = cache
                    playlistAtiva = indice
                    return null
                }
            }
            val catalogoM3u = playlistRepository.carregar(
                urlPlaylist,
                aoProgresso = { lidos, total ->
                    val lidosMb = lidos / 1024.0 / 1024.0
                    val etapaTexto = if (total != null) {
                        val totalMb = total / 1024.0 / 1024.0
                        "Baixando lista de canais... %.1f/%.1f MB".format(Locale.ROOT, lidosMb, totalMb)
                    } else {
                        "Baixando lista de canais... %.1f MB".format(Locale.ROOT, lidosMb)
                    }
                    if (estadoLogin is EstadoLoginMac.Carregando) {
                        val atualEstado = estadoLogin as EstadoLoginMac.Carregando
                        estadoLogin = atualEstado.copy(etapa = etapaTexto)
                    }
                },
                aoParcial = { parcial ->
                    // Mostra a interface assim que os primeiros itens chegarem e continua
                    // atualizando a cada novo lote, em vez de travar no primeiro pedaço.
                    catalogo = parcial
                }
            )
            val seriesXtream = if (XtreamRepository.pareceXtream(urlPlaylist)) {
                xtreamRepository.carregarSeries(urlPlaylist)
            } else {
                emptyList()
            }
            val novoCatalogo = if (seriesXtream.isNotEmpty()) {
                catalogoM3u.copy(series = seriesXtream)
            } else {
                catalogoM3u
            }
            catalogo = novoCatalogo
            playlistAtiva = indice
            preferencias.edit().putInt(CHAVE_PLAYLIST_ATIVA, indice).apply()
            CatalogoCache.salvar(contexto, fingerprint, novoCatalogo)
            return null
        } catch (erro: Exception) {
            return erro.message?.takeIf { it.isNotBlank() } ?: "Não foi possível interpretar o catálogo."
        } finally {
            carregandoCatalogo = false
        }
    }

    suspend fun validarAcesso(macInformado: String, mostrarCarregando: Boolean = true) {
        if (validacaoEmAndamento) return
        validacaoEmAndamento = true
        if (mostrarCarregando || macAutorizado.isBlank()) {
            estadoLogin = EstadoLoginMac.Carregando(etapa = "Validando MAC no servidor...")
        }
        try {
            when (val resultado = repository.buscarConfiguracao(macInformado)) {
                is ResultadoConfiguracao.Sucesso -> {
                    configuracaoAtual = resultado.configuracao
                    fontesConfiguradas = resultado.configuracao.playlistUrls.filter { it.startsWith("http://") || it.startsWith("https://") }
                    playlistAtiva = playlistAtiva.coerceIn(0, (fontesConfiguradas.size - 1).coerceAtLeast(0))
                    if (estadoLogin is EstadoLoginMac.Carregando) {
                        estadoLogin = EstadoLoginMac.Carregando(
                            porcentagem = 35,
                            segundos = (estadoLogin as EstadoLoginMac.Carregando).segundos,
                            etapa = "Verificando playlist..."
                        )
                    }
                    val erroCatalogo = carregarCatalogo(resultado.configuracao, playlistAtiva)
                    if (erroCatalogo == null) {
                        macAutorizado = resultado.configuracao.mac
                        preferencias.edit()
                            .putString(CHAVE_MAC_LOGICO, resultado.configuracao.mac)
                            .putBoolean(CHAVE_MAC_AUTORIZADO, true)
                            .apply()
                        estadoLogin = EstadoLoginMac.Ocioso
                    } else {
                        estadoLogin = EstadoLoginMac.Erro(
                            "Lista indisponível ou credenciais inválidas",
                            erroCatalogo
                        )
                    }
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

    var avisoAtual by remember { mutableStateOf<RenciaApiClient.Aviso?>(null) }
    var vencimentoAtual by remember { mutableStateOf<RenciaApiClient.Vencimento?>(null) }
    var atualizacaoDisponivel by remember { mutableStateOf<RenciaApiClient.InfoAtualizacao?>(null) }
    val ultimoModalVencimentoKey = remember(contexto) {
        contexto.getSharedPreferences("evolux_prefs", android.content.Context.MODE_PRIVATE)
    }

    // Checa atualização uma vez, assim que o MAC for validado.
    LaunchedEffect(macAutorizado) {
        if (macAutorizado.isNotBlank()) {
            val info = renciaApi.checarAtualizacao(macAutorizado)
            if (info != null && info.updateAvailable) {
                atualizacaoDisponivel = info
            }
        }
    }

    // Ciclo de 60s: heartbeat + avisos/sincronização + comandos remotos (seção 4.2 do contrato).
    LaunchedEffect(macAutorizado) {
        if (macAutorizado.isBlank()) return@LaunchedEffect
        while (isActive) {
            val conteudoAtual = reproducao?.titulo
            renciaApi.heartbeat(macAutorizado, conteudoAtual)

            val notificacoes = renciaApi.listNotifications(macAutorizado)
            if (notificacoes != null) {
                if (notificacoes.playlistSyncRequired && !carregandoCatalogo) {
                    configuracaoAtual?.let { configuracao ->
                        carregarCatalogo(configuracao, playlistAtiva, forcar = true)
                    }
                }
                notificacoes.expiration?.let { venc ->
                    if (venc.showModal && venc.modalKey != null) {
                        val jaExibido = ultimoModalVencimentoKey.getString("ultimo_modal_vencimento", null)
                        if (jaExibido != venc.modalKey) {
                            vencimentoAtual = venc
                        }
                    }
                }
                if (avisoAtual == null) {
                    avisoAtual = notificacoes.notifications.firstOrNull { !it.acknowledged }
                }
            }

            renciaApi.buscarComandosRemotos(macAutorizado).forEach { comando ->
                when (comando.command) {
                    "reload_playlist", "sync_playlist" -> {
                        configuracaoAtual?.let { configuracao ->
                            carregarCatalogo(configuracao, playlistAtiva, forcar = true)
                        }
                        renciaApi.confirmarComandoRemoto(macAutorizado, comando.id, "executed")
                    }
                    "logout" -> {
                        val macParaAck = macAutorizado
                        preferencias.edit().putBoolean(CHAVE_MAC_AUTORIZADO, false).apply()
                        macAutorizado = ""
                        estadoLogin = EstadoLoginMac.Ocioso
                        telaAtual = Tela.INICIO
                        renciaApi.confirmarComandoRemoto(macParaAck, comando.id, "executed")
                    }
                    // Comando não reconhecido pelo APK: não confirma (nem executed, nem failed),
                    // conforme a seção 5.2 do contrato.
                }
            }

            delay(60_000)
        }
    }

    LaunchedEffect(macLogico) {
        var tentativasFalhas = 0
        while (isActive) {
            if (macAutorizado.isBlank()) {
                if (tentativasFalhas >= 2) {
                    // Já tentou 3x (1 inicial + 2 automáticas) e falhou: para de tentar
                    // sozinho e deixa a tela de erro com botão manual (aoTentarLogin).
                    break
                }
                validarAcesso(macLogico, mostrarCarregando = true)
                if (estadoLogin is EstadoLoginMac.Erro) {
                    tentativasFalhas++
                } else {
                    tentativasFalhas = 0
                }
                delay(5_000)
            } else {
                // Já autorizado: revalida em background bem mais espaçado
                // (só pra pegar troca de playlist/painel), sem travar a UI.
                validarAcesso(macLogico, mostrarCarregando = false)
                delay(300_000)
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
                        segundos = segundos,
                        etapa = (estadoLogin as EstadoLoginMac.Carregando).etapa
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

    val abrirConteudo: (String, String, Canal?) -> Unit = { titulo, url, canal ->
        if (url.isBlank()) {
            Toast.makeText(contexto, "$titulo ainda não possui stream configurado", Toast.LENGTH_SHORT).show()
        } else {
            reproducao = Reproducao(titulo = titulo, streamUrl = url, canal = canal)
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
        CatalogoLoadingScreen(estadoLogin)
        return
    }

    var lembretesEpg by remember { mutableStateOf(listOf<Pair<Canal, XtreamRepository.ProgramaEpg>>()) }
    var contagemLembrete by remember { mutableStateOf<Pair<Canal, XtreamRepository.ProgramaEpg>?>(null) }
    var segundosContagem by remember { mutableStateOf(7) }

    LaunchedEffect(lembretesEpg.size) {
        while (isActive && lembretesEpg.isNotEmpty()) {
            val agora = System.currentTimeMillis()
            val vencido = lembretesEpg.firstOrNull { (_, programa) -> agora >= programa.inicioMillis }
            if (vencido != null) {
                lembretesEpg = lembretesEpg - vencido
                contagemLembrete = vencido
                segundosContagem = 7
                for (s in 7 downTo 1) {
                    segundosContagem = s
                    delay(1_000)
                }
                reproducao = Reproducao(titulo = vencido.first.nome, streamUrl = vencido.first.streamUrl, canal = vencido.first)
                contagemLembrete = null
            }
            delay(5_000)
        }
    }

    val mostrarContagemLembrete: @Composable () -> Unit = {
        contagemLembrete?.let { (canal, programa) ->
            Box(modifier = Modifier.fillMaxSize().background(Color(0xF0060912)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Vai começar agora", color = Dourado, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(programa.titulo, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("em ${canal.nome}", color = Color(0xFFB8C0D4))
                    Spacer(Modifier.height(24.dp))
                    Text("$segundosContagem", color = Dourado, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(24.dp))
                    Row {
                        Button(onClick = {
                            contagemLembrete = null
                        }) { Text("Cancelar") }
                    }
                }
            }
        }
    }

    val catalogoAtual = catalogo ?: return
    reproducao?.let { atual ->
        PlayerScreen(
            titulo = atual.titulo,
            streamUrl = atual.streamUrl,
            canal = atual.canal,
            urlXtream = playlistUrlAtual,
            xtreamRepository = xtreamRepository,
            lembretesAtivos = lembretesEpg.map { it.second.id }.toSet(),
            aoAgendarLembrete = { canalAlvo, programa ->
                if (lembretesEpg.none { it.second.id == programa.id }) {
                    lembretesEpg = lembretesEpg + (canalAlvo to programa)
                } else {
                    lembretesEpg = lembretesEpg.filterNot { it.second.id == programa.id }
                }
            },
            aoFechar = { reproducao = null },
            aoFalhaDeRede = {
                if (macAutorizado.isNotBlank()) {
                    escopo.launch {
                        val resultado = renciaApi.reportarFalhaReproducao(macAutorizado, playlistAtiva + 1)
                        if (resultado?.switchApplied == true || resultado?.playlistSyncRequired == true) {
                            configuracaoAtual?.let { configuracao ->
                                carregarCatalogo(configuracao, playlistAtiva, forcar = true)
                            }
                        }
                    }
                }
            }
        )
        mostrarContagemLembrete()
        return
    }
    val catalogoApresentacao = remember(catalogoAtual) {
        catalogoAtual.copy(
            filmes = catalogoAtual.filmes.filter { pertenceAFamiliaFilmes(it.categoria) },
            series = catalogoAtual.series.filter { pertenceAFamiliaSeries(it.categoria) }
        )
    }

    var serieSelecionadaFora by remember { mutableStateOf<GrupoSerie?>(null) }

    val abrirMidiaOuSerie: (Midia) -> Unit = { midia ->
        if (midia.tipo == TipoMidia.SERIE) {
            val url = playlistUrlAtual
            if (url != null && XtreamRepository.pareceXtream(url)) {
                escopo.launch {
                    try {
                        val episodios = xtreamRepository.carregarEpisodios(url, midia)
                        serieSelecionadaFora = if (episodios.isNotEmpty()) {
                            agruparGrupoSerie(episodios, episodios.first())
                        } else {
                            agruparGrupoSerie(catalogoAtual.series, midia)
                        }
                    } catch (erro: Exception) {
                        runCatching { serieSelecionadaFora = agruparGrupoSerie(catalogoAtual.series, midia) }
                        Toast.makeText(contexto, "Não consegui carregar os episódios agora.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                runCatching { serieSelecionadaFora = agruparGrupoSerie(catalogoAtual.series, midia) }
                    .onFailure { Toast.makeText(contexto, "Não consegui abrir essa série.", Toast.LENGTH_SHORT).show() }
            }
        } else {
            abrirConteudo(midia.titulo, midia.streamUrl, null)
        }
    }

    val abrirDestaque: (Destaque) -> Unit = { destaque ->
        val midiaRef = if (destaque.tipo == TipoMidia.SERIE) {
            catalogoAtual.series.firstOrNull { it.id == destaque.midiaId }
        } else null
        if (midiaRef != null) {
            abrirMidiaOuSerie(midiaRef)
        } else {
            abrirConteudo(destaque.titulo, destaque.streamUrl, null)
        }
    }

    val todasAsMidias = remember(catalogoApresentacao) {
        catalogoApresentacao.filmes + catalogoApresentacao.series
    }
    val destaques = remember(catalogoApresentacao) {
        gerarDestaques(catalogoApresentacao)
    }
    val fileirasEspeciais = remember(catalogoApresentacao) {
        gerarFileirasEspeciais(catalogoApresentacao)
    }
    val favoritos = remember { mutableStateListOf<Midia>() }

    LaunchedEffect(preferencias) {
        val idsSalvos = preferencias
            .getStringSet(CHAVE_FAVORITOS, emptySet())
            .orEmpty()
        favoritos.addAll(todasAsMidias.filter { it.id in idsSalvos })
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
    val categoriasCanais = catalogoAtual.canais.map { it.categoria.ifBlank { "TV ao vivo" } }.distinct().sorted()
    val categoriasFilmes = catalogoAtual.filmes.map { it.categoria.ifBlank { "Sem categoria" } }.distinct().sorted()
    val categoriasSeries = catalogoAtual.series.map { it.categoria.ifBlank { "Séries" } }.distinct().sorted()
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
    val aoSalvarOrdemCategorias: (String, List<String>) -> Unit = { secao, nova ->
        val chave = when (secao) {
            "canais" -> CHAVE_ORDEM_CAT_CANAIS
            "filmes" -> CHAVE_ORDEM_CAT_FILMES
            else -> CHAVE_ORDEM_CAT_SERIES
        }
        when (secao) {
            "canais" -> ordemCategoriasCanais = nova
            "filmes" -> ordemCategoriasFilmes = nova
            else -> ordemCategoriasSeries = nova
        }
        preferencias.edit().putString(chave, nova.joinToString("|")).apply()
    }
    val aoSalvarPin: (String?) -> Unit = { novoPin ->
        pinAdulto = novoPin
        preferencias.edit().putString(CHAVE_PIN_ADULTO, novoPin).apply()
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
                aoSelecionar = { telaAtual = it }
            )

            when (telaAtual) {
            Tela.INICIO -> HomeScreen(
                destaques = destaques,
                canaisCount = catalogoApresentacao.canais.size,
                filmesCount = catalogoApresentacao.filmes.size,
                seriesCount = catalogoApresentacao.series.size,
                filmes = catalogoApresentacao.filmes,
                series = catalogoApresentacao.series,
                fileirasEspeciais = fileirasEspeciais,
                aoAbrirMidia = abrirMidiaOuSerie,
                aoAssistirDestaque = abrirDestaque,
                aoAbrirCanais = { telaAtual = Tela.TV_AO_VIVO },
                aoAbrirFilmes = { telaAtual = Tela.FILMES },
                aoAbrirSeries = { telaAtual = Tela.SERIES },
                ehFavorito = ehFavorito,
                aoAlternarFavorito = aoAlternarFavorito
            )

            Tela.TV_AO_VIVO -> LiveTvScreen(
                canais = catalogoAtual.canais,
                aoAbrirCanal = { abrirConteudo(it.nome, it.streamUrl, it) },
                categoriasOcultas = ocultasLive,
                ordemInicial = ordens["canais"] ?: OrdemCatalogo.PADRAO,
                aoMudarOrdem = { aoMudarOrdem("canais", it) },
                ordemCategoriasCustom = ordemCategoriasCanais,
                pinAdulto = pinAdulto
            )

            Tela.FILMES -> GradeMidiaScreen(
                titulo = "Filmes",
                itens = catalogoApresentacao.filmes,
                aoSelecionar = { abrirConteudo(it.titulo, it.streamUrl, null) },
                ehFavorito = ehFavorito,
                aoAlternarFavorito = aoAlternarFavorito,
                categoriasOcultas = ocultasFilmes,
                ordemInicial = ordens["filmes"] ?: OrdemCatalogo.PADRAO,
                aoMudarOrdem = { aoMudarOrdem("filmes", it) },
                ordemCategoriasCustom = ordemCategoriasFilmes,
                pinAdulto = pinAdulto
            )

            Tela.SERIES -> SeriesBrowserScreen(
                itens = catalogoApresentacao.series,
                aoAssistir = { abrirConteudo(it.episodioNome ?: it.titulo, it.streamUrl, null) },
                categoriasOcultas = ocultasSeries,
                ordemInicial = ordens["series"] ?: OrdemCatalogo.PADRAO,
                aoMudarOrdem = { aoMudarOrdem("series", it) },
                ordemCategoriasCustom = ordemCategoriasSeries,
                pinAdulto = pinAdulto,
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
                jogos = emptyList(),
                aoAbrirJogo = { abrirConteudo("${it.timeCasaSigla} x ${it.timeVisitanteSigla}", it.streamUrl, null) }
            )

            Tela.FAVORITOS -> GradeMidiaScreen(
                titulo = "Favoritos",
                itens = favoritos,
                aoSelecionar = abrirMidiaOuSerie,
                ehFavorito = ehFavorito,
                aoAlternarFavorito = aoAlternarFavorito,
                mensagemVazio = "Você ainda não adicionou nada aos favoritos.",
                pinAdulto = pinAdulto
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
                ordemCategoriasCanais = ordemCategoriasCanais,
                ordemCategoriasFilmes = ordemCategoriasFilmes,
                ordemCategoriasSeries = ordemCategoriasSeries,
                aoSalvarOrdemCategorias = aoSalvarOrdemCategorias,
                pinAdulto = pinAdulto,
                aoSalvarPin = aoSalvarPin,
                aoTrocarMac = {
                    preferencias.edit().putBoolean(CHAVE_MAC_AUTORIZADO, false).apply()
                    macAutorizado = ""
                    estadoLogin = EstadoLoginMac.Ocioso
                    telaAtual = Tela.INICIO
                }
            )
        }
        }

        if (carregandoCatalogo) {
            val etapaTexto = (estadoLogin as? EstadoLoginMac.Carregando)?.etapa
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xE6111726))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "⏳ Ainda carregando mais conteúdo" + (etapaTexto?.let { " — $it" } ?: "..."),
                    color = Color(0xFFF4D35E)
                )
            }
        }

        mostrarContagemLembrete()

        serieSelecionadaFora?.let { grupo ->
            SeriesDetailDialog(
                grupo = grupo,
                aoFechar = { serieSelecionadaFora = null },
                aoAssistir = { episodio ->
                    serieSelecionadaFora = null
                    abrirConteudo(episodio.episodioNome ?: episodio.titulo, episodio.streamUrl, null)
                }
            )
        }

        vencimentoAtual?.let { venc ->
            InfoDialogSimples(
                titulo = venc.modalTitle ?: "Aviso de vencimento",
                mensagem = venc.modalMessage ?: "Sua assinatura está próxima do vencimento.",
                aoFechar = {
                    venc.modalKey?.let {
                        ultimoModalVencimentoKey.edit().putString("ultimo_modal_vencimento", it).apply()
                    }
                    vencimentoAtual = null
                }
            )
        } ?: avisoAtual?.let { aviso ->
            InfoDialogSimples(
                titulo = aviso.title ?: "Aviso",
                mensagem = aviso.message ?: "",
                aoFechar = {
                    escopo.launch { renciaApi.ackNotification(macAutorizado, aviso.id) }
                    avisoAtual = null
                }
            )
        } ?: atualizacaoDisponivel?.let { info ->
            InfoDialogSimples(
                titulo = "Atualização disponível" + (info.version?.let { " ($it)" } ?: ""),
                mensagem = info.releaseNotes ?: "Uma nova versão do Evolux está disponível.",
                textoBotao = "Baixar agora",
                aoFechar = { atualizacaoDisponivel = null },
                aoConfirmar = {
                    val link = info.apkLink ?: info.url
                    if (!link.isNullOrBlank()) {
                        runCatching {
                            contexto.startActivity(
                                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link))
                            )
                        }
                    }
                    if (!info.forceUpdate) atualizacaoDisponivel = null
                }
            )
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

@Composable
private fun InfoDialogSimples(
    titulo: String,
    mensagem: String,
    aoFechar: () -> Unit,
    textoBotao: String = "OK",
    aoConfirmar: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = aoFechar) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0B1020))
                .padding(24.dp)
        ) {
            Text(text = titulo, color = Color(0xFFF4D35E), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = mensagem, color = Color.White)
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (aoConfirmar != null) {
                    Button(onClick = { aoFechar() }) { Text("Depois") }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = aoConfirmar) { Text(textoBotao) }
                } else {
                    Button(onClick = aoFechar) { Text(textoBotao) }
                }
            }
        }
    }
}
