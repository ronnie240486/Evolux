package com.evolux.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import coil.request.ImageRequest
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import com.evolux.tv.R
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.evolux.tv.data.Midia
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.TextoClaro

@Composable
fun MediaRow(
    titulo: String,
    itens: List<Midia>,
    aoSelecionar: (Midia) -> Unit,
    ehFavorito: (Midia) -> Boolean,
    aoAlternarFavorito: (Midia) -> Unit,
    modifier: Modifier = Modifier,
    emblemaServico: String? = null
) {
    val itensDaHome = remember(itens) { itens.take(24) }
    Column(modifier = modifier.fillMaxWidth()) {
        if (emblemaServico != null) {
            EmblemaServico(
                nome = emblemaServico,
                aoClicar = { itensDaHome.firstOrNull()?.let(aoSelecionar) },
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
            )
        } else {
            Text(
                text = titulo.uppercase(),
                color = Dourado,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
            )
        }
        TvLazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(itensDaHome) { midia ->
                CardMidia(
                    midia = midia,
                    favorito = ehFavorito(midia),
                    aoClicar = { aoSelecionar(midia) },
                    aoAlternarFavorito = { aoAlternarFavorito(midia) }
                )
            }
        }
    }
}

@Composable
private fun CardMidia(
    midia: Midia,
    favorito: Boolean,
    aoClicar: () -> Unit,
    aoAlternarFavorito: () -> Unit
) {
    var focado by remember { mutableStateOf(false) }
    var favoritoFocado by remember { mutableStateOf(false) }
    val contexto = LocalContext.current
    val pedidoImagem = remember(midia.imagemUrl) {
        ImageRequest.Builder(contexto)
            .data(midia.imagemUrl.takeIf { it.isNotBlank() })
            .size(360, 240)
            .crossfade(false)
            .build()
    }

    val descricaoAcessivel = buildString {
        append(midia.titulo)
        if (midia.progresso != null) {
            append(", ${(midia.progresso * 100).toInt()} por cento assistido")
        }
        append(if (favorito) ". Está nos favoritos." else ". Não está nos favoritos.")
    }

    Column(modifier = Modifier.width(180.dp)) {
        EvoluxClickableSurface(
            onClick = aoClicar,
            containerColor = Color(0xFF12172A),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focado = it.isFocused }
                .scale(if (focado) 1.08f else 1f)
                .semantics(mergeDescendants = true) {
                    contentDescription = descricaoAcessivel
                }
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                ) {
                    AsyncImage(
                        model = pedidoImagem,
                        placeholder = painterResource(R.drawable.evolux_logo),
                        error = painterResource(R.drawable.evolux_logo),
                        fallback = painterResource(R.drawable.evolux_logo),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                    )
                    if (midia.progresso != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Color.White.copy(alpha = 0.25f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(midia.progresso.coerceIn(0f, 1f))
                                    .background(Dourado)
                            )
                        }
                    }
                }
                Text(
                    text = midia.titulo.uppercase(),
                    color = TextoClaro,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        EvoluxClickableSurface(
            onClick = aoAlternarFavorito,
            containerColor = Color(0xFF1A2238),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { favoritoFocado = it.isFocused }
                .semantics(mergeDescendants = true) {
                    contentDescription = if (favorito) {
                        "Remover ${midia.titulo} dos favoritos"
                    } else {
                        "Adicionar ${midia.titulo} aos favoritos"
                    }
                }
        ) {
            Text(
                text = if (favorito) "★  FAVORITO" else "☆  FAVORITAR",
                color = if (favorito) Dourado else TextoClaro,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}
