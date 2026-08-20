package com.evolux.tv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.evolux.tv.R
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.TextoClaro

enum class Tela(val rotulo: String, val icone: ImageVector) {
    INICIO("Início", Icons.Filled.Home),
    TV_AO_VIVO("TV ao Vivo", Icons.Filled.PlayArrow),
    FILMES("Filmes", Icons.Filled.List),
    SERIES("Séries", Icons.Filled.List),
    JOGOS("Jogos do Dia", Icons.Filled.Star),
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
    Image(
        painter = painterResource(R.drawable.evolux_logo),
        contentDescription = "Logo Evolux",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .width(176.dp)
            .height(62.dp)
    )
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
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = corFundo),
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
