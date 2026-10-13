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

    @Test
    fun resumenSemanal_limitesLunesADomingoYCalculoSobre168Horas_Sprint3() {
        val categorias = listOf(
            com.senati.mitiemposemanal.ayovera.model.Categoria(1L, "Estudio"),
            com.senati.mitiemposemanal.ayovera.model.Categoria(2L, "Trabajo"),
            com.senati.mitiemposemanal.ayovera.model.Categoria(3L, "Descanso"),
            com.senati.mitiemposemanal.ayovera.model.Categoria(4L, "Ocio")
        )
        val fechaMiercoles = java.time.LocalDate.of(2026, 10, 14)

        // Escenario A: Semana vacía
        val resumenVacio = com.senati.mitiemposemanal.ayovera.model.ResumenSemanal.construir(
            categorias = categorias,
            segundosPorCategoriaId = emptyMap(),
            fechaReferencia = fechaMiercoles
        )
        assertEquals("Semana 12/10/2026 – 18/10/2026", resumenVacio.formatearRangoSemana())
        assertEquals(LocalDateTime.of(2026, 10, 12, 0, 0, 0), resumenVacio.lunesInicio)
        assertEquals(LocalDateTime.of(2026, 10, 19, 0, 0, 0), resumenVacio.lunesSiguienteExclusivo)

        resumenVacio.filasCategorias.forEach { fila ->
            assertEquals("0 h 00 min", fila.formatearHorasYMinutos())
            assertEquals("0.0 %", fila.formatearPorcentaje())
            assertEquals(0, fila.obtenerProgresoVisualPorMil())
        }
        assertEquals("168 h 00 min", resumenVacio.filaSinRegistrar.formatearHorasYMinutos())
        assertEquals("100.0 %", resumenVacio.filaSinRegistrar.formatearPorcentaje())
        assertEquals(1000, resumenVacio.filaSinRegistrar.obtenerProgresoVisualPorMil())

        // Escenario B: 21 horas en Estudio -> 12.5 %, Sin registrar -> 147 h (87.5 %)
        val resumen21h = com.senati.mitiemposemanal.ayovera.model.ResumenSemanal.construir(
            categorias = categorias,
            segundosPorCategoriaId = mapOf(1L to 21L * 3600L),
            fechaReferencia = fechaMiercoles
        )
        val filaEstudio = resumen21h.filasCategorias.first { it.nombreCategoria == "Estudio" }
        assertEquals("21 h 00 min", filaEstudio.formatearHorasYMinutos())
        assertEquals(12.5, filaEstudio.porcentajeSemanal, 0.0001)
        assertEquals("12.5 %", filaEstudio.formatearPorcentaje())

        assertEquals("147 h 00 min", resumen21h.filaSinRegistrar.formatearHorasYMinutos())
        assertEquals(87.5, resumen21h.filaSinRegistrar.porcentajeSemanal, 0.0001)
        assertEquals("87.5 %", resumen21h.filaSinRegistrar.formatearPorcentaje())
    }
}