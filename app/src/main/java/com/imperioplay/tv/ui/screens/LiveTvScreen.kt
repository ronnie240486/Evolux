package com.imperioplay.tv.ui.screens

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
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.imperioplay.tv.data.Canal
import com.imperioplay.tv.data.SampleData
import com.imperioplay.tv.ui.theme.Dourado
import com.imperioplay.tv.ui.theme.TextoCinza
import com.imperioplay.tv.ui.theme.TextoClaro

@Composable
fun LiveTvScreen(aoAbrirCanal: (Canal) -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            "TV AO VIVO",
            color = Dourado,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(20.dp))
        TvLazyVerticalGrid(
            columns = TvGridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(SampleData.canaisAoVivo) { canal ->
                CardCanal(canal, aoClicar = { aoAbrirCanal(canal) })
            }
        }
    }
}

@Composable
private fun CardCanal(canal: Canal, aoClicar: () -> Unit) {
    var focado by remember { mutableStateOf(false) }
    Surface(
        onClick = aoClicar,
        shape = RoundedCornerShape(12.dp),
        colors = SurfaceDefaults.colors(containerColor = Color(0xFF12172A)),
        modifier = Modifier
            .onFocusChanged { focado = it.isFocused }
            .border(
                width = if (focado) 3.dp else 0.dp,
                color = if (focado) Dourado else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
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
