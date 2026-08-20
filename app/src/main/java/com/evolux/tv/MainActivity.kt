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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.evolux.tv.R
import com.evolux.tv.data.EvoluxRepository
import com.evolux.tv.data.EvoluxConfig
import com.evolux.tv.data.MacAddressUtils
import com.evolux.tv.data.PlaylistCatalog
import com.evolux.tv.data.PlaylistRepository
import com.evolux.tv.data.Midia
import com.evolux.tv.data.ResultadoConfiguracao
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

private data class Reproducao(
    val titulo: String,
    val streamUrl: String
)

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
    val escopo = rememberCoroutineScope()
    var macAutorizado by remember { mutableStateOf("") }
    var catalogo by remember { mutableStateOf<PlaylistCatalog?>(null) }
    var estadoLogin by remember { mutableStateOf<EstadoLoginMac>(EstadoLoginMac.Ocioso) }
    var validacaoEmAndamento by remember { mutableStateOf(false) }
    var reproducao by remember { mutableStateOf<Reproducao?>(null) }

    BackHandler(enabled = reproducao != null || telaAtual != Tela.INICIO) {
        if (reproducao != null) {
            reproducao = null
        } else {
            telaAtual = Tela.INICIO
        }
    }

    suspend fun carregarCatalogo(configuracao: EvoluxConfig): String? {
        val urlPlaylist = configuracao.primeiraPlaylistValida
            ?: return "Nenhuma URL de playlist foi encontrada."
        return try {
            catalogo = playlistRepository.carregar(urlPlaylist)
            null
        } catch (erro: Exception) {
            erro.message?.takeIf { it.isNotBlank() } ?: "Não foi possível interpretar o catálogo."
        }
    }

    suspend fun validarAcesso(macInformado: String, mostrarCarregando: Boolean = true) {
        if (validacaoEmAndamento || macAutorizado.isNotBlank()) return
        validacaoEmAndamento = true
        if (mostrarCarregando) estadoLogin = EstadoLoginMac.Carregando
        try {
            when (val resultado = repository.buscarConfiguracao(macInformado)) {
                is ResultadoConfiguracao.Sucesso -> {
                    val erroCatalogo = carregarCatalogo(resultado.configuracao)
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

    LaunchedEffect(macLogico, macAutorizado) {
        if (macAutorizado.isBlank()) {
            while (isActive) {
                delay(5_000)
                if (macAutorizado.isBlank()) {
                    validarAcesso(macLogico, mostrarCarregando = false)
                } else {
                    break
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
    if (macAutorizado.isBlank()) {
        MacLoginScreen(
            estado = estadoLogin,
            macInicial = macInicial,
            aoCopiarMac = {
                val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("MAC do Evolux", macInicial))
                Toast.makeText(contexto, "MAC copiado", Toast.LENGTH_SHORT).show()
            },
            aoTentarLogin = aoTentarLogin
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
    val todasAsMidias = remember(catalogoAtual) {
        catalogoAtual.filmes + catalogoAtual.series
    }
    val destaques = remember(catalogoAtual) {
        gerarDestaques(catalogoAtual)
    }
    val fileirasEspeciais = remember(catalogoAtual) {
        gerarFileirasEspeciais(catalogoAtual)
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
                canaisCount = catalogoAtual.canais.size,
                filmesCount = catalogoAtual.filmes.size,
                seriesCount = catalogoAtual.series.size,
                filmes = catalogoAtual.filmes,
                series = catalogoAtual.series,
                fileirasEspeciais = fileirasEspeciais,
                aoAbrirMidia = { abrirConteudo(it.titulo, it.streamUrl) },
                aoAssistirDestaque = { abrirConteudo(it.titulo, it.streamUrl) },
                aoAbrirCanais = { telaAtual = Tela.TV_AO_VIVO },
                aoAbrirFilmes = { telaAtual = Tela.FILMES },
                aoAbrirSeries = { telaAtual = Tela.SERIES },
                ehFavorito = ehFavorito,
                aoAlternarFavorito = aoAlternarFavorito
            )

            Tela.TV_AO_VIVO -> LiveTvScreen(
                canais = catalogoAtual.canais,
                aoAbrirCanal = { abrirConteudo(it.nome, it.streamUrl) }
            )

            Tela.FILMES -> GradeMidiaScreen(
                titulo = "Filmes",
                itens = catalogoAtual.filmes,
                aoSelecionar = { abrirConteudo(it.titulo, it.streamUrl) },
                ehFavorito = ehFavorito,
                aoAlternarFavorito = aoAlternarFavorito
            )

            Tela.SERIES -> SeriesBrowserScreen(
                itens = catalogoAtual.series,
                aoAssistir = { abrirConteudo(it.episodioNome ?: it.titulo, it.streamUrl) }
            )

            Tela.JOGOS -> GamesScreen(
                jogos = emptyList(),
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
