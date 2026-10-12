package com.senati.mitiemposemanal.ayovera.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.senati.mitiemposemanal.ayovera.model.Categoria
import com.senati.mitiemposemanal.ayovera.model.Tarea

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

/**
 * Gestor de base de datos SQLite mediante SQLiteOpenHelper para MiTiempoSemanal (Sprint 1).
 *
 * Tablas:
 * - `categorias` (id PK, nombre NOT NULL UNIQUE COLLATE NOCASE)
 * - `tareas` (id PK, nombre NOT NULL UNIQUE COLLATE NOCASE, categoria_id FK -> categorias.id ON DELETE RESTRICT)
 */
class AdminSQLiteOpenHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "mi_tiempo_semanal.db"
        const val DATABASE_VERSION = 1

        const val TABLE_CATEGORIAS = "categorias"
        const val COL_CAT_ID = "id"
        const val COL_CAT_NOMBRE = "nombre"

        const val TABLE_TAREAS = "tareas"
        const val COL_TASK_ID = "id"
        const val COL_TASK_NOMBRE = "nombre"
        const val COL_TASK_CATEGORIA_ID = "categoria_id"
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

        insertarCategoriasIniciales(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Conserva datos existentes; solo aplica migraciones incrementales cuando suba DATABASE_VERSION
    }

    /**
     * Inserta las 4 categorías iniciales (Estudio, Trabajo, Descanso, Ocio) usando INSERT OR IGNORE
     * para evitar duplicados al reabrir la aplicación.
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
    // OPERACIONES DE TAREAS (HU-02)
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

        val categoria = obtenerCategoriaPorId(categoriaId!!)
            ?: return ResultadoOperacion.Error("La categoría seleccionada no existe.")

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TASK_NOMBRE, nombreLimpio)
            put(COL_TASK_CATEGORIA_ID, categoria.id)
        }

        val nuevoId = db.insertWithOnConflict(
            TABLE_TAREAS,
            null,
            values,
            SQLiteDatabase.CONFLICT_ABORT
        )
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

    fun eliminarTarea(id: Long): ResultadoOperacion {
        val tarea = obtenerTareaPorId(id)
            ?: return ResultadoOperacion.Error("La tarea no existe.")

        val db = writableDatabase
        val filas = db.delete(
            TABLE_TAREAS,
            "$COL_TASK_ID = ?",
            arrayOf(id.toString())
        )
        return if (filas > 0) {
            ResultadoOperacion.Exito(
                id = id,
                mensaje = "Tarea “${tarea.nombre}” eliminada de la agenda."
            )
        } else {
            ResultadoOperacion.Error("No se pudo eliminar la tarea.")
        }
    }
}

