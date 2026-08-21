package com.evolux.tv.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.evolux.tv.R
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro

@Composable
fun CatalogoLoadingScreen(
    estado: EstadoLoginMac,
    carregandoCatalogo: Boolean = false,
    progressoCatalogo: Int = 1,
    segundosCatalogo: Int = 0
) {
    val progresso = if (carregandoCatalogo) {
        EstadoLoginMac.Carregando(
            porcentagem = progressoCatalogo.coerceIn(1, 99),
            segundos = segundosCatalogo
        )
    } else {
        (estado as? EstadoLoginMac.Carregando) ?: EstadoLoginMac.Carregando()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.evolux_background_futurista),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.46f)
        )
        Box(modifier = Modifier.fillMaxSize().background(Color(0xDE080D1B)))
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.evolux_logo),
                contentDescription = "Evolux",
                modifier = Modifier.size(170.dp)
            )
            Spacer(Modifier.height(18.dp))
            EvoluxCatalogSpinner()
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Carregando conteúdo, canais, filmes e séries",
                color = TextoClaro,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${progresso.porcentagem}% concluído • ${progresso.segundos}s",
                color = Dourado,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Sua lista está sendo carregada. Em breve, você terá o melhor conteúdo para curtir.",
                color = TextoCinza
            )
        }
    }
}

@Composable
private fun EvoluxCatalogSpinner() {
    val transicao = rememberInfiniteTransition(label = "catalogo_spinner")
    val rotacao by transicao.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "catalogo_rotacao"
    )
    Canvas(Modifier.size(58.dp).rotate(rotacao)) {
        drawArc(
            color = Dourado,
            startAngle = 25f,
            sweepAngle = 285f,
            useCenter = false,
            style = Stroke(width = 6.dp.toPx())
        )
    }
}
