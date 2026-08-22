package com.evolux.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.fillMaxSize
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items as tvRowItems
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.evolux.tv.R
import com.evolux.tv.data.Midia
import com.evolux.tv.data.OrdemCatalogo
import com.evolux.tv.data.categoriaChave
import com.evolux.tv.data.filtrarEOrdenarMidias
import com.evolux.tv.ui.components.EvoluxClickableSurface
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.FundoCard
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GradeMidiaScreen(
    titulo: String,
    itens: List<Midia>,
    aoSelecionar: (Midia) -> Unit,
    ehFavorito: (Midia) -> Boolean,
    aoAlternarFavorito: (Midia) -> Unit,
    mensagemVazio: String = "Nada por aqui ainda.",
    categoriasOcultas: Set<String> = emptySet(),
    ordemInicial: OrdemCatalogo = OrdemCatalogo.PADRAO,
    aoMudarOrdem: (OrdemCatalogo) -> Unit = {}
) {
    val chaveItens = Triple(itens.size, itens.firstOrNull()?.id, itens.lastOrNull()?.id)
    val indice by produceState<IndiceMidia?>(null, chaveItens) {
        value = withContext(Dispatchers.Default) { construirIndiceMidia(itens) }
    }
    val chaveIndice = indice?.let { System.identityHashCode(it) } ?: 0
    val categorias = remember(chaveIndice) { listOf("Todos") + (indice?.categorias ?: emptyList()) }
    var categoriaSelecionada by remember(categorias) {
        mutableStateOf(categorias.firstOrNull { it != "Todos" } ?: "Todos")
    }
    var busca by remember(categorias) { mutableStateOf("") }
    var ordem by remember(categorias, ordemInicial) { mutableStateOf(ordemInicial) }
    val itensFiltrados by produceState<List<Midia>>(
        emptyList(), chaveIndice, busca, categoriaSelecionada, ordem
    ) {
        value = withContext(Dispatchers.Default) {
            val base = when {
                indice == null -> emptyList()
                categoriaSelecionada == "Todos" -> indice!!.todos
                else -> indice!!.porCategoria[categoriaChave(categoriaSelecionada)].orEmpty()
            }
            filtrarEOrdenarMidias(
                itens = base,
                busca = busca,
                categoria = "Todos",
                ordem = ordem,
                categoriasOcultas = emptySet()
            )
        }
    }
    // O estado inicial da consulta é vazio enquanto o filtro roda em segundo plano.
    // Mostramos uma página imediata do catálogo para não deixar a tela aparentemente sem conteúdo.
    val itensParaExibir = if (itensFiltrados.isEmpty() && itens.isNotEmpty() && busca.isBlank()) {
        itens.take(30)
    } else {
        itensFiltrados
    }
    val tamanhoLote = 30
    var limiteVisivel by remember(chaveItens, categorias, categoriaSelecionada, busca, ordem) {
        mutableIntStateOf(tamanhoLote)
    }
    val itensDaPagina = remember(itensParaExibir, limiteVisivel) {
        itensParaExibir.take(limiteVisivel)
    }
    val carregamentoContinuo = { indice: Int ->
        if (indice >= itensDaPagina.size - 8 && itensDaPagina.size < itensParaExibir.size) {
            limiteVisivel = (limiteVisivel + tamanhoLote).coerceAtMost(itensParaExibir.size)
        }
    }

    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) {
        Text(
            titulo.uppercase(),
            color = Dourado,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(12.dp))
        CampoBusca(
            valor = busca,
            placeholder = "Buscar em ${titulo.lowercase()}...",
            aoMudar = { busca = it }
        )
        Spacer(Modifier.height(10.dp))
        TvLazyRow(
            modifier = Modifier.focusGroup(),
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tvRowItems(categorias) { categoria ->
                EvoluxClickableSurface(
                    onClick = { categoriaSelecionada = categoria },
                    containerColor = if (categoria == categoriaSelecionada) Color(0xFF283454) else Color(0xFF12172A),
                    borderColor = Dourado,
                    modifier = Modifier
                ) {
                    Text(
                        text = categoria,
                        color = if (categoria == categoriaSelecionada) Dourado else TextoClaro,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        TvLazyRow(
            modifier = Modifier.focusGroup(),
            contentPadding = PaddingValues(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tvRowItems(OrdemCatalogo.entries.toList()) { opcao ->
                EvoluxClickableSurface(
                    onClick = {
                        ordem = opcao
                        aoMudarOrdem(opcao)
                    },
                    containerColor = if (opcao == ordem) Dourado else Color(0xFF12172A),
                    borderColor = if (opcao == ordem) Dourado else Color(0xFF36415A),
                    modifier = Modifier
                ) {
                    Text(
                        text = opcao.rotulo,
                        color = if (opcao == ordem) Color(0xFF111111) else TextoClaro,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        if (itensParaExibir.isNotEmpty() && itensDaPagina.size < itensParaExibir.size) {
            Text(
                text = "Mostrando ${itensDaPagina.size} de ${itensParaExibir.size} • continue descendo para carregar mais",
                color = TextoCinza,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(Modifier.height(6.dp))
        }

        if (itensParaExibir.isEmpty()) {
            Text(
                if (itens.isEmpty()) mensagemVazio else "Nenhum item encontrado nesta categoria ou busca.",
                color = TextoCinza
            )
            return@Column
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val colunas = when {
                maxWidth < 420.dp -> 2
                maxWidth < 760.dp -> 3
                else -> 6
            }
            TvLazyVerticalGrid(
                modifier = Modifier.focusGroup().fillMaxWidth(),
                columns = TvGridCells.Fixed(colunas),
                horizontalArrangement = Arrangement.spacedBy(if (colunas == 2) 10.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                itemsIndexed(itensDaPagina, key = { _, item -> item.id }) { indice, midia ->
                    CardMidiaPoster(
                        midia = midia,
                        favorito = ehFavorito(midia),
                        aoClicar = { aoSelecionar(midia) },
                        aoAlternarFavorito = { aoAlternarFavorito(midia) },
                        aoFocar = { carregamentoContinuo(indice) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CampoBusca(valor: String, placeholder: String, aoMudar: (String) -> Unit) {
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
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focado = it.isFocused }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            decorationBox = { campo ->
                Box {
                    if (valor.isBlank()) Text(placeholder, color = TextoCinza)
                    campo()
                }
            }
        )
    }
}

private data class IndiceMidia(
    val todos: List<Midia>,
    val categorias: List<String>,
    val porCategoria: Map<String, List<Midia>>
)

private fun construirIndiceMidia(itens: List<Midia>): IndiceMidia {
    val nomesPorChave = LinkedHashMap<String, String>()
    val porCategoria = LinkedHashMap<String, MutableList<Midia>>()
    itens.forEach { item ->
        val categoria = item.categoria.ifBlank { "Sem categoria" }.trim()
        val chave = categoriaChave(categoria)
        nomesPorChave.putIfAbsent(chave, categoria)
        porCategoria.getOrPut(chave) { ArrayList() }.add(item)
    }
    return IndiceMidia(
        todos = itens,
        categorias = nomesPorChave.values.sortedWith(String.CASE_INSENSITIVE_ORDER),
        porCategoria = porCategoria
    )
}

@Composable
private fun CardMidiaPoster(
    midia: Midia,
    favorito: Boolean,
    aoClicar: () -> Unit,
    aoAlternarFavorito: () -> Unit,
    aoFocar: () -> Unit = {}
) {
    var focado by remember { mutableStateOf(false) }
    val contexto = LocalContext.current
    val pedidoImagem = remember(midia.imagemUrl) {
        ImageRequest.Builder(contexto)
            .data(midia.imagemUrl.takeIf { it.isNotBlank() })
            .size(240, 360)
            .crossfade(false)
            .build()
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        EvoluxClickableSurface(
            onClick = aoClicar,
            containerColor = Color(0xFF12172A),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    focado = it.isFocused
                    if (it.isFocused) aoFocar()
                }
                .scale(if (focado) 1.06f else 1f)
                .semantics(mergeDescendants = true) {
                    contentDescription = if (favorito) {
                        "${midia.titulo}, ${midia.categoria}, está nos favoritos"
                    } else {
                        "${midia.titulo}, ${midia.categoria}, não está nos favoritos"
                    }
                }
        ) {
            Column {
                AsyncImage(
                    model = pedidoImagem,
                    placeholder = painterResource(R.drawable.evolux_logo),
                    error = painterResource(R.drawable.evolux_logo),
                    fallback = painterResource(R.drawable.evolux_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                )
                Text(
                    text = midia.titulo.uppercase(),
                    color = TextoClaro,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp)
                )
                Text(
                    text = midia.categoria,
                    color = Dourado,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        EvoluxClickableSurface(
            onClick = aoAlternarFavorito,
            containerColor = FundoCard,
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                contentDescription = if (favorito) "Remover ${midia.titulo} dos favoritos" else "Adicionar ${midia.titulo} aos favoritos"
            }
        ) {
            Text(
                text = if (favorito) "★  FAVORITO" else "☆  FAVORITAR",
                color = if (favorito) Dourado else TextoClaro,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }
    }
}
