package com.evolux.tv.data

import android.content.Context
import android.net.wifi.WifiManager
import android.provider.Settings
import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.Locale

/**
 * BUG CRÍTICO corrigido: o Evolux gerava um MAC totalmente aleatório e
 * local (ver MacAddressUtils.gerarMacLogico, agora sem uso) pra cada
 * instalação -- sem NENHUMA relação com o MAC de verdade do aparelho.
 * Isso fazia o Evolux virar, pro painel, um "aparelho" completamente
 * diferente do que o Rencia/Supreme, Maximus, Ouro Pro e Fusion já
 * reportam (que detectam o MAC REAL da placa de rede -- o mesmo valor nos
 * quatro, porque é o mesmo hardware). Resultado observado: abrir o Evolux
 * e escolher qualquer canal nunca atualizava "o que está passando" no
 * painel -- o heartbeat do Evolux batia numa linha (MAC aleatório) que o
 * usuário nunca olha; a linha que ele via no painel era a do MAC de
 * verdade, atualizada por outro app, e por isso ficava "presa" no último
 * canal que ESSE outro app tinha tocado.
 *
 * Mesma estratégia dos outros apps da família, nessa ordem de tentativa:
 * WifiManager -> interfaces de rede conhecidas (wlan0/wifi0/eth0) ->
 * qualquer interface ativa não-loopback -> só em último caso (aparelho
 * bloqueia leitura de MAC, comum a partir do Android 6+) um MAC estável
 * derivado do ANDROID_ID -- mesma fórmula usada nos outros apps, pra
 * convergir no mesmo valor mesmo quando nenhum deles consegue ler o MAC
 * real da placa.
 */
object MacAddressProvider {

    /** Nunca retorna null -- sempre há, no mínimo, o MAC estável derivado do aparelho. */
    fun getFixedMac(contexto: Context): String {
        getMacDaRede(contexto)?.let { return it }
        return criarMacEstavel(contexto)
    }

    private fun getMacDaRede(contexto: Context): String? {
        val macWifi = runCatching {
            val wifiManager = contexto.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiManager?.connectionInfo?.macAddress
        }.getOrNull()
        normalizarInterno(macWifi)?.let { return it }

        val nomesConhecidos = listOf("wlan0", "wifi0", "eth0")
        for (nome in nomesConhecidos) {
            val candidato = runCatching {
                NetworkInterface.getByName(nome)?.hardwareAddress
                    ?.joinToString(":") { byte -> "%02X".format(Locale.ROOT, byte.toInt() and 0xFF) }
            }.getOrNull()
            normalizarInterno(candidato)?.let { return it }
        }

        return runCatching {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.asSequence()
                ?.filter { it.isUp && !it.isLoopback }
                ?.mapNotNull { interfaceRede ->
                    interfaceRede.hardwareAddress
                        ?.joinToString(":") { byte -> "%02X".format(Locale.ROOT, byte.toInt() and 0xFF) }
                }
                ?.mapNotNull(::normalizarInterno)
                ?.firstOrNull()
        }.getOrNull()
    }

    /** Mesma fórmula (SHA-256 do ANDROID_ID, 6 primeiros bytes, bit
     * locally-administered ligado) usada no Rencia/Supreme -- garante o
     * mesmo MAC de reserva quando nenhum app da família consegue ler o
     * MAC real do aparelho. */
    private fun criarMacEstavel(contexto: Context): String {
        val androidId = Settings.Secure.getString(
            contexto.contentResolver,
            Settings.Secure.ANDROID_ID
        ).orEmpty().ifBlank { "supremus-device" }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(androidId.toByteArray(Charsets.UTF_8))
            .copyOf(6)

        // MAC local/unicast: não conflita com endereços físicos de fábrica.
        digest[0] = ((digest[0].toInt() and 0xFC) or 0x02).toByte()
        return digest.joinToString(":") { byte -> "%02X".format(Locale.ROOT, byte.toInt() and 0xFF) }
    }

    private fun normalizarInterno(bruto: String?): String? {
        val compacto = bruto.orEmpty().filter { it.isLetterOrDigit() }.uppercase(Locale.ROOT)
        if (compacto.length != 12 || compacto.any { it !in "0123456789ABCDEF" }) return null
        // Placeholders que o Android devolve quando não deixa ler o MAC real
        // (WifiManager sem permissão/privacidade) -- não são MAC de verdade.
        if (compacto == "020000000000" || compacto == "000000000000") return null
        return compacto.chunked(2).joinToString(":")
    }
}
