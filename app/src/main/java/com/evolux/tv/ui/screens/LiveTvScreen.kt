package com.evolux.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.evolux.tv.data.filtrarEOrdenarCanais
import com.evolux.tv.ui.components.EvoluxClickableSurface
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro

@Composable
fun LiveTvScreen(
    canais: List<Canal>,
    aoAbrirCanal: (Canal) -> Unit,
    categoriasOcultas: Set<String> = emptySet(),
    ordemInicial: OrdemCatalogo = OrdemCatalogo.PADRAO,
    aoMudarOrdem: (OrdemCatalogo) -> Unit = {}
) {
    val categorias = remember(canais, categoriasOcultas) {
        listOf("Todos") + canais
            .map { it.categoria.ifBlank { "TV ao vivo" } }
            .distinct()
            .filter { it !in categoriasOcultas }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
    var categoriaSelecionada by remember(canais) { mutableStateOf("Todos") }
    var busca by remember(canais) { mutableStateOf("") }
    var ordem by remember(canais, ordemInicial) { mutableStateOf(ordemInicial) }
    val canaisFiltrados = filtrarEOrdenarCanais(canais, busca, categoriaSelecionada, ordem, categoriasOcultas)

    Column(modifier = Modifier.padding(24.dp)) {
        Text("TV AO VIVO", color = Dourado, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        CampoBuscaCanais(busca, { busca = it })
        Spacer(Modifier.height(10.dp))
        TvLazyRow(contentPadding = PaddingValues(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tvRowItems(categorias) { categoria ->
                EvoluxClickableSurface(
                    onClick = { categoriaSelecionada = categoria },
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
        TvLazyRow(contentPadding = PaddingValues(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        if (canaisFiltrados.isEmpty()) {
            Text(if (canais.isEmpty()) "Nenhum canal disponível." else "Nenhum canal encontrado.", color = TextoCinza)
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val colunas = when {
                    maxWidth < 420.dp -> 2
                    maxWidth < 760.dp -> 3
                    else -> 5
                }
                TvLazyVerticalGrid(
                    columns = TvGridCells.Fixed(colunas),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (colunas == 2) 10.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(canaisFiltrados) { canal ->
                        CardCanal(canal, aoClicar = { aoAbrirCanal(canal) })
                    }
                }
            }
        }
    }
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
private fun CardCanal(canal: Canal, aoClicar: () -> Unit) {
    EvoluxClickableSurface(
        onClick = aoClicar,
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
            Text(canal.categoria, color = TextoCinza, style = MaterialTheme.typography.labelSmall)
        }
    }
}
