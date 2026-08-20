package com.evolux.tv.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.evolux.tv.R
import com.evolux.tv.data.Jogo
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro

@Composable
fun PainelJogosDoDia(
    jogos: List<Jogo>,
    aoAbrirCanal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        colors = SurfaceDefaults.colors(containerColor = Color(0xFF0E1424)),
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1F2740), RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (jogos.isEmpty()) {
                val transicao = rememberInfiniteTransition(label = "futebol_vazio")
                val brilho by transicao.animateFloat(
                    initialValue = 0.76f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 2400),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "brilho_futebol"
                )
                Image(
                    painter = painterResource(R.drawable.evolux_no_games_football),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(245.dp)
                        .alpha(brilho)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(245.dp)
                        .background(Color(0xA80A0E1A))
                )
            }
            Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics { heading() }
            ) {
                Icon(Icons.Filled.SportsSoccer, contentDescription = null, tint = Dourado)
                Spacer(Modifier.width(8.dp))
                Text(
                    "JOGOS DO DIA",
                    color = Dourado,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(16.dp))
            if (jogos.isEmpty()) {
                Text(
                    text = "Nenhum jogo futuro encontrado hoje.",
                    color = TextoClaro,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "A bola volta a rolar em breve.",
                    color = Dourado,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                jogos.forEach { jogo ->
                    LinhaJogo(jogo, aoClicar = aoAbrirCanal)
                    Spacer(Modifier.height(12.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            EvoluxClickableSurface(
                onClick = aoAbrirCanal,
                containerColor = Color(0xFF111A2B),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .border(1.dp, Dourado.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "VER JOGOS DO DIA",
                        color = Dourado,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
}

@Composable
private fun LinhaJogo(jogo: Jogo, aoClicar: () -> Unit) {
    var focado by remember { mutableStateOf(false) }

    // Uma frase só: "FLA vs PAL, 19:00, Campeonato Brasileiro" — em vez
    // de ler escudo, sigla, horário e campeonato como quatro elementos.
    val descricao = "${jogo.timeCasaSigla} contra ${jogo.timeVisitanteSigla}, " +
        "às ${jogo.horario}, ${jogo.campeonato}"

    EvoluxClickableSurface(
        onClick = aoClicar,
        containerColor = Color(0xFF12172A),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focado = it.isFocused }
            .semantics(mergeDescendants = true) { contentDescription = descricao }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            EscudoTime(jogo.timeCasaLogoUrl, jogo.timeCasaSigla)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(jogo.horario, color = TextoClaro, fontWeight = FontWeight.Bold)
                Text(jogo.campeonato, color = TextoCinza, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            EscudoTime(jogo.timeVisitanteLogoUrl, jogo.timeVisitanteSigla)
        }
    }
}

@Composable
private fun EscudoTime(logoUrl: String, sigla: String) {
    // Sigla escrita embaixo já é o texto legível; o escudo é decorativo
    // para não duplicar a leitura no TalkBack.
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = logoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Text(sigla, color = TextoClaro, style = MaterialTheme.typography.labelSmall)
    }
}
