package com.evolux.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items as tvRowItems
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.evolux.tv.R
import com.evolux.tv.data.Midia
import com.evolux.tv.ui.components.EvoluxClickableSurface
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro

@Composable
fun GradeMidiaScreen(
    titulo: String,
    itens: List<Midia>,
    aoSelecionar: (Midia) -> Unit,
    ehFavorito: (Midia) -> Boolean,
    aoAlternarFavorito: (Midia) -> Unit,
    mensagemVazio: String = "Nada por aqui ainda."
) {
    val categorias = remember(itens) {
        listOf("Todos") + itens
            .map { it.categoria.ifBlank { "Sem categoria" } }
            .distinct()
            .sorted()
    }
    var categoriaSelecionada by remember(itens) { mutableStateOf("Todos") }
    val itensFiltrados = if (categoriaSelecionada == "Todos") {
        itens
    } else {
        itens.filter { it.categoria.ifBlank { "Sem categoria" } == categoriaSelecionada }
    }

    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) {
        Text(
            titulo.uppercase(),
            color = Dourado,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(12.dp))
        TvLazyRow(
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
        Spacer(Modifier.height(14.dp))

        if (itensFiltrados.isEmpty()) {
            Text(
                if (itens.isEmpty()) mensagemVazio else "Nenhum item nesta categoria.",
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
                columns = TvGridCells.Fixed(colunas),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (colunas == 2) 10.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(itensFiltrados) { midia ->
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
private fun CardPoster(
    midia: Midia,
    favorito: Boolean,
    aoClicar: () -> Unit,
    aoAlternarFavorito: () -> Unit
) {
    var focado by remember { mutableStateOf(false) }
    var favoritoFocado by remember { mutableStateOf(false) }

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 7.dp)
                )
                Text(
                    text = midia.categoria,
                    color = Dourado,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        EvoluxClickableSurface(
            onClick = aoAlternarFavorito,
            containerColor = Color(0xFF1A2238),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { favoritoFocado = it.isFocused }
                .semantics(mergeDescendants = true) {
                    contentDescription = if (favorito) {
                        "Remover ${midia.titulo} dos favoritos"
                    } else {
                        "Adicionar ${midia.titulo} aos favoritos"
                    }
                }
        ) {
            Text(
                text = if (favorito) "★  FAVORITO" else "☆  FAVORITAR",
                color = if (favorito) Dourado else TextoClaro,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}
