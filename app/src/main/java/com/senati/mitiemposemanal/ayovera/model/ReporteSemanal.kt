package com.senati.mitiemposemanal.ayovera.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Detalle histórico por categoría dentro de un reporte semanal cerrado (Sprint 4 — HU-07 y HU-08).
 *
 * Almacena los valores calculados y persistidos en `detalle_reportes_semanales`, incluyendo
 * una instantánea histórica del nombre de la categoría (`categoriaNombre`) para que el reporte
 * conserve su integridad aunque la categoría cambie de nombre posteriormente.
 */
data class DetalleReporteCategoria(
    val id: Long = 0L,
    val reporteId: Long,
    val categoriaId: Long,
    val categoriaNombre: String,
    val segundosTotales: Long,
    val minutosTotales: Double,
    val porcentaje: Double
) {
    val esCategoriaInicial: Boolean
        get() = categoriaNombre.trim().lowercase() in NOMBRES_CATEGORIAS_INICIALES

    fun formatearHorasYMinutos(): String {
        return ResumenSemanal.formatearSegundosAHorasYMinutos(segundosTotales)
    }

    fun formatearPorcentaje(): String {
        return String.format(Locale.US, "%.1f %%", porcentaje)
    }

    fun obtenerProgresoVisualPorMil(): Int {
        return (porcentaje * 10.0).roundToInt().coerceIn(0, 1000)
    }

    fun aFilaCategoriaSemanal(): FilaCategoriaSemanal {
        return FilaCategoriaSemanal(
            categoriaId = categoriaId,
            nombreCategoria = categoriaNombre,
            totalSegundos = segundosTotales,
            esSinRegistrar = false,
            esCategoriaInicial = esCategoriaInicial
        )
    }

    companion object {
        private val NOMBRES_CATEGORIAS_INICIALES = setOf(
            "estudio", "trabajo", "descanso", "ocio"
        )
    }
}

/**
 * Entidad de dominio para la tabla `reportes_semanales` (Sprint 4 — HU-07 y HU-08).
 *
 * Representa el cierre consolidado de una semana completa (lunes 00:00 a domingo 23:59:59,
 * base fija de 168 horas = 10 080 minutos = 604 800 segundos).
 */
data class ReporteSemanal(
    val id: Long,
    val fechaInicioSemanaIso: String,
    val fechaFinSemanaIso: String,
    val segundosRegistrados: Long,
    val minutosRegistrados: Double,
    val porcentajeRegistrado: Double,
    val segundosSinRegistrar: Long,
    val minutosSinRegistrar: Double,
    val porcentajeSinRegistrar: Double,
    val totalEventos: Int,
    val fechaCierreIso: String,
    val detallesCategorias: List<DetalleReporteCategoria> = emptyList()
) {
    fun fechaLunesLocalDate(): LocalDate = LocalDate.parse(fechaInicioSemanaIso.trim())

    fun fechaDomingoLocalDate(): LocalDate = LocalDate.parse(fechaFinSemanaIso.trim())

    fun lunesInicioLocalDateTime(): LocalDateTime = fechaLunesLocalDate().atStartOfDay()

    fun lunesSiguienteExclusivoLocalDateTime(): LocalDateTime =
        fechaLunesLocalDate().plusDays(7).atStartOfDay()

    /**
     * Formato obligatorio para el Spinner de la pantalla Reportes (Sección 8):
     * `06/10/2026 – 12/10/2026`
     */
    fun formatearRangoSpinner(): String {
        val ini = fechaLunesLocalDate().format(FORMATTER_FECHA_REPORTE)
        val fin = fechaDomingoLocalDate().format(FORMATTER_FECHA_REPORTE)
        return "$ini – $fin"
    }

    fun formatearHorasRegistradas(): String {
        return ResumenSemanal.formatearSegundosAHorasYMinutos(segundosRegistrados)
    }

    fun formatearPorcentajeRegistrado(): String {
        return String.format(Locale.US, "%.1f %%", porcentajeRegistrado)
    }

    fun formatearHorasSinRegistrar(): String {
        return ResumenSemanal.formatearSegundosAHorasYMinutos(segundosSinRegistrar)
    }

    fun formatearPorcentajeSinRegistrar(): String {
        return String.format(Locale.US, "%.1f %%", porcentajeSinRegistrar)
    }

    fun obtenerProgresoRegistradoPorMil(): Int {
        return (porcentajeRegistrado * 10.0).roundToInt().coerceIn(0, 1000)
    }

    companion object {
        private val FORMATTER_FECHA_REPORTE: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US)

        fun formatearFechaIso(fecha: LocalDate): String {
            return fecha.format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }
}

