package com.evolux.tv.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Índice persistente do catálogo, equivalente ao papel do Realm no Ouro Pro.
 * Mantém todos os metadados e URLs no disco e permite consultar somente a página
 * atual. Nenhuma imagem é armazenada como bitmap aqui.
 */
class CatalogoStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    "evolux_catalogo_v1.db",
    null,
    1
) {
    data class Resumo(
        val fingerprint: String,
        val canais: Int,
        val filmes: Int,
        val series: Int,
        val categoriasCanais: List<String>,
        val categoriasFilmes: List<String>,
        val categoriasSeries: List<String>
    )

    data class Pagina<T>(val itens: List<T>, val total: Int) {
        val totalPaginas: Int get() = ((total + 29) / 30).coerceAtLeast(1)
    }

    data class GrupoSerieResumo(
        val chave: String,
        val nome: String,
        val categoria: String,
        val capa: String,
        val sinopse: String,
        val representante: Midia
    )

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE catalogo (
                id TEXT NOT NULL,
                tipo TEXT NOT NULL,
                titulo TEXT NOT NULL,
                imagem_url TEXT NOT NULL,
                stream_url TEXT NOT NULL,
                categoria TEXT NOT NULL,
                progresso REAL,
                nota REAL,
                popularidade INTEGER,
                sinopse TEXT NOT NULL,
                serie_id TEXT,
                serie_nome TEXT,
                episodio_nome TEXT,
                temporada INTEGER,
                episodio INTEGER,
                ordem INTEGER NOT NULL,
                PRIMARY KEY (id, tipo)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_catalogo_tipo_categoria_ordem ON catalogo(tipo, categoria, ordem)")
        db.execSQL("CREATE INDEX idx_catalogo_tipo_titulo ON catalogo(tipo, titulo COLLATE NOCASE)")
        db.execSQL("CREATE INDEX idx_catalogo_serie ON catalogo(tipo, categoria, serie_nome, temporada, episodio)")
        db.execSQL("CREATE TABLE estado (chave TEXT PRIMARY KEY, valor TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS catalogo")
        db.execSQL("DROP TABLE IF EXISTS estado")
        onCreate(db)
    }

    suspend fun substituir(fingerprint: String, catalogo: PlaylistCatalog): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val db = writableDatabase
            db.beginTransaction()
            try {
                db.delete("catalogo", null, null)
                var ordem = 0
                catalogo.canais.forEach { inserirCanal(db, it, ordem++) }
                catalogo.filmes.forEach { inserirMidia(db, it, ordem++) }
                catalogo.series.forEach { inserirMidia(db, it, ordem++) }
                salvarEstado(db, "fingerprint", fingerprint)
                salvarEstado(db, "total_canais", catalogo.canais.size.toString())
                salvarEstado(db, "total_filmes", catalogo.filmes.size.toString())
                salvarEstado(db, "total_series", catalogo.series.size.toString())
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            true
        }.getOrDefault(false)
    }

    suspend fun pronto(fingerprint: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val db = readableDatabase
            val salvo = estado(db, "fingerprint")
            if (salvo != fingerprint) return@runCatching false
            val c = db.rawQuery("SELECT COUNT(*) FROM catalogo WHERE tipo='CANAL'", null)
            val f = db.rawQuery("SELECT COUNT(*) FROM catalogo WHERE tipo='FILME'", null)
            val s = db.rawQuery("SELECT COUNT(*) FROM catalogo WHERE tipo='SERIE'", null)
            c.use { if (!it.moveToFirst() || it.getInt(0) == 0) return@runCatching false }
            f.use { if (!it.moveToFirst() || it.getInt(0) == 0) return@runCatching false }
            s.use { if (!it.moveToFirst() || it.getInt(0) == 0) return@runCatching false }
            true
        }.getOrDefault(false)
    }

    suspend fun resumo(fingerprint: String): Resumo? = withContext(Dispatchers.IO) {
        runCatching {
            val db = readableDatabase
            if (estado(db, "fingerprint") != fingerprint) return@runCatching null
            Resumo(
                fingerprint = fingerprint,
                canais = contar(db, "CANAL", null, ""),
                filmes = contar(db, "FILME", null, ""),
                series = contar(db, "SERIE", null, ""),
                categoriasCanais = categorias(db, "CANAL"),
                categoriasFilmes = categorias(db, "FILME"),
                categoriasSeries = categorias(db, "SERIE")
            )
        }.getOrNull()
    }

    suspend fun categorias(fingerprint: String, tipo: String): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            val db = readableDatabase
            if (estado(db, "fingerprint") != fingerprint) return@runCatching emptyList()
            categorias(db, tipo)
        }.getOrDefault(emptyList())
    }

    suspend fun paginaMidias(
        fingerprint: String,
        tipo: String,
        categoria: String,
        busca: String,
        ordem: OrdemCatalogo,
        pagina: Int,
        limite: Int = 30
    ): Pagina<Midia> = withContext(Dispatchers.IO) {
        runCatching {
            val db = readableDatabase
            if (estado(db, "fingerprint") != fingerprint) return@runCatching Pagina(emptyList(), 0)
            val args = ArrayList<String>()
            val where = StringBuilder("tipo = ?")
            args += tipo
            if (categoria != "Todos") {
                where.append(" AND categoria = ?")
                args += categoria
            }
            if (busca.isNotBlank()) {
                where.append(" AND (titulo LIKE ? OR categoria LIKE ?)")
                val termo = "%${busca.trim()}%"
                args += termo
                args += termo
            }
            val order = when (ordem) {
                OrdemCatalogo.NOME_AZ -> "titulo COLLATE NOCASE ASC"
                OrdemCatalogo.NOME_ZA -> "titulo COLLATE NOCASE DESC"
                OrdemCatalogo.NOTA, OrdemCatalogo.POPULARIDADE -> "COALESCE(nota, 0) DESC, COALESCE(popularidade, 0) DESC, ordem ASC"
                OrdemCatalogo.PADRAO -> "ordem ASC"
            }
            val total = contar(db, tipo, if (categoria == "Todos") null else categoria, busca)
            val offset = pagina.coerceAtLeast(0) * limite
            val itens = ArrayList<Midia>(limite)
            db.query(
                "catalogo",
                null,
                where.toString(),
                args.toTypedArray(),
                null,
                null,
                order,
                "$offset, $limite"
            ).use { cursor ->
                while (cursor.moveToNext()) itens += midia(cursor)
            }
            Pagina(itens, total)
        }.getOrDefault(Pagina(emptyList(), 0))
    }

    suspend fun paginaCanais(
        fingerprint: String,
        categoria: String,
        busca: String,
        pagina: Int,
        limite: Int = 30
    ): Pagina<Canal> = withContext(Dispatchers.IO) {
        runCatching {
            val db = readableDatabase
            if (estado(db, "fingerprint") != fingerprint) return@runCatching Pagina(emptyList(), 0)
            val args = ArrayList<String>()
            val where = StringBuilder("tipo = 'CANAL'")
            if (categoria != "Todos") {
                where.append(" AND categoria = ?")
                args += categoria
            }
            if (busca.isNotBlank()) {
                where.append(" AND titulo LIKE ?")
                args += "%${busca.trim()}%"
            }
            val total = contar(db, "CANAL", if (categoria == "Todos") null else categoria, busca)
            val offset = pagina.coerceAtLeast(0) * limite
            val itens = ArrayList<Canal>(limite)
            db.query("catalogo", null, where.toString(), args.toTypedArray(), null, null, "ordem ASC", "$offset, $limite").use { cursor ->
                while (cursor.moveToNext()) itens += canal(cursor)
            }
            Pagina(itens, total)
        }.getOrDefault(Pagina(emptyList(), 0))
    }

    suspend fun gruposSeries(
        fingerprint: String,
        categoria: String,
        busca: String,
        ordem: OrdemCatalogo,
        pagina: Int,
        limite: Int = 24
    ): Pagina<GrupoSerieResumo> = withContext(Dispatchers.IO) {
        runCatching {
            val db = readableDatabase
            if (estado(db, "fingerprint") != fingerprint) return@runCatching Pagina(emptyList(), 0)
            val args = ArrayList<String>()
            val where = StringBuilder("tipo = 'SERIE'")
            if (categoria != "Todos") { where.append(" AND categoria = ?"); args += categoria }
            if (busca.isNotBlank()) { where.append(" AND (titulo LIKE ? OR categoria LIKE ? OR serie_nome LIKE ?)"); val termo = "%${busca.trim()}%"; args += termo; args += termo; args += termo }
            val nomeSql = "COALESCE(NULLIF(serie_nome, ''), titulo)"
            val direcao = if (ordem == OrdemCatalogo.NOME_ZA) "DESC" else "ASC"
            val filtroArgs = args.toTypedArray()
            val total = db.rawQuery(
                "SELECT COUNT(*) FROM (SELECT 1 FROM catalogo WHERE $where GROUP BY categoria, $nomeSql)",
                filtroArgs
            ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
            val offset = pagina.coerceAtLeast(0) * limite
            val itens = ArrayList<GrupoSerieResumo>(limite)
            val sql = """
                SELECT categoria, $nomeSql AS nome,
                       MIN(id) AS id, MIN(imagem_url) AS imagem_url,
                       MIN(stream_url) AS stream_url, MIN(sinopse) AS sinopse,
                       MIN(serie_id) AS serie_id
                FROM catalogo WHERE $where
                GROUP BY categoria, $nomeSql
                ORDER BY nome COLLATE NOCASE $direcao
                LIMIT ? OFFSET ?
            """.trimIndent()
            val consultaArgs = ArrayList<String>(args).apply { add(limite.toString()); add(offset.toString()) }
            db.rawQuery(sql, consultaArgs.toTypedArray()).use { c ->
                val categoriaCol = c.getColumnIndexOrThrow("categoria")
                val nomeCol = c.getColumnIndexOrThrow("nome")
                val idCol = c.getColumnIndexOrThrow("id")
                val imagemCol = c.getColumnIndexOrThrow("imagem_url")
                val streamCol = c.getColumnIndexOrThrow("stream_url")
                val sinopseCol = c.getColumnIndexOrThrow("sinopse")
                val serieIdCol = c.getColumnIndexOrThrow("serie_id")
                while (c.moveToNext()) {
                    val categoriaAtual = c.getString(categoriaCol)
                    val nomeAtual = c.getString(nomeCol)
                    val representante = Midia(
                        id = c.getString(idCol), titulo = nomeAtual,
                        imagemUrl = c.getString(imagemCol).orEmpty(), tipo = TipoMidia.SERIE,
                        streamUrl = c.getString(streamCol), categoria = categoriaAtual,
                        sinopse = c.getString(sinopseCol).orEmpty(), serieId = c.getStringOrNull("serie_id"), serieNome = nomeAtual
                    )
                    itens += GrupoSerieResumo(
                        chave = "${categoriaAtual}::${nomeAtual.trim().lowercase()}", nome = nomeAtual,
                        categoria = categoriaAtual, capa = representante.imagemUrl,
                        sinopse = representante.sinopse, representante = representante
                    )
                }
            }
            Pagina(itens, total)
        }.getOrDefault(Pagina(emptyList(), 0))
    }

    suspend fun episodiosDaSerie(fingerprint: String, categoria: String, nome: String): List<Midia> = withContext(Dispatchers.IO) {
        runCatching {
            val db = readableDatabase
            if (estado(db, "fingerprint") != fingerprint) return@runCatching emptyList()
            val out = ArrayList<Midia>()
            db.query("catalogo", null, "tipo = 'SERIE' AND categoria = ? AND (serie_nome = ? OR titulo = ?)", arrayOf(categoria, nome, nome), null, null, "temporada ASC, episodio ASC, titulo COLLATE NOCASE ASC").use { c ->
                while (c.moveToNext()) out += midia(c)
            }
            out
        }.getOrDefault(emptyList())
    }

    suspend fun limparSeOutroFingerprint(fingerprint: String) = withContext(Dispatchers.IO) {
        runCatching {
            val db = writableDatabase
            if (estado(db, "fingerprint") != fingerprint) {
                db.delete("catalogo", null, null)
                db.delete("estado", null, null)
            }
        }
    }

    private fun inserirCanal(db: SQLiteDatabase, canal: Canal, ordem: Int) {
        val v = ContentValues().apply {
            put("id", canal.id); put("tipo", "CANAL"); put("titulo", canal.nome)
            put("imagem_url", canal.logoUrl); put("stream_url", canal.streamUrl)
            put("categoria", canal.categoria.ifBlank { "TV ao vivo" }); put("sinopse", ""); put("ordem", ordem)
        }
        db.insertOrThrow("catalogo", null, v)
    }

    private fun inserirMidia(db: SQLiteDatabase, midia: Midia, ordem: Int) {
        val v = ContentValues().apply {
            put("id", midia.id); put("tipo", midia.tipo.name); put("titulo", midia.titulo)
            put("imagem_url", midia.imagemUrl); put("stream_url", midia.streamUrl)
            put("categoria", midia.categoria.ifBlank { "Sem categoria" }); put("progresso", midia.progresso)
            put("nota", midia.nota); put("popularidade", midia.popularidade); put("sinopse", midia.sinopse)
            put("serie_id", midia.serieId); put("serie_nome", midia.serieNome)
            put("episodio_nome", midia.episodioNome); put("temporada", midia.temporadaNumero)
            put("episodio", midia.episodioNumero); put("ordem", ordem)
        }
        db.insertOrThrow("catalogo", null, v)
    }

    private fun categorias(db: SQLiteDatabase, tipo: String): List<String> {
        val out = ArrayList<String>()
        db.query(true, "catalogo", arrayOf("categoria"), "tipo = ?", arrayOf(tipo), null, null, "categoria COLLATE NOCASE ASC", null).use { c ->
            while (c.moveToNext()) out += c.getString(0)
        }
        return out
    }

    private fun contar(db: SQLiteDatabase, tipo: String, categoria: String?, busca: String): Int {
        val args = ArrayList<String>()
        val where = StringBuilder("tipo = ?")
        args += tipo
        if (categoria != null) { where.append(" AND categoria = ?"); args += categoria }
        if (busca.isNotBlank()) { where.append(" AND (titulo LIKE ? OR categoria LIKE ?)"); args += "%${busca.trim()}%"; args += "%${busca.trim()}%" }
        db.rawQuery("SELECT COUNT(*) FROM catalogo WHERE $where", args.toTypedArray()).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    private fun estado(db: SQLiteDatabase, chave: String): String? {
        db.query("estado", arrayOf("valor"), "chave = ?", arrayOf(chave), null, null, null).use { c ->
            return if (c.moveToFirst()) c.getString(0) else null
        }
    }

    private fun salvarEstado(db: SQLiteDatabase, chave: String, valor: String) {
        db.insertWithOnConflict("estado", null, ContentValues().apply { put("chave", chave); put("valor", valor) }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun canal(c: android.database.Cursor) = Canal(
        id = c.getString(c.getColumnIndexOrThrow("id")),
        nome = c.getString(c.getColumnIndexOrThrow("titulo")),
        logoUrl = c.getString(c.getColumnIndexOrThrow("imagem_url")),
        streamUrl = c.getString(c.getColumnIndexOrThrow("stream_url")),
        categoria = c.getString(c.getColumnIndexOrThrow("categoria"))
    )

    private fun midia(c: android.database.Cursor) = Midia(
        id = c.getString(c.getColumnIndexOrThrow("id")),
        titulo = c.getString(c.getColumnIndexOrThrow("titulo")),
        imagemUrl = c.getString(c.getColumnIndexOrThrow("imagem_url")),
        tipo = runCatching { TipoMidia.valueOf(c.getString(c.getColumnIndexOrThrow("tipo"))) }.getOrDefault(TipoMidia.FILME),
        streamUrl = c.getString(c.getColumnIndexOrThrow("stream_url")),
        progresso = c.getDoubleOrNull("progresso")?.toFloat(), categoria = c.getString(c.getColumnIndexOrThrow("categoria")),
        nota = c.getDoubleOrNull("nota"), popularidade = c.getLongOrNull("popularidade"), sinopse = c.getString(c.getColumnIndexOrThrow("sinopse")),
        serieId = c.getStringOrNull("serie_id"), serieNome = c.getStringOrNull("serie_nome"), episodioNome = c.getStringOrNull("episodio_nome"),
        temporadaNumero = c.getIntOrNull("temporada"), episodioNumero = c.getIntOrNull("episodio")
    )

    private fun android.database.Cursor.getStringOrNull(name: String): String? = getColumnIndex(name).takeIf { it >= 0 && !isNull(it) }?.let(::getString)
    private fun android.database.Cursor.getDoubleOrNull(name: String): Double? = getColumnIndex(name).takeIf { it >= 0 && !isNull(it) }?.let(::getDouble)
    private fun android.database.Cursor.getLongOrNull(name: String): Long? = getColumnIndex(name).takeIf { it >= 0 && !isNull(it) }?.let(::getLong)
    private fun android.database.Cursor.getIntOrNull(name: String): Int? = getColumnIndex(name).takeIf { it >= 0 && !isNull(it) }?.let(::getInt)
}
