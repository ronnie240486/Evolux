package com.evolux.tv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evolux.tv.data.Destaque
import com.evolux.tv.data.Midia
import com.evolux.tv.data.SampleData
import com.evolux.tv.ui.components.FeaturedBanner
import com.evolux.tv.ui.components.MediaRow
import com.evolux.tv.ui.components.PainelJogosDoDia

@Composable
fun HomeScreen(
    filmes: List<Midia>,
    series: List<Midia>,
    aoAbrirMidia: (Midia) -> Unit,
    aoAssistirDestaque: (Destaque) -> Unit,
    ehFavorito: (Midia) -> Boolean,
    aoAlternarFavorito: (Midia) -> Unit
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
                itens = filmes,
                aoSelecionar = aoAbrirMidia,
                ehFavorito = ehFavorito,
                aoAlternarFavorito = aoAlternarFavorito
            )
        }
        item {
            MediaRow(
                titulo = "Lançamentos de Séries",
                itens = series,
                aoSelecionar = aoAbrirMidia,
                ehFavorito = ehFavorito,
                aoAlternarFavorito = aoAlternarFavorito
            )
        }
    }
}
