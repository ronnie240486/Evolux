package com.evolux.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.Text

/**
 * Diálogo simples de PIN numérico (4 dígitos), usado tanto para desbloquear
 * conteúdo adulto quanto para configurar/trocar o PIN em Configurações.
 */
@Composable
fun PinEntryDialog(
    titulo: String,
    subtitulo: String? = null,
    aoConfirmar: (String) -> Unit,
    aoCancelar: () -> Unit,
    erro: String? = null
) {
    var pin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = aoCancelar) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0B1020))
                .padding(24.dp)
        ) {
            Text(text = titulo, color = Color(0xFFF4D35E), fontWeight = FontWeight.Bold)
            if (subtitulo != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = subtitulo, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            BasicTextField(
                value = pin,
                onValueChange = { novo -> if (novo.length <= 6 && novo.all { it.isDigit() }) pin = novo },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation('•'),
                textStyle = TextStyle(color = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1B2238))
                    .padding(14.dp)
            )
            if (!erro.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = erro, color = Color(0xFFFF6B6B))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = aoCancelar) { Text("Cancelar") }
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = { if (pin.isNotBlank()) aoConfirmar(pin) }) { Text("Confirmar") }
            }
        }
    }
}
