package com.evolux.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.evolux.tv.ui.components.EvoluxClickableSurface
import androidx.tv.material3.Text
import com.evolux.tv.data.Jogo
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.FundoCard
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro

@Composable
fun GamesScreen(
    jogos: List<Jogo>,
    aoAbrirJogo: (Jogo) -> Unit
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = "JOGOS",
            color = Dourado,
            style = MaterialTheme.typography.headlineSmall
        )
        if (jogos.isEmpty()) {
            Text(
                text = "Nenhum jogo disponível na playlist autorizada.",
                color = TextoCinza,
                modifier = Modifier.padding(top = 20.dp)
            )
            return@Column
        }
        LazyColumn(
            modifier = Modifier.padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(jogos) { jogo ->
                EvoluxClickableSurface(
                    onClick = { aoAbrirJogo(jogo) },
                    containerColor = FundoCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${jogo.timeCasaSigla} x ${jogo.timeVisitanteSigla}",
                            color = TextoClaro
                        )
                        Text(
                            text = "${jogo.horario} • ${jogo.campeonato}",
                            color = TextoCinza
                        )
                    }
                }
            }
        }
    }
}
