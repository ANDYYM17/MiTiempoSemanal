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

        // 1. Abrir MainActivity con un evento activo en SQLite y verificar versión v0.3.0 (3)
        //    y recuperación del estado activo (Iniciar deshabilitado, Finalizar habilitado)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val tvStudentVersion = activity.findViewById<TextView>(R.id.tvStudentVersion)
                assertNotNull(tvStudentVersion)
                assertTrue(tvStudentVersion.text.toString().endsWith("· v0.3.0 (3)"))

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

    /**
     * Sección 13 (Escenarios A, B, C, D, E, F, G, H e I):
     * Prueba integral del Sprint 3 en SQLite y en el panel dinámico de MainActivity.
     */
    @Test
    fun sprint3_escenariosObligatoriosPanelSemanalYConservacionDatos() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(AdminSQLiteOpenHelper.DATABASE_NAME)

        // =====================================================================
        // ESCENARIO I: Actualización de BD v2 (Sprint 2) a v3 (Sprint 3)
        // conservando categorías, tareas, eventos finalizados y evento en curso
        // =====================================================================
        val dbFile = context.getDatabasePath(AdminSQLiteOpenHelper.DATABASE_NAME)
        dbFile.parentFile?.mkdirs()
        val dbV2 = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        dbV2.execSQL(
            """
            CREATE TABLE categorias (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL UNIQUE COLLATE NOCASE
            )
            """.trimIndent()
        )
        dbV2.execSQL(
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
        dbV2.execSQL(
            """
            CREATE TABLE eventos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tarea_id INTEGER NOT NULL,
                fecha_inicio TEXT NOT NULL,
                fecha_fin TEXT,
                duracion_segundos INTEGER,
                estado TEXT NOT NULL DEFAULT 'EN_CURSO',
                FOREIGN KEY (tarea_id) REFERENCES tareas(id)
                    ON DELETE RESTRICT ON UPDATE CASCADE
            )
            """.trimIndent()
        )
        listOf("Estudio", "Trabajo", "Descanso", "Ocio").forEach { catName ->
            val cv = ContentValues().apply { put("nombre", catName) }
            dbV2.insert("categorias", null, cv)
        }
        val cvTareaHeredada = ContentValues().apply {
            put("nombre", "Taller Heredado Sprint 2")
            put("categoria_id", 1L)
        }
        val tareaHeredadaId = dbV2.insert("tareas", null, cvTareaHeredada)
        val cvEventoFinalizadoV2 = ContentValues().apply {
            put("tarea_id", tareaHeredadaId)
            put("fecha_inicio", "2026-10-05T08:00:00")
            put("fecha_fin", "2026-10-05T10:00:00")
            put("duracion_segundos", 7200L)
            put("estado", "FINALIZADO")
        }
        dbV2.insert("eventos", null, cvEventoFinalizadoV2)
        val cvEventoEnCursoV2 = ContentValues().apply {
            put("tarea_id", tareaHeredadaId)
            put("fecha_inicio", "2026-10-06T09:00:00")
            putNull("fecha_fin")
            putNull("duracion_segundos")
            put("estado", "EN_CURSO")
        }
        dbV2.insert("eventos", null, cvEventoEnCursoV2)
        dbV2.version = 2
        dbV2.close()

        // Abrir con AdminSQLiteOpenHelper v3 y verificar conservación total
        val dbHelperV3 = AdminSQLiteOpenHelper(context)
        assertEquals(4, dbHelperV3.listarCategorias().size)
        assertEquals(1, dbHelperV3.listarTareas().size)
        assertEquals(2, dbHelperV3.listarEventos().size)
        assertNotNull(dbHelperV3.obtenerEventoActivo())
        dbHelperV3.close()

        // Reiniciar BD limpia para probar con exactitud los Escenarios A, B, C, D, E, F, G y H
        context.deleteDatabase(AdminSQLiteOpenHelper.DATABASE_NAME)
        val dbHelper = AdminSQLiteOpenHelper(context)
        dbHelper.asegurarCategoriasInicialesSiVacio()

        val categoriasBase = dbHelper.listarCategorias()
        val idEstudio = categoriasBase.first { it.nombre == "Estudio" }.id
        val idTrabajo = categoriasBase.first { it.nombre == "Trabajo" }.id
        val idDescanso = categoriasBase.first { it.nombre == "Descanso" }.id
        val idOcio = categoriasBase.first { it.nombre == "Ocio" }.id

        val idTareaEstudio = (dbHelper.crearTarea("Leer Kotlin", idEstudio) as ResultadoOperacion.Exito).id
        val idTareaTrabajo = (dbHelper.crearTarea("Prácticas CNC", idTrabajo) as ResultadoOperacion.Exito).id
        val idTareaDescanso = (dbHelper.crearTarea("Descanso Nocturno", idDescanso) as ResultadoOperacion.Exito).id
        val idTareaOcio = (dbHelper.crearTarea("Partido de Fútbol", idOcio) as ResultadoOperacion.Exito).id

        val fechaSemana = java.time.LocalDate.of(2026, 10, 14) // Miércoles de la semana 12/10/2026 – 18/10/2026

        // =====================================================================
        // ESCENARIO A: Semana vacía
        // =====================================================================
        val resumenA = dbHelper.obtenerResumenSemanal(fechaSemana)
        assertEquals("Semana 12/10/2026 – 18/10/2026", resumenA.formatearRangoSemana())
        assertEquals(4, resumenA.filasCategorias.size)
        resumenA.filasCategorias.forEach { fila ->
            assertEquals(0L, fila.totalSegundos)
            assertEquals("0 h 00 min", fila.formatearHorasYMinutos())
            assertEquals("0.0 %", fila.formatearPorcentaje())
        }
        assertEquals("168 h 00 min", resumenA.filaSinRegistrar.formatearHorasYMinutos())
        assertEquals("100.0 %", resumenA.filaSinRegistrar.formatearPorcentaje())

        // =====================================================================
        // ESCENARIO B: Evento de 21 horas en Estudio (12.5 %)
        // =====================================================================
        val iniEstudio = LocalDateTime.of(2026, 10, 12, 8, 0, 0)
        val finEstudio = iniEstudio.plusHours(21) // 21 h = 1260 min = 12.5 % de 168 h
        dbHelper.iniciarEvento(idTareaEstudio, iniEstudio)
        dbHelper.finalizarEventoActivo(finEstudio)

        val resumenB = dbHelper.obtenerResumenSemanal(fechaSemana)
        val filaEstudioB = resumenB.filasCategorias.first { it.nombreCategoria == "Estudio" }
        assertEquals("21 h 00 min", filaEstudioB.formatearHorasYMinutos())
        assertEquals("12.5 %", filaEstudioB.formatearPorcentaje())
        assertEquals("147 h 00 min", resumenB.filaSinRegistrar.formatearHorasYMinutos())
        assertEquals("87.5 %", resumenB.filaSinRegistrar.formatearPorcentaje())

        // =====================================================================
        // ESCENARIO C: Varias categorías agrupadas con SUM y GROUP BY
        // =====================================================================
        val iniTrabajo = LocalDateTime.of(2026, 10, 13, 8, 0, 0)
        dbHelper.iniciarEvento(idTareaTrabajo, iniTrabajo)
        dbHelper.finalizarEventoActivo(iniTrabajo.plusHours(14)) // 14 h = 8.3 %

        val iniDescanso = LocalDateTime.of(2026, 10, 14, 0, 0, 0)
        dbHelper.iniciarEvento(idTareaDescanso, iniDescanso)
        dbHelper.finalizarEventoActivo(iniDescanso.plusHours(7)) // 7 h = 4.2 %

        val resumenC = dbHelper.obtenerResumenSemanal(fechaSemana)
        assertEquals("21 h 00 min", resumenC.filasCategorias.first { it.nombreCategoria == "Estudio" }.formatearHorasYMinutos())
        assertEquals("12.5 %", resumenC.filasCategorias.first { it.nombreCategoria == "Estudio" }.formatearPorcentaje())
        assertEquals("14 h 00 min", resumenC.filasCategorias.first { it.nombreCategoria == "Trabajo" }.formatearHorasYMinutos())
        assertEquals("8.3 %", resumenC.filasCategorias.first { it.nombreCategoria == "Trabajo" }.formatearPorcentaje())
        assertEquals("7 h 00 min", resumenC.filasCategorias.first { it.nombreCategoria == "Descanso" }.formatearHorasYMinutos())
        assertEquals("4.2 %", resumenC.filasCategorias.first { it.nombreCategoria == "Descanso" }.formatearPorcentaje())
        assertEquals("0 h 00 min", resumenC.filasCategorias.first { it.nombreCategoria == "Ocio" }.formatearHorasYMinutos())
        assertEquals("0.0 %", resumenC.filasCategorias.first { it.nombreCategoria == "Ocio" }.formatearPorcentaje())
        assertEquals("126 h 00 min", resumenC.filaSinRegistrar.formatearHorasYMinutos())
        assertEquals("75.0 %", resumenC.filaSinRegistrar.formatearPorcentaje())

        // =====================================================================
        // ESCENARIO D: Evento en curso NO suma al panel hasta finalizarse
        // =====================================================================
        val iniOcioEnCurso = LocalDateTime.of(2026, 10, 15, 16, 0, 0)
        dbHelper.iniciarEvento(idTareaOcio, iniOcioEnCurso)

        val resumenD = dbHelper.obtenerResumenSemanal(fechaSemana)
        assertEquals("0 h 00 min", resumenD.filasCategorias.first { it.nombreCategoria == "Ocio" }.formatearHorasYMinutos())
        assertEquals("0.0 %", resumenD.filasCategorias.first { it.nombreCategoria == "Ocio" }.formatearPorcentaje())
        assertEquals("126 h 00 min", resumenD.filaSinRegistrar.formatearHorasYMinutos())

        // Finalizar el evento de Ocio (2 horas) y comprobar actualización
        dbHelper.finalizarEventoActivo(iniOcioEnCurso.plusHours(2))

        // =====================================================================
        // ESCENARIO F: Evento que empieza el domingo y termina el lunes siguiente
        // Se atribuye íntegramente a la semana en que comenzó
        // =====================================================================
        val domingoNoche = LocalDateTime.of(2026, 10, 18, 22, 0, 0) // Domingo 22:00
        val lunesMadrugada = LocalDateTime.of(2026, 10, 19, 2, 0, 0) // Lunes siguiente 02:00 (4 horas)
        dbHelper.iniciarEvento(idTareaOcio, domingoNoche)
        dbHelper.finalizarEventoActivo(lunesMadrugada)

        val resumenF = dbHelper.obtenerResumenSemanal(fechaSemana)
        // Ocio tenía 2 h + estas 4 h iniciadas el domingo = 6 h en la semana del 12/10 al 18/10
        val filaOcioF = resumenF.filasCategorias.first { it.nombreCategoria == "Ocio" }
        assertEquals("6 h 00 min", filaOcioF.formatearHorasYMinutos())
        assertEquals("3.6 %", filaOcioF.formatearPorcentaje())

        // =====================================================================
        // ESCENARIO G: Cambio de semana (semana siguiente 19/10/2026 – 25/10/2026)
        // No incorpora eventos iniciados en semanas anteriores
        // =====================================================================
        val fechaSemanaSiguiente = java.time.LocalDate.of(2026, 10, 19)
        val resumenG = dbHelper.obtenerResumenSemanal(fechaSemanaSiguiente)
        assertEquals("Semana 19/10/2026 – 25/10/2026", resumenG.formatearRangoSemana())
        resumenG.filasCategorias.forEach { fila ->
            assertEquals("0 h 00 min", fila.formatearHorasYMinutos())
            assertEquals("0.0 %", fila.formatearPorcentaje())
        }
        assertEquals("168 h 00 min", resumenG.filaSinRegistrar.formatearHorasYMinutos())
        assertEquals("100.0 %", resumenG.filaSinRegistrar.formatearPorcentaje())

        // =====================================================================
        // ESCENARIO H y E: Categoría nueva en contenedor XML dinámico +
        // Actualización inmediata del panel en MainActivity tras finalizar
        // =====================================================================
        val resNuevaCat = dbHelper.crearCategoria("Proyecto Titulación")
        assertTrue(resNuevaCat is ResultadoOperacion.Exito)

        // Iniciar un evento en la semana actual del dispositivo para probar Escenario E en vivo en MainActivity
        val ahora = LocalDateTime.now()
        dbHelper.iniciarEvento(idTareaEstudio, ahora.minusMinutes(30))
        dbHelper.close()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNav)
                bottomNav.selectedItemId = R.id.nav_events

                val container = activity.findViewById< android.widget.LinearLayout>(R.id.llWeeklyCategoryRowsContainer)
                assertNotNull(container)
                // 5 categorías (4 iniciales + "Proyecto Titulación") + 1 fila "Sin registrar" = 6 filas dinámicas
                assertEquals(6, container.childCount)

                // Verificar que "Proyecto Titulación" y "Sin registrar" están en el contenedor dinámico
                val nombresFilas = (0 until container.childCount).map { idx ->
                    container.getChildAt(idx)
                        .findViewById<TextView>(R.id.tvWeeklyRowCategoryName)
                        .text.toString()
                }
                assertTrue(nombresFilas.contains("Proyecto Titulación"))
                assertEquals("Sin registrar", nombresFilas.last())

                // Escenario E: Finalizar el evento en curso pulsando el botón Finalizar sin salir de Eventos
                val btnStop = activity.findViewById<Button>(R.id.btnStopEvent)
                assertTrue(btnStop.isEnabled)
                btnStop.performClick()

                // Comprobar que el panel se actualizó inmediatamente con el evento recién finalizado
                val filaEstudioView = (0 until container.childCount)
                    .map { container.getChildAt(it) }
                    .first {
                        it.findViewById<TextView>(R.id.tvWeeklyRowCategoryName).text.toString() == "Estudio"
                    }
                val textoHorasEstudio = filaEstudioView.findViewById<TextView>(R.id.tvWeeklyRowHours).text.toString()
                assertNotEquals("0 h 00 min", textoHorasEstudio)
            }
        }
    }
}