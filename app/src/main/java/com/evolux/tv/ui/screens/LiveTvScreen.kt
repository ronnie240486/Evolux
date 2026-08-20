package com.evolux.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.evolux.tv.data.Canal
import com.evolux.tv.ui.components.EvoluxClickableSurface
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro

@Composable
fun LiveTvScreen(
    canais: List<Canal>,
    aoAbrirCanal: (Canal) -> Unit
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            "TV AO VIVO",
            color = Dourado,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(20.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val colunas = when {
                maxWidth < 420.dp -> 2
                maxWidth < 760.dp -> 3
                else -> 5
            }
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(colunas),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (colunas == 2) 10.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(canais) { canal ->
                    CardCanal(canal, aoClicar = { aoAbrirCanal(canal) })
                }
            }
        }
    }
}

@Composable
private fun CardCanal(canal: Canal, aoClicar: () -> Unit) {
    var focado by remember { mutableStateOf(false) }
    EvoluxClickableSurface(
        onClick = aoClicar,
        containerColor = Color(0xFF12172A),
        modifier = Modifier
            .onFocusChanged { focado = it.isFocused }
            .semantics(mergeDescendants = true) {
                contentDescription = "${canal.nome}, ${canal.categoria}"
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = canal.logoUrl,
                contentDescription = null, // decorativo; nome/categoria já cobertos acima
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.height(8.dp))
            Text(canal.nome, color = TextoClaro, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(canal.categoria, color = TextoCinza, style = MaterialTheme.typography.labelSmall)
        }
    }
}
