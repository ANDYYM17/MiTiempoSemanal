package com.senati.mitiemposemanal.ayovera

import com.senati.mitiemposemanal.ayovera.data.ValidationLogic
import com.senati.mitiemposemanal.ayovera.model.Evento
import com.senati.mitiemposemanal.ayovera.model.Tarea
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

/**
 * Pruebas unitarias locales para Sprints 1 y 2 de MiTiempoSemanal.
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

        assertNotNull(ValidationLogic.validarNombreCategoria("", existentes))
        assertNotNull(ValidationLogic.validarNombreCategoria("   ", existentes))
        assertNotNull(ValidationLogic.validarNombreCategoria("Estudio", existentes))
        assertNotNull(ValidationLogic.validarNombreCategoria("  estudio  ", existentes))
        assertNull(ValidationLogic.validarNombreCategoria("Proyectos SENATI", existentes))
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

        assertNotNull(ValidationLogic.validarTarea("   ", 1L, tareasExistentes))
        assertNotNull(ValidationLogic.validarTarea("Nueva tarea", null, tareasExistentes))
        assertNotNull(ValidationLogic.validarTarea("  leer   kotlin ", 1L, tareasExistentes))
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

    @Test
    fun busquedaTareas_requiereAlMenosDosCaracteresUtiles_HU03() {
        assertFalse(ValidationLogic.esConsultaBusquedaValida(""))
        assertFalse(ValidationLogic.esConsultaBusquedaValida("   "))
        assertFalse(ValidationLogic.esConsultaBusquedaValida("k"))
        assertFalse(ValidationLogic.esConsultaBusquedaValida("  k  "))
        assertTrue(ValidationLogic.esConsultaBusquedaValida("ko"))
        assertTrue(ValidationLogic.esConsultaBusquedaValida("  ko "))
        assertTrue(ValidationLogic.esConsultaBusquedaValida("kotlin"))
    }

    @Test
    fun evento_calculoDuracionYFormateoFechas_HU05() {
        val inicio = LocalDateTime.of(2026, 10, 12, 18, 30, 15)
        val fin = LocalDateTime.of(2026, 10, 12, 19, 55, 15)

        val segundos = Evento.calcularDuracionSegundos(inicio, fin)
        assertEquals(85L * 60L, segundos)
        assertEquals("01:25:00", Evento.formatearCronometro(segundos))
        assertEquals("1 h 25 min", Evento.formatearDuracionLegible(segundos))

        val eventoEnCurso = Evento(
            id = 10L,
            tareaId = 1L,
            tareaNombre = "Leer Kotlin",
            categoriaId = 1L,
            categoriaNombre = "Estudio",
            fechaInicioIso = Evento.formatearIso(inicio),
            fechaFinIso = null,
            duracionSegundos = null,
            estado = Evento.ESTADO_EN_CURSO
        )
        assertTrue(eventoEnCurso.enCurso)
        assertEquals("Leer Kotlin · Estudio", eventoEnCurso.textoTareaConCategoria)
        assertEquals("18:30:15", eventoEnCurso.formatearHoraInicioConSegundos())
        assertEquals("En curso", eventoEnCurso.formatearHoraFinConSegundos())

        val eventoFinalizado = eventoEnCurso.copy(
            fechaFinIso = Evento.formatearIso(fin),
            duracionSegundos = segundos,
            estado = Evento.ESTADO_FINALIZADO
        )
        assertFalse(eventoFinalizado.enCurso)
        assertEquals("19:55:15", eventoFinalizado.formatearHoraFinConSegundos())
        assertEquals("1 h 25 min", eventoFinalizado.formatearDuracion(fin))
    }
}