package com.evolux.tv

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.evolux.tv.data.EvoluxRepository
import com.evolux.tv.data.MacAddressProvider
import com.evolux.tv.data.Midia
import com.evolux.tv.data.ResultadoConfiguracao
import com.evolux.tv.data.SampleData
import com.evolux.tv.ui.components.Tela
import com.evolux.tv.ui.components.TopNavBar
import com.evolux.tv.ui.screens.*
import com.evolux.tv.ui.theme.FundoEscuro
import com.evolux.tv.ui.theme.EvoluxTheme

private const val CHAVE_FAVORITOS = "favoritos_ids"
private const val CHAVE_MAC = "mac_autorizado"

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
    val macSalvo = preferencias.getString(CHAVE_MAC, "").orEmpty()
    val macDetectado = remember { MacAddressProvider.detectar().orEmpty() }
    val macInicial = macSalvo.ifBlank { macDetectado }
    val repository = remember { EvoluxRepository() }
    val escopo = rememberCoroutineScope()
    var macAutorizado by remember { mutableStateOf("") }
    var estadoLogin by remember { mutableStateOf<EstadoLoginMac>(EstadoLoginMac.Ocioso) }

    LaunchedEffect(Unit) {
        if (macSalvo.isNotBlank()) {
            estadoLogin = EstadoLoginMac.Carregando
            when (val resultado = repository.buscarConfiguracao(macSalvo)) {
                is ResultadoConfiguracao.Sucesso -> {
                    macAutorizado = resultado.configuracao.mac
                    preferencias.edit()
                        .putString(CHAVE_MAC, resultado.configuracao.mac)
                        .apply()
                    estadoLogin = EstadoLoginMac.Ocioso
                }
                is ResultadoConfiguracao.Erro -> {
                    estadoLogin = EstadoLoginMac.Erro(resultado.mensagem)
                }
            }
        }
    }

    val aoTentarLogin: (String) -> Unit = { macInformado ->
        escopo.launch {
            estadoLogin = EstadoLoginMac.Carregando
            when (val resultado = repository.buscarConfiguracao(macInformado)) {
                is ResultadoConfiguracao.Sucesso -> {
                    macAutorizado = resultado.configuracao.mac
                    preferencias.edit()
                        .putString(CHAVE_MAC, resultado.configuracao.mac)
                        .apply()
                    estadoLogin = EstadoLoginMac.Ocioso
                }
                is ResultadoConfiguracao.Erro -> {
                    macAutorizado = ""
                    estadoLogin = EstadoLoginMac.Erro(resultado.mensagem)
                }
            }
        }
    }
    if (macAutorizado.isBlank()) {
        MacLoginScreen(
            estado = estadoLogin,
            macInicial = macInicial,
            aoTentarLogin = aoTentarLogin
        )
        return
    }

    val todasAsMidias = remember {
        SampleData.lancamentosFilmes +
            SampleData.lancamentosSeries +
            SampleData.continuarAssistindo
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
                aoAbrirMidia = { /* abrir player com midia.streamUrl */ },
                aoAssistirDestaque = { /* abrir player com destaque.streamUrl */ },
                ehFavorito = ehFavorito,
                aoAlternarFavorito = aoAlternarFavorito
            )

            Tela.TV_AO_VIVO -> LiveTvScreen(
                aoAbrirCanal = { /* abrir player com canal.streamUrl */ }
            )

            Tela.FILMES -> GradeMidiaScreen(
                titulo = "Filmes",
                itens = SampleData.lancamentosFilmes,
                aoSelecionar = { /* abrir player */ },
                ehFavorito = ehFavorito,
                aoAlternarFavorito = aoAlternarFavorito
            )

            Tela.SERIES -> GradeMidiaScreen(
                titulo = "Séries",
                itens = SampleData.lancamentosSeries,
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
                    preferencias.edit().remove(CHAVE_MAC).apply()
                    macAutorizado = ""
                    telaAtual = Tela.INICIO
                }
            )
        }
    }
}
