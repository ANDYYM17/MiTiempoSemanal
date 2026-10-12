package com.senati.mitiemposemanal.ayovera

import android.content.Intent
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.senati.mitiemposemanal.ayovera.data.AdminSQLiteOpenHelper
import com.senati.mitiemposemanal.ayovera.data.ResultadoEliminacionCategoria
import com.senati.mitiemposemanal.ayovera.data.ResultadoOperacion

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.senati.mitiemposemanal.ayovera", appContext.packageName)
    }

    @Test
    fun sqlite_flujoCompletoCategoriasYTareas_Sprint1() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(AdminSQLiteOpenHelper.DATABASE_NAME)

        val dbHelper = AdminSQLiteOpenHelper(context)
        dbHelper.asegurarCategoriasInicialesSiVacio()
        // Llamar por segunda vez no debe duplicar las 4 categorías iniciales
        dbHelper.asegurarCategoriasInicialesSiVacio()

        val categoriasIniciales = dbHelper.listarCategorias()
        assertEquals(4, categoriasIniciales.size)
        assertEquals(
            listOf("Estudio", "Trabajo", "Descanso", "Ocio"),
            categoriasIniciales.map { it.nombre }
        )

        // Validar nombres vacíos y espacios innecesarios
        assertTrue(dbHelper.crearCategoria("   ") is ResultadoOperacion.Error)

        // Crear categoría nueva y rechazar duplicado
        val creacionCat = dbHelper.crearCategoria("  Laboratorio   ")
        assertTrue(creacionCat is ResultadoOperacion.Exito)
        val idLaboratorio = (creacionCat as ResultadoOperacion.Exito).id
        assertEquals("Laboratorio", dbHelper.obtenerCategoriaPorId(idLaboratorio)?.nombre)

        val duplicadoCat = dbHelper.crearCategoria("laboratorio")
        assertTrue(duplicadoCat is ResultadoOperacion.Error)

        // Editar nombre de categoría
        val edicionCat = dbHelper.actualizarCategoria(idLaboratorio, "  Laboratorio SENATI ")
        assertTrue(edicionCat is ResultadoOperacion.Exito)
        assertEquals("Laboratorio SENATI", dbHelper.obtenerCategoriaPorId(idLaboratorio)?.nombre)

        // Rechazar edición a nombre duplicado existente
        val edicionCatDuplicada = dbHelper.actualizarCategoria(idLaboratorio, "Estudio")
        assertTrue(edicionCatDuplicada is ResultadoOperacion.Error)

        // Crear tarea asociada a Estudio y validar vacíos/duplicados
        val idEstudio = categoriasIniciales.first { it.nombre == "Estudio" }.id
        val idTrabajo = categoriasIniciales.first { it.nombre == "Trabajo" }.id

        assertTrue(dbHelper.crearTarea("   ", idEstudio) is ResultadoOperacion.Error)
        assertTrue(dbHelper.crearTarea("Tarea sin categoría", null) is ResultadoOperacion.Error)

        val creacionTarea = dbHelper.crearTarea("  Leer Kotlin  ", idEstudio)
        assertTrue(creacionTarea is ResultadoOperacion.Exito)
        val idTarea = (creacionTarea as ResultadoOperacion.Exito).id

        // Rechazar tarea duplicada
        assertTrue(dbHelper.crearTarea("leer kotlin", idTrabajo) is ResultadoOperacion.Error)

        val tareas = dbHelper.listarTareas()
        assertEquals(1, tareas.size)
        assertEquals("Leer Kotlin · Estudio", tareas.first().textoConCategoria)

        // Bloquear eliminación de categoría con tareas asociadas
        val intentoEliminarConTareas = dbHelper.eliminarCategoria(idEstudio)
        assertTrue(intentoEliminarConTareas is ResultadoEliminacionCategoria.BloqueadaConTareas)

        // Editar nombre y categoría de la tarea
        val edicionTarea = dbHelper.actualizarTarea(idTarea, "Leer Kotlin Avanzado", idTrabajo)
        assertTrue(edicionTarea is ResultadoOperacion.Exito)
        assertEquals(
            "Leer Kotlin Avanzado · Trabajo",
            dbHelper.obtenerTareaPorId(idTarea)?.textoConCategoria
        )

        // Comprobar persistencia tras cerrar y volver a abrir la instancia de SQLiteOpenHelper
        dbHelper.close()

        val dbHelperReabierto = AdminSQLiteOpenHelper(context)
        dbHelperReabierto.asegurarCategoriasInicialesSiVacio()

        val categoriasReabiertas = dbHelperReabierto.listarCategorias()
        // 4 iniciales + 1 personalizada ("Laboratorio SENATI") = 5 (sin duplicar las iniciales)
        assertEquals(5, categoriasReabiertas.size)

        val tareasReabiertas = dbHelperReabierto.listarTareas()
        assertEquals(1, tareasReabiertas.size)
        assertEquals("Leer Kotlin Avanzado · Trabajo", tareasReabiertas.first().textoConCategoria)

        // Eliminar tarea y luego eliminar categoría sin tareas
        assertTrue(dbHelperReabierto.eliminarTarea(idTarea) is ResultadoOperacion.Exito)
        assertTrue(
            dbHelperReabierto.eliminarCategoria(idLaboratorio) is ResultadoEliminacionCategoria.Eliminada
        )

        // Dejar estado inicial limpio (4 categorías iniciales) para el uso normal en el dispositivo
        dbHelperReabierto.close()
    }

    @Test
    fun activities_navegacionYFormatoVersion_Sprint1() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // 1. Pantalla Principal (Inicio, Categorías y Tareas) + formato "Nombre Apellido · v0.1.0 (1)"
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val tvStudentVersion = activity.findViewById<TextView>(R.id.tvStudentVersion)
                assertNotNull(tvStudentVersion)
                assertTrue(tvStudentVersion.text.toString().endsWith("· v0.1.0 (1)"))
            }
        }

        // 2. Pantalla Crear Categoría
        ActivityScenario.launch(CategoryFormActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }

        // 3. Pantalla Crear Tarea
        ActivityScenario.launch(TaskFormActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }

        // 4. Pantalla 8: Confirmaciones y Mensajes (Stitch)
        val intentMessages = Intent(context, SystemMessagesActivity::class.java)
        ActivityScenario.launch<SystemMessagesActivity>(intentMessages).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }
    }
}