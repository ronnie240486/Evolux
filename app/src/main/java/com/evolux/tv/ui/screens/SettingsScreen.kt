package com.evolux.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.evolux.tv.data.OrdemCatalogo
import com.evolux.tv.ui.components.EvoluxClickableSurface
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro

@Composable
fun SettingsScreen(
    aoTrocarMac: () -> Unit,
    playlistUrls: List<String> = emptyList(),
    playlistAtiva: Int = 0,
    aoSelecionarPlaylist: (Int) -> Unit = {},
    aoRecarregarCatalogo: () -> Unit = {},
    categoriasCanais: List<String> = emptyList(),
    categoriasFilmes: List<String> = emptyList(),
    categoriasSeries: List<String> = emptyList(),
    categoriasOcultas: Set<String> = emptySet(),
    aoAlternarCategoriaOculta: (String, String) -> Unit = { _, _ -> },
    ordens: Map<String, OrdemCatalogo> = emptyMap(),
    aoMudarOrdem: (String, OrdemCatalogo) -> Unit = { _, _ -> }
) {
    var mostrarPlaylists by remember { mutableStateOf(false) }
    var mostrarCategorias by remember { mutableStateOf(false) }
    var mostrarOrdenacao by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("CONFIGURAÇÕES", color = Dourado, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
        }
        item {
            LinhaConfig(
                titulo = "Conta",
                descricao = "MAC autorizado e sessão do aparelho",
                aoClicar = aoTrocarMac
            )
        }
        item {
            LinhaConfig(
                titulo = "Trocar lista",
                descricao = if (playlistUrls.isEmpty()) "Nenhuma lista recebida pelo painel" else "${playlistUrls.size} lista(s) sincronizada(s)",
                aoClicar = { mostrarPlaylists = !mostrarPlaylists }
            )
        }
        if (mostrarPlaylists) {
            items(playlistUrls.indices.toList()) { indice ->
                val url = playlistUrls[indice]
                LinhaConfig(
                    titulo = if (indice == playlistAtiva) "✓ Lista ${indice + 1} ativa" else "Lista ${indice + 1}",
                    descricao = hostSeguro(url),
                    aoClicar = { aoSelecionarPlaylist(indice) },
                    recuada = true
                )
            }
        }
        item {
            LinhaConfig(
                titulo = "Atualizar agora",
                descricao = "Consultar o painel e recarregar somente se a lista mudou",
                aoClicar = aoRecarregarCatalogo
            )
        }
        item {
            LinhaConfig(
                titulo = "Categorias ocultas",
                descricao = "Ocultar grupos de canais, filmes ou séries",
                aoClicar = { mostrarCategorias = !mostrarCategorias }
            )
        }
        if (mostrarCategorias) {
            item { SeletorOcultas("live", "TV ao vivo", categoriasCanais, categoriasOcultas, aoAlternarCategoriaOculta) }
            item { SeletorOcultas("filmes", "Filmes", categoriasFilmes, categoriasOcultas, aoAlternarCategoriaOculta) }
            item { SeletorOcultas("series", "Séries", categoriasSeries, categoriasOcultas, aoAlternarCategoriaOculta) }
        }
        item {
            LinhaConfig(
                titulo = "Ordenação",
                descricao = "Salvar ordem dos canais, filmes e séries",
                aoClicar = { mostrarOrdenacao = !mostrarOrdenacao }
            )
        }
        if (mostrarOrdenacao) {
            item { SeletorOrdem("Canais", ordens["canais"] ?: OrdemCatalogo.PADRAO, aoMudarOrdem) }
            item { SeletorOrdem("Filmes", ordens["filmes"] ?: OrdemCatalogo.PADRAO, aoMudarOrdem) }
            item { SeletorOrdem("Séries", ordens["series"] ?: OrdemCatalogo.PADRAO, aoMudarOrdem) }
        }
        item {
            LinhaConfig(
                titulo = "Sobre o aplicativo",
                descricao = "Evolux • catálogo autorizado e player interno",
                aoClicar = {}
            )
        }
    }
}

@Composable
private fun SeletorOcultas(
    chaveSecao: String,
    secao: String,
    categorias: List<String>,
    ocultas: Set<String>,
    aoAlternar: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 18.dp)) {
        Text(secao, color = Dourado, fontWeight = FontWeight.Bold)
        if (categorias.isEmpty()) {
            Text("Nenhuma categoria disponível", color = TextoCinza, style = MaterialTheme.typography.bodySmall)
        } else {
            categorias.forEach { categoria ->
                val chave = "$chaveSecao|$categoria"
                val escondida = chave in ocultas
                EvoluxClickableSurface(
                    onClick = { aoAlternar(chaveSecao, categoria) },
                    containerColor = if (escondida) Color(0xFF2A2030) else Color(0xFF12172A),
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(categoria, color = TextoClaro)
                        Text(if (escondida) "OCULTA" else "VISÍVEL", color = if (escondida) Dourado else TextoCinza)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeletorOrdem(secao: String, selecionada: OrdemCatalogo, aoMudar: (String, OrdemCatalogo) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 18.dp)) {
        Text(secao, color = Dourado, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 5.dp)) {
            OrdemCatalogo.entries.forEach { ordem ->
                EvoluxClickableSurface(
                    onClick = { aoMudar(secao.lowercase(), ordem) },
                    containerColor = if (ordem == selecionada) Dourado else Color(0xFF12172A)
                ) {
                    Text(
                        ordem.rotulo,
                        color = if (ordem == selecionada) Color(0xFF111111) else TextoClaro,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaConfig(
    titulo: String,
    descricao: String,
    aoClicar: () -> Unit,
    recuada: Boolean = false
) {
    EvoluxClickableSurface(
        onClick = aoClicar,
        containerColor = Color(0xFF12172A),
        modifier = Modifier.fillMaxWidth().padding(start = if (recuada) 18.dp else 0.dp).semantics(mergeDescendants = true) {
            contentDescription = "$titulo. $descricao"
        }
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(titulo, color = TextoClaro, fontWeight = FontWeight.SemiBold)
            Text(descricao, color = TextoCinza, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun hostSeguro(url: String): String = runCatching {
    java.net.URI(url).host?.takeIf { it.isNotBlank() } ?: "URL configurada no painel"
}.getOrDefault("URL configurada no painel")
