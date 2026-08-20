package com.evolux.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.FundoCard
import com.evolux.tv.ui.theme.TextoClaro

@Composable
fun EmblemaServico(
    nome: String,
    modifier: Modifier = Modifier
) {
    val normalizado = nome.lowercase()
    val cor = when {
        "netflix" in normalizado -> Color(0xFFE50914)
        "pluto" in normalizado -> Color(0xFF6E49FF)
        "disney" in normalizado -> Color(0xFF1769FF)
        "prime" in normalizado -> Color(0xFF00A8E1)
        "max" in normalizado || "hbo" in normalizado -> Color(0xFF7C3AED)
        "globoplay" in normalizado -> Color(0xFF00BFA5)
        "paramount" in normalizado -> Color(0xFF1D72D8)
        "star" in normalizado -> Color(0xFF111111)
        "crunchyroll" in normalizado || "funimation" in normalizado -> Color(0xFFF47521)
        "apple" in normalizado -> Color(0xFF252525)
        "discovery" in normalizado -> Color(0xFF0A4D9C)
        "telecine" in normalizado -> Color(0xFF111111)
        else -> Color(0xFF243253)
    }
    val simbolo = when {
        "netflix" in normalizado -> "N"
        "pluto" in normalizado -> "P"
        "disney" in normalizado -> "D+"
        "prime" in normalizado -> "PV"
        "max" in normalizado || "hbo" in normalizado -> "MAX"
        "globoplay" in normalizado -> "G"
        "paramount" in normalizado -> "P+"
        "crunchyroll" in normalizado -> "CR"
        "funimation" in normalizado -> "F"
        "apple" in normalizado -> ""
        "discovery" in normalizado -> "D"
        else -> nome.take(2).uppercase()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FundoCard.copy(alpha = 0.86f))
            .border(1.dp, cor.copy(alpha = 0.78f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(cor)
                .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (simbolo == "") {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            } else {
                Text(
                    text = simbolo,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = nome,
            color = TextoClaro,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "SÉRIES",
            color = Dourado,
            fontWeight = FontWeight.Bold
        )
    }
}
