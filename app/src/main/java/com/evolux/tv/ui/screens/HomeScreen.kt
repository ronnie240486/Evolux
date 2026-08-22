package com.evolux.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.evolux.tv.data.Destaque
import com.evolux.tv.data.Jogo
import com.evolux.tv.data.Midia
import com.evolux.tv.ui.components.EvoluxClickableSurface
import com.evolux.tv.ui.components.FeaturedBanner
import com.evolux.tv.ui.components.FileiraLogosServicos
import com.evolux.tv.ui.components.MediaRow
import com.evolux.tv.ui.components.PainelJogosDoDia
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.TextoClaro
import com.evolux.tv.ui.theme.TextoCinza

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
    aoAbrirServico: (String) -> Unit = {},
    jogosDoDia: List<Jogo> = emptyList(),
    aoAbrirJogos: () -> Unit = {},
    aoAbrirJogo: (Jogo) -> Unit = {},
    ehFavorito: (Midia) -> Boolean,
    aoAlternarFavorito: (Midia) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compacto = maxWidth < 760.dp
        val servicosDaHome = fileirasEspeciais.mapNotNull { it.servico }.distinct()
        val fileirasGeraisDaHome = remember(fileirasEspeciais, filmes, series) {
            val exibidos = HashSet<String>()
            fun chave(item: Midia): String {
                val titulo = item.titulo.lowercase().replace("[^a-z0-9]+".toRegex(), "")
                return "${item.tipo}:$titulo"
            }
            val especiais = fileirasEspeciais
                .filter { it.servico == null }
                .mapNotNull { fileira ->
                    val unicos = fileira.itens.filter { exibidos.add(chave(it)) }
                    fileira.copy(itens = unicos).takeIf { it.itens.isNotEmpty() }
                }
            val filmesUnicos = filmes.filter { exibidos.add(chave(it)) }
            val seriesUnicas = series.filter { exibidos.add(chave(it)) }
            Triple(especiais, filmesUnicos, seriesUnicas)
        }
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
                        if (destaques.isNotEmpty()) {
                            FeaturedBanner(
                                destaques = destaques,
                                intervaloMs = 8000L,
                                aoAssistir = aoAssistirDestaque,
                                aoVerTrailer = aoAssistirDestaque,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            )
                        } else {
                            HomeHeroFallback(
                                canais = canaisCount,
                                filmes = filmesCount,
                                series = seriesCount,
                                aoAbrirCanais = aoAbrirCanais,
                                aoAbrirFilmes = aoAbrirFilmes,
                                aoAbrirSeries = aoAbrirSeries,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            )
                        }
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
                        if (destaques.isNotEmpty()) {
                            FeaturedBanner(
                                destaques = destaques,
                                intervaloMs = 8000L,
                                aoAssistir = aoAssistirDestaque,
                                aoVerTrailer = aoAssistirDestaque,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(300.dp)
                            )
                        } else {
                            HomeHeroFallback(
                                canais = canaisCount,
                                filmes = filmesCount,
                                series = seriesCount,
                                aoAbrirCanais = aoAbrirCanais,
                                aoAbrirFilmes = aoAbrirFilmes,
                                aoAbrirSeries = aoAbrirSeries,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(300.dp)
                            )
                        }
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
                    aoSelecionar = aoAbrirServico
                )
            }
            fileirasGeraisDaHome.first.forEach { fileira ->
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
                    itens = fileirasGeraisDaHome.second,
                    aoSelecionar = aoAbrirMidia,
                    ehFavorito = ehFavorito,
                    aoAlternarFavorito = aoAlternarFavorito
                )
            }
            item {
                MediaRow(
                    titulo = "SÉRIES DO SEU CATÁLOGO",
                    itens = fileirasGeraisDaHome.third,
                    aoSelecionar = aoAbrirMidia,
                    ehFavorito = ehFavorito,
                    aoAlternarFavorito = aoAlternarFavorito
                )
            }
        }
    }
}

@Composable
private fun HomeHeroFallback(
    canais: Int,
    filmes: Int,
    series: Int,
    aoAbrirCanais: () -> Unit,
    aoAbrirFilmes: () -> Unit,
    aoAbrirSeries: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = SurfaceDefaults.colors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF162B55), Color(0xFF0B1225), Color(0xFF321A48))
                    ),
                    RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "EVOLUX",
                    color = Dourado,
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "Seu conteúdo está pronto para curtir",
                    color = TextoClaro,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Escolha uma categoria para começar",
                    color = TextoCinza,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroStat("CANAIS", canais, aoAbrirCanais, Modifier.weight(1f))
                    HeroStat("FILMES", filmes, aoAbrirFilmes, Modifier.weight(1f))
                    HeroStat("SÉRIES", series, aoAbrirSeries, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HeroStat(
    titulo: String,
    quantidade: Int,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier
) {
    EvoluxClickableSurface(
        onClick = aoClicar,
        containerColor = Color(0xB51A2644),
        focusedColor = Color(0xFF2A3C68),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = titulo, color = TextoCinza, style = MaterialTheme.typography.labelSmall)
            Text(text = quantidade.toString(), color = Dourado, style = MaterialTheme.typography.titleMedium)
        }
    }
}
