package com.evolux.tv.data

import java.util.Locale

object MacAddressUtils {
    private const val TAMANHO_HEX = 12
    private val caracteresHexadecimais = "0123456789ABCDEF"

    fun normalizar(valor: String): String? {
        val hexadecimal = valor
            .filter { it.isLetterOrDigit() }
            .uppercase(Locale.ROOT)

        if (hexadecimal.length != TAMANHO_HEX) return null
        if (hexadecimal.any { it !in caracteresHexadecimais }) return null

        return hexadecimal
            .chunked(2)
            .joinToString(":")
    }
}
