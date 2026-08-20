package com.evolux.tv.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Surface
import com.evolux.tv.ui.components.EvoluxClickableSurface
import androidx.tv.material3.Text

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    titulo: String,
    streamUrl: String,
    aoFechar: () -> Unit
) {
    val contexto = LocalContext.current
    val player = remember(contexto) { ExoPlayer.Builder(contexto).build() }
    var carregando by remember(streamUrl) { mutableStateOf(true) }
    var erroReproducao by remember(streamUrl) { mutableStateOf<String?>(null) }

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
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
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
