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
import com.evolux.tv.R
import com.evolux.tv.data.Midia
import com.evolux.tv.data.OrdemCatalogo
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
    val categorias by produceState<List<String>>(emptyList(), itens, categoriasOcultas) {
        value = withContext(Dispatchers.Default) {
            listOf("Todos") + itens
                .asSequence()
                .map { it.categoria.ifBlank { "Sem categoria" } }
                .distinct()
                .filter { categoria -> categoria !in categoriasOcultas }
                .sortedWith(String.CASE_INSENSITIVE_ORDER)
                .toList()
        }
    }
    var categoriaSelecionada by remember(categorias) {
        mutableStateOf(categorias.firstOrNull { it != "Todos" } ?: "Todos")
    }
    var busca by remember(categorias) { mutableStateOf("") }
    var ordem by remember(categorias, ordemInicial) { mutableStateOf(ordemInicial) }
    val itensFiltrados by produceState<List<Midia>>(
        emptyList(), itens, busca, categoriaSelecionada, ordem, categoriasOcultas
    ) {
        value = withContext(Dispatchers.Default) {
            filtrarEOrdenarMidias(
                itens = itens,
                busca = busca,
                categoria = categoriaSelecionada,
                ordem = ordem,
                categoriasOcultas = categoriasOcultas
            )
        }
    }
    val tamanhoPagina = 30
    var pagina by remember(itens, categorias, categoriaSelecionada, busca, ordem) { mutableIntStateOf(0) }
    val totalPaginas = ((itensFiltrados.size + tamanhoPagina - 1) / tamanhoPagina).coerceAtLeast(1)
    val paginaAtual = pagina.coerceIn(0, totalPaginas - 1)
    val itensDaPagina = remember(itensFiltrados, paginaAtual) {
        itensFiltrados.drop(paginaAtual * tamanhoPagina).take(tamanhoPagina)
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

        if (itensFiltrados.isNotEmpty() && totalPaginas > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                EvoluxClickableSurface(
                    onClick = { if (paginaAtual > 0) pagina-- },
                    containerColor = if (paginaAtual > 0) Color(0xFF12172A) else Color.Transparent,
                    modifier = Modifier.width(54.dp).height(42.dp)
                ) { Text("‹", color = TextoClaro, style = MaterialTheme.typography.titleLarge) }
                Text(
                    text = "Página ${paginaAtual + 1}/$totalPaginas • ${itensFiltrados.size} itens",
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

        if (itensFiltrados.isEmpty()) {
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
                items(itensDaPagina) { midia ->
                    CardPoster(
                        midia = midia,
                        favorito = ehFavorito(midia),
                        aoClicar = { aoSelecionar(midia) },
                        aoAlternarFavorito = { aoAlternarFavorito(midia) }
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

@Composable
private fun CardPoster(
    midia: Midia,
    favorito: Boolean,
    aoClicar: () -> Unit,
    aoAlternarFavorito: () -> Unit
) {
    var focado by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        EvoluxClickableSurface(
            onClick = aoClicar,
            containerColor = Color(0xFF12172A),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focado = it.isFocused }
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
                    model = midia.imagemUrl.takeIf { it.isNotBlank() },
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
