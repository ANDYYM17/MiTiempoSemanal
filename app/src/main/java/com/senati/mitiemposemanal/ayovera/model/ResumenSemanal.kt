package com.senati.mitiemposemanal.ayovera.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Representa una fila individual dentro del panel "Mi tiempo esta semana" (Sprint 3).
 * Puede corresponder a una categoría registrada en SQLite o a la fila especial "Sin registrar".
 */
data class FilaCategoriaSemanal(
    val categoriaId: Long,
    val nombreCategoria: String,
    val totalSegundos: Long,
    val esSinRegistrar: Boolean = false,
    val esCategoriaInicial: Boolean = false
) {
    /**
     * Total en minutos calculado con precisión decimal a partir de los segundos acumulados,
     * sin redondear cada evento antes de sumar (Sección 4).
     */
    val minutosAcumulados: Double
        get() = totalSegundos.coerceAtLeast(0L) / 60.0

    /**
     * Fórmula obligatoria del Sprint 3 (Secciones 4 y 8):
     * `porcentaje = minutos_categoria / 10080.0 * 100.0`
     */
    val porcentajeSemanal: Double
        get() = (minutosAcumulados / ResumenSemanal.MINUTOS_SEMANA) * 100.0

    /**
     * Formatea el porcentaje semanal siempre con un decimal (ej. `12.5 %`, `0.0 %`, `100.0 %`).
     */
    fun formatearPorcentaje(): String {
        return String.format(Locale.US, "%.1f %%", porcentajeSemanal)
    }

    /**
     * Formatea las horas y minutos acumulados (ej. `21 h 00 min`, `0 h 00 min`, `168 h 00 min`).
     * Si existe una fracción de segundos en pruebas rápidas (ej. un evento corto de pocos segundos),
     * muestra también los segundos para reflejar inmediatamente el cambio sin perder legibilidad.
     */
    fun formatearHorasYMinutos(): String {
        return ResumenSemanal.formatearSegundosAHorasYMinutos(totalSegundos)
    }

    /**
     * Valor entero entre 0 y 1000 para barras de progreso con `max = 1000` (precisión de 0.1 %).
     * Acota el valor visual al 100 % (1000) para que un exceso de datos nunca rompa la interfaz (Sección 9).
     */
    fun obtenerProgresoVisualPorMil(): Int {
        return (porcentajeSemanal * 10.0).roundToInt().coerceIn(0, 1000)
    }
}

/**
 * Modelo de dominio para el panel semanal del Sprint 3 ("Mi tiempo esta semana").
 *
 * Reglas de negocio (Secciones 4, 7, 8, 9 y 11):
 * - La semana inicia el lunes a las 00:00:00 (inclusive) y termina el domingo a las 23:59:59
 *   (consulta semiabierta `[lunes 00:00, lunes_siguiente 00:00)`).
 * - Una semana tiene exactamente 7 días = 168 horas = 10 080 minutos = 604 800 segundos.
 * - Un evento pertenece íntegramente a la semana en que comenzó (`fecha_inicio`), aunque termine el lunes siguiente.
 * - Solo se contabilizan eventos finalizados (`fecha_fin IS NOT NULL` y `estado = 'FINALIZADO'`).
 */
