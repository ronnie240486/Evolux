package com.imperioplay.tv

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
import com.imperioplay.tv.data.Midia
import com.imperioplay.tv.data.SampleData
import com.imperioplay.tv.ui.components.Tela
import com.imperioplay.tv.ui.components.TopNavBar
import com.imperioplay.tv.ui.screens.*
import com.imperioplay.tv.ui.theme.FundoEscuro
import com.imperioplay.tv.ui.theme.ImperioPlayTheme

private const val CHAVE_FAVORITOS = "favoritos_ids"

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
    val contexto = LocalContext.current
    val preferencias = remember(contexto) {
        contexto.getSharedPreferences("imperio_play_preferencias", Context.MODE_PRIVATE)
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

            Tela.CONFIGURACOES -> SettingsScreen()
        }
    }
}
