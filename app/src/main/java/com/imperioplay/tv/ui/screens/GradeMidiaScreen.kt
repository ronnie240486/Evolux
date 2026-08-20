package com.imperioplay.tv.ui.screens

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.imperioplay.tv.data.Midia
import com.imperioplay.tv.ui.theme.Dourado
import com.imperioplay.tv.ui.theme.TextoClaro

/**
 * Grade reutilizável de pôsteres. Usada nas telas de Filmes, Séries e
 * Favoritos — basta passar títulos e listas diferentes.
 */
@Composable
fun GradeMidiaScreen(
    titulo: String,
    itens: List<Midia>,
    aoSelecionar: (Midia) -> Unit,
    mensagemVazio: String = "Nada por aqui ainda."
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            titulo.uppercase(),
            color = Dourado,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(20.dp))

        if (itens.isEmpty()) {
            Text(mensagemVazio, color = TextoClaro)
            return@Column
        }

        TvLazyVerticalGrid(
            columns = TvGridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(itens) { midia ->
                CardPoster(midia, aoClicar = { aoSelecionar(midia) })
            }
        }
    }
}

@Composable
private fun CardPoster(midia: Midia, aoClicar: () -> Unit) {
    var focado by remember { mutableStateOf(false) }
    Surface(
        onClick = aoClicar,
        shape = RoundedCornerShape(10.dp),
        colors = SurfaceDefaults.colors(containerColor = Color(0xFF12172A)),
        modifier = Modifier
            .onFocusChanged { focado = it.isFocused }
            .scale(if (focado) 1.06f else 1f)
            .border(
                width = if (focado) 3.dp else 0.dp,
                color = if (focado) Dourado else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .semantics(mergeDescendants = true) { contentDescription = midia.titulo }
    ) {
        Column {
            AsyncImage(
                model = midia.imagemUrl,
                contentDescription = null, // decorativa; título já cobre via semantics acima
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            )
            Text(
                text = midia.titulo.uppercase(),
                color = TextoClaro,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
