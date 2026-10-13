package com.senati.mitiemposemanal.ayovera.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.senati.mitiemposemanal.ayovera.model.Categoria
import com.senati.mitiemposemanal.ayovera.model.DetalleReporteCategoria
import com.senati.mitiemposemanal.ayovera.model.Evento
import com.senati.mitiemposemanal.ayovera.model.ReporteSemanal
import com.senati.mitiemposemanal.ayovera.model.ResumenSemanal
import com.senati.mitiemposemanal.ayovera.model.Tarea
import java.time.LocalDate
import java.time.LocalDateTime

sealed class ResultadoOperacion {
    data class Exito(val id: Long, val mensaje: String) : ResultadoOperacion()
    data class Error(val mensaje: String) : ResultadoOperacion()
}

sealed class ResultadoEliminacionCategoria {
    data class Eliminada(val mensaje: String) : ResultadoEliminacionCategoria()
    data class BloqueadaConTareas(
        val categoriaNombre: String,
        val tareasAsociadas: List<Tarea>,
        val mensaje: String
    ) : ResultadoEliminacionCategoria()
    data class Error(val mensaje: String) : ResultadoEliminacionCategoria()
}

sealed class ResultadoEvento {
    data class Exito(val evento: Evento, val mensaje: String) : ResultadoEvento()
    data class Error(val mensaje: String) : ResultadoEvento()
}

/**
 * Gestor de base de datos SQLite mediante SQLiteOpenHelper para MiTiempoSemanal (Sprints 1, 2, 3 y 4).
 *
 * Tablas:
 * - `categorias` (id PK, nombre NOT NULL UNIQUE COLLATE NOCASE)
 * - `tareas` (id PK, nombre NOT NULL UNIQUE COLLATE NOCASE, categoria_id FK -> categorias.id ON DELETE RESTRICT)
 * - `eventos` (id PK, tarea_id FK -> tareas.id ON DELETE RESTRICT, fecha_inicio NOT NULL, fecha_fin NULL,
 *              duracion_segundos NULL, estado NOT NULL, tarea_nombre_snapshot TEXT, categoria_nombre_snapshot TEXT)
 *              + índice único parcial para garantizar un único evento activo
 *              + índice por fecha_inicio y estado para consultas semanales agrupadas (Sprint 3).
 * - `reportes_semanales` (id PK, fecha_inicio_semana, fecha_fin_semana, segundos_registrados,
 *                         minutos_registrados, porcentaje_registrado, segundos_sin_registrar,
 *                         minutos_sin_registrar, porcentaje_sin_registrar, total_eventos, fecha_cierre,
 *                         UNIQUE(fecha_inicio_semana, fecha_fin_semana))
 * - `detalle_reportes_semanales` (id PK, reporte_id FK -> reportes_semanales.id ON DELETE CASCADE,
 *                                 categoria_id, categoria_nombre, segundos_totales, minutos_totales,
 *                                 porcentaje, UNIQUE(reporte_id, categoria_id))
 */
class AdminSQLiteOpenHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "mi_tiempo_semanal.db"
        const val DATABASE_VERSION = 4

        const val TABLE_CATEGORIAS = "categorias"
        const val COL_CAT_ID = "id"
        const val COL_CAT_NOMBRE = "nombre"

        const val TABLE_TAREAS = "tareas"
        const val COL_TASK_ID = "id"
        const val COL_TASK_NOMBRE = "nombre"
        const val COL_TASK_CATEGORIA_ID = "categoria_id"

        const val TABLE_EVENTOS = "eventos"
        const val COL_EVENT_ID = "id"
        const val COL_EVENT_TAREA_ID = "tarea_id"
        const val COL_EVENT_FECHA_INICIO = "fecha_inicio"
        const val COL_EVENT_FECHA_FIN = "fecha_fin"
        const val COL_EVENT_DURACION_SEGUNDOS = "duracion_segundos"
        const val COL_EVENT_ESTADO = "estado"
        const val COL_EVENT_TAREA_SNAPSHOT = "tarea_nombre_snapshot"
        const val COL_EVENT_CAT_SNAPSHOT = "categoria_nombre_snapshot"
        const val INDEX_UNICO_EVENTO_ACTIVO = "idx_unico_evento_activo"
        const val INDEX_EVENTOS_SEMANA = "idx_eventos_semana"

        const val TABLE_REPORTES_SEMANALES = "reportes_semanales"
        const val COL_REP_ID = "id"
        const val COL_REP_FECHA_INICIO = "fecha_inicio_semana"
        const val COL_REP_FECHA_FIN = "fecha_fin_semana"
        const val COL_REP_SEG_REGISTRADOS = "segundos_registrados"
        const val COL_REP_MIN_REGISTRADOS = "minutos_registrados"
        const val COL_REP_PORC_REGISTRADO = "porcentaje_registrado"
        const val COL_REP_SEG_SIN_REGISTRAR = "segundos_sin_registrar"
        const val COL_REP_MIN_SIN_REGISTRAR = "minutos_sin_registrar"
        const val COL_REP_PORC_SIN_REGISTRAR = "porcentaje_sin_registrar"
        const val COL_REP_TOTAL_EVENTOS = "total_eventos"
        const val COL_REP_FECHA_CIERRE = "fecha_cierre"

        const val TABLE_DETALLE_REPORTES = "detalle_reportes_semanales"
        const val COL_DET_ID = "id"
        const val COL_DET_REPORTE_ID = "reporte_id"
        const val COL_DET_CATEGORIA_ID = "categoria_id"
        const val COL_DET_CATEGORIA_NOMBRE = "categoria_nombre"
        const val COL_DET_SEGUNDOS_TOTALES = "segundos_totales"
        const val COL_DET_MINUTOS_TOTALES = "minutos_totales"
        const val COL_DET_PORCENTAJE = "porcentaje"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIAS (
                $COL_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CAT_NOMBRE TEXT NOT NULL UNIQUE COLLATE NOCASE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_TAREAS (
                $COL_TASK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TASK_NOMBRE TEXT NOT NULL UNIQUE COLLATE NOCASE,
                $COL_TASK_CATEGORIA_ID INTEGER NOT NULL,
                FOREIGN KEY ($COL_TASK_CATEGORIA_ID) REFERENCES $TABLE_CATEGORIAS($COL_CAT_ID)
                    ON DELETE RESTRICT ON UPDATE CASCADE
            )
            """.trimIndent()
        )

        crearTablaEventosEIndices(db)
        crearIndiceConsultasSemanales(db)
        asegurarColumnasSnapshotEventos(db)
        crearTablasReportesSemanales(db)
        insertarCategoriasIniciales(db)
    }

    /**
     * Migración incremental preservando todos los datos de los Sprints 1, 2 y 3:
     * - v1 -> v2: Crea la tabla `eventos` y su índice único de evento activo sin eliminar `categorias` ni `tareas`.
     * - v2 -> v3: Crea el índice `idx_eventos_semana` para optimizar el cómputo semanal con SUM y GROUP BY.
     * - v3 -> v4: Añade columnas de instantánea histórica en `eventos` y crea las tablas `reportes_semanales`
     *             y `detalle_reportes_semanales` con restricciones UNIQUE sin perder categorías, tareas ni eventos.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            crearTablaEventosEIndices(db)
            insertarCategoriasIniciales(db)
        }
        if (oldVersion < 3) {
            crearTablaEventosEIndices(db)
            crearIndiceConsultasSemanales(db)
            insertarCategoriasIniciales(db)
        }
        if (oldVersion < 4) {
            crearTablaEventosEIndices(db)
            crearIndiceConsultasSemanales(db)
            asegurarColumnasSnapshotEventos(db)
            crearTablasReportesSemanales(db)
            insertarCategoriasIniciales(db)
        }
    }

    private fun crearTablaEventosEIndices(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_EVENTOS (
                $COL_EVENT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_EVENT_TAREA_ID INTEGER NOT NULL,
                $COL_EVENT_FECHA_INICIO TEXT NOT NULL,
                $COL_EVENT_FECHA_FIN TEXT,
                $COL_EVENT_DURACION_SEGUNDOS INTEGER,
                $COL_EVENT_ESTADO TEXT NOT NULL DEFAULT '${Evento.ESTADO_EN_CURSO}',
                $COL_EVENT_TAREA_SNAPSHOT TEXT,
                $COL_EVENT_CAT_SNAPSHOT TEXT,
                FOREIGN KEY ($COL_EVENT_TAREA_ID) REFERENCES $TABLE_TAREAS($COL_TASK_ID)
                    ON DELETE RESTRICT ON UPDATE CASCADE
            )
            """.trimIndent()
        )

        // Restricción a nivel de SQLite para garantizar que nunca exista más de un evento en curso
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS $INDEX_UNICO_EVENTO_ACTIVO
            ON $TABLE_EVENTOS($COL_EVENT_ESTADO)
            WHERE $COL_EVENT_ESTADO = '${Evento.ESTADO_EN_CURSO}'
            """.trimIndent()
        )
    }

    private fun crearIndiceConsultasSemanales(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS $INDEX_EVENTOS_SEMANA
            ON $TABLE_EVENTOS($COL_EVENT_FECHA_INICIO, $COL_EVENT_ESTADO)
            """.trimIndent()
        )
    }

    /**
     * En migraciones desde v2 o v3, añade mediante ALTER TABLE las columnas opcionales de instantánea
     * histórica en `eventos` si todavía no existen.
     */
    private fun asegurarColumnasSnapshotEventos(db: SQLiteDatabase) {
        val columnasExistentes = mutableSetOf<String>()
        val cursor = db.rawQuery("PRAGMA table_info($TABLE_EVENTOS)", null)
        cursor.use {
            val nameIdx = it.getColumnIndex("name")
            while (it.moveToNext()) {
                if (nameIdx >= 0) {
                    columnasExistentes.add(it.getString(nameIdx))
                }
            }
        }
        if (COL_EVENT_TAREA_SNAPSHOT !in columnasExistentes) {
            db.execSQL("ALTER TABLE $TABLE_EVENTOS ADD COLUMN $COL_EVENT_TAREA_SNAPSHOT TEXT")
        }
        if (COL_EVENT_CAT_SNAPSHOT !in columnasExistentes) {
            db.execSQL("ALTER TABLE $TABLE_EVENTOS ADD COLUMN $COL_EVENT_CAT_SNAPSHOT TEXT")
        }
    }

    /**
     * Sección 6: Crea las tablas `reportes_semanales` y `detalle_reportes_semanales` con restricciones
     * UNIQUE a nivel de SQLite para garantizar idempotencia y ausencia de duplicados.
     */
    private fun crearTablasReportesSemanales(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_REPORTES_SEMANALES (
                $COL_REP_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_REP_FECHA_INICIO TEXT NOT NULL,
                $COL_REP_FECHA_FIN TEXT NOT NULL,
                $COL_REP_SEG_REGISTRADOS INTEGER NOT NULL DEFAULT 0,
                $COL_REP_MIN_REGISTRADOS REAL NOT NULL DEFAULT 0.0,
                $COL_REP_PORC_REGISTRADO REAL NOT NULL DEFAULT 0.0,
                $COL_REP_SEG_SIN_REGISTRAR INTEGER NOT NULL DEFAULT ${ResumenSemanal.SEGUNDOS_SEMANA},
                $COL_REP_MIN_SIN_REGISTRAR REAL NOT NULL DEFAULT ${ResumenSemanal.MINUTOS_SEMANA},
                $COL_REP_PORC_SIN_REGISTRAR REAL NOT NULL DEFAULT 100.0,
                $COL_REP_TOTAL_EVENTOS INTEGER NOT NULL DEFAULT 0,
                $COL_REP_FECHA_CIERRE TEXT NOT NULL,
                UNIQUE($COL_REP_FECHA_INICIO, $COL_REP_FECHA_FIN)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_DETALLE_REPORTES (
                $COL_DET_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_DET_REPORTE_ID INTEGER NOT NULL,
                $COL_DET_CATEGORIA_ID INTEGER NOT NULL,
                $COL_DET_CATEGORIA_NOMBRE TEXT NOT NULL,
                $COL_DET_SEGUNDOS_TOTALES INTEGER NOT NULL DEFAULT 0,
                $COL_DET_MINUTOS_TOTALES REAL NOT NULL DEFAULT 0.0,
                $COL_DET_PORCENTAJE REAL NOT NULL DEFAULT 0.0,
                FOREIGN KEY ($COL_DET_REPORTE_ID) REFERENCES $TABLE_REPORTES_SEMANALES($COL_REP_ID)
                    ON DELETE CASCADE ON UPDATE CASCADE,
                UNIQUE($COL_DET_REPORTE_ID, $COL_DET_CATEGORIA_ID)
            )
            """.trimIndent()
        )
    }

    /**
     * Inserta las 4 categorías iniciales (Estudio, Trabajo, Descanso, Ocio) usando INSERT OR IGNORE
     * para evitar duplicados al reabrir o actualizar la aplicación.
     */
    private fun insertarCategoriasIniciales(db: SQLiteDatabase) {
        ValidationLogic.CATEGORIAS_INICIALES.forEach { nombreCategoria ->
            val values = ContentValues().apply {
                put(COL_CAT_NOMBRE, nombreCategoria)
            }
            db.insertWithOnConflict(
                TABLE_CATEGORIAS,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }
    }

    /**
     * Garantiza que si la tabla de categorías está completamente vacía (primera apertura),
     * existan las 4 categorías iniciales sin duplicarlas en aperturas posteriores.
     */
    fun asegurarCategoriasInicialesSiVacio() {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CATEGORIAS", null)
        var count = 0
        cursor.use {
            if (it.moveToFirst()) {
                count = it.getInt(0)
            }
        }
        if (count == 0) {
            insertarCategoriasIniciales(db)
        }
    }

    // =========================================================================
    // OPERACIONES DE CATEGORÍAS (HU-01)
    // =========================================================================

    fun listarCategorias(): List<Categoria> {
        val lista = mutableListOf<Categoria>()
        val db = readableDatabase
        val query = """
            SELECT c.$COL_CAT_ID, c.$COL_CAT_NOMBRE, COUNT(t.$COL_TASK_ID) AS total_tareas
            FROM $TABLE_CATEGORIAS c
            LEFT JOIN $TABLE_TAREAS t ON c.$COL_CAT_ID = t.$COL_TASK_CATEGORIA_ID
            GROUP BY c.$COL_CAT_ID, c.$COL_CAT_NOMBRE
            ORDER BY c.$COL_CAT_ID ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                lista.add(
                    Categoria(
                        id = it.getLong(0),
                        nombre = it.getString(1),
                        cantidadTareas = it.getInt(2)
                    )
                )
            }
        }
        return lista
    }

    fun obtenerCategoriaPorId(id: Long): Categoria? {
        val db = readableDatabase
        val query = """
            SELECT c.$COL_CAT_ID, c.$COL_CAT_NOMBRE, COUNT(t.$COL_TASK_ID) AS total_tareas
            FROM $TABLE_CATEGORIAS c
            LEFT JOIN $TABLE_TAREAS t ON c.$COL_CAT_ID = t.$COL_TASK_CATEGORIA_ID
            WHERE c.$COL_CAT_ID = ?
            GROUP BY c.$COL_CAT_ID, c.$COL_CAT_NOMBRE
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(id.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                return Categoria(
                    id = it.getLong(0),
                    nombre = it.getString(1),
                    cantidadTareas = it.getInt(2)
                )
            }
        }
        return null
    }

    fun crearCategoria(nombreInput: String): ResultadoOperacion {
        val nombreLimpio = ValidationLogic.normalizarNombre(nombreInput)
        val nombresExistentes = listarCategorias().map { it.nombre }

        val error = ValidationLogic.validarNombreCategoria(
            nombreRaw = nombreLimpio,
            nombresExistentes = nombresExistentes
        )
        if (error != null) {
            return ResultadoOperacion.Error(error)
        }

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CAT_NOMBRE, nombreLimpio)
        }
        val nuevoId = db.insertWithOnConflict(
            TABLE_CATEGORIAS,
            null,
            values,
            SQLiteDatabase.CONFLICT_ABORT
        )
        return if (nuevoId != -1L) {
            ResultadoOperacion.Exito(
                id = nuevoId,
                mensaje = "¡Categoría “$nombreLimpio” creada con éxito!"
            )
        } else {
            ResultadoOperacion.Error("Ya existe una categoría con el nombre “$nombreLimpio”.")
        }
    }

    fun actualizarCategoria(id: Long, nuevoNombreInput: String): ResultadoOperacion {
        val categoriaActual = obtenerCategoriaPorId(id)
            ?: return ResultadoOperacion.Error("La categoría seleccionada no existe.")

        val nombreLimpio = ValidationLogic.normalizarNombre(nuevoNombreInput)
        val nombresExistentes = listarCategorias().map { it.nombre }

        val error = ValidationLogic.validarNombreCategoria(
            nombreRaw = nombreLimpio,
            nombresExistentes = nombresExistentes,
            nombreActualEnEdicion = categoriaActual.nombre
        )
        if (error != null) {
            return ResultadoOperacion.Error(error)
        }

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CAT_NOMBRE, nombreLimpio)
        }
        val filas = db.update(
            TABLE_CATEGORIAS,
            values,
            "$COL_CAT_ID = ?",
            arrayOf(id.toString())
        )
        return if (filas > 0) {
            ResultadoOperacion.Exito(
                id = id,
                mensaje = "Categoría actualizada a “$nombreLimpio”."
            )
        } else {
            ResultadoOperacion.Error("No se pudo actualizar la categoría.")
        }
    }

    fun eliminarCategoria(id: Long): ResultadoEliminacionCategoria {
        val categoria = obtenerCategoriaPorId(id)
            ?: return ResultadoEliminacionCategoria.Error("La categoría no existe.")

        val tareasAsociadas = listarTareas(categoriaIdFiltro = id)
        if (tareasAsociadas.isNotEmpty()) {
            val cantidad = tareasAsociadas.size
            val plural = if (cantidad == 1) "1 tarea asociada" else "$cantidad tareas asociadas"
            return ResultadoEliminacionCategoria.BloqueadaConTareas(
                categoriaNombre = categoria.nombre,
                tareasAsociadas = tareasAsociadas,
                mensaje = "No se puede eliminar la categoría “${categoria.nombre}” porque tiene $plural. Reasigna o elimina primero sus tareas."
            )
        }

        val db = writableDatabase
        return try {
            val filas = db.delete(
                TABLE_CATEGORIAS,
                "$COL_CAT_ID = ?",
                arrayOf(id.toString())
            )
            if (filas > 0) {
                ResultadoEliminacionCategoria.Eliminada(
                    "Categoría “${categoria.nombre}” eliminada correctamente."
                )
            } else {
                ResultadoEliminacionCategoria.Error("No se pudo eliminar la categoría.")
            }
        } catch (e: Exception) {
            ResultadoEliminacionCategoria.Error(
                "No se puede eliminar una categoría que tiene tareas asociadas."
            )
        }
    }

    // =========================================================================
    // OPERACIONES DE TAREAS (HU-02, HU-03 y HU-04)
    // =========================================================================

    fun listarTareas(categoriaIdFiltro: Long? = null): List<Tarea> {
        val lista = mutableListOf<Tarea>()
        val db = readableDatabase

        val whereClause = if (categoriaIdFiltro != null && categoriaIdFiltro > 0L) {
            "WHERE t.$COL_TASK_CATEGORIA_ID = ?"
        } else {
            ""
        }
        val args = if (categoriaIdFiltro != null && categoriaIdFiltro > 0L) {
            arrayOf(categoriaIdFiltro.toString())
        } else {
            emptyArray()
        }

        val query = """
            SELECT t.$COL_TASK_ID, t.$COL_TASK_NOMBRE, t.$COL_TASK_CATEGORIA_ID, c.$COL_CAT_NOMBRE
            FROM $TABLE_TAREAS t
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COL_TASK_CATEGORIA_ID = c.$COL_CAT_ID
            $whereClause
            ORDER BY t.$COL_TASK_ID DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, args)
        cursor.use {
            while (it.moveToNext()) {
                lista.add(
                    Tarea(
                        id = it.getLong(0),
                        nombre = it.getString(1),
                        categoriaId = it.getLong(2),
                        categoriaNombre = it.getString(3)
                    )
                )
            }
        }
        return lista
    }

    fun obtenerTareaPorId(id: Long): Tarea? {
        val db = readableDatabase
        val query = """
            SELECT t.$COL_TASK_ID, t.$COL_TASK_NOMBRE, t.$COL_TASK_CATEGORIA_ID, c.$COL_CAT_NOMBRE
            FROM $TABLE_TAREAS t
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COL_TASK_CATEGORIA_ID = c.$COL_CAT_ID
            WHERE t.$COL_TASK_ID = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(id.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                return Tarea(
                    id = it.getLong(0),
                    nombre = it.getString(1),
                    categoriaId = it.getLong(2),
                    categoriaNombre = it.getString(3)
                )
            }
        }
        return null
    }

    /**
     * Busca una tarea cuyo nombre coincida exactamente (ignorando mayúsculas/minúsculas y espacios extra).
     * Se utiliza en HU-04 antes de crear una tarea desde Eventos para evitar duplicados.
     */
    fun obtenerTareaPorNombreExacto(nombreInput: String): Tarea? {
        val limpio = ValidationLogic.normalizarNombre(nombreInput)
        if (limpio.isEmpty()) return null

        val db = readableDatabase
        val query = """
            SELECT t.$COL_TASK_ID, t.$COL_TASK_NOMBRE, t.$COL_TASK_CATEGORIA_ID, c.$COL_CAT_NOMBRE
            FROM $TABLE_TAREAS t
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COL_TASK_CATEGORIA_ID = c.$COL_CAT_ID
            WHERE t.$COL_TASK_NOMBRE = ? COLLATE NOCASE
            LIMIT 1
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(limpio))
        cursor.use {
            if (it.moveToFirst()) {
                return Tarea(
                    id = it.getLong(0),
                    nombre = it.getString(1),
                    categoriaId = it.getLong(2),
                    categoriaNombre = it.getString(3)
                )
            }
        }
        return null
    }

    /**
     * HU-03: Búsqueda de tareas en tiempo real desde 2 caracteres.
     * - Con menos de 2 caracteres útiles, devuelve una lista vacía sin consultar coincidencias.
     * - Desde 2 caracteres, realiza una consulta parametrizada con LIKE ? sobre SQLite,
     *   encontrando coincidencias parciales en cualquier parte del nombre sin distinguir mayúsculas/minúsculas.
     */
    fun buscarTareasPorTexto(textoBusqueda: String): List<Tarea> {
        val limpio = ValidationLogic.normalizarNombre(textoBusqueda)
        if (!ValidationLogic.esConsultaBusquedaValida(limpio)) {
            return emptyList()
        }

        val patronEscapado = limpio
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

        val lista = mutableListOf<Tarea>()
        val db = readableDatabase
        val query = """
            SELECT t.$COL_TASK_ID, t.$COL_TASK_NOMBRE, t.$COL_TASK_CATEGORIA_ID, c.$COL_CAT_NOMBRE
            FROM $TABLE_TAREAS t
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COL_TASK_CATEGORIA_ID = c.$COL_CAT_ID
            WHERE LOWER(t.$COL_TASK_NOMBRE) LIKE LOWER(?) ESCAPE '\'
            ORDER BY t.$COL_TASK_NOMBRE COLLATE NOCASE ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf("%$patronEscapado%"))
        cursor.use {
            while (it.moveToNext()) {
                lista.add(
                    Tarea(
                        id = it.getLong(0),
                        nombre = it.getString(1),
                        categoriaId = it.getLong(2),
                        categoriaNombre = it.getString(3)
                    )
                )
            }
        }
        return lista
    }

    fun crearTarea(nombreInput: String, categoriaId: Long?): ResultadoOperacion {
        val nombreLimpio = ValidationLogic.normalizarNombre(nombreInput)
        val nombresExistentes = listarTareas().map { it.nombre }

        val error = ValidationLogic.validarTarea(
            nombreRaw = nombreLimpio,
            categoriaId = categoriaId,
            nombresTareasExistentes = nombresExistentes
        )
        if (error != null) {
            return ResultadoOperacion.Error(error)
        }

        // Doble verificación en SQLite contra duplicados
        if (obtenerTareaPorNombreExacto(nombreLimpio) != null) {
            return ResultadoOperacion.Error("Ya existe una tarea con el nombre “$nombreLimpio”.")
        }

        val categoria = obtenerCategoriaPorId(categoriaId!!)
            ?: return ResultadoOperacion.Error("La categoría seleccionada no existe.")

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TASK_NOMBRE, nombreLimpio)
            put(COL_TASK_CATEGORIA_ID, categoria.id)
        }

        val nuevoId = try {
            db.insertWithOnConflict(
                TABLE_TAREAS,
                null,
                values,
                SQLiteDatabase.CONFLICT_ABORT
            )
        } catch (_: Exception) {
            -1L
        }
        return if (nuevoId != -1L) {
            ResultadoOperacion.Exito(
                id = nuevoId,
                mensaje = "Tarea “$nombreLimpio · ${categoria.nombre}” guardada con éxito."
            )
        } else {
            ResultadoOperacion.Error("Ya existe una tarea con el nombre “$nombreLimpio”.")
        }
    }

    fun actualizarTarea(id: Long, nuevoNombreInput: String, nuevaCategoriaId: Long?): ResultadoOperacion {
        val tareaActual = obtenerTareaPorId(id)
            ?: return ResultadoOperacion.Error("La tarea seleccionada no existe.")

        val nombreLimpio = ValidationLogic.normalizarNombre(nuevoNombreInput)
        val nombresExistentes = listarTareas().map { it.nombre }

        val error = ValidationLogic.validarTarea(
            nombreRaw = nombreLimpio,
            categoriaId = nuevaCategoriaId,
            nombresTareasExistentes = nombresExistentes,
            nombreActualEnEdicion = tareaActual.nombre
        )
        if (error != null) {
            return ResultadoOperacion.Error(error)
        }

        val categoria = obtenerCategoriaPorId(nuevaCategoriaId!!)
            ?: return ResultadoOperacion.Error("La categoría seleccionada no existe.")

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TASK_NOMBRE, nombreLimpio)
            put(COL_TASK_CATEGORIA_ID, categoria.id)
        }
        val filas = db.update(
            TABLE_TAREAS,
            values,
            "$COL_TASK_ID = ?",
            arrayOf(id.toString())
        )
        return if (filas > 0) {
            ResultadoOperacion.Exito(
                id = id,
                mensaje = "Tarea actualizada: “$nombreLimpio · ${categoria.nombre}”."
            )
        } else {
            ResultadoOperacion.Error("No se pudo actualizar la tarea.")
        }
    }

    fun contarEventosPorTarea(tareaId: Long): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_EVENTOS WHERE $COL_EVENT_TAREA_ID = ?",
            arrayOf(tareaId.toString())
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }
        return 0
    }

    fun eliminarTarea(id: Long): ResultadoOperacion {
        val tarea = obtenerTareaPorId(id)
            ?: return ResultadoOperacion.Error("La tarea no existe.")

        val totalEventos = contarEventosPorTarea(id)
        if (totalEventos > 0) {
            val plural = if (totalEventos == 1) "1 evento registrado" else "$totalEventos eventos registrados"
            return ResultadoOperacion.Error(
                "No se puede eliminar la tarea “${tarea.nombre}” porque tiene $plural en el historial de tiempo."
            )
        }

        val db = writableDatabase
        return try {
            val filas = db.delete(
                TABLE_TAREAS,
                "$COL_TASK_ID = ?",
                arrayOf(id.toString())
            )
            if (filas > 0) {
                ResultadoOperacion.Exito(
                    id = id,
                    mensaje = "Tarea “${tarea.nombre}” eliminada de la agenda."
                )
            } else {
                ResultadoOperacion.Error("No se pudo eliminar la tarea.")
            }
        } catch (_: Exception) {
            ResultadoOperacion.Error(
                "No se puede eliminar la tarea “${tarea.nombre}” porque tiene eventos asociados."
            )
        }
    }

    // =========================================================================
    // OPERACIONES DE EVENTOS (HU-04, HU-05, PERSISTENCIA E HISTORIAL)
    // =========================================================================

    /**
     * Consulta si existe un evento actualmente en curso (`estado = 'EN_CURSO'` o `fecha_fin IS NULL`).
     * Permite recuperar el evento activo al iniciar o reabrir la aplicación.
     */
    fun obtenerEventoActivo(): Evento? {
        val db = readableDatabase
        val query = """
            SELECT e.$COL_EVENT_ID, e.$COL_EVENT_TAREA_ID, t.$COL_TASK_NOMBRE,
                   t.$COL_TASK_CATEGORIA_ID, c.$COL_CAT_NOMBRE,
                   e.$COL_EVENT_FECHA_INICIO, e.$COL_EVENT_FECHA_FIN,
                   e.$COL_EVENT_DURACION_SEGUNDOS, e.$COL_EVENT_ESTADO
            FROM $TABLE_EVENTOS e
            INNER JOIN $TABLE_TAREAS t ON e.$COL_EVENT_TAREA_ID = t.$COL_TASK_ID
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COL_TASK_CATEGORIA_ID = c.$COL_CAT_ID
            WHERE e.$COL_EVENT_ESTADO = ? OR e.$COL_EVENT_FECHA_FIN IS NULL
            ORDER BY e.$COL_EVENT_ID DESC
            LIMIT 1
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(Evento.ESTADO_EN_CURSO))
        cursor.use {
            if (it.moveToFirst()) {
                return mapearCursorAEvento(it)
            }
        }
        return null
    }

    fun obtenerEventoPorId(eventoId: Long): Evento? {
        val db = readableDatabase
        val query = """
            SELECT e.$COL_EVENT_ID, e.$COL_EVENT_TAREA_ID, t.$COL_TASK_NOMBRE,
                   t.$COL_TASK_CATEGORIA_ID, c.$COL_CAT_NOMBRE,
                   e.$COL_EVENT_FECHA_INICIO, e.$COL_EVENT_FECHA_FIN,
                   e.$COL_EVENT_DURACION_SEGUNDOS, e.$COL_EVENT_ESTADO
            FROM $TABLE_EVENTOS e
            INNER JOIN $TABLE_TAREAS t ON e.$COL_EVENT_TAREA_ID = t.$COL_TASK_ID
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COL_TASK_CATEGORIA_ID = c.$COL_CAT_ID
            WHERE e.$COL_EVENT_ID = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(eventoId.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                return mapearCursorAEvento(it)
            }
        }
        return null
    }

    /**
     * Lista el historial de eventos registrados desde SQLite.
     * Muestra primero el evento en curso (si existe) y luego los finalizados del más reciente al más antiguo.
     */
    fun listarEventos(categoriaIdFiltro: Long? = null, limite: Int? = null): List<Evento> {
        val lista = mutableListOf<Evento>()
        val db = readableDatabase

        val whereClause = if (categoriaIdFiltro != null && categoriaIdFiltro > 0L) {
            "WHERE t.$COL_TASK_CATEGORIA_ID = ?"
        } else {
            ""
        }
        val args = if (categoriaIdFiltro != null && categoriaIdFiltro > 0L) {
            arrayOf(categoriaIdFiltro.toString())
        } else {
            emptyArray()
        }
        val limitClause = if (limite != null && limite > 0) "LIMIT $limite" else ""

        val query = """
            SELECT e.$COL_EVENT_ID, e.$COL_EVENT_TAREA_ID, t.$COL_TASK_NOMBRE,
                   t.$COL_TASK_CATEGORIA_ID, c.$COL_CAT_NOMBRE,
                   e.$COL_EVENT_FECHA_INICIO, e.$COL_EVENT_FECHA_FIN,
                   e.$COL_EVENT_DURACION_SEGUNDOS, e.$COL_EVENT_ESTADO
            FROM $TABLE_EVENTOS e
            INNER JOIN $TABLE_TAREAS t ON e.$COL_EVENT_TAREA_ID = t.$COL_TASK_ID
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COL_TASK_CATEGORIA_ID = c.$COL_CAT_ID
            $whereClause
            ORDER BY CASE WHEN e.$COL_EVENT_ESTADO = '${Evento.ESTADO_EN_CURSO}' THEN 0 ELSE 1 END ASC,
                     e.$COL_EVENT_ID DESC
            $limitClause
        """.trimIndent()

        val cursor = db.rawQuery(query, args)
        cursor.use {
            while (it.moveToNext()) {
                lista.add(mapearCursorAEvento(it))
            }
        }
        return lista
    }

    /**
     * HU-05: Inicia un evento asociado a una tarea existente.
     * - Toma la fecha y hora actuales del sistema (`LocalDateTime.now()`).
     * - Verifica que no exista ningún otro evento en curso (tanto en lógica de negocio como por índice único en SQLite).
     */
    fun iniciarEvento(
        tareaId: Long,
        fechaInicio: LocalDateTime = LocalDateTime.now()
    ): ResultadoEvento {
        val activo = obtenerEventoActivo()
        if (activo != null) {
            return ResultadoEvento.Error(
                "Ya existe un evento en curso (“${activo.tareaNombre}”). Debes finalizarlo antes de iniciar otro."
            )
        }

        val tarea = obtenerTareaPorId(tareaId)
            ?: return ResultadoEvento.Error("Debes seleccionar una tarea válida para iniciar el evento.")

        val fechaInicioIso = Evento.formatearIso(fechaInicio)
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_EVENT_TAREA_ID, tarea.id)
            put(COL_EVENT_FECHA_INICIO, fechaInicioIso)
            putNull(COL_EVENT_FECHA_FIN)
            putNull(COL_EVENT_DURACION_SEGUNDOS)
            put(COL_EVENT_ESTADO, Evento.ESTADO_EN_CURSO)
        }

        val nuevoId = try {
            db.insertWithOnConflict(
                TABLE_EVENTOS,
                null,
                values,
                SQLiteDatabase.CONFLICT_ABORT
            )
        } catch (_: Exception) {
            -1L
        }

        if (nuevoId == -1L) {
            return ResultadoEvento.Error(
                "No se pudo iniciar el evento porque ya hay una actividad en curso."
            )
        }

        val eventoCreado = obtenerEventoPorId(nuevoId)
            ?: Evento(
                id = nuevoId,
                tareaId = tarea.id,
                tareaNombre = tarea.nombre,
                categoriaId = tarea.categoriaId,
                categoriaNombre = tarea.categoriaNombre,
                fechaInicioIso = fechaInicioIso,
                fechaFinIso = null,
                duracionSegundos = null,
                estado = Evento.ESTADO_EN_CURSO
            )

        return ResultadoEvento.Exito(
            evento = eventoCreado,
            mensaje = "Evento iniciado: “${tarea.textoConCategoria}”."
        )
    }

    /**
     * HU-04: Crea una nueva tarea desde la pantalla Eventos validando nombre, categoría y ausencia de duplicados,
     * la guarda en `tareas` (para que aparezca también en Tareas del Sprint 1) e inicia de inmediato su evento.
     */
    fun crearTareaEIniciarEvento(
        nombreTareaInput: String,
        categoriaId: Long?,
        fechaInicio: LocalDateTime = LocalDateTime.now()
    ): ResultadoEvento {
        val activo = obtenerEventoActivo()
        if (activo != null) {
            return ResultadoEvento.Error(
                "Ya existe un evento en curso (“${activo.tareaNombre}”). Finalízalo antes de crear e iniciar otro."
            )
        }

        val resultadoTarea = crearTarea(nombreTareaInput, categoriaId)
        return when (resultadoTarea) {
            is ResultadoOperacion.Error -> ResultadoEvento.Error(resultadoTarea.mensaje)
            is ResultadoOperacion.Exito -> iniciarEvento(resultadoTarea.id, fechaInicio)
        }
    }

    /**
     * HU-05: Finaliza el evento actualmente en curso obteniendo la fecha y hora actuales del sistema,
     * calculando su duración con `java.time.Duration` y guardando `fecha_fin` y `duracion_segundos`.
     */
    fun finalizarEventoActivo(
        fechaFin: LocalDateTime = LocalDateTime.now()
    ): ResultadoEvento {
        val activo = obtenerEventoActivo()
            ?: return ResultadoEvento.Error("No hay ningún evento en curso para finalizar.")
        return finalizarEvento(activo.id, fechaFin)
    }

    /**
     * Finaliza un evento específico por su ID, protegiendo contra dobles pulsaciones rápidas.
     */
    fun finalizarEvento(
        eventoId: Long,
        fechaFin: LocalDateTime = LocalDateTime.now()
    ): ResultadoEvento {
        val eventoActual = obtenerEventoPorId(eventoId)
            ?: return ResultadoEvento.Error("El evento indicado no existe.")

        if (!eventoActual.enCurso) {
            return ResultadoEvento.Error("Este evento ya fue finalizado previamente.")
        }

        val inicioDateTime = eventoActual.fechaInicioLocalDateTime()
        val finAjustado = if (fechaFin.isBefore(inicioDateTime)) inicioDateTime else fechaFin
        val duracionSegundos = Evento.calcularDuracionSegundos(inicioDateTime, finAjustado)
        val fechaFinIso = Evento.formatearIso(finAjustado)

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_EVENT_FECHA_FIN, fechaFinIso)
            put(COL_EVENT_DURACION_SEGUNDOS, duracionSegundos)
            put(COL_EVENT_ESTADO, Evento.ESTADO_FINALIZADO)
        }

        val filas = db.update(
            TABLE_EVENTOS,
            values,
            "$COL_EVENT_ID = ? AND $COL_EVENT_ESTADO = ?",
            arrayOf(eventoId.toString(), Evento.ESTADO_EN_CURSO)
        )

        if (filas == 0) {
            return ResultadoEvento.Error("El evento ya fue finalizado previamente.")
        }

        val eventoFinalizado = obtenerEventoPorId(eventoId)
            ?: eventoActual.copy(
                fechaFinIso = fechaFinIso,
                duracionSegundos = duracionSegundos,
                estado = Evento.ESTADO_FINALIZADO
            )

        return ResultadoEvento.Exito(
            evento = eventoFinalizado,
            mensaje = "Evento finalizado: “${eventoFinalizado.tareaNombre}” (${eventoFinalizado.formatearDuracion(finAjustado)})."
        )
    }

    // =========================================================================
    // SPRINT 3: CONSULTA SQL OBLIGATORIA (SUM + GROUP BY) Y RESUMEN SEMANAL
    // =========================================================================

    /**
     * Sección 7: Consulta SQL obligatoria con `SUM` y `GROUP BY` para obtener los segundos
     * acumulados por categoría dentro de la semana indicada `[lunesInicio, lunesSiguienteExclusivo)`.
     *
     * Reglas cumplidas:
     * 1. Relaciona `eventos` con `tareas` (`JOIN tareas t ON t.id = e.tarea_id`).
     * 2. Relaciona `tareas` con `categorias` (`JOIN categorias c ON c.id = t.categoria_id`).
     * 3. Selecciona únicamente eventos finalizados (`e.fecha_fin IS NOT NULL AND e.estado = 'FINALIZADO'`).
     * 4. Filtra por la fecha de inicio del evento dentro del intervalo semiabierto `[?, ?)`
     *    (`e.fecha_inicio >= ? AND e.fecha_inicio < ?`), de modo que un evento iniciado el domingo
     *    y terminado el lunes siguiente pertenece íntegramente a la semana en que empezó.
     * 5. Agrupa por categoría (`GROUP BY c.id, c.nombre`) y suma las duraciones en SQLite sin
     *    recorrer los eventos en un ciclo Kotlin.
     */
    fun consultarSegundosSemanalesPorCategoriaSql(
        lunesInicio: LocalDateTime,
        lunesSiguienteExclusivo: LocalDateTime
    ): Map<Long, Long> {
        val inicioIso = Evento.formatearIso(lunesInicio)
        val finExclusivoIso = Evento.formatearIso(lunesSiguienteExclusivo)

        val db = readableDatabase
        val query = """
            SELECT c.$COL_CAT_ID,
                   c.$COL_CAT_NOMBRE,
                   COALESCE(
                       SUM(
                           COALESCE(
                               e.$COL_EVENT_DURACION_SEGUNDOS,
                               CAST(ROUND((julianday(e.$COL_EVENT_FECHA_FIN) - julianday(e.$COL_EVENT_FECHA_INICIO)) * 86400.0) AS INTEGER)
                           )
                       ),
                       0
                   ) AS total_segundos
            FROM $TABLE_EVENTOS e
            JOIN $TABLE_TAREAS t ON t.$COL_TASK_ID = e.$COL_EVENT_TAREA_ID
            JOIN $TABLE_CATEGORIAS c ON c.$COL_CAT_ID = t.$COL_TASK_CATEGORIA_ID
            WHERE e.$COL_EVENT_FECHA_FIN IS NOT NULL
              AND e.$COL_EVENT_ESTADO = '${Evento.ESTADO_FINALIZADO}'
              AND e.$COL_EVENT_FECHA_INICIO >= ?
              AND e.$COL_EVENT_FECHA_INICIO < ?
            GROUP BY c.$COL_CAT_ID, c.$COL_CAT_NOMBRE
        """.trimIndent()

        val resultado = mutableMapOf<Long, Long>()
        val cursor = db.rawQuery(query, arrayOf(inicioIso, finExclusivoIso))
        cursor.use {
            while (it.moveToNext()) {
                val catId = it.getLong(0)
                val totalSegundos = it.getLong(2).coerceAtLeast(0L)
                resultado[catId] = totalSegundos
            }
        }
        return resultado
    }

    /**
     * Obtiene el `ResumenSemanal` completo para la semana que contiene a `fechaReferencia`
     * (por defecto, la fecha actual del dispositivo `LocalDate.now()`).
     *
     * Incluye automáticamente todas las categorías existentes en SQLite (incluyendo las nuevas
     * creadas en el Sprint 1 aunque tengan 0 eventos) más la fila "Sin registrar" sobre las 168 horas.
     */
    fun obtenerResumenSemanal(fechaReferencia: LocalDate = LocalDate.now()): ResumenSemanal {
        val lunesInicio = ResumenSemanal.obtenerLunesInicioSemana(fechaReferencia)
        val lunesSiguiente = ResumenSemanal.obtenerLunesSiguienteExclusivo(fechaReferencia)
        val categorias = listarCategorias()
        val segundosPorCategoria = consultarSegundosSemanalesPorCategoriaSql(lunesInicio, lunesSiguiente)
        return ResumenSemanal.construir(
            categorias = categorias,
            segundosPorCategoriaId = segundosPorCategoria,
            fechaReferencia = fechaReferencia
        )
    }

    // =========================================================================
    // SPRINT 4: CIERRE SEMANAL AUTOMÁTICO (HU-07) Y CONSULTAS DE REPORTES (HU-08)
    // =========================================================================

    /**
     * HU-07: Cierre semanal automático, cronológico, transaccional e idempotente.
     *
     * Reglas de negocio cumplidas (Secciones 4, 6 y 7):
     * 1. Determina las semanas terminadas (cuyo domingo ya finalizó, es decir, `lunesInicio < lunesSemanaActual`)
     *    que contienen al menos un evento finalizado (`fecha_fin IS NOT NULL AND estado = 'FINALIZADO'`).
     * 2. Identifica qué semanas con eventos finalizados todavía no tienen reporte en `reportes_semanales`.
     * 3. Procesa las semanas pendientes en estricto orden cronológico (de la más antigua a la más reciente).
     * 4. No genera reportes para la semana actual (en curso), ni para semanas futuras, ni para semanas
     *    anteriores vacías sin eventos finalizados.
     * 5. Guarda cada reporte y su detalle por categoría dentro de una transacción atómica SQLite
     *    con restricciones `UNIQUE(fecha_inicio_semana, fecha_fin_semana)` y `UNIQUE(reporte_id, categoria_id)`.
     * 6. Congela instantáneas históricas (`categoria_nombre` en `detalle_reportes_semanales` y
     *    `tarea_nombre_snapshot`/`categoria_nombre_snapshot` en `eventos`) para preservar la fidelidad
     *    histórica aunque una tarea o categoría cambie de nombre después del cierre.
     * 7. Nota de diseño (Sección 7): Como la tabla `categorias` del Sprint 1 no almacena marca temporal
     *    de creación, el detalle del reporte incluye todas las categorías existentes en SQLite en el
     *    momento del cierre (mostrando 0 min y 0.0 % para las que no tuvieron eventos esa semana).
     * 8. Conserva intactos todos los eventos originales en la tabla `eventos`.
     *
     * @param fechaActual Fecha actual de referencia (por defecto `LocalDate.now()`).
     * @return Lista de nuevos reportes generados en esta ejecución (vacía si ya estaban todos cerrados).
     */
    fun ejecutarCierreSemanalAutomatico(
        fechaActual: LocalDate = LocalDate.now()
    ): List<ReporteSemanal> {
        val lunesSemanaActual = ResumenSemanal.obtenerLunesInicioSemana(fechaActual)
        val lunesSemanaActualIso = Evento.formatearIso(lunesSemanaActual)

        val db = writableDatabase

        // 1. Obtener las fechas de inicio de todos los eventos finalizados pertenecientes a semanas ya terminadas
        val lunesConEventosPendientes = sortedSetOf<LocalDate>()
        val cursorFechas = db.rawQuery(
            """
            SELECT $COL_EVENT_FECHA_INICIO
            FROM $TABLE_EVENTOS
            WHERE $COL_EVENT_FECHA_FIN IS NOT NULL
              AND $COL_EVENT_ESTADO = '${Evento.ESTADO_FINALIZADO}'
              AND $COL_EVENT_FECHA_INICIO < ?
            ORDER BY $COL_EVENT_FECHA_INICIO ASC
            """.trimIndent(),
            arrayOf(lunesSemanaActualIso)
        )
        cursorFechas.use {
            while (it.moveToNext()) {
                val fechaInicioStr = it.getString(0) ?: continue
                val fechaInicioDt = Evento.parsearIso(fechaInicioStr)
                val lunesEvento = ResumenSemanal.obtenerLunesInicioSemana(fechaInicioDt.toLocalDate()).toLocalDate()
                if (lunesEvento.isBefore(lunesSemanaActual.toLocalDate())) {
                    lunesConEventosPendientes.add(lunesEvento)
                }
            }
        }

        if (lunesConEventosPendientes.isEmpty()) {
            return emptyList()
        }

        val categoriasActuales = listarCategorias()
        val nuevosReportesGenerados = mutableListOf<ReporteSemanal>()

        // 2. Generar en orden cronológico únicamente las semanas que aún no tengan reporte guardado
        for (lunesSemana in lunesConEventosPendientes) {
            val domingoSemana = ResumenSemanal.obtenerDomingoFinSemana(lunesSemana)
            val lunesIsoFecha = ReporteSemanal.formatearFechaIso(lunesSemana)
            val domingoIsoFecha = ReporteSemanal.formatearFechaIso(domingoSemana)

            if (existeReporteParaSemana(db, lunesIsoFecha, domingoIsoFecha)) {
                continue
            }

            val lunesInicioDt = lunesSemana.atStartOfDay()
            val lunesSiguienteDt = lunesSemana.plusDays(7).atStartOfDay()
            val segundosPorCategoria = consultarSegundosSemanalesPorCategoriaSql(
                lunesInicio = lunesInicioDt,
                lunesSiguienteExclusivo = lunesSiguienteDt
            )
            val resumenSemana = ResumenSemanal.construir(
                categorias = categoriasActuales,
                segundosPorCategoriaId = segundosPorCategoria,
                fechaReferencia = lunesSemana
            )

            // Solo cerramos semanas que efectivamente tengan eventos finalizados
            val totalEventosSemana = contarEventosFinalizadosEnRango(
                db = db,
                inicioIso = Evento.formatearIso(lunesInicioDt),
                finExclusivoIso = Evento.formatearIso(lunesSiguienteDt)
            )
            if (totalEventosSemana <= 0) {
                continue
            }

            var reporteCreadoId = -1L
            db.beginTransaction()
            try {
                if (!existeReporteParaSemana(db, lunesIsoFecha, domingoIsoFecha)) {
                    // Congelar instantáneas de nombres de tarea y categoría en los eventos de la semana cerrada
                    congelarSnapshotsEventosSemana(
                        db = db,
                        inicioIso = Evento.formatearIso(lunesInicioDt),
                        finExclusivoIso = Evento.formatearIso(lunesSiguienteDt)
                    )

                    val fechaCierreIso = Evento.formatearIso(LocalDateTime.now())
                    val valoresReporte = ContentValues().apply {
                        put(COL_REP_FECHA_INICIO, lunesIsoFecha)
                        put(COL_REP_FECHA_FIN, domingoIsoFecha)
                        put(COL_REP_SEG_REGISTRADOS, resumenSemana.totalSegundosRegistrados)
                        put(COL_REP_MIN_REGISTRADOS, resumenSemana.totalMinutosRegistrados)
                        put(COL_REP_PORC_REGISTRADO, resumenSemana.porcentajeTotalRegistrado)
                        put(COL_REP_SEG_SIN_REGISTRAR, resumenSemana.filaSinRegistrar.totalSegundos)
                        put(COL_REP_MIN_SIN_REGISTRAR, resumenSemana.filaSinRegistrar.minutosAcumulados)
                        put(COL_REP_PORC_SIN_REGISTRAR, resumenSemana.filaSinRegistrar.porcentajeSemanal)
                        put(COL_REP_TOTAL_EVENTOS, totalEventosSemana)
                        put(COL_REP_FECHA_CIERRE, fechaCierreIso)
                    }

                    reporteCreadoId = db.insertWithOnConflict(
                        TABLE_REPORTES_SEMANALES,
                        null,
                        valoresReporte,
                        SQLiteDatabase.CONFLICT_ABORT
                    )

                    if (reporteCreadoId != -1L) {
                        for (filaCat in resumenSemana.filasCategorias) {
                            val valoresDetalle = ContentValues().apply {
                                put(COL_DET_REPORTE_ID, reporteCreadoId)
                                put(COL_DET_CATEGORIA_ID, filaCat.categoriaId)
                                put(COL_DET_CATEGORIA_NOMBRE, filaCat.nombreCategoria)
                                put(COL_DET_SEGUNDOS_TOTALES, filaCat.totalSegundos)
                                put(COL_DET_MINUTOS_TOTALES, filaCat.minutosAcumulados)
                                put(COL_DET_PORCENTAJE, filaCat.porcentajeSemanal)
                            }
                            db.insertWithOnConflict(
                                TABLE_DETALLE_REPORTES,
                                null,
                                valoresDetalle,
                                SQLiteDatabase.CONFLICT_ABORT
                            )
                        }
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            if (reporteCreadoId != -1L) {
                obtenerReporteSemanalPorId(reporteCreadoId)?.let { nuevosReportesGenerados.add(it) }
            }
        }

        return nuevosReportesGenerados
    }

    private fun existeReporteParaSemana(
        db: SQLiteDatabase,
        lunesIsoFecha: String,
        domingoIsoFecha: String
    ): Boolean {
        val cursor = db.rawQuery(
            """
            SELECT 1 FROM $TABLE_REPORTES_SEMANALES
            WHERE $COL_REP_FECHA_INICIO = ? AND $COL_REP_FECHA_FIN = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(lunesIsoFecha, domingoIsoFecha)
        )
        cursor.use {
            return it.moveToFirst()
        }
    }

    private fun contarEventosFinalizadosEnRango(
        db: SQLiteDatabase,
        inicioIso: String,
        finExclusivoIso: String
    ): Int {
        val cursor = db.rawQuery(
            """
            SELECT COUNT(*)
            FROM $TABLE_EVENTOS
            WHERE $COL_EVENT_FECHA_FIN IS NOT NULL
              AND $COL_EVENT_ESTADO = '${Evento.ESTADO_FINALIZADO}'
              AND $COL_EVENT_FECHA_INICIO >= ?
              AND $COL_EVENT_FECHA_INICIO < ?
            """.trimIndent(),
            arrayOf(inicioIso, finExclusivoIso)
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }
        return 0
    }

    /**
     * Guarda en `eventos` las instantáneas del nombre de la tarea y de la categoría vigentes al momento
     * del cierre semanal, asegurando que ediciones posteriores no alteren el historial del reporte cerrado.
     */
    private fun congelarSnapshotsEventosSemana(
        db: SQLiteDatabase,
        inicioIso: String,
        finExclusivoIso: String
    ) {
        db.execSQL(
            """
            UPDATE $TABLE_EVENTOS
            SET $COL_EVENT_TAREA_SNAPSHOT = COALESCE(
                    $COL_EVENT_TAREA_SNAPSHOT,
                    (SELECT t.$COL_TASK_NOMBRE FROM $TABLE_TAREAS t WHERE t.$COL_TASK_ID = $TABLE_EVENTOS.$COL_EVENT_TAREA_ID)
                ),
                $COL_EVENT_CAT_SNAPSHOT = COALESCE(
                    $COL_EVENT_CAT_SNAPSHOT,
                    (SELECT c.$COL_CAT_NOMBRE
                     FROM $TABLE_TAREAS t
                     JOIN $TABLE_CATEGORIAS c ON c.$COL_CAT_ID = t.$COL_TASK_CATEGORIA_ID
                     WHERE t.$COL_TASK_ID = $TABLE_EVENTOS.$COL_EVENT_TAREA_ID)
                )
            WHERE $COL_EVENT_FECHA_FIN IS NOT NULL
              AND $COL_EVENT_ESTADO = '${Evento.ESTADO_FINALIZADO}'
              AND $COL_EVENT_FECHA_INICIO >= ?
              AND $COL_EVENT_FECHA_INICIO < ?
            """.trimIndent(),
            arrayOf(inicioIso, finExclusivoIso)
        )
    }

    /**
     * HU-08 y Sección 8: Consulta todos los reportes semanales guardados en SQLite,
     * ordenados desde la semana más reciente hasta la más antigua (`ORDER BY fecha_inicio_semana DESC`).
     */
    fun listarReportesSemanales(): List<ReporteSemanal> {
        val db = readableDatabase
        val reportesBase = mutableListOf<ReporteSemanal>()

        val cursor = db.rawQuery(
            """
            SELECT $COL_REP_ID,
                   $COL_REP_FECHA_INICIO,
                   $COL_REP_FECHA_FIN,
                   $COL_REP_SEG_REGISTRADOS,
                   $COL_REP_MIN_REGISTRADOS,
                   $COL_REP_PORC_REGISTRADO,
                   $COL_REP_SEG_SIN_REGISTRAR,
                   $COL_REP_MIN_SIN_REGISTRAR,
                   $COL_REP_PORC_SIN_REGISTRAR,
                   $COL_REP_TOTAL_EVENTOS,
                   $COL_REP_FECHA_CIERRE
            FROM $TABLE_REPORTES_SEMANALES
            ORDER BY $COL_REP_FECHA_INICIO DESC, $COL_REP_ID DESC
            """.trimIndent(),
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                reportesBase.add(mapearCursorAReporteBase(it))
            }
        }

        return reportesBase.map { rep ->
            rep.copy(detallesCategorias = listarDetallesDeReporte(rep.id))
        }
    }

    /**
     * Obtiene un reporte semanal específico junto con el detalle histórico de sus categorías.
     */
    fun obtenerReporteSemanalPorId(reporteId: Long): ReporteSemanal? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT $COL_REP_ID,
                   $COL_REP_FECHA_INICIO,
                   $COL_REP_FECHA_FIN,
                   $COL_REP_SEG_REGISTRADOS,
                   $COL_REP_MIN_REGISTRADOS,
                   $COL_REP_PORC_REGISTRADO,
                   $COL_REP_SEG_SIN_REGISTRAR,
                   $COL_REP_MIN_SIN_REGISTRAR,
                   $COL_REP_PORC_SIN_REGISTRAR,
                   $COL_REP_TOTAL_EVENTOS,
                   $COL_REP_FECHA_CIERRE
            FROM $TABLE_REPORTES_SEMANALES
            WHERE $COL_REP_ID = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(reporteId.toString())
        )

        val reporteBase = cursor.use {
            if (it.moveToFirst()) mapearCursorAReporteBase(it) else null
        } ?: return null

        return reporteBase.copy(detallesCategorias = listarDetallesDeReporte(reporteBase.id))
    }

    /**
     * Consulta los detalles por categoría almacenados en `detalle_reportes_semanales` para un reporte.
     */
    fun listarDetallesDeReporte(reporteId: Long): List<DetalleReporteCategoria> {
        val db = readableDatabase
        val lista = mutableListOf<DetalleReporteCategoria>()
        val cursor = db.rawQuery(
            """
            SELECT $COL_DET_ID,
                   $COL_DET_REPORTE_ID,
                   $COL_DET_CATEGORIA_ID,
                   $COL_DET_CATEGORIA_NOMBRE,
                   $COL_DET_SEGUNDOS_TOTALES,
                   $COL_DET_MINUTOS_TOTALES,
                   $COL_DET_PORCENTAJE
            FROM $TABLE_DETALLE_REPORTES
            WHERE $COL_DET_REPORTE_ID = ?
            ORDER BY $COL_DET_CATEGORIA_ID ASC, $COL_DET_ID ASC
            """.trimIndent(),
            arrayOf(reporteId.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                lista.add(
                    DetalleReporteCategoria(
                        id = it.getLong(0),
                        reporteId = it.getLong(1),
                        categoriaId = it.getLong(2),
                        categoriaNombre = it.getString(3),
                        segundosTotales = it.getLong(4),
                        minutosTotales = it.getDouble(5),
                        porcentaje = it.getDouble(6)
                    )
                )
            }
        }
        return lista
    }

    /**
     * Sección 9: Lista los eventos finalizados pertenecientes a la semana seleccionada
     * (`fecha_inicio >= lunesInicio` y `fecha_inicio < lunesSiguienteExclusivo`), incluyendo
     * aquellos que comenzaron el domingo y finalizaron el lunes siguiente.
     *
     * Utiliza las instantáneas históricas (`tarea_nombre_snapshot` y `categoria_nombre_snapshot`)
     * cuando estén disponibles para mantener la coherencia histórica si una tarea o categoría
     * cambió de nombre después del cierre semanal.
     */
    fun listarEventosFinalizadosPorSemana(
        lunesInicio: LocalDateTime,
        lunesSiguienteExclusivo: LocalDateTime
    ): List<Evento> {
        val inicioIso = Evento.formatearIso(lunesInicio)
        val finExclusivoIso = Evento.formatearIso(lunesSiguienteExclusivo)

        val lista = mutableListOf<Evento>()
        val db = readableDatabase
        val query = """
            SELECT e.$COL_EVENT_ID,
                   e.$COL_EVENT_TAREA_ID,
                   COALESCE(e.$COL_EVENT_TAREA_SNAPSHOT, t.$COL_TASK_NOMBRE) AS tarea_nombre_hist,
                   t.$COL_TASK_CATEGORIA_ID,
                   COALESCE(e.$COL_EVENT_CAT_SNAPSHOT, c.$COL_CAT_NOMBRE) AS categoria_nombre_hist,
                   e.$COL_EVENT_FECHA_INICIO,
                   e.$COL_EVENT_FECHA_FIN,
                   e.$COL_EVENT_DURACION_SEGUNDOS,
                   e.$COL_EVENT_ESTADO
            FROM $TABLE_EVENTOS e
            INNER JOIN $TABLE_TAREAS t ON e.$COL_EVENT_TAREA_ID = t.$COL_TASK_ID
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COL_TASK_CATEGORIA_ID = c.$COL_CAT_ID
            WHERE e.$COL_EVENT_FECHA_FIN IS NOT NULL
              AND e.$COL_EVENT_ESTADO = '${Evento.ESTADO_FINALIZADO}'
              AND e.$COL_EVENT_FECHA_INICIO >= ?
              AND e.$COL_EVENT_FECHA_INICIO < ?
            ORDER BY e.$COL_EVENT_FECHA_INICIO DESC, e.$COL_EVENT_ID DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(inicioIso, finExclusivoIso))
        cursor.use {
            while (it.moveToNext()) {
                lista.add(mapearCursorAEvento(it))
            }
        }
        return lista
    }

    fun listarEventosFinalizadosPorSemana(reporte: ReporteSemanal): List<Evento> {
        return listarEventosFinalizadosPorSemana(
            lunesInicio = reporte.lunesInicioLocalDateTime(),
            lunesSiguienteExclusivo = reporte.lunesSiguienteExclusivoLocalDateTime()
        )
    }

    private fun mapearCursorAReporteBase(cursor: android.database.Cursor): ReporteSemanal {
        return ReporteSemanal(
            id = cursor.getLong(0),
            fechaInicioSemanaIso = cursor.getString(1),
            fechaFinSemanaIso = cursor.getString(2),
            segundosRegistrados = cursor.getLong(3),
            minutosRegistrados = cursor.getDouble(4),
            porcentajeRegistrado = cursor.getDouble(5),
            segundosSinRegistrar = cursor.getLong(6),
            minutosSinRegistrar = cursor.getDouble(7),
            porcentajeSinRegistrar = cursor.getDouble(8),
            totalEventos = cursor.getInt(9),
            fechaCierreIso = cursor.getString(10)
        )
    }

    private fun mapearCursorAEvento(cursor: android.database.Cursor): Evento {
        val fechaFin = if (cursor.isNull(6)) null else cursor.getString(6)
        val duracion = if (cursor.isNull(7)) null else cursor.getLong(7)
        return Evento(
            id = cursor.getLong(0),
            tareaId = cursor.getLong(1),
            tareaNombre = cursor.getString(2),
            categoriaId = cursor.getLong(3),
            categoriaNombre = cursor.getString(4),
            fechaInicioIso = cursor.getString(5),
            fechaFinIso = fechaFin,
            duracionSegundos = duracion,
            estado = cursor.getString(8) ?: if (fechaFin == null) Evento.ESTADO_EN_CURSO else Evento.ESTADO_FINALIZADO
        )
    }
}
