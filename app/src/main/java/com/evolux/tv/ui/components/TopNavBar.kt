package com.evolux.tv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
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
    JOGOS("Jogos", Icons.Filled.Star),
    FAVORITOS("Favoritos", Icons.Filled.Favorite),
    CONFIGURACOES("Configurações", Icons.Filled.Settings)
}

@Composable
fun TopNavBar(
    telaSelecionada: Tela,
    aoSelecionar: (Tela) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequesterInicial = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    LaunchedEffect(Unit) {
        focusRequesterInicial.requestFocus()
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compacto = maxWidth < 700.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = if (compacto) 12.dp else 24.dp, vertical = if (compacto) 10.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compacto) 8.dp else 14.dp)
        ) {
            LogoEvolux(compacto)
            Spacer(modifier = Modifier.width(if (compacto) 4.dp else 14.dp))
            Tela.values().forEach { tela ->
                ItemNav(
                    tela = tela,
                    selecionado = tela == telaSelecionada,
                    aoClicar = { aoSelecionar(tela) },
                    compacto = compacto,
                    focusRequester = if (tela == telaSelecionada) focusRequesterInicial else null
                )
            }
        }
    }
}

@Composable
private fun LogoEvolux(compacto: Boolean) {
    Image(
        painter = painterResource(R.drawable.evolux_logo),
        contentDescription = "Logo Evolux",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .width(if (compacto) 108.dp else 176.dp)
            .padding(vertical = if (compacto) 2.dp else 0.dp)
    )
}

@Composable
private fun ItemNav(
    tela: Tela,
    selecionado: Boolean,
    aoClicar: () -> Unit,
    compacto: Boolean,
    focusRequester: FocusRequester? = null
) {
    var focado by remember { mutableStateOf(false) }
    val corBorda = if (focado || selecionado) Dourado else Color.Transparent
    val corFundo = if (selecionado) Color(0xFF202A47) else Color(0xD912172A)

    Surface(
        onClick = aoClicar,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = corFundo),
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focado = it.isFocused }
            .border(
                width = if (focado || selecionado) 2.dp else 1.dp,
                color = corBorda,
                shape = RoundedCornerShape(12.dp)
            )
            .semantics {
                contentDescription = tela.rotulo
                role = Role.Tab
                selected = selecionado
            }
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compacto) 12.dp else 16.dp,
                vertical = if (compacto) 10.dp else 12.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = tela.icone,
                contentDescription = null,
                tint = if (selecionado || focado) Dourado else TextoClaro
            )
            Text(
                text = tela.rotulo,
                color = if (selecionado || focado) Dourado else TextoClaro,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
