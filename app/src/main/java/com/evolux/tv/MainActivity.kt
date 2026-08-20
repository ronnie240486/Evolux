package com.evolux.tv

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.evolux.tv.data.EvoluxRepository
import com.evolux.tv.data.EvoluxConfig
import com.evolux.tv.data.MacAddressUtils
import com.evolux.tv.data.PlaylistCatalog
import com.evolux.tv.data.PlaylistRepository
import com.evolux.tv.data.Midia
import com.evolux.tv.data.ResultadoConfiguracao
import com.evolux.tv.data.SampleData
import com.evolux.tv.ui.components.Tela
import com.evolux.tv.ui.components.TopNavBar
import com.evolux.tv.ui.screens.*
import com.evolux.tv.ui.theme.FundoEscuro
import com.evolux.tv.ui.theme.EvoluxTheme

private const val CHAVE_FAVORITOS = "favoritos_ids"
private const val CHAVE_MAC_LOGICO = "mac_logico_evolux"
private const val CHAVE_MAC_AUTORIZADO = "mac_autorizado_confirmado"

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

    suspend fun carregarCatalogo(configuracao: EvoluxConfig): Boolean {
        val urlPlaylist = configuracao.primeiraPlaylistValida
        if (urlPlaylist == null) return false
        return try {
            catalogo = playlistRepository.carregar(urlPlaylist)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun validarAcesso(macInformado: String, mostrarCarregando: Boolean = true) {
        if (validacaoEmAndamento || macAutorizado.isNotBlank()) return
        validacaoEmAndamento = true
        if (mostrarCarregando) estadoLogin = EstadoLoginMac.Carregando
        try {
            when (val resultado = repository.buscarConfiguracao(macInformado)) {
                is ResultadoConfiguracao.Sucesso -> {
                    if (carregarCatalogo(resultado.configuracao)) {
                        macAutorizado = resultado.configuracao.mac
                        preferencias.edit()
                            .putString(CHAVE_MAC_LOGICO, resultado.configuracao.mac)
                            .putBoolean(CHAVE_MAC_AUTORIZADO, true)
                            .apply()
                        estadoLogin = EstadoLoginMac.Ocioso
                    } else {
                        estadoLogin = EstadoLoginMac.Erro(
                            "Lista indisponível ou credenciais inválidas",
                            "A resposta não pôde ser interpretada como catálogo de canais, filmes ou séries."
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
    val todasAsMidias = remember(catalogoAtual) {
        catalogoAtual.filmes + catalogoAtual.series
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoEscuro)
    ) {
        TopNavBar(
            telaSelecionada = telaAtual,
            aoSelecionar = { telaAtual = it }
        )

        when (telaAtual) {
            Tela.INICIO -> HomeScreen(
                filmes = catalogoAtual.filmes,
                series = catalogoAtual.series,
                aoAbrirMidia = { /* abrir player com midia.streamUrl */ },
                aoAssistirDestaque = { /* abrir player com destaque.streamUrl */ },
                ehFavorito = ehFavorito,
                aoAlternarFavorito = aoAlternarFavorito
            )

            Tela.TV_AO_VIVO -> LiveTvScreen(
                canais = catalogoAtual.canais,
                aoAbrirCanal = { /* abrir player com canal.streamUrl */ }
            )

            Tela.FILMES -> GradeMidiaScreen(
                titulo = "Filmes",
                itens = catalogoAtual.filmes,
                aoSelecionar = { /* abrir player */ },
                ehFavorito = ehFavorito,
                aoAlternarFavorito = aoAlternarFavorito
            )

            Tela.SERIES -> GradeMidiaScreen(
                titulo = "Séries",
                itens = catalogoAtual.series,
                aoSelecionar = { /* abrir player */ },
                ehFavorito = ehFavorito,
                aoAlternarFavorito = aoAlternarFavorito
            )

            Tela.JOGOS -> GamesScreen(
                aoAbrirJogo = { /* abrir player com jogo.streamUrl */ }
            )

            Tela.FAVORITOS -> GradeMidiaScreen(
                titulo = "Favoritos",
                itens = favoritos,
                aoSelecionar = { /* abrir player */ },
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
