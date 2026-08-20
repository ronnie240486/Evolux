package com.evolux.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Text
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro

data class OpcaoConfig(val titulo: String, val descricao: String)

private val opcoes = listOf(
    OpcaoConfig("Conta", "MAC autorizado e sessão do aparelho"),
    OpcaoConfig("Qualidade de vídeo", "Automática, 480p, 720p, 1080p, 4K"),
    OpcaoConfig("Legendas e áudio", "Idioma padrão de legenda e faixa de áudio"),
    OpcaoConfig("Controle parental", "Bloqueio por classificação indicativa"),
    OpcaoConfig("Sobre o aplicativo", "Versão e informações do Evolux")
)

@Composable
fun SettingsScreen(
    aoTrocarMac: () -> Unit
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            "CONFIGURAÇÕES",
            color = Dourado,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(20.dp))
        opcoes.forEach { opcao ->
            LinhaConfig(
                opcao = opcao,
                aoClicar = if (opcao.titulo == "Conta") aoTrocarMac else { {} }
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LinhaConfig(
    opcao: OpcaoConfig,
    aoClicar: () -> Unit
) {
    var focado by remember { mutableStateOf(false) }
    Surface(
        onClick = aoClicar,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFF12172A)),
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .onFocusChanged { focado = it.isFocused }
            .border(
                width = if (focado) 2.dp else 0.dp,
                color = if (focado) Dourado else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "${opcao.titulo}. ${opcao.descricao}"
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(opcao.titulo, color = TextoClaro, fontWeight = FontWeight.SemiBold)
            Text(opcao.descricao, color = TextoCinza, style = MaterialTheme.typography.bodySmall)
        }
    }
}
