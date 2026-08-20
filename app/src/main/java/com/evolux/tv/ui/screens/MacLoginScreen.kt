package com.evolux.tv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.evolux.tv.R
import com.evolux.tv.data.MacAddressUtils
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.FundoCard
import com.evolux.tv.ui.theme.FundoEscuro
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro

sealed interface EstadoLoginMac {
    data object Ocioso : EstadoLoginMac
    data object Carregando : EstadoLoginMac
    data class Erro(val mensagem: String, val detalhe: String? = null) : EstadoLoginMac
}

@Composable
fun MacLoginScreen(
    estado: EstadoLoginMac,
    macInicial: String = "",
    aoCopiarMac: () -> Unit,
    aoTentarLogin: (String) -> Unit
) {
    var macDigitado by remember(macInicial) { mutableStateOf(macInicial) }
    var campoFocado by remember { mutableStateOf(false) }
    val focoInicial = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focoInicial.requestFocus()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoEscuro)
    ) {
        val celular = maxWidth < 500.dp
        val espacamento = if (celular) 12.dp else 20.dp
        val alturaLogo = if (celular) 120.dp else 190.dp
        val paddingHorizontal = if (celular) 16.dp else 24.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = paddingHorizontal, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.evolux_logo),
                    contentDescription = "Logo Evolux",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(alturaLogo)
                )
                Spacer(Modifier.height(if (celular) 2.dp else 8.dp))
                Text(
                    text = "ENTRAR COM ENDEREÇO MAC",
                    color = TextoClaro,
                    fontWeight = FontWeight.Bold,
                    style = if (celular) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Este é o MAC lógico deste aparelho. Copie-o, cadastre-o no painel e depois valide o acesso.",
                    color = TextoCinza,
                    style = if (celular) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(espacamento))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    colors = SurfaceDefaults.colors(containerColor = FundoCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (campoFocado) 2.dp else 1.dp,
                            color = if (campoFocado) Dourado else Color(0xFF36415A),
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    BasicTextField(
                        value = macDigitado,
                        onValueChange = { novoValor ->
                            macDigitado = novoValor
                                .filter { it.isLetterOrDigit() || it == ':' || it == '-' }
                                .take(17)
                                .uppercase()
                        },
                        singleLine = true,
                        readOnly = true,
                        textStyle = TextStyle(
                            color = TextoClaro,
                            fontSize = if (celular) MaterialTheme.typography.titleMedium.fontSize else MaterialTheme.typography.titleLarge.fontSize,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focoInicial)
                            .onFocusChanged { campoFocado = it.isFocused }
                            .padding(horizontal = 12.dp, vertical = if (celular) 14.dp else 18.dp),
                        decorationBox = { campo ->
                            Box(contentAlignment = Alignment.Center) {
                                if (macDigitado.isEmpty()) {
                                    Text(
                                        text = "AA:BB:CC:DD:EE:FF",
                                        color = TextoCinza,
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                campo()
                            }
                        }
                    )
                }
                Spacer(Modifier.height(if (celular) 12.dp else 18.dp))

                if (celular) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = aoCopiarMac,
                            enabled = estado !is EstadoLoginMac.Carregando,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("COPIAR MAC", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { aoTentarLogin(macDigitado) },
                            enabled = estado !is EstadoLoginMac.Carregando,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (estado is EstadoLoginMac.Carregando) "VALIDANDO..." else "VALIDAR APARELHO",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = aoCopiarMac,
                            enabled = estado !is EstadoLoginMac.Carregando,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("COPIAR MAC", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { aoTentarLogin(macDigitado) },
                            enabled = estado !is EstadoLoginMac.Carregando,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (estado is EstadoLoginMac.Carregando) "VALIDANDO..." else "VALIDAR APARELHO",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                when (val estadoAtual = estado) {
                    EstadoLoginMac.Ocioso -> Unit
                    EstadoLoginMac.Carregando -> {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Consultando configuração segura...",
                            color = TextoCinza,
                            textAlign = TextAlign.Center
                        )
                    }
                    is EstadoLoginMac.Erro -> {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = estadoAtual.mensagem,
                            color = Color(0xFFFFB4AB),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        estadoAtual.detalhe?.takeIf { it.isNotBlank() }?.let { detalhe ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = detalhe,
                                color = TextoCinza,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "O MAC permanece o mesmo nesta instalação. Se reinstalar o APK, um novo MAC será gerado.",
                    color = TextoCinza,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )

                val macValido = MacAddressUtils.normalizar(macDigitado) != null
                if (macDigitado.isNotBlank() && !macValido && estado !is EstadoLoginMac.Carregando) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "MAC inválido. O formato deve ser AA:BB:CC:DD:EE:FF.",
                        color = Color(0xFFFFB4AB),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
