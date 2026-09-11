package com.evolux.tv.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Surface
import com.evolux.tv.data.Canal
import com.evolux.tv.data.XtreamRepository
import com.evolux.tv.data.extrairStreamId
import com.evolux.tv.ui.components.EvoluxClickableSurface
import com.evolux.tv.ui.theme.Dourado
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

private data class ModoTela(val resizeMode: Int, val rotulo: String)

private val MODOS_TELA = listOf(
    ModoTela(AspectRatioFrameLayout.RESIZE_MODE_FIT, "Ajustar"),
    ModoTela(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Zoom"),
    ModoTela(AspectRatioFrameLayout.RESIZE_MODE_FILL, "Esticar")
)

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    titulo: String,
    streamUrl: String,
    aoFechar: () -> Unit,
    aoFalhaDeRede: () -> Unit = {},
    canal: Canal? = null,
    urlXtream: String? = null,
    xtreamRepository: XtreamRepository? = null,
    lembretesAtivos: Set<String> = emptySet(),
    aoAgendarLembrete: (Canal, XtreamRepository.ProgramaEpg) -> Unit = { _, _ -> }
) {
    val contexto = LocalContext.current
    val player = remember(contexto) { ExoPlayer.Builder(contexto).build() }
    var carregando by remember(streamUrl) { mutableStateOf(true) }
    var erroReproducao by remember(streamUrl) { mutableStateOf<String?>(null) }
    var indiceModoTela by remember { mutableStateOf(0) }
    var mostrarGuia by remember { mutableStateOf(false) }
    var mostrarControles by remember { mutableStateOf(true) }
    var epg by remember(canal?.id) { mutableStateOf<List<XtreamRepository.ProgramaEpg>>(emptyList()) }
    var carregandoEpg by remember { mutableStateOf(false) }

    val streamIdCanal = remember(canal?.streamUrl) { canal?.streamUrl?.let(::extrairStreamId) }
    val epgDisponivel = canal != null && streamIdCanal != null && urlXtream != null &&
        xtreamRepository != null && XtreamRepository.pareceXtream(urlXtream)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) carregando = false
            }

            override fun onPlayerError(error: PlaybackException) {
                carregando = false
                erroReproducao = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Falha de conexão com o stream."
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Tempo limite ao conectar ao stream."
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "Formato de mídia inválido ou incompatível."
                    else -> "O player não conseguiu reproduzir esta fonte."
                }
                // Reporta pro painel só erro real de rede/timeout/indisponibilidade,
                // conforme a especificação (não erro de formato nem ação do usuário).
                when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> aoFalhaDeRede()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.stop()
            player.release()
        }
    }

    LaunchedEffect(streamUrl) {
        carregando = true
        erroReproducao = null
        val item = MediaItem.Builder()
            .setUri(streamUrl)
            .setMimeType(
                when {
                    streamUrl.contains(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
                    streamUrl.contains(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
                    else -> null
                }
            )
            .build()
        player.setMediaItem(item)
        player.prepare()
        player.playWhenReady = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { contextoView ->
                PlayerView(contextoView).apply {
                    this.player = player
                    useController = true
                    isFocusable = true
                    isFocusableInTouchMode = true
                    resizeMode = MODOS_TELA[indiceModoTela].resizeMode
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Os botões de VOLTAR/Zoom/Guia seguem a mesma visibilidade
                    // dos controles nativos: some tudo junto, aparece junto.
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibilidade ->
                            mostrarControles = visibilidade == android.view.View.VISIBLE
                        }
                    )
                    requestFocus()
                }
            },
            update = { view -> view.resizeMode = MODOS_TELA[indiceModoTela].resizeMode },
            modifier = Modifier.fillMaxSize()
        )
        if (mostrarControles) {
            EvoluxClickableSurface(
                onClick = aoFechar,
                containerColor = Color(0xCC10182A),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "VOLTAR  •  $titulo",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
            ) {
                EvoluxClickableSurface(
                    onClick = { indiceModoTela = (indiceModoTela + 1) % MODOS_TELA.size },
                    containerColor = Color(0xCC10182A)
                ) {
                    Text(
                        text = "⛶ ${MODOS_TELA[indiceModoTela].rotulo}",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
                if (epgDisponivel) {
                    EvoluxClickableSurface(
                        onClick = { mostrarGuia = true },
                        containerColor = Color(0xCC10182A)
                    ) {
                        Text(
                            text = "📺 Guia",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
        if (mostrarGuia && canal != null && xtreamRepository != null && urlXtream != null && streamIdCanal != null) {
            LaunchedEffect(canal.id) {
                carregandoEpg = true
                epg = xtreamRepository.carregarEpg(urlXtream, streamIdCanal, limite = 11)
                carregandoEpg = false
            }
            Dialog(
                onDismissRequest = { mostrarGuia = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .background(Color(0xFF0B1020), RoundedCornerShape(18.dp))
                        .padding(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(canal.nome, color = Dourado, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Toque no sino pra ser avisado quando o programa começar", color = Color(0xFFB8C0D4))
                        Spacer(Modifier.height(16.dp))
                        if (carregandoEpg) {
                            Text("Carregando guia de programação...", color = Color.White)
                        } else if (epg.isEmpty()) {
                            Text("Guia de programação não disponível pra esse canal.", color = Color.White)
                        } else {
                            val agora = System.currentTimeMillis()
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(epg) { programa ->
                                    val jaPassou = programa.fimMillis in 1 until agora
                                    val emExibicao = agora in programa.inicioMillis..programa.fimMillis
                                    val lembreteAtivo = programa.id in lembretesAtivos
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .background(
                                                if (emExibicao) Color(0xFF23304F) else Color(0xFF12172A),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.width(0.dp).weight(1f, fill = true)) {
                                            Text(
                                                formatarHorarioEpg(programa.inicioMillis) + (if (emExibicao) " • Agora" else ""),
                                                color = if (emExibicao) Dourado else Color(0xFF9AA4BE)
                                            )
                                            Text(
                                                programa.titulo.ifBlank { "Sem informação" },
                                                color = if (jaPassou) Color(0xFF6E7690) else Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (!jaPassou && !emExibicao) {
                                            EvoluxClickableSurface(
                                                onClick = { aoAgendarLembrete(canal, programa) },
                                                containerColor = if (lembreteAtivo) Color(0xFF3A3115) else Color(0xFF1B2238)
                                            ) {
                                                Text(
                                                    if (lembreteAtivo) "🔔" else "🔕",
                                                    color = if (lembreteAtivo) Dourado else Color(0xFF9AA4BE),
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        EvoluxClickableSurface(onClick = { mostrarGuia = false }, containerColor = Color(0xFF1B2238)) {
                            Text("Fechar", color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                        }
                    }
                }
            }
        }
        if (carregando) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                colors = androidx.tv.material3.SurfaceDefaults.colors(containerColor = Color(0xE610182A)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = "CARREGANDO STREAM...",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        }
        erroReproducao?.let { mensagem ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                colors = androidx.tv.material3.SurfaceDefaults.colors(containerColor = Color(0xF0181A28)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Text(
                    text = mensagem,
                    color = Color(0xFFFFB4AB),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)
                )
            }
        }
    }
}

private fun formatarHorarioEpg(millis: Long): String {
    if (millis <= 0L) return "--:--"
    val formato = java.text.SimpleDateFormat("HH:mm", java.util.Locale.ROOT)
    return formato.format(java.util.Date(millis))
}
