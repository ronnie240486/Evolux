package com.evolux.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.tv.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items as tvRowItems
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.evolux.tv.R
import com.evolux.tv.data.Midia
import com.evolux.tv.data.OrdemCatalogo
import com.evolux.tv.data.ehCategoriaAdulto
import com.evolux.tv.data.ehCategoriaKids
import com.evolux.tv.data.filtrarEOrdenarMidias
import com.evolux.tv.data.ordenarCategorias
import com.evolux.tv.ui.components.EvoluxClickableSurface
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.FundoCard
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro

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
    aoMudarOrdem: (OrdemCatalogo) -> Unit = {},
    ordemCategoriasCustom: List<String> = emptyList(),
    pinAdulto: String? = null
) {
    val categorias = remember(itens, categoriasOcultas, ordemCategoriasCustom) {
        val brutas = itens
            .map { it.categoria.ifBlank { "Sem categoria" } }
            .distinct()
            .filter { categoria -> categoria !in categoriasOcultas }
        listOf("Todos") + ordenarCategorias(brutas, ordemCategoriasCustom)
    }
    var categoriaSelecionada by remember(categorias) { mutableStateOf("Todos") }
    val focusRequesterPrimeiraCategoria = remember { FocusRequester() }
    LaunchedEffect(categorias) {
        if (categorias.isNotEmpty()) {
            runCatching { focusRequesterPrimeiraCategoria.requestFocus() }
        }
    }
    var busca by remember(categorias) { mutableStateOf("") }
    var ordem by remember(categorias, ordemInicial) { mutableStateOf(ordemInicial) }
    val itensFiltrados = remember(itens, busca, categoriaSelecionada, ordem, categoriasOcultas) {
        filtrarEOrdenarMidias(
            itens = itens,
            busca = busca,
            categoria = categoriaSelecionada,
            ordem = ordem,
            categoriasOcultas = categoriasOcultas
        )
    }
    var categoriasDesbloqueadas by remember { mutableStateOf(setOf<String>()) }
    var categoriaAguardandoPin by remember { mutableStateOf<String?>(null) }
    var erroPin by remember { mutableStateOf<String?>(null) }

    fun selecionarCategoria(categoria: String) {
        if (ehCategoriaAdulto(categoria) && categoria !in categoriasDesbloqueadas) {
            categoriaAguardandoPin = categoria
        } else {
            categoriaSelecionada = categoria
        }
    }

    categoriaAguardandoPin?.let { categoria ->
        if (pinAdulto.isNullOrBlank()) {
            InfoDialogSimples(
                titulo = "Conteúdo bloqueado",
                mensagem = "Nenhum PIN foi configurado ainda. Vá em Configurações > Criar PIN de conteúdo adulto para liberar o acesso.",
                aoFechar = { categoriaAguardandoPin = null }
            )
        } else {
            PinEntryDialog(
                titulo = "Conteúdo adulto",
                subtitulo = "Digite o PIN para acessar \"$categoria\".",
                erro = erroPin,
                aoCancelar = { categoriaAguardandoPin = null; erroPin = null },
                aoConfirmar = { digitado ->
                    if (digitado == pinAdulto) {
                        categoriasDesbloqueadas = categoriasDesbloqueadas + categoria
                        categoriaSelecionada = categoria
                        categoriaAguardandoPin = null
                        erroPin = null
                    } else {
                        erroPin = "PIN incorreto."
                    }
                }
            )
        }
    }

    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(
            titulo.uppercase(),
            color = Dourado,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(6.dp))
        CampoBusca(
            valor = busca,
            placeholder = "Buscar em ${titulo.lowercase()}...",
            aoMudar = { busca = it }
        )
        Spacer(Modifier.height(6.dp))
        TvLazyRow(
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tvRowItems(categorias) { categoria ->
                EvoluxClickableSurface(
                    onClick = { selecionarCategoria(categoria) },
                    containerColor = if (categoria == categoriaSelecionada) Color(0xFF283454) else Color(0xFF12172A),
                    borderColor = Dourado,
                    modifier = if (categorias.indexOf(categoria) == 0) {
                        Modifier.focusRequester(focusRequesterPrimeiraCategoria)
                    } else {
                        Modifier
                    }
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
        Spacer(Modifier.height(6.dp))
        TvLazyRow(
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
        Spacer(Modifier.height(8.dp))

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
                columns = TvGridCells.Fixed(colunas),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (colunas == 2) 10.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(itensFiltrados, key = { it.id }) { midia ->
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

/**
 * Detalhe de UM filme antes de assistir -- pôster grande, categoria e
 * sinopse (mesmo estilo do SeriesDetailDialog). Listas M3U/Xtream quase
 * nunca trazem sinopse de filme nenhuma; quando vier em branco, busca no
 * TMDB pelo nome via [aoBuscarSinopse].
 */
@Composable
fun MovieDetailDialog(
    midia: Midia,
    aoFechar: () -> Unit,
    aoAssistir: (Midia) -> Unit,
    aoBuscarSinopse: suspend (String) -> String? = { null }
) {
    val sinopseExibida by produceState(initialValue = midia.sinopse, midia.id) {
        value = midia.sinopse.ifBlank {
            runCatching { aoBuscarSinopse(midia.titulo) }.getOrNull().orEmpty()
        }
    }

    Dialog(
        onDismissRequest = aoFechar,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .fillMaxHeight(0.78f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF0B1020))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AsyncImage(
                            model = midia.imagemUrl.takeIf { it.isNotBlank() },
                            placeholder = painterResource(R.drawable.evolux_logo),
                            error = painterResource(R.drawable.evolux_logo),
                            fallback = painterResource(R.drawable.evolux_logo),
                            contentDescription = midia.titulo,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(130.dp)
                                .height(180.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(midia.titulo, color = TextoClaro, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(midia.categoria.ifBlank { "Filme" }, color = Dourado)
                            Text(
                                sinopseExibida.ifBlank { "Sinopse não disponível." },
                                color = TextoCinza,
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    EvoluxClickableSurface(
                        onClick = aoFechar,
                        containerColor = FundoCard,
                        modifier = Modifier.width(54.dp).height(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TextoClaro)
                    }
                }
                Spacer(Modifier.height(18.dp))
                EvoluxClickableSurface(
                    onClick = { aoAssistir(midia) },
                    containerColor = Dourado,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF111111))
                        Spacer(Modifier.width(8.dp))
                        Text("ASSISTIR", color = Color(0xFF111111), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
