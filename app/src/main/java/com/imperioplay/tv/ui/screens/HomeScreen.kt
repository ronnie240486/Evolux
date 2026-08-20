package com.imperioplay.tv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.imperioplay.tv.data.Destaque
import com.imperioplay.tv.data.Midia
import com.imperioplay.tv.data.SampleData
import com.imperioplay.tv.ui.components.FeaturedBanner
import com.imperioplay.tv.ui.components.MediaRow
import com.imperioplay.tv.ui.components.PainelJogosDoDia

@Composable
fun HomeScreen(
    aoAbrirMidia: (Midia) -> Unit,
    aoAssistirDestaque: (Destaque) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FeaturedBanner(
                    destaques = SampleData.destaques, // <- troca a lista aqui, o banner se atualiza sozinho
                    intervaloMs = 8000L,
                    aoAssistir = aoAssistirDestaque,
                    aoVerTrailer = { /* abrir player em modo trailer */ },
                    modifier = Modifier.weight(1f)
                )
                PainelJogosDoDia(
                    jogos = SampleData.jogosDoDia,
                    aoAbrirCanal = { /* abrir canal ao vivo do jogo em destaque */ }
                )
            }
        }
        item {
            MediaRow(
                titulo = "Lançamentos de Filmes 2026",
                itens = SampleData.lancamentosFilmes,
                aoSelecionar = aoAbrirMidia
            )
        }
        item {
            MediaRow(
                titulo = "Lançamentos de Séries",
                itens = SampleData.lancamentosSeries,
                aoSelecionar = aoAbrirMidia
            )
        }
        item {
            MediaRow(
                titulo = "Continue Assistindo",
                itens = SampleData.continuarAssistindo,
                aoSelecionar = aoAbrirMidia
            )
        }
    }
}
