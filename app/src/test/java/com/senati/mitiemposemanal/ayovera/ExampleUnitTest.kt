package com.senati.mitiemposemanal.ayovera

import com.senati.mitiemposemanal.ayovera.data.ValidationLogic
import com.senati.mitiemposemanal.ayovera.model.Tarea
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun categoriasIniciales_sonLasCuatroRequeridas() {
        assertEquals(
            listOf("Estudio", "Trabajo", "Descanso", "Ocio"),
            ValidationLogic.CATEGORIAS_INICIALES
        )
    }

    @Test
    fun normalizarNombre_eliminaEspaciosInnecesarios() {
        assertEquals("Leer Kotlin", ValidationLogic.normalizarNombre("   Leer    Kotlin   "))
        assertEquals("", ValidationLogic.normalizarNombre("     "))
    }

    @Test
    fun validarNombreCategoria_rechazaVaciosYDuplicados() {
        val existentes = listOf("Estudio", "Trabajo", "Descanso", "Ocio")

        // Vacío o solo espacios
        assertNotNull(ValidationLogic.validarNombreCategoria("", existentes))
        assertNotNull(ValidationLogic.validarNombreCategoria("   ", existentes))

        // Duplicado exacto o con mayúsculas/espacios
        assertNotNull(ValidationLogic.validarNombreCategoria("Estudio", existentes))
        assertNotNull(ValidationLogic.validarNombreCategoria("  estudio  ", existentes))

        // Nombre nuevo válido
        assertNull(ValidationLogic.validarNombreCategoria("Proyectos SENATI", existentes))

        // Edición conservando su propio nombre (con cambio de mayúsculas o igual)
        assertNull(
            ValidationLogic.validarNombreCategoria(
                nombreRaw = "Estudio",
                nombresExistentes = existentes,
                nombreActualEnEdicion = "Estudio"
            )
        )
    }

    @Test
    fun validarTarea_rechazaVaciosSinCategoriaYDuplicados() {
        val tareasExistentes = listOf("Leer Kotlin", "Informe Técnico")

        // Sin nombre
        assertNotNull(ValidationLogic.validarTarea("   ", 1L, tareasExistentes))

        // Sin categoría seleccionada
        assertNotNull(ValidationLogic.validarTarea("Nueva tarea", null, tareasExistentes))

        // Duplicado (case-insensitive y con espacios extra)
        assertNotNull(ValidationLogic.validarTarea("  leer   kotlin ", 1L, tareasExistentes))

        // Tarea válida
        assertNull(ValidationLogic.validarTarea("Practicar SQLite", 1L, tareasExistentes))
    }

    @Test
    fun tarea_formatoConCategoriaCumpleHU02() {
        val tarea = Tarea(
            id = 1L,
            nombre = "Leer Kotlin",
            categoriaId = 1L,
            categoriaNombre = "Estudio"
        )
        assertEquals("Leer Kotlin · Estudio", tarea.textoConCategoria)
    }
}