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
import androidx.compose.foundation.layout.padding
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
import com.evolux.tv.ui.components.EvoluxClickableSurface
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro

@Composable
fun CatalogoLoadingScreen(
    estado: EstadoLoginMac,
    erro: String? = null,
    carregando: Boolean = estado is EstadoLoginMac.Carregando,
    progressoPercentual: Int? = null,
    segundosDecorridos: Int? = null,
    aoTentarNovamente: (() -> Unit)? = null
) {
    val progresso = (estado as? EstadoLoginMac.Carregando) ?: EstadoLoginMac.Carregando()
    val porcentagemAtual = progressoPercentual ?: progresso.porcentagem
    val segundosAtual = segundosDecorridos ?: progresso.segundos
    val carregandoVisual = erro == null && carregando
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
            if (carregandoVisual) {
                EvoluxCatalogSpinner()
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Carregando conteúdo, canais, filmes e séries",
                    color = TextoClaro,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                        text = "${porcentagemAtual}% concluído • ${segundosAtual}s",
                    color = Dourado,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "A lista autorizada será mantida em cache para a próxima abertura.",
                    color = TextoCinza
                )
            } else {
                Text(
                    text = "Não foi possível carregar o catálogo",
                    color = Color(0xFFFFB4AB),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = erro ?: "O painel não forneceu uma playlist válida.",
                    color = TextoCinza,
                    modifier = Modifier.padding(horizontal = 28.dp)
                )
                aoTentarNovamente?.let { tentarNovamente ->
                    Spacer(Modifier.height(18.dp))
                    EvoluxClickableSurface(
                        onClick = tentarNovamente,
                        containerColor = Dourado,
                        modifier = Modifier.size(width = 220.dp, height = 52.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TENTAR NOVAMENTE",
                                color = Color(0xFF111111),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
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
