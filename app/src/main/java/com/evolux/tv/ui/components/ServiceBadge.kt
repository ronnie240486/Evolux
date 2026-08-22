package com.evolux.tv.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Text
import com.evolux.tv.R

private data class ServiceBadgeInfo(
    @DrawableRes val drawable: Int,
    val label: String
)

@Composable
fun FileiraLogosServicos(
    servicos: List<String>,
    aoSelecionar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (servicos.isEmpty()) return
    TvLazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(servicos.distinct()) { servico ->
            EmblemaServico(
                nome = servico,
                aoClicar = { aoSelecionar(servico) }
            )
        }
    }
}

@Composable
fun EmblemaServico(
    nome: String,
    aoClicar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val info = infoServico(nome) ?: return

    EvoluxClickableSurface(
        onClick = aoClicar,
        containerColor = Color.Transparent,
        focusedColor = Color(0x332E456F),
        borderColor = Color(0xFFFFD56A),
        shape = RoundedCornerShape(18.dp),
        focusedScale = 1.08f,
        focusedBorderWidth = 3.dp,
        modifier = modifier
            .width(142.dp)
            .height(110.dp)
            .semantics { contentDescription = "Abrir séries de ${info.label}" }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(info.drawable),
                contentDescription = info.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
            )
            Text(
                text = info.label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 1.dp)
            )
        }
    }
}

private fun infoServico(nome: String): ServiceBadgeInfo? {
    val normalizado = nome.lowercase()
    return when {
        "netflix" in normalizado -> ServiceBadgeInfo(R.drawable.service_badge_netflix, "Netflix")
        "prime" in normalizado -> ServiceBadgeInfo(R.drawable.service_badge_prime_video, "Prime Video")
        "apple" in normalizado -> ServiceBadgeInfo(R.drawable.service_badge_apple_tv, "Apple TV+")
        "disney" in normalizado -> ServiceBadgeInfo(R.drawable.service_badge_disney_plus, "Disney+")
        "pluto" in normalizado -> ServiceBadgeInfo(R.drawable.service_badge_pluto_tv, "Pluto TV")
        "star plus" in normalizado || "star+" in normalizado -> ServiceBadgeInfo(R.drawable.service_badge_star_plus, "Star+")
        "max" in normalizado || "hbo" in normalizado -> ServiceBadgeInfo(R.drawable.service_badge_hbo_max, "HBO Max")
        "globoplay" in normalizado -> ServiceBadgeInfo(R.drawable.service_badge_globoplay, "Globoplay")
        "paramount" in normalizado -> ServiceBadgeInfo(R.drawable.service_badge_paramount, "Paramount+")
        "crunchyroll" in normalizado -> ServiceBadgeInfo(R.drawable.service_badge_crunchyroll, "Crunchyroll")
        "funimation" in normalizado -> ServiceBadgeInfo(R.drawable.service_badge_funimation, "Funimation")
        "discovery" in normalizado -> ServiceBadgeInfo(R.drawable.service_badge_discovery_plus, "Discovery+")
        "reelshort" in normalizado -> ServiceBadgeInfo(R.drawable.service_badge_reelshort, "Reelshort")
        else -> null
    }
}
