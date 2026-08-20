package com.evolux.tv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.evolux.tv.R
import com.evolux.tv.data.Midia
import com.evolux.tv.data.OrdemCatalogo
import com.evolux.tv.data.normalizarConsulta
import com.evolux.tv.ui.components.EvoluxClickableSurface
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.FundoCard
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro

private data class GrupoSerie(
    val chave: String,
    val nome: String,
    val categoria: String,
    val capa: String,
    val sinopse: String,
    val episodios: List<Midia>
)

@Composable
fun SeriesBrowserScreen(
    itens: List<Midia>,
    aoAssistir: (Midia) -> Unit,
    categoriasOcultas: Set<String> = emptySet(),
    ordemInicial: OrdemCatalogo = OrdemCatalogo.PADRAO,
    aoMudarOrdem: (OrdemCatalogo) -> Unit = {},
    carregarEpisodios: suspend (Midia) -> List<Midia> = { emptyList() }
) {
    val categorias = remember(itens, categoriasOcultas) {
        itens.asSequence()
            .map { it.categoria.ifBlank { "Séries" } }
            .distinct()
            .filter { it !in categoriasOcultas }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
            .toList()
    }
    var categoriaSelecionada by remember(categorias) {
        mutableStateOf(categorias.firstOrNull().orEmpty())
    }
    var busca by remember(itens) { mutableStateOf("") }
    var ordem by remember(itens, ordemInicial) { mutableStateOf(ordemInicial) }
    val grupos = remember(itens, categoriaSelecionada, busca, ordem, categoriasOcultas) {
        val consulta = normalizarConsulta(busca)
        val resultado = itens.asSequence()
            .filter { it.categoria.ifBlank { "Séries" } == categoriaSelecionada }
            .filter { it.categoria.ifBlank { "Séries" } !in categoriasOcultas }
            .groupBy { item ->
                val categoria = item.categoria.ifBlank { "Séries" }
                val nomeBase = item.serieNome?.takeIf { it.isNotBlank() }
                    ?: removerMarcadorDeEpisodio(item.titulo)
                "${normalizarChave(categoria)}::${normalizarChave(nomeBase)}"
            }
            .map { (chave, episodios) ->
                val ordenados = episodios.sortedWith(
                    compareBy<Midia> { it.temporadaNumero ?: 1 }
                        .thenBy { it.episodioNumero ?: Int.MAX_VALUE }
                        .thenBy { it.titulo }
                )
                GrupoSerie(
                    chave = chave,
                    nome = episodios.firstNotNullOfOrNull { it.serieNome }
                        ?: removerMarcadorDeEpisodio(episodios.first().titulo),
                    categoria = episodios.firstOrNull()?.categoria.orEmpty().ifBlank { "Séries" },
                    capa = selecionarCapaSerie(episodios),
                    sinopse = episodios.firstOrNull { it.sinopse.isNotBlank() }?.sinopse.orEmpty(),
                    episodios = ordenados
                )
            }
            .filter { grupo -> consulta.isBlank() || normalizarConsulta("${grupo.nome} ${grupo.categoria} ${grupo.sinopse}").contains(consulta) }
            .toList()
        when (ordem) {
            OrdemCatalogo.NOME_ZA -> resultado.sortedByDescending { normalizarConsulta(it.nome) }
            else -> resultado.sortedBy { normalizarConsulta(it.nome) }
        }
    }
    var serieSelecionada by remember { mutableStateOf<GrupoSerie?>(null) }
    var episodiosCarregados by remember { mutableStateOf<Map<String, List<Midia>>>(emptyMap()) }
    var chaveCarregando by remember { mutableStateOf<String?>(null) }
    val escopo = rememberCoroutineScope()

    fun abrirGrupo(grupo: GrupoSerie) {
        val episodiosExistentes = episodiosCarregados[grupo.chave]
        if (episodiosExistentes != null) {
            serieSelecionada = grupo.copy(episodios = episodiosExistentes)
            return
        }
        val representante = grupo.episodios.firstOrNull()
        val ehXtream = representante?.streamUrl?.startsWith("xtream://series/") == true
        if (!ehXtream) {
            serieSelecionada = grupo
            return
        }
        if (chaveCarregando == grupo.chave) return
        chaveCarregando = grupo.chave
        escopo.launch {
            val carregados = representante?.let { carregarEpisodios(it) }.orEmpty()
            if (carregados.isNotEmpty()) {
                episodiosCarregados = episodiosCarregados + (grupo.chave to carregados)
                serieSelecionada = grupo.copy(episodios = carregados)
            } else {
                serieSelecionada = grupo
            }
            chaveCarregando = null
        }
    }

    BackHandler(enabled = serieSelecionada != null) {
        serieSelecionada = null
    }

    val gruposDaCategoria = grupos.filter { it.categoria == categoriaSelecionada }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 18.dp)
    ) {
        Text(
            text = "SÉRIES",
            color = Dourado,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(12.dp))
        CampoBuscaSeries(valor = busca, aoMudar = { busca = it })
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(OrdemCatalogo.entries.toList(), key = { it.name }) { opcao ->
                FiltroCategoria(
                    nome = opcao.rotulo,
                    selecionada = opcao == ordem,
                    aoClicar = { ordem = opcao; aoMudarOrdem(opcao) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (categorias.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 24.dp)
            ) {
                items(categorias, key = { it }) { categoria ->
                    FiltroCategoria(
                        nome = categoria,
                        selecionada = categoria == categoriaSelecionada,
                        aoClicar = { categoriaSelecionada = categoria }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (gruposDaCategoria.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nenhuma série encontrada em ${categoriaSelecionada.ifBlank { "esta categoria" }}.",
                    color = TextoCinza,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "${categoriaSelecionada.ifBlank { "Séries" }} • ${gruposDaCategoria.size} séries",
                        color = TextoCinza,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                items(gruposDaCategoria, key = { it.chave }) { grupo ->
                    SerieCard(grupo, carregando = chaveCarregando == grupo.chave) { abrirGrupo(grupo) }
                }
            }
        }
    }

    serieSelecionada?.let { grupo ->
        SeriesDetailDialog(
            grupo = grupo,
            aoFechar = { serieSelecionada = null },
            aoAssistir = aoAssistir
        )
    }
}

@Composable
private fun FiltroCategoria(nome: String, selecionada: Boolean, aoClicar: () -> Unit) {
    EvoluxClickableSurface(
        onClick = aoClicar,
        containerColor = if (selecionada) Dourado else FundoCard,
        focusedColor = if (selecionada) Dourado else Color(0xFF2A3558),
        modifier = Modifier.height(48.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = nome,
                color = if (selecionada) Color(0xFF111111) else TextoClaro,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SerieCard(grupo: GrupoSerie, carregando: Boolean = false, aoClicar: () -> Unit) {
    EvoluxClickableSurface(
        onClick = aoClicar,
        containerColor = FundoCard,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 900.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = grupo.capa.takeIf { it.isNotBlank() },
                placeholder = painterResource(R.drawable.evolux_logo),
                error = painterResource(R.drawable.evolux_logo),
                fallback = painterResource(R.drawable.evolux_logo),
                contentDescription = grupo.nome,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(126.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = grupo.nome,
                    color = TextoClaro,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = grupo.categoria,
                    color = Dourado,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = grupo.sinopse.ifBlank { "Sinopse não fornecida pela lista." },
                    color = TextoCinza,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (carregando) "Carregando episódios..." else "${grupo.episodios.count { it.episodioNumero != null }.coerceAtLeast(grupo.episodios.size)} episódio(s)",
                    color = TextoCinza,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SeriesDetailDialog(
    grupo: GrupoSerie,
    aoFechar: () -> Unit,
    aoAssistir: (Midia) -> Unit
) {
    val temporadas = grupo.episodios.groupBy { it.temporadaNumero ?: 1 }.toSortedMap()
    var temporadaSelecionada by remember(grupo.chave) {
        mutableStateOf(temporadas.keys.firstOrNull() ?: 1)
    }
    val episodios = temporadas[temporadaSelecionada].orEmpty()
        .sortedWith(compareBy<Midia> { it.episodioNumero ?: Int.MAX_VALUE }.thenBy { it.titulo })

    Dialog(onDismissRequest = aoFechar) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .height(620.dp)
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
                            model = grupo.capa.takeIf { it.isNotBlank() },
                            placeholder = painterResource(R.drawable.evolux_logo),
                            error = painterResource(R.drawable.evolux_logo),
                            fallback = painterResource(R.drawable.evolux_logo),
                            contentDescription = grupo.nome,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(120.dp)
                                .height(150.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(grupo.nome, color = TextoClaro, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(grupo.categoria, color = Dourado)
                            Text(
                                grupo.sinopse.ifBlank { "Sinopse não fornecida pela lista." },
                                color = TextoCinza,
                                maxLines = 5,
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
                Text("Temporadas", color = TextoClaro, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(temporadas.keys.toList(), key = { _, numero -> numero }) { _, numero ->
                        FiltroCategoria(
                            nome = "Temporada $numero",
                            selecionada = numero == temporadaSelecionada,
                            aoClicar = { temporadaSelecionada = numero }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Episódios", color = TextoClaro, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(episodios, key = { it.id }) { episodio ->
                        EpisodioRow(episodio) { aoAssistir(episodio) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodioRow(episodio: Midia, aoClicar: () -> Unit) {
    EvoluxClickableSurface(
        onClick = aoClicar,
        containerColor = FundoCard,
        modifier = Modifier.fillMaxWidth().height(70.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Assistir", tint = Dourado)
            Column {
                Text(
                    text = episodio.episodioNome ?: episodio.titulo,
                    color = TextoClaro,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = episodio.episodioNumero?.let { "Episódio $it" } ?: "Assistir episódio",
                    color = TextoCinza,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun CampoBuscaSeries(valor: String, aoMudar: (String) -> Unit) {
    var focado by remember { mutableStateOf(false) }
    EvoluxClickableSurface(
        onClick = {},
        containerColor = FundoCard,
        modifier = Modifier.fillMaxWidth().onFocusChanged { focado = it.isFocused }
    ) {
        BasicTextField(
            value = valor,
            onValueChange = aoMudar,
            singleLine = true,
            textStyle = TextStyle(color = TextoClaro, fontSize = MaterialTheme.typography.bodyLarge.fontSize),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            decorationBox = { campo ->
                Box {
                    if (valor.isBlank()) Text("Buscar séries nesta categoria...", color = TextoCinza)
                    campo()
                }
            }
        )
    }
}

private fun selecionarCapaSerie(episodios: List<Midia>): String {
    return episodios
        .asSequence()
        .map { it.imagemUrl.trim() }
        .filter { it.startsWith("http://") || it.startsWith("https://") }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .firstOrNull()
        ?.key
        .orEmpty()
}

private fun normalizarChave(valor: String): String = valor.trim().lowercase().replace("\\s+".toRegex(), " ")

private fun removerMarcadorDeEpisodio(valor: String): String {
    return valor.replace("(?i)\\s*[-_.| ]*(s|t|season|temporada)\\s*\\d{1,2}.*$".toRegex(), "").trim()
}
