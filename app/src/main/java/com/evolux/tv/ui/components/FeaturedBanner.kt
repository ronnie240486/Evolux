package com.evolux.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.evolux.tv.R
import com.evolux.tv.data.Destaque
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.FundoCard
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro
import kotlinx.coroutines.delay

@Composable
fun FeaturedBanner(
    destaques: List<Destaque>,
    intervaloMs: Long,
    aoAssistir: (Destaque) -> Unit,
    aoVerTrailer: (Destaque) -> Unit,
    modifier: Modifier = Modifier
) {
    if (destaques.isEmpty()) return
    val indice by produceState(initialValue = 0, destaques, intervaloMs) {
        while (true) {
            delay(intervaloMs)
            value = (value + 1) % destaques.size
        }
    }
    val destaque = destaques[indice % destaques.size]

    EvoluxClickableSurface(
        onClick = { aoAssistir(destaque) },
        containerColor = FundoCard,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FundoCard),
            contentAlignment = Alignment.BottomStart
        ) {
            AsyncImage(
                model = destaque.imagemUrl.takeIf { it.isNotBlank() },
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
                    .background(Color.Black.copy(alpha = 0.58f))
            )
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = destaque.subtitulo,
                    color = Dourado,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = destaque.titulo,
                    color = TextoClaro,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = destaque.sinopse,
                    color = TextoCinza,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${indice + 1}/${destaques.size}",
                        color = Color(0xFFE5D3A2),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = destaque.ano,
                        color = TextoCinza,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
