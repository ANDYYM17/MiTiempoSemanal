package com.senati.mitiemposemanal.ayovera.ui

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.senati.mitiemposemanal.ayovera.R

data class EstiloCategoria(
    @param:ColorRes val accentColorRes: Int,
    @param:ColorRes val badgeBgColorRes: Int,
    @param:ColorRes val badgeTextColorRes: Int,
    @param:DrawableRes val iconRes: Int,
    val descripcionBreve: String
)

/**
 * Asocia cada categoría con los tokens visuales de "SENATI Academic Cadence" en Stitch.
 */
object CategoryVisualStyle {

    fun obtenerEstilo(nombreCategoria: String): EstiloCategoria {
        return when (nombreCategoria.trim().lowercase()) {
            "estudio" -> EstiloCategoria(
                accentColorRes = R.color.cat_estudio_accent,
                badgeBgColorRes = R.color.primary_fixed,
                badgeTextColorRes = R.color.primary,
                iconRes = R.drawable.ic_school,
                descripcionBreve = "Talleres & Algoritmos"
            )
            "trabajo" -> EstiloCategoria(
                accentColorRes = R.color.cat_trabajo_accent,
                badgeBgColorRes = R.color.secondary_fixed,
                badgeTextColorRes = R.color.on_secondary_fixed_variant,
                iconRes = R.drawable.ic_work,
                descripcionBreve = "Turnos y Prácticas"
            )
            "descanso" -> EstiloCategoria(
                accentColorRes = R.color.cat_descanso_accent,
                badgeBgColorRes = R.color.tertiary_fixed,
                badgeTextColorRes = R.color.on_tertiary_fixed_variant,
                iconRes = R.drawable.ic_bedtime,
                descripcionBreve = "Sueño y Bienestar"
            )
            "ocio" -> EstiloCategoria(
                accentColorRes = R.color.cat_ocio_accent,
                badgeBgColorRes = R.color.primary_fixed_dim,
                badgeTextColorRes = R.color.primary_container,
                iconRes = R.drawable.ic_esports,
                descripcionBreve = "Tiempo personal"
            )
            else -> EstiloCategoria(
                accentColorRes = R.color.secondary,
                badgeBgColorRes = R.color.secondary_container,
                badgeTextColorRes = R.color.on_secondary_container,
                iconRes = R.drawable.ic_label,
                descripcionBreve = "Categoría personalizada"
            )
        }
    }
}

