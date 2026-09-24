package com.evolux.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import coil.compose.AsyncImage
import com.evolux.tv.ui.components.EvoluxClickableSurface
import androidx.tv.material3.Text
import com.evolux.tv.data.Jogo
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
            text = "JOGOS",
            color = Dourado,
            style = MaterialTheme.typography.headlineSmall
        )
        if (jogos.isEmpty()) {
            Text(
                text = "Nenhum jogo disponível na playlist autorizada.",
                color = TextoCinza,
                modifier = Modifier.padding(top = 20.dp)
            )
            return@Column
        }
        LazyColumn(
            modifier = Modifier.padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(jogos) { jogo ->
                EvoluxClickableSurface(
                    onClick = { aoAbrirJogo(jogo) },
                    containerColor = FundoCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimeComEscudo(jogo.timeCasaLogoUrl, jogo.timeCasaSigla, Modifier.weight(1f))
                            PlacarOuHorario(jogo)
                            TimeComEscudo(
                                jogo.timeVisitanteLogoUrl,
                                jogo.timeVisitanteSigla,
                                Modifier.weight(1f),
                                inverterOrdem = true
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Text(
                                text = "${jogo.horario} • ${jogo.campeonato}",
                                color = TextoCinza,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeComEscudo(
    logoUrl: String,
    sigla: String,
    modifier: Modifier = Modifier,
    inverterOrdem: Boolean = false
) {
    val escudo: @Composable () -> Unit = {
        if (logoUrl.isNotBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = sigla,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(32.dp)
            )
        }
    }
    val nome: @Composable () -> Unit = {
        Text(
            text = sigla,
            color = TextoClaro,
            fontWeight = FontWeight.Bold,
            maxLines = 2
        )
    }
    Row(
        modifier = modifier,
        horizontalArrangement = if (inverterOrdem) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (inverterOrdem) {
            nome()
            Spacer(Modifier.width(8.dp))
            escudo()
        } else {
            escudo()
            Spacer(Modifier.width(8.dp))
            nome()
        }
    }
}

@Composable
private fun PlacarOuHorario(jogo: Jogo) {
    Column(
        modifier = Modifier.padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (jogo.placarCasa != null && jogo.placarVisitante != null) {
            Text(
                text = "${jogo.placarCasa} - ${jogo.placarVisitante}",
                color = if (jogo.aoVivo) Dourado else TextoClaro,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(text = "x", color = TextoCinza)
        }
        if (jogo.aoVivo) {
            Text(text = "AO VIVO", color = Color(0xFFFF5B5B), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        } else if (jogo.encerrado) {
            Text(text = "Encerrado", color = TextoCinza, style = MaterialTheme.typography.bodySmall)
        }
    }
}
