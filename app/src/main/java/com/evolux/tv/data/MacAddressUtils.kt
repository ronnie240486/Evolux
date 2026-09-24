package com.evolux.tv.data

import java.util.Locale

object MacAddressUtils {
    private const val TAMANHO_HEX = 12
    private val caracteresHexadecimais = "0123456789ABCDEF"

    // gerarMacLogico() foi removida -- gerava um MAC aleatório sem relação
    // com o aparelho de verdade, causa do bug em que o Evolux virava um
    // "aparelho" diferente pro painel (ver MacAddressProvider.kt). O MAC
    // usado agora vem sempre de MacAddressProvider.getFixedMac(context).

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
