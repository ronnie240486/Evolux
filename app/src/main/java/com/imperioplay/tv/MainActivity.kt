package com.imperioplay.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.imperioplay.tv.data.SampleData
import com.imperioplay.tv.ui.components.Tela
import com.imperioplay.tv.ui.components.TopNavBar
import com.imperioplay.tv.ui.screens.*
import com.imperioplay.tv.ui.theme.FundoEscuro
import com.imperioplay.tv.ui.theme.ImperioPlayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImperioPlayTheme {
                ImperioPlayApp()
            }
        }
    }
}

@Composable
fun ImperioPlayApp() {
    var telaAtual by remember { mutableStateOf(Tela.INICIO) }

    // Estado simples de favoritos, em memória. Numa versão real, troque
    // por DataStore/Room para persistir entre sessões.
    val favoritos = remember { mutableStateListOf<com.imperioplay.tv.data.Midia>() }

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
                aoAssistirDestaque = { /* abrir player com destaque.streamUrl */ }
            )

            Tela.TV_AO_VIVO -> LiveTvScreen(
                aoAbrirCanal = { /* abrir player com canal.streamUrl */ }
            )

            Tela.FILMES -> GradeMidiaScreen(
                titulo = "Filmes",
                itens = SampleData.lancamentosFilmes,
                aoSelecionar = { /* abrir player */ }
            )

            Tela.SERIES -> GradeMidiaScreen(
                titulo = "Séries",
                itens = SampleData.lancamentosSeries,
                aoSelecionar = { /* abrir player */ }
            )

            Tela.JOGOS -> GamesScreen(
                aoAbrirJogo = { /* abrir player com jogo.streamUrl */ }
            )

            Tela.FAVORITOS -> GradeMidiaScreen(
                titulo = "Favoritos",
                itens = favoritos,
                aoSelecionar = { /* abrir player */ },
                mensagemVazio = "Você ainda não adicionou nada aos favoritos."
            )

            Tela.CONFIGURACOES -> SettingsScreen()
        }
    }
}
