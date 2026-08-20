package com.evolux.tv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.evolux.tv.data.Jogo
import com.evolux.tv.data.SampleData
import com.evolux.tv.ui.components.PainelJogosDoDia
import com.evolux.tv.ui.theme.Dourado

@Composable
fun GamesScreen(aoAbrirJogo: (Jogo) -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            "JOGOS DO DIA",
            color = Dourado,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(20.dp))
        // Reaproveita o mesmo painel da Home; numa versão maior dá pra
        // trocar por uma TvLazyColumn com cada jogo em um card grande.
        PainelJogosDoDia(
            jogos = SampleData.jogosDoDia,
            aoAbrirCanal = { SampleData.jogosDoDia.firstOrNull()?.let(aoAbrirJogo) }
        )
    }
}
