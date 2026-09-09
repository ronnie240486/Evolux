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
import com.evolux.tv.data.ordenarCategorias
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
    aoMudarOrdem: (String, OrdemCatalogo) -> Unit = { _, _ -> },
    ordemCategoriasCanais: List<String> = emptyList(),
    ordemCategoriasFilmes: List<String> = emptyList(),
    ordemCategoriasSeries: List<String> = emptyList(),
    aoSalvarOrdemCategorias: (String, List<String>) -> Unit = { _, _ -> },
    pinAdulto: String? = null,
    aoSalvarPin: (String?) -> Unit = {}
) {
    var mostrarPlaylists by remember { mutableStateOf(false) }
    var mostrarCategorias by remember { mutableStateOf(false) }
    var mostrarOrdenacao by remember { mutableStateOf(false) }
    var mostrarReordenarCategorias by remember { mutableStateOf(false) }
    var mostrarConfigPin by remember { mutableStateOf(false) }
    var dialogoPin by remember { mutableStateOf<String?>(null) } // "criar" | "remover"
    var erroPin by remember { mutableStateOf<String?>(null) }

    dialogoPin?.let { modo ->
        PinEntryDialog(
            titulo = if (modo == "remover") "Remover PIN" else if (pinAdulto == null) "Criar PIN adulto" else "Trocar PIN adulto",
            subtitulo = if (modo == "remover") "Digite o PIN atual para remover a proteção." else "Digite um PIN de 4 a 6 dígitos.",
            erro = erroPin,
            aoCancelar = { dialogoPin = null; erroPin = null },
            aoConfirmar = { digitado ->
                if (modo == "remover") {
                    if (digitado == pinAdulto) {
                        aoSalvarPin(null)
                        dialogoPin = null
                        erroPin = null
                    } else {
                        erroPin = "PIN incorreto."
                    }
                } else {
                    if (digitado.length < 4) {
                        erroPin = "Use pelo menos 4 dígitos."
                    } else {
                        aoSalvarPin(digitado)
                        dialogoPin = null
                        erroPin = null
                    }
                }
            }
        )
    }

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
                titulo = "Posição das categorias",
                descricao = "Escolher a ordem em que Filmes, Séries e Canais aparecem (ex: HBO no topo)",
                aoClicar = { mostrarReordenarCategorias = !mostrarReordenarCategorias }
            )
        }
        if (mostrarReordenarCategorias) {
            item {
                SeletorOrdemCategorias(
                    "TV ao vivo", categoriasCanais, ordemCategoriasCanais,
                    aoSalvar = { aoSalvarOrdemCategorias("canais", it) }
                )
            }
            item {
                SeletorOrdemCategorias(
                    "Filmes", categoriasFilmes, ordemCategoriasFilmes,
                    aoSalvar = { aoSalvarOrdemCategorias("filmes", it) }
                )
            }
            item {
                SeletorOrdemCategorias(
                    "Séries", categoriasSeries, ordemCategoriasSeries,
                    aoSalvar = { aoSalvarOrdemCategorias("series", it) }
                )
            }
        }
        item {
            LinhaConfig(
                titulo = if (pinAdulto == null) "Criar PIN de conteúdo adulto" else "PIN de conteúdo adulto ativo",
                descricao = if (pinAdulto == null) {
                    "Categorias adultas ficam sempre no fim da lista; crie um PIN pra exigir senha ao abrir"
                } else {
                    "Categorias adultas protegidas e no fim da lista"
                },
                aoClicar = { mostrarConfigPin = !mostrarConfigPin }
            )
        }
        if (mostrarConfigPin) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EvoluxClickableSurface(
                        onClick = { dialogoPin = "criar" },
                        containerColor = Color(0xFF12172A)
                    ) {
                        Text(
                            if (pinAdulto == null) "Criar PIN" else "Trocar PIN",
                            color = TextoClaro,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                    if (pinAdulto != null) {
                        EvoluxClickableSurface(
                            onClick = { dialogoPin = "remover" },
                            containerColor = Color(0xFF2A2030)
                        ) {
                            Text("Remover PIN", color = Dourado, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                        }
                    }
                }
            }
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
private fun SeletorOrdemCategorias(
    secao: String,
    categoriasBrutas: List<String>,
    ordemSalva: List<String>,
    aoSalvar: (List<String>) -> Unit
) {
    var ordemAtual by remember(categoriasBrutas, ordemSalva) {
        mutableStateOf(ordenarCategorias(categoriasBrutas, ordemSalva))
    }
    var selecionada by remember(secao) { mutableStateOf<String?>(null) }

    fun mover(indice: Int, delta: Int) {
        val novoIndice = indice + delta
        if (novoIndice !in ordemAtual.indices) return
        val nova = ordemAtual.toMutableList()
        val temp = nova[indice]
        nova[indice] = nova[novoIndice]
        nova[novoIndice] = temp
        ordemAtual = nova
        aoSalvar(nova)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 6.dp)) {
        Text(secao, color = Dourado, fontWeight = FontWeight.Bold)
        if (categoriasBrutas.isEmpty()) {
            Text("Nenhuma categoria disponível", color = TextoCinza, style = MaterialTheme.typography.bodySmall)
        } else {
            Text(
                "Toque numa categoria pra selecionar, depois use ▲ ▼ pra mover",
                color = TextoCinza,
                style = MaterialTheme.typography.bodySmall
            )
            ordemAtual.forEachIndexed { indice, categoria ->
                val estaSelecionada = categoria == selecionada
                EvoluxClickableSurface(
                    onClick = { selecionada = if (estaSelecionada) null else categoria },
                    containerColor = if (estaSelecionada) Color(0xFF283454) else Color(0xFF12172A),
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(categoria, color = TextoClaro)
                        if (estaSelecionada) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                EvoluxClickableSurface(
                                    onClick = { mover(indice, -1) },
                                    containerColor = Color(0xFF1B2238)
                                ) {
                                    Text("▲", color = Dourado, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                                }
                                EvoluxClickableSurface(
                                    onClick = { mover(indice, 1) },
                                    containerColor = Color(0xFF1B2238)
                                ) {
                                    Text("▼", color = Dourado, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                                }
                            }
                        }
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
