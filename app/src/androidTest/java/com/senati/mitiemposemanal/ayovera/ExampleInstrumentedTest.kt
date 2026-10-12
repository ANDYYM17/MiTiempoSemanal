package com.senati.mitiemposemanal.ayovera

import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.senati.mitiemposemanal.ayovera.data.AdminSQLiteOpenHelper
import com.senati.mitiemposemanal.ayovera.data.ResultadoEliminacionCategoria
import com.senati.mitiemposemanal.ayovera.data.ResultadoEvento
import com.senati.mitiemposemanal.ayovera.data.ResultadoOperacion
import com.senati.mitiemposemanal.ayovera.model.Evento
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * Pruebas instrumentadas en dispositivo/emulador Android para Sprints 1 y 2 de MiTiempoSemanal.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.senati.mitiemposemanal.ayovera", appContext.packageName)
    }

    @Test
    fun sqlite_flujoCompletoCategoriasYTareas_Sprint1() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(AdminSQLiteOpenHelper.DATABASE_NAME)

        val dbHelper = AdminSQLiteOpenHelper(context)
        dbHelper.asegurarCategoriasInicialesSiVacio()
        dbHelper.asegurarCategoriasInicialesSiVacio()

        val categoriasIniciales = dbHelper.listarCategorias()
        assertEquals(4, categoriasIniciales.size)
        assertEquals(
            listOf("Estudio", "Trabajo", "Descanso", "Ocio"),
            categoriasIniciales.map { it.nombre }
        )

        assertTrue(dbHelper.crearCategoria("   ") is ResultadoOperacion.Error)

        val creacionCat = dbHelper.crearCategoria("  Laboratorio   ")
        assertTrue(creacionCat is ResultadoOperacion.Exito)
        val idLaboratorio = (creacionCat as ResultadoOperacion.Exito).id
        assertEquals("Laboratorio", dbHelper.obtenerCategoriaPorId(idLaboratorio)?.nombre)

        val duplicadoCat = dbHelper.crearCategoria("laboratorio")
        assertTrue(duplicadoCat is ResultadoOperacion.Error)

        val edicionCat = dbHelper.actualizarCategoria(idLaboratorio, "  Laboratorio SENATI ")
        assertTrue(edicionCat is ResultadoOperacion.Exito)
        assertEquals("Laboratorio SENATI", dbHelper.obtenerCategoriaPorId(idLaboratorio)?.nombre)

        val edicionCatDuplicada = dbHelper.actualizarCategoria(idLaboratorio, "Estudio")
        assertTrue(edicionCatDuplicada is ResultadoOperacion.Error)

        val idEstudio = categoriasIniciales.first { it.nombre == "Estudio" }.id
        val idTrabajo = categoriasIniciales.first { it.nombre == "Trabajo" }.id

        assertTrue(dbHelper.crearTarea("   ", idEstudio) is ResultadoOperacion.Error)
        assertTrue(dbHelper.crearTarea("Tarea sin categoría", null) is ResultadoOperacion.Error)

        val creacionTarea = dbHelper.crearTarea("  Leer Kotlin  ", idEstudio)
        assertTrue(creacionTarea is ResultadoOperacion.Exito)
        val idTarea = (creacionTarea as ResultadoOperacion.Exito).id

        assertTrue(dbHelper.crearTarea("leer kotlin", idTrabajo) is ResultadoOperacion.Error)

        val tareas = dbHelper.listarTareas()
        assertEquals(1, tareas.size)
        assertEquals("Leer Kotlin · Estudio", tareas.first().textoConCategoria)

        val intentoEliminarConTareas = dbHelper.eliminarCategoria(idEstudio)
        assertTrue(intentoEliminarConTareas is ResultadoEliminacionCategoria.BloqueadaConTareas)

        val edicionTarea = dbHelper.actualizarTarea(idTarea, "Leer Kotlin Avanzado", idTrabajo)
        assertTrue(edicionTarea is ResultadoOperacion.Exito)
        assertEquals(
            "Leer Kotlin Avanzado · Trabajo",
            dbHelper.obtenerTareaPorId(idTarea)?.textoConCategoria
        )

        dbHelper.close()

        val dbHelperReabierto = AdminSQLiteOpenHelper(context)
        dbHelperReabierto.asegurarCategoriasInicialesSiVacio()

        val categoriasReabiertas = dbHelperReabierto.listarCategorias()
        assertEquals(5, categoriasReabiertas.size)

        val tareasReabiertas = dbHelperReabierto.listarTareas()
        assertEquals(1, tareasReabiertas.size)
        assertEquals("Leer Kotlin Avanzado · Trabajo", tareasReabiertas.first().textoConCategoria)

        assertTrue(dbHelperReabierto.eliminarTarea(idTarea) is ResultadoOperacion.Exito)
        assertTrue(
            dbHelperReabierto.eliminarCategoria(idLaboratorio) is ResultadoEliminacionCategoria.Eliminada
        )

        dbHelperReabierto.close()
    }

    /**
     * Sección 8 y Sección 11 (Migración):
     * Construye una base de datos real en versión 1 (Sprint 1) con categorías y tareas,
     * la cierra y abre `AdminSQLiteOpenHelper` (versión 2 del Sprint 2) encima de ella,
     * verificando que `onUpgrade(db, 1, 2)` conserve todas las categorías y tareas originales
     * y cree la tabla `eventos`.
     */
    @Test
    fun sqlite_migracionRealSprint1ASprint2_conservaCategoriasYTareas() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(AdminSQLiteOpenHelper.DATABASE_NAME)

        val dbFile = context.getDatabasePath(AdminSQLiteOpenHelper.DATABASE_NAME)
        dbFile.parentFile?.mkdirs()

        // 1. Simular instalación del Sprint 1 (DATABASE_VERSION = 1)
        val dbV1 = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        dbV1.execSQL(
            """
            CREATE TABLE categorias (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL UNIQUE COLLATE NOCASE
            )
            """.trimIndent()
        )
        dbV1.execSQL(
            """
            CREATE TABLE tareas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL UNIQUE COLLATE NOCASE,
                categoria_id INTEGER NOT NULL,
                FOREIGN KEY (categoria_id) REFERENCES categorias(id)
                    ON DELETE RESTRICT ON UPDATE CASCADE
            )
            """.trimIndent()
        )
        listOf("Estudio", "Trabajo", "Descanso", "Ocio", "Taller Mecatrónica").forEach { catName ->
            val cv = ContentValues().apply { put("nombre", catName) }
            dbV1.insert("categorias", null, cv)
        }
        val cvTarea1 = ContentValues().apply {
            put("nombre", "Leer Kotlin")
            put("categoria_id", 1L) // Estudio
        }
        val cvTarea2 = ContentValues().apply {
            put("nombre", "Mantenimiento CNC")
            put("categoria_id", 2L) // Trabajo
        }
        dbV1.insert("tareas", null, cvTarea1)
        dbV1.insert("tareas", null, cvTarea2)
        dbV1.version = 1
        dbV1.close()

        // 2. Abrir AdminSQLiteOpenHelper del Sprint 2 (DATABASE_VERSION = 2) encima de la BD v1
        val dbHelperV2 = AdminSQLiteOpenHelper(context)
        dbHelperV2.asegurarCategoriasInicialesSiVacio()

        val categoriasTrasUpgrade = dbHelperV2.listarCategorias()
        assertEquals(5, categoriasTrasUpgrade.size)
        assertTrue(categoriasTrasUpgrade.any { it.nombre == "Taller Mecatrónica" })

        val tareasTrasUpgrade = dbHelperV2.listarTareas()
        assertEquals(2, tareasTrasUpgrade.size)
        assertTrue(tareasTrasUpgrade.any { it.textoConCategoria == "Leer Kotlin · Estudio" })
        assertTrue(tareasTrasUpgrade.any { it.textoConCategoria == "Mantenimiento CNC · Trabajo" })

        // 3. Confirmar que la nueva tabla `eventos` funciona con las tareas heredadas del Sprint 1
        val tareaKotlin = tareasTrasUpgrade.first { it.nombre == "Leer Kotlin" }
        val inicio = LocalDateTime.of(2026, 10, 12, 16, 0, 0)
        val fin = LocalDateTime.of(2026, 10, 12, 17, 15, 0)

        val resInicio = dbHelperV2.iniciarEvento(tareaKotlin.id, inicio)
        assertTrue(resInicio is ResultadoEvento.Exito)
        assertNotNull(dbHelperV2.obtenerEventoActivo())

        val resFin = dbHelperV2.finalizarEventoActivo(fin)
        assertTrue(resFin is ResultadoEvento.Exito)
        assertNull(dbHelperV2.obtenerEventoActivo())

        val historial = dbHelperV2.listarEventos()
        assertEquals(1, historial.size)
        assertEquals(75L * 60L, historial.first().duracionSegundos)

        dbHelperV2.close()
    }

    /**
     * Sección 11: Prueba integral de HU-03 (Búsqueda), HU-04 (Creación desde Eventos),
     * HU-05 (Iniciar y finalizar eventos, regla de evento único) y Sección 7 (Persistencia tras cerrar y reabrir).
     */
    @Test
    fun sqlite_busquedaCreacionYEventosPersistentes_Sprint2() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(AdminSQLiteOpenHelper.DATABASE_NAME)

        val dbHelper = AdminSQLiteOpenHelper(context)
        dbHelper.asegurarCategoriasInicialesSiVacio()

        val categorias = dbHelper.listarCategorias()
        val idEstudio = categorias.first { it.nombre == "Estudio" }.id
        val idTrabajo = categorias.first { it.nombre == "Trabajo" }.id

        dbHelper.crearTarea("Leer Kotlin", idEstudio)
        dbHelper.crearTarea("Practicar Kotlin", idEstudio)
        dbHelper.crearTarea("Taller de Torno CNC", idTrabajo)

        // --- HU-03: Búsqueda en tiempo real ---
        // Menos de 2 caracteres -> sin coincidencias
        assertTrue(dbHelper.buscarTareasPorTexto("").isEmpty())
        assertTrue(dbHelper.buscarTareasPorTexto("k").isEmpty())
        assertTrue(dbHelper.buscarTareasPorTexto("  k  ").isEmpty())

        // Desde 2 caracteres y coincidencias parciales insensibles a mayúsculas/minúsculas
        val resKo = dbHelper.buscarTareasPorTexto("ko")
        assertEquals(2, resKo.size)
        assertTrue(resKo.any { it.textoConCategoria == "Leer Kotlin · Estudio" })
        assertTrue(resKo.any { it.textoConCategoria == "Practicar Kotlin · Estudio" })

        // Coincidencia parcial en medio o final del texto ("tli", "torno")
        val resParcial = dbHelper.buscarTareasPorTexto("TLI")
        assertEquals(2, resParcial.size)

        val resTorno = dbHelper.buscarTareasPorTexto("torno")
        assertEquals(1, resTorno.size)
        assertEquals("Taller de Torno CNC · Trabajo", resTorno.first().textoConCategoria)

        // Búsqueda sin resultados
        assertTrue(dbHelper.buscarTareasPorTexto("xyz").isEmpty())

        // --- HU-04: Crear una tarea desde Eventos e iniciar inmediatamente su evento ---
        assertTrue(dbHelper.crearTareaEIniciarEvento("   ", idEstudio) is ResultadoEvento.Error)
        assertTrue(dbHelper.crearTareaEIniciarEvento("Tarea Nueva", null) is ResultadoEvento.Error)
        assertTrue(dbHelper.crearTareaEIniciarEvento("leer kotlin", idEstudio) is ResultadoEvento.Error)

        val inicioEvt1 = LocalDateTime.of(2026, 10, 12, 18, 30, 15)
        val creacionDesdeEventos = dbHelper.crearTareaEIniciarEvento(
            nombreTareaInput = "  Informe de Laboratorio  ",
            categoriaId = idEstudio,
            fechaInicio = inicioEvt1
        )
        assertTrue(creacionDesdeEventos is ResultadoEvento.Exito)
        val eventoActivo = (creacionDesdeEventos as ResultadoEvento.Exito).evento
        assertEquals("Informe de Laboratorio", eventoActivo.tareaNombre)
        assertEquals("Estudio", eventoActivo.categoriaNombre)
        assertTrue(eventoActivo.enCurso)

        // Verificar que la tarea creada desde Eventos también aparece en la lista de Tareas del Sprint 1
        val todasTareas = dbHelper.listarTareas()
        assertTrue(todasTareas.any { it.textoConCategoria == "Informe de Laboratorio · Estudio" })

        // --- HU-05: Impedir iniciar un segundo evento mientras exista uno activo ---
        val tareaExistenteId = todasTareas.first { it.nombre == "Leer Kotlin" }.id
        val segundoIntento = dbHelper.iniciarEvento(tareaExistenteId)
        assertTrue(segundoIntento is ResultadoEvento.Error)

        // --- Sección 7: Persistencia y recuperación tras cerrar y volver a abrir ---
        dbHelper.close()

        val dbHelperReabierto = AdminSQLiteOpenHelper(context)
        val recuperado = dbHelperReabierto.obtenerEventoActivo()
        assertNotNull(recuperado)
        assertEquals(eventoActivo.id, recuperado?.id)
        assertEquals("Informe de Laboratorio · Estudio", recuperado?.textoTareaConCategoria)
        assertEquals(Evento.formatearIso(inicioEvt1), recuperado?.fechaInicioIso)
        assertTrue(recuperado!!.enCurso)

        // --- HU-05: Finalizar el evento y verificar fecha/hora de fin y duración ---
        val finEvt1 = LocalDateTime.of(2026, 10, 12, 19, 55, 15)
        val finalizacion = dbHelperReabierto.finalizarEventoActivo(finEvt1)
        assertTrue(finalizacion is ResultadoEvento.Exito)
        val eventoFinalizado = (finalizacion as ResultadoEvento.Exito).evento
        assertFalse(eventoFinalizado.enCurso)
        assertEquals(Evento.formatearIso(finEvt1), eventoFinalizado.fechaFinIso)
        assertEquals(85L * 60L, eventoFinalizado.duracionSegundos)
        assertEquals("1 h 25 min", eventoFinalizado.formatearDuracion(finEvt1))

        // Impedir doble finalización
        assertTrue(dbHelperReabierto.finalizarEvento(eventoFinalizado.id, finEvt1) is ResultadoEvento.Error)
        assertNull(dbHelperReabierto.obtenerEventoActivo())

        // Verificar Historial de Eventos
        val historial = dbHelperReabierto.listarEventos()
        assertEquals(1, historial.size)
        assertFalse(historial.first().enCurso)
        assertEquals("Informe de Laboratorio · Estudio", historial.first().textoTareaConCategoria)

        dbHelperReabierto.close()
    }

    @Test
    fun activities_navegacionVersionYRecuperacionEventoActivo_Sprint2() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(AdminSQLiteOpenHelper.DATABASE_NAME)

        val dbHelper = AdminSQLiteOpenHelper(context)
        dbHelper.asegurarCategoriasInicialesSiVacio()
        val idEstudio = dbHelper.listarCategorias().first { it.nombre == "Estudio" }.id
        val resTarea = dbHelper.crearTarea("Leer Kotlin", idEstudio) as ResultadoOperacion.Exito
        dbHelper.iniciarEvento(resTarea.id, LocalDateTime.now().minusMinutes(5))
        dbHelper.close()

        // 1. Abrir MainActivity con un evento activo en SQLite y verificar versión v0.2.0 (2)
        //    y recuperación del estado activo (Iniciar deshabilitado, Finalizar habilitado)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val tvStudentVersion = activity.findViewById<TextView>(R.id.tvStudentVersion)
                assertNotNull(tvStudentVersion)
                assertTrue(tvStudentVersion.text.toString().endsWith("· v0.2.0 (2)"))

                val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNav)
                bottomNav.selectedItemId = R.id.nav_events

                val btnStart = activity.findViewById<Button>(R.id.btnStartEvent)
                val btnStop = activity.findViewById<Button>(R.id.btnStopEvent)
                val cardActive = activity.findViewById<View>(R.id.cardEventActiveState)
                val tvActiveTitle = activity.findViewById<TextView>(R.id.tvActiveEventTaskTitle)

                assertFalse(btnStart.isEnabled)
                assertTrue(btnStop.isEnabled)
                assertEquals(View.VISIBLE, cardActive.visibility)
                assertEquals("Leer Kotlin", tvActiveTitle.text.toString())
            }
        }

        // 2. Pantalla 15: Historial de Eventos
        val intentHistory = Intent(context, EventHistoryActivity::class.java)
        ActivityScenario.launch<EventHistoryActivity>(intentHistory).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }

        // Limpiar el evento de prueba y dejar las 4 categorías iniciales listas para uso normal
        val dbClean = AdminSQLiteOpenHelper(context)
        dbClean.finalizarEventoActivo()
        dbClean.close()
    }
}