package com.evolux.tv.data

import java.security.SecureRandom
import java.util.Locale

object MacAddressUtils {
    private const val TAMANHO_HEX = 12
    private val caracteresHexadecimais = "0123456789ABCDEF"
    private val geradorSeguro = SecureRandom()

    /** Gera um MAC lógico localmente administrado para o cadastro do aparelho. */
    fun gerarMacLogico(): String {
        val bytes = ByteArray(6)
        geradorSeguro.nextBytes(bytes)
        // Unicast + locally administered: não se apresenta como fabricante real.
        bytes[0] = ((bytes[0].toInt() and 0xFC) or 0x02).toByte()
        return bytes.joinToString(":") { byte ->
            "%02X".format(Locale.ROOT, byte.toInt() and 0xFF)
        }
    }

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
