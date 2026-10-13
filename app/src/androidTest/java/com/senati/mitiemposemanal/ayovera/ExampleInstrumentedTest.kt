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

        // 1. Abrir MainActivity con un evento activo en SQLite y verificar versión v1.0.0 (4)
        //    y recuperación del estado activo (Iniciar deshabilitado, Finalizar habilitado)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val tvStudentVersion = activity.findViewById<TextView>(R.id.tvStudentVersion)
                assertNotNull(tvStudentVersion)
                assertTrue(tvStudentVersion.text.toString().endsWith("· v1.0.0 (4)"))

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

    /**
     * Sección 15 (Escenarios A, B, C, D, E, F, G, H e I del Sprint 4):
     * Prueba integral del cierre semanal automático, reportes históricos, Spinner,
     * conservación del historial y migración no destructiva desde Sprint 3 (v3 -> v4).
     */
    @Test
    fun sprint4_escenariosObligatoriosCierreSemanalReportesYMigracion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(AdminSQLiteOpenHelper.DATABASE_NAME)

        // =====================================================================
        // ESCENARIO I: Migración de base de datos v3 (Sprint 3) a v4 (Sprint 4)
        // conservando categorías, tareas y eventos, y creando tablas de reportes
        // =====================================================================
        val dbFile = context.getDatabasePath(AdminSQLiteOpenHelper.DATABASE_NAME)
        dbFile.parentFile?.mkdirs()
        val dbV3 = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        dbV3.execSQL(
            """
            CREATE TABLE categorias (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL UNIQUE COLLATE NOCASE
            )
            """.trimIndent()
        )
        dbV3.execSQL(
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
        dbV3.execSQL(
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
            dbV3.insert("categorias", null, cv)
        }
        val cvTareaV3 = ContentValues().apply {
            put("nombre", "Estudio SQLite Sprint 3")
            put("categoria_id", 1L)
        }
        val idTareaV3 = dbV3.insert("tareas", null, cvTareaV3)
        val cvEventoPasadoV3 = ContentValues().apply {
            put("tarea_id", idTareaV3)
            put("fecha_inicio", "2026-09-14T08:00:00") // Lunes de semana pasada
            put("fecha_fin", "2026-09-14T15:00:00")    // 7 horas
            put("duracion_segundos", 7L * 3600L)
            put("estado", "FINALIZADO")
        }
        dbV3.insert("eventos", null, cvEventoPasadoV3)
        dbV3.version = 3
        dbV3.close()

        val dbHelperV4 = AdminSQLiteOpenHelper(context)
        assertEquals(4, dbHelperV4.listarCategorias().size)
        assertEquals(1, dbHelperV4.listarTareas().size)
        assertEquals(1, dbHelperV4.listarEventos().size)

        val generadosTrasMigracion = dbHelperV4.ejecutarCierreSemanalAutomatico(
            java.time.LocalDate.of(2026, 10, 12)
        )
        assertEquals(1, generadosTrasMigracion.size)
        assertEquals(0, dbHelperV4.ejecutarCierreSemanalAutomatico(java.time.LocalDate.of(2026, 10, 12)).size)
        assertEquals(1, dbHelperV4.listarReportesSemanales().size)
        dbHelperV4.close()

        // =====================================================================
        // ESCENARIO A: Sin reportes
        // =====================================================================
        context.deleteDatabase(AdminSQLiteOpenHelper.DATABASE_NAME)
        val dbHelper = AdminSQLiteOpenHelper(context)
        dbHelper.asegurarCategoriasInicialesSiVacio()
        assertEquals(0, dbHelper.ejecutarCierreSemanalAutomatico(java.time.LocalDate.of(2026, 10, 12)).size)
        assertTrue(dbHelper.listarReportesSemanales().isEmpty())
        dbHelper.close()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNav)
                bottomNav.selectedItemId = R.id.nav_reports

                val spinner = activity.findViewById<android.widget.Spinner>(R.id.spinnerReportWeeks)
                val emptyContainer = activity.findViewById<View>(R.id.llReportsEmptyStateContainer)
                val contentContainer = activity.findViewById<View>(R.id.llReportsContentContainer)
                val tvEmptyTitle = activity.findViewById<TextView>(R.id.tvReportsEmptyTitle)

                assertEquals(0, spinner.adapter?.count ?: 0)
                assertFalse(spinner.isEnabled)
                assertEquals(View.VISIBLE, emptyContainer.visibility)
                assertEquals(View.GONE, contentContainer.visibility)
                assertEquals("Aún no hay reportes", tvEmptyTitle.text.toString())
            }
        }

        // =====================================================================
        // ESCENARIOS B, C, D, F y G:
        // - B: Cierre semanal con horas, porcentajes y "Sin registrar"
        // - C: No duplicar reportes al reabrir o reejecutar
        // - D: Varias semanas pendientes cerradas en orden cronológico
        // - F: Historial de eventos permanece en BD y conserva snapshots históricos
        // - G: Evento que empieza el domingo y termina el lunes pertenece a la semana en que empezó
        // =====================================================================
        val db = AdminSQLiteOpenHelper(context)
        val categorias = db.listarCategorias()
        val idEstudio = categorias.first { it.nombre == "Estudio" }.id
        val idTrabajo = categorias.first { it.nombre == "Trabajo" }.id
        val idDescanso = categorias.first { it.nombre == "Descanso" }.id
        val idOcio = categorias.first { it.nombre == "Ocio" }.id

        val idTareaEstudio = (db.crearTarea("Arquitectura Android", idEstudio) as ResultadoOperacion.Exito).id
        val idTareaTrabajo = (db.crearTarea("Desarrollo Backend", idTrabajo) as ResultadoOperacion.Exito).id
        val idTareaDescanso = (db.crearTarea("Sueño Reparador", idDescanso) as ResultadoOperacion.Exito).id
        val idTareaOcio = (db.crearTarea("Cine Dominical", idOcio) as ResultadoOperacion.Exito).id

        // Semana 1 (más antigua): 21/09/2026 al 27/09/2026 -> 14 h en Trabajo (8.3 %)
        val iniSem1 = LocalDateTime.of(2026, 9, 22, 8, 0, 0)
        db.iniciarEvento(idTareaTrabajo, iniSem1)
        db.finalizarEventoActivo(iniSem1.plusHours(14))

        // Semana 2 (intermedia): 28/09/2026 al 04/10/2026 ->
        // Incluye Escenario G: Evento que empieza el domingo 04/10 a las 22:00 y termina el lunes 05/10 a las 05:00 (7 h = 4.2 %)
        val domingoSem2 = LocalDateTime.of(2026, 10, 4, 22, 0, 0)
        val lunesSem3 = LocalDateTime.of(2026, 10, 5, 5, 0, 0)
        db.iniciarEvento(idTareaOcio, domingoSem2)
        db.finalizarEventoActivo(lunesSem3)

        // Semana 3 (más reciente cerrada): 05/10/2026 al 11/10/2026 ->
        // Estudio: 21 h (12.5 %), Descanso: 42 h (25.0 %) => Total registrado: 63 h (37.5 %), Sin registrar: 105 h (62.5 %)
        val iniSem3Estudio = LocalDateTime.of(2026, 10, 6, 8, 0, 0)
        db.iniciarEvento(idTareaEstudio, iniSem3Estudio)
        db.finalizarEventoActivo(iniSem3Estudio.plusHours(21))

        val iniSem3Descanso = LocalDateTime.of(2026, 10, 7, 0, 0, 0)
        db.iniciarEvento(idTareaDescanso, iniSem3Descanso)
        db.finalizarEventoActivo(iniSem3Descanso.plusHours(42))

        // Ejecutar cierre semanal automático simulando que hoy es miércoles 14/10/2026
        val fechaActualSimulada = java.time.LocalDate.of(2026, 10, 14)
        val nuevosReportes = db.ejecutarCierreSemanalAutomatico(fechaActualSimulada)
        assertEquals(3, nuevosReportes.size)

        // Escenario C: Reejecutar cierre varias veces NO duplica ningún reporte
        assertEquals(0, db.ejecutarCierreSemanalAutomatico(fechaActualSimulada).size)
        assertEquals(0, db.ejecutarCierreSemanalAutomatico(fechaActualSimulada).size)

        // Verificar que listarReportesSemanales() devuelve desde la más reciente hasta la más antigua
        val listaReportes = db.listarReportesSemanales()
        assertEquals(3, listaReportes.size)
        assertEquals("05/10/2026 – 11/10/2026", listaReportes[0].formatearRangoSpinner())
        assertEquals("28/09/2026 – 04/10/2026", listaReportes[1].formatearRangoSpinner())
        assertEquals("21/09/2026 – 27/09/2026", listaReportes[2].formatearRangoSpinner())

        // Escenario D: Verificar que se insertaron en orden cronológico ascendente (id[2] < id[1] < id[0])
        assertTrue(listaReportes[2].id < listaReportes[1].id)
        assertTrue(listaReportes[1].id < listaReportes[0].id)

        // Escenario B: Verificar métricas exactas de la semana 05/10/2026 – 11/10/2026
        val repReciente = listaReportes[0]
        assertEquals("63 h 00 min", repReciente.formatearHorasRegistradas())
        assertEquals("37.5 %", repReciente.formatearPorcentajeRegistrado())
        assertEquals("105 h 00 min", repReciente.formatearHorasSinRegistrar())
        assertEquals("62.5 %", repReciente.formatearPorcentajeSinRegistrar())
        assertEquals(2, repReciente.totalEventos)

        val detEstudio = repReciente.detallesCategorias.first { it.categoriaNombre == "Estudio" }
        assertEquals("21 h 00 min", detEstudio.formatearHorasYMinutos())
        assertEquals("12.5 %", detEstudio.formatearPorcentaje())

        val detDescanso = repReciente.detallesCategorias.first { it.categoriaNombre == "Descanso" }
        assertEquals("42 h 00 min", detDescanso.formatearHorasYMinutos())
        assertEquals("25.0 %", detDescanso.formatearPorcentaje())

        // Escenario G: Verificar que el evento domingo->lunes quedó en la semana 28/09/2026 – 04/10/2026
        val repIntermedio = listaReportes[1]
        assertEquals("7 h 00 min", repIntermedio.formatearHorasRegistradas())
        assertEquals("4.2 %", repIntermedio.formatearPorcentajeRegistrado())
        val eventosSem2 = db.listarEventosFinalizadosPorSemana(repIntermedio)
        assertEquals(1, eventosSem2.size)
        assertEquals("Cine Dominical", eventosSem2.first().tareaNombre)

        // Escenario F: Renombrar la tarea después del cierre y comprobar que el reporte conserva el snapshot histórico
        db.actualizarTarea(idTareaEstudio, "Arquitectura Renombrada Post-Cierre", idEstudio)
        val eventosSem3 = db.listarEventosFinalizadosPorSemana(repReciente)
        assertEquals(2, eventosSem3.size)
        assertTrue(eventosSem3.any { it.tareaNombre == "Arquitectura Android" })
        // Y los eventos siguen existiendo en la tabla general de eventos
        assertEquals(4, db.listarEventos().size)
        db.close()

        // =====================================================================
        // ESCENARIOS E y H en interfaz (MainActivity):
        // - E: Spinner muestra semanas cerradas y al seleccionar otra semana actualiza porcentajes e historial
        // - H: El panel semanal de Eventos (Sprint 3) muestra limpia la semana actual sin mezclar semanas anteriores
        // =====================================================================
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNav)

                // Escenario H: En Eventos, la semana actual está limpia (0 h 00 min en categorías, 168 h Sin registrar)
                bottomNav.selectedItemId = R.id.nav_events
                val tvWeeklyRegistered = activity.findViewById<TextView>(R.id.tvWeeklyTotalRegisteredSummary)
                assertTrue(tvWeeklyRegistered.text.toString().startsWith("0 h 00 min"))

                // Escenario E: Navegar a Reportes y verificar el Spinner y el contenido
                bottomNav.selectedItemId = R.id.nav_reports
                val spinner = activity.findViewById<android.widget.Spinner>(R.id.spinnerReportWeeks)
                val emptyContainer = activity.findViewById<View>(R.id.llReportsEmptyStateContainer)
                val contentContainer = activity.findViewById<View>(R.id.llReportsContentContainer)
                val tvTotalHours = activity.findViewById<TextView>(R.id.tvReportRegisteredHours)
                val tvTotalPercent = activity.findViewById<TextView>(R.id.tvReportRegisteredPercent)

                assertEquals(View.GONE, emptyContainer.visibility)
                assertEquals(View.VISIBLE, contentContainer.visibility)
                assertEquals(3, spinner.adapter.count)
                assertEquals("05/10/2026 – 11/10/2026", spinner.adapter.getItem(0).toString())
                assertEquals("28/09/2026 – 04/10/2026", spinner.adapter.getItem(1).toString())
                assertEquals("21/09/2026 – 27/09/2026", spinner.adapter.getItem(2).toString())

                // Por defecto está seleccionada la posición 0 (más reciente: 63 h 00 min / 37.5 %)
                assertEquals("63 h 00 min", tvTotalHours.text.toString())
                assertEquals("37.5 % de la semana", tvTotalPercent.text.toString())
                val rvWeekEvents = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvReportWeekEvents)
                assertEquals(2, rvWeekEvents.adapter?.itemCount)

                // Cambiar selección en el Spinner a la semana índice 2 (21/09/2026 – 27/09/2026: 14 h 00 min / 8.3 %)
                spinner.setSelection(2)
                val listener = spinner.onItemSelectedListener
                listener?.onItemSelected(spinner, spinner.selectedView, 2, 2L)

                assertEquals("14 h 00 min", tvTotalHours.text.toString())
                assertEquals("8.3 % de la semana", tvTotalPercent.text.toString())
                assertEquals(1, rvWeekEvents.adapter?.itemCount)
            }
        }
    }
}