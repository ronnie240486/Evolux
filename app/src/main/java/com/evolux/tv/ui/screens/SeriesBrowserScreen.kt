package com.evolux.tv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.evolux.tv.R
import com.evolux.tv.data.Midia
import com.evolux.tv.ui.components.EvoluxClickableSurface
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.FundoCard
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro

private data class GrupoSerie(
    val chave: String,
    val nome: String,
    val categoria: String,
    val capa: String,
    val episodios: List<Midia>
)

@Composable
fun SeriesBrowserScreen(
    itens: List<Midia>,
    aoAssistir: (Midia) -> Unit
) {
    val grupos = remember(itens) {
        itens.groupBy { item ->
            item.serieId ?: normalizarChave(item.serieNome ?: removerMarcadorDeEpisodio(item.titulo))
        }.map { (chave, episodios) ->
            GrupoSerie(
                chave = chave,
                nome = episodios.firstNotNullOfOrNull { it.serieNome } ?: removerMarcadorDeEpisodio(episodios.first().titulo),
                categoria = episodios.firstOrNull()?.categoria.orEmpty(),
                capa = episodios.firstOrNull { it.imagemUrl.isNotBlank() }?.imagemUrl.orEmpty(),
                episodios = episodios.sortedWith(compareBy<Midia> { it.temporadaNumero ?: 1 }.thenBy { it.episodioNumero ?: Int.MAX_VALUE }.thenBy { it.titulo })
            )
        }.sortedBy { it.nome.lowercase() }
    }

    var serieSelecionada by remember { mutableStateOf<GrupoSerie?>(null) }
    var temporadaSelecionada by remember { mutableStateOf<Int?>(null) }

    BackHandler(enabled = serieSelecionada != null || temporadaSelecionada != null) {
        if (temporadaSelecionada != null) temporadaSelecionada = null else serieSelecionada = null
    }

    val serieAtual = serieSelecionada
    val temporadaAtual = temporadaSelecionada
    val titulo = when {
        serieAtual == null -> "SÉRIES"
        temporadaAtual == null -> serieAtual.nome
        else -> "${serieAtual.nome} • TEMPORADA $temporadaAtual"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (serieAtual != null) {
                EvoluxClickableSurface(
                    onClick = {
                        if (temporadaAtual != null) temporadaSelecionada = null else serieSelecionada = null
                    },
                    containerColor = FundoCard,
                    modifier = Modifier.width(58.dp).height(52.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextoClaro)
                }
            }
            Text(
                text = titulo,
                color = Dourado,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Spacer(Modifier.height(16.dp))

        when {
            serieAtual == null -> {
                if (grupos.isEmpty()) {
                    TextoVazioSeries("Nenhuma série foi encontrada na categoria Séries.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(grupos, key = { it.chave }) { grupo ->
                            SerieCard(grupo) { serieSelecionada = grupo }
                        }
                    }
                }
            }
            temporadaAtual == null -> {
                val temporadas = serieAtual.episodios.groupBy { it.temporadaNumero ?: 1 }.toSortedMap()
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "${temporadas.size} temporada(s) • ${serieAtual.episodios.size} episódio(s)",
                            color = TextoCinza,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    items(temporadas.keys.toList(), key = { it }) { numero ->
                        TemporadaCard(numero, temporadas[numero].orEmpty().size) {
                            temporadaSelecionada = numero
                        }
                    }
                }
            }
            else -> {
                val episodios = serieAtual.episodios
                    .filter { (it.temporadaNumero ?: 1) == temporadaAtual }
                    .sortedWith(compareBy<Midia> { it.episodioNumero ?: Int.MAX_VALUE }.thenBy { it.titulo })
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(episodios, key = { it.id }) { episodio ->
                        EpisodioCard(episodio) { aoAssistir(episodio) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SerieCard(grupo: GrupoSerie, aoClicar: () -> Unit) {
    EvoluxClickableSurface(
        onClick = aoClicar,
        containerColor = FundoCard,
        modifier = Modifier.fillMaxWidth().height(150.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = grupo.capa.takeIf { it.isNotBlank() },
                placeholder = painterResource(R.drawable.evolux_logo),
                error = painterResource(R.drawable.evolux_logo),
                fallback = painterResource(R.drawable.evolux_logo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(100.dp).fillMaxSize().clip(RoundedCornerShape(10.dp))
            )
            Column(modifier = Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(grupo.nome, color = TextoClaro, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(grupo.categoria.ifBlank { "Série" }, color = Dourado, style = MaterialTheme.typography.bodyMedium)
                Text("${grupo.episodios.map { it.temporadaNumero ?: 1 }.distinct().size} temporada(s) • ${grupo.episodios.size} episódio(s)", color = TextoCinza)
            }
        }
    }
}

@Composable
private fun TemporadaCard(numero: Int, quantidade: Int, aoClicar: () -> Unit) {
    EvoluxClickableSurface(
        onClick = aoClicar,
        containerColor = FundoCard,
        modifier = Modifier.fillMaxWidth().height(76.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Temporada $numero", color = TextoClaro, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Text("$quantidade episódios", color = Dourado)
        }
    }
}

@Composable
private fun EpisodioCard(episodio: Midia, aoClicar: () -> Unit) {
    EvoluxClickableSurface(
        onClick = aoClicar,
        containerColor = FundoCard,
        modifier = Modifier.fillMaxWidth().height(82.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Assistir", tint = Dourado)
            Column {
                Text(
                    text = episodio.episodioNome ?: episodio.titulo,
                    color = TextoClaro,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Text(
                    text = if (episodio.episodioNumero != null) "Episódio ${episodio.episodioNumero}" else "Assistir episódio",
                    color = TextoCinza
                )
            }
        }
    }
}

@Composable
private fun TextoVazioSeries(texto: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(texto, color = TextoCinza, style = MaterialTheme.typography.titleMedium)
    }
}

private fun normalizarChave(valor: String): String = valor.trim().lowercase().replace("\\s+".toRegex(), " ")

private fun removerMarcadorDeEpisodio(valor: String): String {
    return valor.replace("(?i)\\s*[-_.| ]*(s|t|season|temporada)\\s*\\d{1,2}.*$".toRegex(), "").trim()
}
