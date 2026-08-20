package com.evolux.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.FundoCard
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro

@Composable
fun CatalogoSummaryPanel(
    canais: Int,
    filmes: Int,
    series: Int,
    aoAbrirCanais: () -> Unit,
    aoAbrirFilmes: () -> Unit,
    aoAbrirSeries: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        colors = androidx.tv.material3.SurfaceDefaults.colors(containerColor = Color(0xE60E1424)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "SEU CATÁLOGO",
                color = Dourado,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Conteúdo carregado da playlist autorizada",
                color = TextoCinza,
                style = MaterialTheme.typography.bodySmall
            )
            ResumoLinha("TV AO VIVO", canais, aoAbrirCanais)
            ResumoLinha("FILMES", filmes, aoAbrirFilmes)
            ResumoLinha("SÉRIES", series, aoAbrirSeries)
        }
    }
}

@Composable
private fun ResumoLinha(
    titulo: String,
    quantidade: Int,
    aoClicar: () -> Unit
) {
    Surface(
        onClick = aoClicar,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = FundoCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(titulo, color = TextoClaro)
            Text("$quantidade itens", color = Dourado)
        }
    }
}
