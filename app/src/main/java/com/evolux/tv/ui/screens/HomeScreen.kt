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
import com.evolux.tv.data.Jogo
import com.evolux.tv.data.Midia
import com.evolux.tv.ui.components.CatalogoSummaryPanel
import com.evolux.tv.ui.components.FeaturedBanner
import com.evolux.tv.ui.components.FileiraLogosServicos
import com.evolux.tv.ui.components.MediaRow
import com.evolux.tv.ui.components.PainelJogosDoDia

@Composable
fun HomeScreen(
    destaques: List<Destaque>,
    canaisCount: Int,
    filmesCount: Int,
    seriesCount: Int,
    filmes: List<Midia>,
    series: List<Midia>,
    fileirasEspeciais: List<com.evolux.tv.data.FileiraCatalogo>,
    aoAbrirMidia: (Midia) -> Unit,
    aoAssistirDestaque: (Destaque) -> Unit,
    aoAbrirCanais: () -> Unit,
    aoAbrirFilmes: () -> Unit,
    aoAbrirSeries: () -> Unit,
    jogosDoDia: List<Jogo> = emptyList(),
    aoAbrirJogos: () -> Unit = {},
    aoAbrirJogo: (Jogo) -> Unit = {},
    ehFavorito: (Midia) -> Boolean,
    aoAlternarFavorito: (Midia) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compacto = maxWidth < 760.dp
        val servicosDaHome = fileirasEspeciais.mapNotNull { it.servico }.distinct()
        LazyColumn(
            contentPadding = PaddingValues(
                start = if (compacto) 12.dp else 24.dp,
                end = if (compacto) 12.dp else 24.dp,
                bottom = 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (compacto) 20.dp else 32.dp)
        ) {
            item {
                if (destaques.isEmpty()) {
                    CatalogoSummaryPanel(
                        canais = canaisCount,
                        filmes = filmesCount,
                        series = seriesCount,
                        aoAbrirCanais = aoAbrirCanais,
                        aoAbrirFilmes = aoAbrirFilmes,
                        aoAbrirSeries = aoAbrirSeries,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (compacto) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        FeaturedBanner(
                            destaques = destaques,
                            intervaloMs = 8000L,
                            aoAssistir = aoAssistirDestaque,
                            aoVerTrailer = aoAssistirDestaque,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                        PainelJogosDoDia(
                            jogos = jogosDoDia,
                            aoAbrirJogo = aoAbrirJogo,
                            aoAbrirTodos = aoAbrirJogos,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        FeaturedBanner(
                            destaques = destaques,
                            intervaloMs = 8000L,
                            aoAssistir = aoAssistirDestaque,
                            aoVerTrailer = aoAssistirDestaque,
                            modifier = Modifier
                                .weight(1f)
                                .height(300.dp)
                        )
                        PainelJogosDoDia(
                            jogos = jogosDoDia,
                            aoAbrirJogo = aoAbrirJogo,
                            aoAbrirTodos = aoAbrirJogos,
                            modifier = Modifier
                                .fillMaxWidth(0.32f)
                                .height(300.dp)
                        )
                    }
                }
            }
            item {
                FileiraLogosServicos(
                    servicos = servicosDaHome,
                    aoSelecionar = { servico ->
                        fileirasEspeciais
                            .firstOrNull { it.servico == servico }
                            ?.itens
                            ?.firstOrNull()
                            ?.let(aoAbrirMidia)
                    }
                )
            }
            fileirasEspeciais.filter { it.servico == null }.forEach { fileira ->
                item {
                    MediaRow(
                        titulo = fileira.titulo,
                        itens = fileira.itens,
                        aoSelecionar = aoAbrirMidia,
                        ehFavorito = ehFavorito,
                        aoAlternarFavorito = aoAlternarFavorito
                    )
                }
            }
            item {
                MediaRow(
                    titulo = "FILMES DO SEU CATÁLOGO",
                    itens = filmes,
                    aoSelecionar = aoAbrirMidia,
                    ehFavorito = ehFavorito,
                    aoAlternarFavorito = aoAlternarFavorito
                )
            }
            item {
                MediaRow(
                    titulo = "SÉRIES DO SEU CATÁLOGO",
                    itens = series,
                    aoSelecionar = aoAbrirMidia,
                    ehFavorito = ehFavorito,
                    aoAlternarFavorito = aoAlternarFavorito
                )
            }
        }
    }
}
