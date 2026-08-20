package com.evolux.tv.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.evolux.tv.data.Destaque
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.TextoClaro
import kotlinx.coroutines.delay

/**
 * Banner grande da Home. Recebe a LISTA inteira de destaques e troca
 * sozinho de item a cada [intervaloMs] milissegundos. É só alimentar
 * `destaques` (em SampleData) com novos itens que o banner passa a
 * rodar por eles automaticamente — não precisa mexer neste arquivo.
 */
@Composable
fun FeaturedBanner(
    destaques: List<Destaque>,
    intervaloMs: Long = 8000L,
    aoAssistir: (Destaque) -> Unit,
    aoVerTrailer: (Destaque) -> Unit,
    modifier: Modifier = Modifier
) {
    if (destaques.isEmpty()) return

    var indice by remember { mutableStateOf(0) }

    // Roda pela lista automaticamente. Reinicia o timer sempre que o
    // usuário troca a lista (tamanho diferente) para não ficar preso
    // num índice inválido.
    LaunchedEffect(destaques) {
        indice = 0
        while (true) {
            delay(intervaloMs)
            indice = (indice + 1) % destaques.size
        }
    }

    val destaqueAtual = destaques[indice]

    Crossfade(targetState = destaqueAtual, animationSpec = tween(600), label = "banner") { item ->
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = item.imagemUrl,
                // Decorativa: o texto ao lado (título + sinopse) já
                // descreve o destaque, então a imagem some do TalkBack
                // para não duplicar a leitura.
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradiente escuro pra legenda ficar legível, igual ao layout original
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.92f),
                                Color.Black.copy(alpha = 0.55f),
                                Color.Transparent
                            ),
                            endX = 900f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(32.dp)
                    .widthIn(max = 600.dp)
                    // Título + sinopse viram um único anúncio para o
                    // TalkBack, em vez de linhas soltas. `liveRegion`
                    // faz o leitor de tela avisar "Legado Real, 2026..."
                    // sempre que o banner troca sozinho — sem precisar
                    // que o usuário esteja com foco nele.
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${item.subtitulo}. ${item.titulo}, ${item.ano}. ${item.sinopse}"
                        liveRegion = LiveRegionMode.Polite
                    }
            ) {
                Text(
                    text = item.subtitulo.uppercase(),
                    color = Dourado,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.titulo.uppercase(),
                    color = Dourado,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    text = item.ano,
                    color = TextoClaro.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = item.sinopse,
                    color = TextoClaro,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 4
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { aoAssistir(item) }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("ASSISTIR AGORA", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { aoVerTrailer(item) }) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("VER TRAILER", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Indicadores (bolinhas) mostrando a posição atual na lista.
            // Puramente visuais — a posição já é anunciada pelo
            // liveRegion do texto acima, então tiramos do TalkBack.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .clearAndSetSemantics {},
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                destaques.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (i == indice) 10.dp else 7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (i == indice) Dourado else Color.White.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}
