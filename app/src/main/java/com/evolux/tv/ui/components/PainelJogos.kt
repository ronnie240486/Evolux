package com.evolux.tv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
    aoAbrirJogo: (Jogo) -> Unit,
    aoAbrirTodos: () -> Unit,
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
        var pagina by remember(jogos) { mutableIntStateOf(0) }
        val totalPaginas = ((jogos.size + 2) / 3).coerceAtLeast(1)
        val paginaAtual = pagina.coerceIn(0, totalPaginas - 1)
        val jogosDaPagina = jogos.drop(paginaAtual * 3).take(3)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(jogos, paginaAtual) {
                    detectHorizontalDragGestures { _, deslocamento ->
                        when {
                            deslocamento < -48f && paginaAtual < totalPaginas - 1 -> pagina++
                            deslocamento > 48f && paginaAtual > 0 -> pagina--
                        }
                    }
                }
        ) {
            Image(
                painter = painterResource(R.drawable.evolux_no_games_football),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                // Fundo estático: evita uma animação infinita competindo com o foco do D-pad.
                modifier = Modifier.matchParentSize().alpha(0.84f)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0x55050A16))
            )
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
                Spacer(Modifier.height(14.dp))
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
                    jogosDaPagina.forEach { jogo ->
                        LinhaJogo(jogo, aoClicar = { aoAbrirJogo(jogo) })
                        Spacer(Modifier.height(8.dp))
                    }
                    if (totalPaginas > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EvoluxClickableSurface(
                                onClick = { if (paginaAtual > 0) pagina-- },
                                containerColor = Color.Transparent,
                                focusedColor = Color.Transparent,
                                modifier = Modifier.size(36.dp)
                            ) { Text("‹", color = TextoClaro, style = MaterialTheme.typography.titleLarge) }
                            Text(
                                "${paginaAtual + 1}/$totalPaginas",
                                color = TextoClaro,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            EvoluxClickableSurface(
                                onClick = { if (paginaAtual < totalPaginas - 1) pagina++ },
                                containerColor = Color.Transparent,
                                focusedColor = Color.Transparent,
                                modifier = Modifier.size(36.dp)
                            ) { Text("›", color = TextoClaro, style = MaterialTheme.typography.titleLarge) }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EvoluxClickableSurface(
                        onClick = aoAbrirTodos,
                        containerColor = Color.Transparent,
                        focusedColor = Color.Transparent,
                        modifier = Modifier
                            .widthIn(min = 220.dp, max = 280.dp)
                            .height(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "VER JOGOS DO DIA",
                                color = TextoClaro,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
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
    val escudoLocal = recursoEscudo(sigla)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF202A43)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = sigla,
                color = TextoClaro,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
            when {
                escudoLocal != null -> Image(
                    painter = painterResource(escudoLocal),
                    contentDescription = sigla,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(38.dp)
                )
                logoUrl.isNotBlank() -> AsyncImage(
                    model = logoUrl,
                    contentDescription = sigla,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                else -> Icon(
                    imageVector = Icons.Filled.SportsSoccer,
                    contentDescription = sigla,
                    tint = Dourado,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(sigla, color = TextoClaro, style = MaterialTheme.typography.labelSmall)
    }
}

private fun recursoEscudo(sigla: String): Int? {
    val chave = sigla.lowercase().replace(".", "").replace(" ", "")
    return when {
        chave.startsWith("oli") -> R.drawable.club_badge_olimpia
        chave.startsWith("vas") -> R.drawable.club_badge_vasco
        chave.startsWith("cor") -> R.drawable.club_badge_corinthians
        chave == "rc" || chave.startsWith("ros") -> R.drawable.club_badge_rosario_central
        chave.startsWith("bot") -> R.drawable.club_badge_botafogo
        chave.startsWith("cie") -> R.drawable.club_badge_cienciano
        chave.startsWith("ldu") -> R.drawable.club_badge_ldu
        chave.startsWith("mir") -> R.drawable.club_badge_mirassol
        chave.startsWith("san") -> R.drawable.club_badge_santos
        else -> null
    }
}
