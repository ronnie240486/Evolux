package com.evolux.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme

val FundoEscuro = Color(0xFF0A0E1A)
val FundoCard = Color(0xFF12172A)
val Dourado = Color(0xFFD7B56D)
val TextoClaro = Color(0xFFF4F1EA)
val TextoCinza = Color(0xFFB9BECC)

@Composable
fun EvoluxTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
