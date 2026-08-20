package com.evolux.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.evolux.tv.R
import com.evolux.tv.data.Jogo
import com.evolux.tv.ui.components.EvoluxClickableSurface
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.FundoCard
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro

@Composable
fun GamesScreen(
    jogos: List<Jogo>,
    aoAbrirJogo: (Jogo) -> Unit
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = "JOGOS DO DIA",
            color = Dourado,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Partidas futuras encontradas na agenda de futebol",
            color = TextoCinza,
            style = MaterialTheme.typography.bodyMedium
        )
        if (jogos.isEmpty()) {
            Text(
                text = "Nenhum jogo futuro encontrado hoje.",
                color = TextoCinza,
                modifier = Modifier.padding(top = 20.dp)
            )
            return@Column
        }
        LazyColumn(
            modifier = Modifier.padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(jogos, key = { it.id }) { jogo ->
                EvoluxClickableSurface(
                    onClick = { if (jogo.streamUrl.isNotBlank()) aoAbrirJogo(jogo) },
                    containerColor = FundoCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TimeLogo(jogo.timeCasaLogoUrl, jogo.timeCasaSigla)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(jogo.horario, color = TextoClaro, style = MaterialTheme.typography.titleMedium)
                            Text(jogo.campeonato, color = TextoCinza, style = MaterialTheme.typography.bodySmall)
                        }
                        TimeLogo(jogo.timeVisitanteLogoUrl, jogo.timeVisitanteSigla)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeLogo(url: String, sigla: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = url.takeIf { it.isNotBlank() },
            placeholder = painterResource(R.drawable.evolux_logo),
            error = painterResource(R.drawable.evolux_logo),
            fallback = painterResource(R.drawable.evolux_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Text(sigla, color = TextoClaro, style = MaterialTheme.typography.labelMedium)
    }
}
