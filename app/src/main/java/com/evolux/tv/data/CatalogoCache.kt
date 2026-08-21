package com.evolux.tv.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

object CatalogoCache {
    private const val NOME_ARQUIVO = "evolux-catalogo-cache-v4.bin"
    private const val MAGIC = "EVOLUX-CATALOG-4"
    private const val PARSER_VERSION = "xtream-series-entities-v6-complete-catalog"
    // O catálogo real possui dezenas de milhares de itens; o limite precisa comportar o snapshot completo.
    private const val MAX_CACHE_BYTES = 256L * 1024L * 1024L

    suspend fun carregar(contexto: Context, fingerprint: String): PlaylistCatalog? = withContext(Dispatchers.IO) {
        val arquivo = File(contexto.filesDir, NOME_ARQUIVO)
        if (!arquivo.exists() || arquivo.length() > MAX_CACHE_BYTES) return@withContext null
        runCatching {
            DataInputStream(BufferedInputStream(FileInputStream(arquivo))).use { entrada ->
                if (entrada.readUTF() != MAGIC) return@use null
                if (entrada.readUTF() != fingerprint) return@use null
                val canais = readCanais(entrada)
                val filmes = readMidias(entrada)
                val series = readMidias(entrada)
                val truncado = entrada.readBoolean()
                if (canais.isEmpty() || filmes.isEmpty() || series.isEmpty()) null
                else PlaylistCatalog(canais, filmes, series, truncado)
            }
        }.getOrElse {
            arquivo.delete()
            null
        }
    }

    suspend fun salvar(contexto: Context, fingerprint: String, catalogo: PlaylistCatalog) = withContext(Dispatchers.IO) {
        val temporario = File(contexto.filesDir, "$NOME_ARQUIVO.tmp")
        val destino = File(contexto.filesDir, NOME_ARQUIVO)
        runCatching {
            DataOutputStream(BufferedOutputStream(FileOutputStream(temporario))).use { saida ->
                saida.writeUTF(MAGIC)
                saida.writeUTF(fingerprint)
                writeCanais(saida, catalogo.canais)
                writeMidias(saida, catalogo.filmes)
                writeMidias(saida, catalogo.series)
                saida.writeBoolean(catalogo.truncado)
            }
            if (temporario.length() <= MAX_CACHE_BYTES) {
                if (destino.exists() && !destino.delete()) {
                    temporario.delete()
                } else {
                    if (!temporario.renameTo(destino)) {
                        temporario.delete()
                    } else {
                        Unit
                    }
                }
            } else {
                temporario.delete()
            }
        }
    }

    fun fingerprint(configuracao: EvoluxConfig, urlAtiva: String? = null): String {
        val entrada = buildString {
            append(configuracao.playlistUrls.joinToString("|"))
            append("|active=")
            append(urlAtiva.orEmpty())
            append("|app=")
            append(configuracao.appId)
            append("|parser=")
            append(PARSER_VERSION)
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(entrada.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun writeCanais(saida: DataOutputStream, canais: List<Canal>) {
        saida.writeInt(canais.size)
        canais.forEach { canal ->
            saida.writeUTF(canal.id)
            saida.writeUTF(canal.nome)
            saida.writeUTF(canal.logoUrl)
            saida.writeUTF(canal.streamUrl)
            saida.writeUTF(canal.categoria)
        }
    }

    private fun readCanais(entrada: DataInputStream): List<Canal> {
        val quantidade = entrada.readInt().coerceIn(0, 100_000)
        return buildList(quantidade) {
            repeat(quantidade) {
                add(
                    Canal(
                        id = entrada.readUTF(),
                        nome = entrada.readUTF(),
                        logoUrl = entrada.readUTF(),
                        streamUrl = entrada.readUTF(),
                        categoria = entrada.readUTF()
                    )
                )
            }
        }
    }

    private fun writeMidias(saida: DataOutputStream, midias: List<Midia>) {
        saida.writeInt(midias.size)
        midias.forEach { midia ->
            saida.writeUTF(midia.id)
            saida.writeUTF(midia.titulo)
            saida.writeUTF(midia.imagemUrl)
            saida.writeUTF(midia.tipo.name)
            saida.writeUTF(midia.streamUrl)
            writeNullableFloat(saida, midia.progresso)
            saida.writeUTF(midia.categoria)
            writeNullableDouble(saida, midia.nota)
            writeNullableLong(saida, midia.popularidade)
            saida.writeUTF(midia.sinopse)
            writeNullableString(saida, midia.serieId)
            writeNullableString(saida, midia.serieNome)
            writeNullableString(saida, midia.episodioNome)
            writeNullableInt(saida, midia.temporadaNumero)
            writeNullableInt(saida, midia.episodioNumero)
        }
    }

    private fun readMidias(entrada: DataInputStream): List<Midia> {
        val quantidade = entrada.readInt().coerceIn(0, 100_000)
        return buildList(quantidade) {
            repeat(quantidade) {
                add(
                    Midia(
                        id = entrada.readUTF(),
                        titulo = entrada.readUTF(),
                        imagemUrl = entrada.readUTF(),
                        tipo = runCatching { TipoMidia.valueOf(entrada.readUTF()) }.getOrDefault(TipoMidia.FILME),
                        streamUrl = entrada.readUTF(),
                        progresso = readNullableFloat(entrada),
                        categoria = entrada.readUTF(),
                        nota = readNullableDouble(entrada),
                        popularidade = readNullableLong(entrada),
                        sinopse = entrada.readUTF(),
                        serieId = readNullableString(entrada),
                        serieNome = readNullableString(entrada),
                        episodioNome = readNullableString(entrada),
                        temporadaNumero = readNullableInt(entrada),
                        episodioNumero = readNullableInt(entrada)
                    )
                )
            }
        }
    }

    private fun writeNullableString(saida: DataOutputStream, valor: String?) {
        saida.writeBoolean(valor != null)
        if (valor != null) saida.writeUTF(valor)
    }

    private fun readNullableString(entrada: DataInputStream): String? = if (entrada.readBoolean()) entrada.readUTF() else null

    private fun writeNullableInt(saida: DataOutputStream, valor: Int?) {
        saida.writeBoolean(valor != null)
        if (valor != null) saida.writeInt(valor)
    }

    private fun readNullableInt(entrada: DataInputStream): Int? = if (entrada.readBoolean()) entrada.readInt() else null

    private fun writeNullableLong(saida: DataOutputStream, valor: Long?) {
        saida.writeBoolean(valor != null)
        if (valor != null) saida.writeLong(valor)
    }

    private fun readNullableLong(entrada: DataInputStream): Long? = if (entrada.readBoolean()) entrada.readLong() else null

    private fun writeNullableDouble(saida: DataOutputStream, valor: Double?) {
        saida.writeBoolean(valor != null)
        if (valor != null) saida.writeDouble(valor)
    }

    private fun readNullableDouble(entrada: DataInputStream): Double? = if (entrada.readBoolean()) entrada.readDouble() else null

    private fun writeNullableFloat(saida: DataOutputStream, valor: Float?) {
        saida.writeBoolean(valor != null)
        if (valor != null) saida.writeFloat(valor)
    }

    private fun readNullableFloat(entrada: DataInputStream): Float? = if (entrada.readBoolean()) entrada.readFloat() else null
}
