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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    titulo: String,
    streamUrl: String,
    aoFechar: () -> Unit
) {
    val contexto = LocalContext.current
    val player = remember(contexto) {
        ExoPlayer.Builder(contexto).build()
    }

    LaunchedEffect(streamUrl) {
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

    DisposableEffect(player) {
        onDispose {
            player.stop()
            player.release()
        }
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
        Surface(
            onClick = aoFechar,
            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xCC10182A)),
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
    }
}
