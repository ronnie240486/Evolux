package com.evolux.tv.data

import java.net.NetworkInterface
import java.util.Locale

object MacAddressProvider {
    fun detectar(): String? {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            while (interfaces.hasMoreElements()) {
                val interfaceRede = interfaces.nextElement()
                if (interfaceRede.isLoopback || !interfaceRede.isUp) continue

                val bytes = interfaceRede.hardwareAddress ?: continue
                if (bytes.size != 6) continue

                val mac = bytes.joinToString(":") { byte ->
                    "%02X".format(Locale.ROOT, byte.toInt() and 0xFF)
                }
                if (mac != "00:00:00:00:00:00" && mac != "02:00:00:00:00:00") {
                    return mac
                }
            }
            null
        }.getOrNull()
    }
}
