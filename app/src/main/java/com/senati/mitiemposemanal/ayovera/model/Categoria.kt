package com.senati.mitiemposemanal.ayovera.model

/**
 * Entidad de dominio para la tabla `categorias` (HU-01).
 */
data class Categoria(
    val id: Long,
    val nombre: String,
    val cantidadTareas: Int = 0
)