data class ResumenSemanal(
    val lunesInicio: LocalDateTime,
    val domingoFin: LocalDate,
    val lunesSiguienteExclusivo: LocalDateTime,
    val filasCategorias: List<FilaCategoriaSemanal>,
    val filaSinRegistrar: FilaCategoriaSemanal,
    val totalSegundosRegistrados: Long,
    val excedeLimiteSemanal: Boolean,
    val excesoSegundos: Long
) {
    val totalMinutosRegistrados: Double
        get() = totalSegundosRegistrados.coerceAtLeast(0L) / 60.0

    val porcentajeTotalRegistrado: Double
        get() = (totalMinutosRegistrados / MINUTOS_SEMANA) * 100.0

    /**
     * Todas las filas que se renderizan dinámicamente en el contenedor XML:
     * primero cada categoría existente en SQLite y al final la fila "Sin registrar".
     */
    val todasLasFilas: List<FilaCategoriaSemanal>
        get() = filasCategorias + filaSinRegistrar

    /**
     * Ejemplo: `Semana 12/10/2026 – 18/10/2026`
     */
    fun formatearRangoSemana(): String {
        val ini = lunesInicio.toLocalDate().format(FORMATTER_FECHA_SEMANA)
        val fin = domingoFin.format(FORMATTER_FECHA_SEMANA)
        return "Semana $ini – $fin"
    }

    /**
     * Ejemplo: `Semana 12/10/2026 – 18/10/2026 (Lun - Dom)`
     */
    fun formatearRangoSemanaConDias(): String {
        return "${formatearRangoSemana()} (Lun - Dom)"
    }

    /**
     * Texto de referencia a las 168 horas para la cabecera de distribución.
     * Ejemplo: `21 h 00 min / 168 h registradas`
     */
    fun formatearResumenRegistradoSobre168h(): String {
        val tiempoStr = formatearSegundosAHorasYMinutos(totalSegundosRegistrados)
        return "$tiempoStr / 168 h registradas"
    }

    fun formatearPorcentajeOcupado(): String {
        return String.format(Locale.US, "%.1f %%", porcentajeTotalRegistrado)
    }

    companion object {
        const val DIAS_SEMANA = 7
        const val HORAS_SEMANA = 168
        const val MINUTOS_SEMANA = 10080.0
        const val SEGUNDOS_SEMANA = 604800L
        const val ID_SIN_REGISTRAR = -1L
        const val NOMBRE_SIN_REGISTRAR = "Sin registrar"

        private val FORMATTER_FECHA_SEMANA: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US)

        private val NOMBRES_CATEGORIAS_INICIALES = setOf(
            "estudio", "trabajo", "descanso", "ocio"
        )

        /**
         * Calcula el lunes a las 00:00:00 de la semana correspondiente a `fechaReferencia`.
         */
        fun obtenerLunesInicioSemana(fechaReferencia: LocalDate = LocalDate.now()): LocalDateTime {
            val lunes = fechaReferencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return lunes.atStartOfDay()
        }

        /**
         * Calcula el domingo de la semana correspondiente a `fechaReferencia`.
         */
        fun obtenerDomingoFinSemana(fechaReferencia: LocalDate = LocalDate.now()): LocalDate {
            val lunes = fechaReferencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return lunes.plusDays(6)
        }

        /**
         * Calcula el lunes siguiente a las 00:00:00 (límite superior exclusivo para la consulta SQL).
         */
        fun obtenerLunesSiguienteExclusivo(fechaReferencia: LocalDate = LocalDate.now()): LocalDateTime {
            val lunes = fechaReferencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return lunes.plusDays(7).atStartOfDay()
        }

        /**
         * Construye el resumen semanal completo a partir de la lista de categorías existentes
         * y del mapa de segundos acumulados devuelto por la consulta SQL `SUM ... GROUP BY`.
         */
        fun construir(
            categorias: List<Categoria>,
            segundosPorCategoriaId: Map<Long, Long>,
            fechaReferencia: LocalDate = LocalDate.now()
        ): ResumenSemanal {
            val lunesInicio = obtenerLunesInicioSemana(fechaReferencia)
            val domingoFin = obtenerDomingoFinSemana(fechaReferencia)
            val lunesSiguiente = obtenerLunesSiguienteExclusivo(fechaReferencia)

            val filasCategorias = categorias.map { cat ->
                val seg = (segundosPorCategoriaId[cat.id] ?: 0L).coerceAtLeast(0L)
                FilaCategoriaSemanal(
                    categoriaId = cat.id,
                    nombreCategoria = cat.nombre,
                    totalSegundos = seg,
                    esSinRegistrar = false,
                    esCategoriaInicial = cat.nombre.trim().lowercase() in NOMBRES_CATEGORIAS_INICIALES
                )
            }

            val totalSegundosRegistrados = filasCategorias.sumOf { it.totalSegundos }
            val excedeLimite = totalSegundosRegistrados > SEGUNDOS_SEMANA
            val excesoSegundos = (totalSegundosRegistrados - SEGUNDOS_SEMANA).coerceAtLeast(0L)

            // Sección 8: minutos_sin_registrar = 10080 - minutos_registrados
            val segundosSinRegistrar = (SEGUNDOS_SEMANA - totalSegundosRegistrados).coerceAtLeast(0L)
            val filaSinRegistrar = FilaCategoriaSemanal(
                categoriaId = ID_SIN_REGISTRAR,
                nombreCategoria = NOMBRE_SIN_REGISTRAR,
                totalSegundos = segundosSinRegistrar,
                esSinRegistrar = true,
                esCategoriaInicial = false
            )

            return ResumenSemanal(
                lunesInicio = lunesInicio,
                domingoFin = domingoFin,
                lunesSiguienteExclusivo = lunesSiguiente,
                filasCategorias = filasCategorias,
                filaSinRegistrar = filaSinRegistrar,
                totalSegundosRegistrados = totalSegundosRegistrados,
                excedeLimiteSemanal = excedeLimite,
                excesoSegundos = excesoSegundos
            )
        }

        /**
         * Formatea una cantidad de segundos a horas y minutos legibles:
         * - `0` -> `"0 h 00 min"`
         * - `75600` (21 h) -> `"21 h 00 min"`
         * - `604800` (168 h) -> `"168 h 00 min"`
         * - Si existen segundos sueltos (ej. un evento corto de prueba de 15 s o 1 min 20 s):
         *   - `< 60 s`: `"0 h 00 min (15 s)"`
         *   - `>= 60 s con s > 0`: `"0 h 01 min 20 s"`
         */
        fun formatearSegundosAHorasYMinutos(totalSegundos: Long): String {
            val seg = totalSegundos.coerceAtLeast(0L)
            if (seg == 0L) return "0 h 00 min"

            val horas = seg / 3600
            val minutos = (seg % 3600) / 60
            val segundos = seg % 60

            val minPadded = String.format(Locale.US, "%02d", minutos)
            return when {
                horas == 0L && minutos == 0L -> "0 h 00 min (${segundos} s)"
                segundos == 0L -> "$horas h $minPadded min"
                else -> "$horas h $minPadded min ${segundos} s"
            }
        }
    }
}

