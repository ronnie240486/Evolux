package com.imperioplay.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val FundoEscuro = Color(0xFF0A0E1A)
val FundoCard = Color(0xFF111827)
val Dourado = Color(0xFFF2B90C)
val DouradoClaro = Color(0xFFF7D74A)
val TextoClaro = Color(0xFFF5F5F5)
val TextoCinza = Color(0xFF9CA3AF)

private val ImperioColorScheme = darkColorScheme(
    primary = Dourado,
    onPrimary = Color.Black,
    secondary = DouradoClaro,
    background = FundoEscuro,
    onBackground = TextoClaro,
    surface = FundoCard,
    onSurface = TextoClaro
)

@Composable
fun ImperioPlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ImperioColorScheme,
        content = content
    )
}
