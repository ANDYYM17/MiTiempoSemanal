package com.senati.mitiemposemanal.ayovera.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Entidad de dominio para la tabla `eventos` (Sprint 2 — HU-05 e Historial).
 *
 * Almacena las marcas de tiempo completas (día, mes, año, hora, minutos y segundos) en formato ISO-8601
 * y calcula duraciones reales mediante `java.time.LocalDateTime` y `java.time.Duration`.
 */
data class Evento(
    val id: Long,
    val tareaId: Long,
    val tareaNombre: String,
    val categoriaId: Long,
    val categoriaNombre: String,
    val fechaInicioIso: String,
    val fechaFinIso: String?,
    val duracionSegundos: Long?,
    val estado: String = if (fechaFinIso == null) ESTADO_EN_CURSO else ESTADO_FINALIZADO
) {
    val enCurso: Boolean
        get() = estado == ESTADO_EN_CURSO || fechaFinIso == null

    val textoTareaConCategoria: String
        get() = "$tareaNombre · $categoriaNombre"

    fun fechaInicioLocalDateTime(): LocalDateTime = parsearIso(fechaInicioIso)

    fun fechaFinLocalDateTime(): LocalDateTime? = fechaFinIso?.let { parsearIso(it) }

    /**
     * Calcula los segundos transcurridos:
     * - Si el evento ya finalizó, devuelve `duracionSegundos` (o la diferencia entre inicio y fin).
     * - Si el evento sigue en curso, calcula la diferencia entre `fecha_inicio` y `ahora`.
     */
    fun obtenerSegundosTranscurridos(ahora: LocalDateTime = LocalDateTime.now()): Long {
        if (!enCurso) {
            if (duracionSegundos != null) return duracionSegundos.coerceAtLeast(0L)
            val fin = fechaFinLocalDateTime() ?: return 0L
            return calcularDuracionSegundos(fechaInicioLocalDateTime(), fin)
        }
        return calcularDuracionSegundos(fechaInicioLocalDateTime(), ahora)
    }

    fun formatearFechaInicioCompleta(hoy: LocalDate = LocalDate.now()): String {
        val inicio = fechaInicioLocalDateTime()
        val fecha = inicio.toLocalDate()
        val mesNombre = MESES_ES[fecha.monthValue - 1]
        return when (fecha) {
            hoy -> "Hoy, ${fecha.dayOfMonth} $mesNombre ${fecha.year}"
            hoy.minusDays(1) -> "Ayer, ${fecha.dayOfMonth} $mesNombre ${fecha.year}"
            else -> {
                val diaSemana = DIAS_SEMANA_ES[fecha.dayOfWeek.value - 1]
                "$diaSemana, ${fecha.dayOfMonth} $mesNombre ${fecha.year}"
            }
        }
    }

    fun formatearHoraInicioConSegundos(): String {
        return fechaInicioLocalDateTime().format(FORMATTER_HORA_SEGUNDOS)
    }

    fun formatearHoraInicioCorta(): String {
        return fechaInicioLocalDateTime().format(FORMATTER_HORA_CORTA)
    }

    fun formatearHoraFinConSegundos(): String {
        val fin = fechaFinLocalDateTime() ?: return "En curso"
        return fin.format(FORMATTER_HORA_SEGUNDOS)
    }

    fun formatearHoraFinCorta(): String {
        val fin = fechaFinLocalDateTime() ?: return "En curso"
        return fin.format(FORMATTER_HORA_CORTA)
    }

    fun formatearRangoHorario(): String {
        val inicioStr = formatearHoraInicioConSegundos()
        val finStr = fechaFinLocalDateTime()?.format(FORMATTER_HORA_SEGUNDOS) ?: "En curso"
        return "$inicioStr - $finStr"
    }

    fun formatearDetalleFechasHistorial(): String {
        val inicioStr = fechaInicioLocalDateTime().format(FORMATTER_FECHA_HORA_HISTORIAL)
        val finStr = fechaFinLocalDateTime()?.format(FORMATTER_FECHA_HORA_HISTORIAL)
            ?: "En curso (sin finalizar)"
        return "Inicio: $inicioStr | Fin: $finStr"
    }

    fun formatearResumenReciente(hoy: LocalDate = LocalDate.now()): String {
        val inicio = fechaInicioLocalDateTime()
        val prefijoDia = when (inicio.toLocalDate()) {
            hoy -> "Hoy"
            hoy.minusDays(1) -> "Ayer"
            else -> inicio.format(FORMATTER_FECHA_CORTA)
        }
        val horaIni = inicio.format(FORMATTER_HORA_CORTA)
        val horaFin = fechaFinLocalDateTime()?.format(FORMATTER_HORA_CORTA) ?: "En curso"
        return "$prefijoDia $horaIni - $horaFin"
    }

    fun formatearDuracion(ahora: LocalDateTime = LocalDateTime.now()): String {
        return formatearDuracionLegible(obtenerSegundosTranscurridos(ahora))
    }

    companion object {
        const val ESTADO_EN_CURSO = "EN_CURSO"
        const val ESTADO_FINALIZADO = "FINALIZADO"

        private val FORMATTER_ISO_SECONDS: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

        private val FORMATTER_HORA_SEGUNDOS: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)

        private val FORMATTER_HORA_CORTA: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm", Locale.US)

        private val FORMATTER_FECHA_CORTA: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US)

        private val FORMATTER_FECHA_HORA_HISTORIAL: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.US)

        private val MESES_ES = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )

        private val DIAS_SEMANA_ES = listOf(
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
        )

        /**
         * Convierte un `LocalDateTime` a cadena ISO-8601 con precisión de segundos (`yyyy-MM-dd'T'HH:mm:ss`).
         */
        fun formatearIso(dateTime: LocalDateTime): String {
            return dateTime.truncatedTo(ChronoUnit.SECONDS).format(FORMATTER_ISO_SECONDS)
        }

        /**
         * Parsea una cadena ISO-8601 almacenada en SQLite a `LocalDateTime`.
         */
        fun parsearIso(isoString: String): LocalDateTime {
            return try {
                LocalDateTime.parse(isoString.trim(), FORMATTER_ISO_SECONDS)
            } catch (_: Exception) {
                LocalDateTime.parse(isoString.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .truncatedTo(ChronoUnit.SECONDS)
            }
        }

        /**
         * Calcula la duración en segundos entre la fecha/hora de inicio y la de fin usando `java.time.Duration`.
         */
        fun calcularDuracionSegundos(inicio: LocalDateTime, fin: LocalDateTime): Long {
            return Duration.between(inicio, fin).seconds.coerceAtLeast(0L)
        }

        /**
         * Formato de cronómetro digital `HH:MM:SS` (ej. `00:42:21`).
         */
        fun formatearCronometro(totalSegundos: Long): String {
            val seg = totalSegundos.coerceAtLeast(0L)
            val horas = seg / 3600
            val minutos = (seg % 3600) / 60
            val segundos = seg % 60
            return String.format(Locale.US, "%02d:%02d:%02d", horas, minutos, segundos)
        }

        /**
         * Formato legible para historial y confirmación (ej. `1 h 25 min`, `4 min 12 s`, `38 s`).
         */
        fun formatearDuracionLegible(totalSegundos: Long): String {
            val seg = totalSegundos.coerceAtLeast(0L)
            val horas = seg / 3600
            val minutos = (seg % 3600) / 60
            val segundos = seg % 60

            return when {
                horas > 0 && minutos > 0 -> "$horas h $minutos min"
                horas > 0 -> "$horas h"
                minutos > 0 && segundos > 0 -> "$minutos min $segundos s"
                minutos > 0 -> "$minutos min"
                else -> "$segundos s"
            }
        }
    }
}

