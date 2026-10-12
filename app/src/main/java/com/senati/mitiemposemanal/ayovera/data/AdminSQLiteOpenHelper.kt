package com.senati.mitiemposemanal.ayovera.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.senati.mitiemposemanal.ayovera.model.Categoria
import com.senati.mitiemposemanal.ayovera.model.Evento
import com.senati.mitiemposemanal.ayovera.model.Tarea
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
 * Gestor de base de datos SQLite mediante SQLiteOpenHelper para MiTiempoSemanal (Sprints 1 y 2).
 *
 * Tablas:
 * - `categorias` (id PK, nombre NOT NULL UNIQUE COLLATE NOCASE)
 * - `tareas` (id PK, nombre NOT NULL UNIQUE COLLATE NOCASE, categoria_id FK -> categorias.id ON DELETE RESTRICT)
 * - `eventos` (id PK, tarea_id FK -> tareas.id ON DELETE RESTRICT, fecha_inicio NOT NULL, fecha_fin NULL,
 *              duracion_segundos NULL, estado NOT NULL) + índice único parcial para garantizar un único evento activo.
 */
class AdminSQLiteOpenHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "mi_tiempo_semanal.db"
        const val DATABASE_VERSION = 2

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
        const val INDEX_UNICO_EVENTO_ACTIVO = "idx_unico_evento_activo"
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
        insertarCategoriasIniciales(db)
    }

    /**
     * Migración incremental del Sprint 1 (v1) al Sprint 2 (v2):
     * Crea la tabla `eventos` y su índice único de evento activo sin eliminar `categorias` ni `tareas`.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            crearTablaEventosEIndices(db)
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
