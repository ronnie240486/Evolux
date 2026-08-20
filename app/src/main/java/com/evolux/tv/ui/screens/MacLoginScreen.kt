package com.evolux.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import com.evolux.tv.data.MacAddressUtils
import com.evolux.tv.ui.theme.Dourado
import com.evolux.tv.ui.theme.FundoCard
import com.evolux.tv.ui.theme.FundoEscuro
import com.evolux.tv.ui.theme.TextoCinza
import com.evolux.tv.ui.theme.TextoClaro

sealed interface EstadoLoginMac {
    data object Ocioso : EstadoLoginMac
    data object Carregando : EstadoLoginMac
    data class Erro(val mensagem: String) : EstadoLoginMac
}

@Composable
fun MacLoginScreen(
    estado: EstadoLoginMac,
    macInicial: String = "",
    aoTentarLogin: (String) -> Unit
) {
    var macDigitado by remember(macInicial) { mutableStateOf(macInicial) }
    var campoFocado by remember { mutableStateOf(false) }
    val focoInicial = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focoInicial.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoEscuro),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.width(560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "EVOLUX",
                color = Dourado,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "ENTRAR COM ENDEREÇO MAC",
                color = TextoClaro,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Informe o MAC autorizado para carregar a configuração do aparelho.",
                color = TextoCinza,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

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
                    textStyle = TextStyle(
                        color = TextoClaro,
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focoInicial)
                        .onFocusChanged { campoFocado = it.isFocused }
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    decorationBox = { campo ->
                        Box(contentAlignment = Alignment.Center) {
                            if (macDigitado.isEmpty()) {
                                Text(
                                    text = "AA:BB:CC:DD:EE:FF",
                                    color = TextoCinza,
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            campo()
                        }
                    }
                )
            }
            Spacer(Modifier.height(18.dp))

            Button(
                onClick = { aoTentarLogin(macDigitado) },
                enabled = estado !is EstadoLoginMac.Carregando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (estado is EstadoLoginMac.Carregando) {
                        "VALIDANDO..."
                    } else {
                        "VALIDAR APARELHO"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            when (val estadoAtual = estado) {
                EstadoLoginMac.Ocioso -> Unit
                EstadoLoginMac.Carregando -> {
                    Spacer(Modifier.height(14.dp))
                    Text("Consultando configuração segura...", color = TextoCinza)
                }
                is EstadoLoginMac.Erro -> {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = estadoAtual.mensagem,
                        color = Color(0xFFFFB4AB),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            val macValido = MacAddressUtils.normalizar(macDigitado) != null
            if (macDigitado.isNotBlank() && !macValido && estado !is EstadoLoginMac.Carregando) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Use 12 caracteres hexadecimais, por exemplo AA:BB:CC:DD:EE:FF.",
                    color = Color(0xFFFFB4AB),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
