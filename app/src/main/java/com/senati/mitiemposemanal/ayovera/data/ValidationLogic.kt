package com.senati.mitiemposemanal.ayovera.data

/**
 * Reglas de normalización y validación de nombres para categorías (HU-01) y tareas (HU-02).
 */
object ValidationLogic {

    val CATEGORIAS_INICIALES = listOf("Estudio", "Trabajo", "Descanso", "Ocio")

    /**
     * Elimina espacios al inicio y al final y colapsa múltiples espacios internos consecutivos.
     */
    fun normalizarNombre(raw: String): String {
        return raw.trim().replace(Regex("\\s+"), " ")
    }

    /**
     * Compara dos nombres de forma consistente ignorando mayúsculas/minúsculas y espacios extra.
     */
    fun sonNombresEquivalentes(nombreA: String, nombreB: String): Boolean {
        return normalizarNombre(nombreA).equals(normalizarNombre(nombreB), ignoreCase = true)
    }

    /**
     * Valida el nombre de una categoría antes de guardar o actualizar (HU-01).
     * @return null si es válido, o el mensaje de error claro si es inválido.
     */
    fun validarNombreCategoria(
        nombreRaw: String,
        nombresExistentes: List<String>,
        nombreActualEnEdicion: String? = null
    ): String? {
        val limpio = normalizarNombre(nombreRaw)
        if (limpio.isEmpty()) {
            return "El nombre de la categoría no puede estar vacío."
        }
        val duplicado = nombresExistentes.any { existente ->
            val esMismoQueEditado = nombreActualEnEdicion != null &&
                sonNombresEquivalentes(existente, nombreActualEnEdicion)
            !esMismoQueEditado && sonNombresEquivalentes(existente, limpio)
        }
        if (duplicado) {
            return "Ya existe una categoría con el nombre “$limpio”. Elige un nombre único."
        }
        return null
    }

    /**
     * Valida el nombre y la categoría de una tarea antes de guardar o actualizar (HU-02).
     * @return null si es válido, o el mensaje de error claro si es inválido.
     */
    fun validarTarea(
        nombreRaw: String,
        categoriaId: Long?,
        nombresTareasExistentes: List<String>,
        nombreActualEnEdicion: String? = null
    ): String? {
        val limpio = normalizarNombre(nombreRaw)
        if (limpio.isEmpty()) {
            return "El nombre de la tarea es obligatorio y no puede estar vacío."
        }
        if (categoriaId == null || categoriaId <= 0L) {
            return "Debes seleccionar una categoría para continuar."
        }
        val duplicado = nombresTareasExistentes.any { existente ->
            val esMismaQueEditada = nombreActualEnEdicion != null &&
                sonNombresEquivalentes(existente, nombreActualEnEdicion)
            !esMismaQueEditada && sonNombresEquivalentes(existente, limpio)
        }
        if (duplicado) {
            return "Ya existe una tarea con el nombre “$limpio”."
        }
        return null
    }
}

