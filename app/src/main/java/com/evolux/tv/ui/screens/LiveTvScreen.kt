package com.evolux.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items as tvRowItems
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.evolux.tv.data.Canal
import com.evolux.tv.data.OrdemCatalogo
import com.evolux.tv.data.categoriaChave
import com.evolux.tv.data.categoriasReaisDeCanais
import com.evolux.tv.data.filtrarEOrdenarCanais
import com.evolux.tv.ui.components.EvoluxClickableSurface
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LiveTvScreen(
    canais: List<Canal>,
    canaisFavoritos: List<Canal> = emptyList(),
    aoAbrirCanal: (Canal) -> Unit,
    aoAlternarFavorito: (Canal) -> Unit = {},
    categoriasOcultas: Set<String> = emptySet(),
    ordemInicial: OrdemCatalogo = OrdemCatalogo.PADRAO,
    aoMudarOrdem: (OrdemCatalogo) -> Unit = {}
) {
    val indice by produceState<IndiceCanal?>(null, canais) {
        value = withContext(Dispatchers.Default) { construirIndiceCanal(canais) }
    }
    val categorias = remember(indice) {
        listOf("Todos", "Favoritos") + (indice?.categorias ?: emptyList())
            .filterNot { categoriaChave(it) == categoriaChave("Favoritos") }
    }
    var categoriaSelecionada by remember(categorias) {
        mutableStateOf("Todos")
    }
    var busca by remember(categorias) { mutableStateOf("") }
    var ordem by remember(canais, ordemInicial) { mutableStateOf(ordemInicial) }
    val canaisFiltrados by produceState<List<Canal>>(
        emptyList(), indice, canaisFavoritos, busca, categoriaSelecionada, ordem
    ) {
        value = withContext(Dispatchers.Default) {
            val base = when {
                indice == null -> emptyList()
                categoriaSelecionada == "Todos" -> indice!!.todos
                categoriaSelecionada == "Favoritos" -> canaisFavoritos
                else -> indice!!.porCategoria[categoriaChave(categoriaSelecionada)].orEmpty()
            }
            filtrarEOrdenarCanais(base, busca, "Todos", ordem, emptySet())
        }
    }
    // Enquanto a família é calculada em segundo plano, não deixe a tela parecer vazia.
    val canaisParaExibir = if (canaisFiltrados.isEmpty() && canais.isNotEmpty() && busca.isBlank() && categoriaSelecionada == "Todos") {
        canais.take(30)
    } else {
        canaisFiltrados
    }
    val tamanhoPagina = 30
    var pagina by remember(canais, categorias, categoriaSelecionada, busca, ordem) { mutableIntStateOf(0) }
    val totalPaginas = ((canaisParaExibir.size + tamanhoPagina - 1) / tamanhoPagina).coerceAtLeast(1)
    val paginaAtual = pagina.coerceIn(0, totalPaginas - 1)
    val canaisDaPagina = remember(canaisParaExibir, paginaAtual) {
        canaisParaExibir.drop(paginaAtual * tamanhoPagina).take(tamanhoPagina)
    }

    Column(modifier = Modifier.padding(24.dp)) {
        Text("TV AO VIVO", color = Dourado, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        CampoBuscaCanais(busca, { busca = it })
        Spacer(Modifier.height(10.dp))
        TvLazyRow(modifier = Modifier.focusGroup(), contentPadding = PaddingValues(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tvRowItems(categorias) { categoria ->
                EvoluxClickableSurface(
                    onClick = {
                        categoriaSelecionada = categoria
                    },
                    containerColor = if (categoria == categoriaSelecionada) Color(0xFF283454) else Color(0xFF12172A),
                    borderColor = Dourado
                ) {
                    Text(
                        categoria,
                        color = if (categoria == categoriaSelecionada) Dourado else TextoClaro,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        TvLazyRow(modifier = Modifier.focusGroup(), contentPadding = PaddingValues(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tvRowItems(listOf(OrdemCatalogo.PADRAO, OrdemCatalogo.NOME_AZ, OrdemCatalogo.NOME_ZA)) { opcao ->
                EvoluxClickableSurface(
                    onClick = { ordem = opcao; aoMudarOrdem(opcao) },
                    containerColor = if (opcao == ordem) Dourado else Color(0xFF12172A),
                    borderColor = if (opcao == ordem) Dourado else Color(0xFF36415A)
                ) {
                    Text(
                        opcao.rotulo,
                        color = if (opcao == ordem) Color(0xFF111111) else TextoClaro,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (canaisParaExibir.isNotEmpty() && totalPaginas > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EvoluxClickableSurface(
                    onClick = { if (paginaAtual > 0) pagina-- },
                    containerColor = if (paginaAtual > 0) Color(0xFF12172A) else Color.Transparent,
                    modifier = Modifier.width(54.dp).height(42.dp)
                ) { Text("‹", color = TextoClaro, style = MaterialTheme.typography.titleLarge) }
                Text(
                    text = "Página ${paginaAtual + 1}/$totalPaginas • ${canaisParaExibir.size} canais",
                    color = TextoCinza,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                EvoluxClickableSurface(
                    onClick = { if (paginaAtual < totalPaginas - 1) pagina++ },
                    containerColor = if (paginaAtual < totalPaginas - 1) Color(0xFF12172A) else Color.Transparent,
                    modifier = Modifier.width(54.dp).height(42.dp)
                ) { Text("›", color = TextoClaro, style = MaterialTheme.typography.titleLarge) }
            }
            Spacer(Modifier.height(10.dp))
        }
        if (canaisParaExibir.isEmpty()) {
            Text(if (canais.isEmpty()) "Nenhum canal disponível." else "Nenhum canal encontrado.", color = TextoCinza)
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val colunas = when {
                    maxWidth < 420.dp -> 2
                    maxWidth < 760.dp -> 3
                    else -> 5
                }
                TvLazyVerticalGrid(
                    modifier = Modifier.focusGroup().fillMaxWidth(),
                    columns = TvGridCells.Fixed(colunas),
                    horizontalArrangement = Arrangement.spacedBy(if (colunas == 2) 10.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                        items(canaisDaPagina, key = { it.id }) { canal ->
                        CardCanal(
                            canal = canal,
                            favorito = canaisFavoritos.any { it.id == canal.id },
                            aoClicar = { aoAbrirCanal(canal) },
                            aoLongClick = { aoAlternarFavorito(canal) }
                        )
                    }
                }
            }
        }
    }
}

private data class IndiceCanal(
    val todos: List<Canal>,
    val categorias: List<String>,
    val porCategoria: Map<String, List<Canal>>
)

private fun construirIndiceCanal(canais: List<Canal>): IndiceCanal {
    val nomesPorChave = LinkedHashMap<String, String>()
    val porCategoria = LinkedHashMap<String, MutableList<Canal>>()
    canais.forEach { canal ->
        val categoria = canal.categoria.ifBlank { "TV ao vivo" }.trim()
        val chave = categoriaChave(categoria)
        nomesPorChave.putIfAbsent(chave, categoria)
        porCategoria.getOrPut(chave) { ArrayList() }.add(canal)
    }
    return IndiceCanal(
        todos = canais,
        categorias = categoriasReaisDeCanais(canais),
        porCategoria = porCategoria
    )
}

@Composable
private fun CampoBuscaCanais(valor: String, aoMudar: (String) -> Unit) {
    var focado by remember { mutableStateOf(false) }
    EvoluxClickableSurface(
        onClick = {},
        containerColor = Color(0xFF12172A),
        borderColor = if (focado) Dourado else Color(0xFF36415A),
        modifier = Modifier.fillMaxWidth()
    ) {
        BasicTextField(
            value = valor,
            onValueChange = aoMudar,
            singleLine = true,
            textStyle = TextStyle(color = TextoClaro, fontSize = MaterialTheme.typography.bodyLarge.fontSize),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focado = it.isFocused }.padding(horizontal = 16.dp, vertical = 13.dp),
            decorationBox = { campo ->
                Box {
                    if (valor.isBlank()) Text("Buscar canais...", color = TextoCinza)
                    campo()
                }
            }
        )
    }
}

@Composable
private fun CardCanal(
    canal: Canal,
    favorito: Boolean,
    aoClicar: () -> Unit,
    aoLongClick: () -> Unit
) {
    EvoluxClickableSurface(
        onClick = aoClicar,
        onLongClick = aoLongClick,
        containerColor = Color(0xFF12172A),
        modifier = Modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = canal.logoUrl.takeIf { it.isNotBlank() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.height(8.dp))
            Text(canal.nome, color = TextoClaro, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                if (favorito) "★ Favorito" else "Segure para favoritar",
                color = if (favorito) Dourado else TextoCinza,
                style = MaterialTheme.typography.labelSmall
            )
            Text(canal.categoria, color = TextoCinza, style = MaterialTheme.typography.labelSmall)
        }
    }
}
