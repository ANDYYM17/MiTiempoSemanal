package com.senati.mitiemposemanal.ayovera.model

/**
 * Entidad de dominio para la tabla `tareas` (HU-02).
 */
data class Tarea(
    val id: Long,
    val nombre: String,
    val categoriaId: Long,
    val categoriaNombre: String
) {
    /**
     * Formato requerido por HU-02 punto 5: "Leer Kotlin · Estudio"
     */
    val textoConCategoria: String
        get() = "$nombre · $categoriaNombre"
}

