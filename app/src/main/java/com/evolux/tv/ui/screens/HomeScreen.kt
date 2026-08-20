package com.evolux.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    aoAbrirCanalDoJogo: () -> Unit = {},
    ehFavorito: (Midia) -> Boolean,
    aoAlternarFavorito: (Midia) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compacto = maxWidth < 760.dp
        LazyColumn(
            contentPadding = PaddingValues(
                start = if (compacto) 12.dp else 24.dp,
                end = if (compacto) 12.dp else 24.dp,
                bottom = 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (compacto) 20.dp else 32.dp)
        ) {
            item {
                if (compacto) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        FeaturedBanner(
                            destaques = SampleData.destaques,
                            intervaloMs = 8000L,
                            aoAssistir = aoAssistirDestaque,
                            aoVerTrailer = { aoAssistirDestaque(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                        PainelJogosDoDia(
                            jogos = SampleData.jogosDoDia,
                            aoAbrirCanal = aoAbrirCanalDoJogo,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        FeaturedBanner(
                            destaques = SampleData.destaques,
                            intervaloMs = 8000L,
                            aoAssistir = aoAssistirDestaque,
                            aoVerTrailer = { aoAssistirDestaque(it) },
                            modifier = Modifier
                                .weight(1f)
                                .height(300.dp)
                        )
                        PainelJogosDoDia(
                            jogos = SampleData.jogosDoDia,
                            aoAbrirCanal = aoAbrirCanalDoJogo,
                            modifier = Modifier.fillMaxWidth(0.32f)
                        )
                    }
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
}
