package com.evolux.tv.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.evolux.tv.R

private data class ServiceBadgeInfo(
    @DrawableRes val drawable: Int,
    val label: String
)

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
        modifier = modifier
            .width(128.dp)
            .height(84.dp)
            .semantics { contentDescription = "Abrir séries de ${info.label}" }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(info.drawable),
                contentDescription = info.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
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
