package com.evolux.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.TextoClaro

enum class Tela(val rotulo: String, val icone: ImageVector) {
    INICIO("Início", Icons.Filled.Home),
    TV_AO_VIVO("TV ao Vivo", Icons.Filled.LiveTv),
    FILMES("Filmes", Icons.Filled.Movie),
    SERIES("Séries", Icons.Filled.Tv),
    JOGOS("Jogos do Dia", Icons.Filled.SportsSoccer),
    FAVORITOS("Favoritos", Icons.Filled.Star),
    CONFIGURACOES("Configurações", Icons.Filled.Settings)
}

@Composable
fun TopNavBar(
    telaSelecionada: Tela,
    aoSelecionar: (Tela) -> Unit,
    modifier: Modifier = Modifier
) {
    // Foco inicial: numa TV, se nada estiver focado quando o app abre, o
    // D-pad simplesmente não responde. Focamos automaticamente a aba
    // selecionada assim que a barra aparece na tela.
    val focusRequesterInicial = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequesterInicial.requestFocus()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LogoEvolux()
        Spacer(modifier = Modifier.width(24.dp))
        Tela.values().forEach { tela ->
            ItemNav(
                tela = tela,
                selecionado = tela == telaSelecionada,
                aoClicar = { aoSelecionar(tela) },
                focusRequester = if (tela == telaSelecionada) focusRequesterInicial else null
            )
        }
    }
}

@Composable
private fun LogoEvolux() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        // O logo é puramente decorativo — sem isso o TalkBack leria
        // "I P, Evolux" como dois elementos separados e focáveis.
        modifier = Modifier.clearAndSetSemantics {}
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            colors = androidx.tv.material3.SurfaceDefaults.colors(containerColor = Dourado),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("IP", color = Color.Black, fontWeight = FontWeight.Black)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "IMPÉRIO PLAY",
            color = Dourado,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun ItemNav(
    tela: Tela,
    selecionado: Boolean,
    aoClicar: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    var focado by remember { mutableStateOf(false) }

    val corBorda = if (focado || selecionado) Dourado else Color.Transparent
    val corFundo = if (selecionado) Color(0xFF1A2035) else Color(0xFF12172A)

    Surface(
        onClick = aoClicar,
        shape = RoundedCornerShape(10.dp),
        colors = androidx.tv.material3.SurfaceDefaults.colors(containerColor = corFundo),
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focado = it.isFocused }
            .border(2.dp, corBorda, RoundedCornerShape(10.dp))
            // Um único nó de acessibilidade por aba: TalkBack lê
            // "Início, aba, selecionada" em vez de ler o ícone e o
            // texto como dois elementos.
            .semantics {
                contentDescription = tela.rotulo
                role = Role.Tab
                selected = selecionado
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // contentDescription = null: o ícone é decorativo aqui, o
            // texto ao lado (e o semantics do Surface acima) já cobre
            // a descrição para leitores de tela.
            Icon(imageVector = tela.icone, contentDescription = null, tint = if (selecionado) Dourado else TextoClaro)
            Text(text = tela.rotulo.uppercase(), color = if (selecionado) Dourado else TextoClaro, fontWeight = FontWeight.Bold)
        }
    }
}
