package com.senati.mitiemposemanal.ayovera.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.senati.mitiemposemanal.ayovera.R
import com.senati.mitiemposemanal.ayovera.model.Tarea

/**
 * Adaptador para las coincidencias de búsqueda de tareas en tiempo real (Pantalla 11 — HU-03).
 * Muestra cada tarea en formato "Nombre · Categoría" (ej. "Leer Kotlin · Estudio") y resalta la seleccionada.
 */
class EventTaskSearchAdapter(
    private var tareas: List<Tarea>,
    private var tareaSeleccionadaId: Long? = null,
    private val onTareaSeleccionada: (Tarea) -> Unit
) : RecyclerView.Adapter<EventTaskSearchAdapter.MatchViewHolder>() {

    fun actualizarResultados(nuevasTareas: List<Tarea>, idSeleccionado: Long?) {
        tareas = nuevasTareas
        tareaSeleccionadaId = idSeleccionado
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_search_match, parent, false)
        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        holder.bind(tareas[position])
    }

    override fun getItemCount(): Int = tareas.size

    inner class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.cardSearchMatchItem)
        private val flIconBg: FrameLayout = itemView.findViewById(R.id.flSearchMatchIconBg)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivSearchMatchIcon)
        private val tvTitleWithCategory: TextView = itemView.findViewById(R.id.tvSearchMatchTitleWithCategory)
        private val tvCategoryChip: TextView = itemView.findViewById(R.id.tvSearchMatchCategoryChip)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSearchMatchSubtitle)
        private val flSelectBadge: FrameLayout = itemView.findViewById(R.id.flSearchMatchSelectBadge)
        private val ivSelectIcon: ImageView = itemView.findViewById(R.id.ivSearchMatchSelectIcon)

        fun bind(tarea: Tarea) {
            val context = itemView.context
            val estilo = CategoryVisualStyle.obtenerEstilo(tarea.categoriaNombre)
            val seleccionada = tarea.id == tareaSeleccionadaId

            val accentColor = ContextCompat.getColor(context, estilo.accentColorRes)
            val badgeBgColor = ContextCompat.getColor(context, estilo.badgeBgColorRes)
            val badgeTextColor = ContextCompat.getColor(context, estilo.badgeTextColorRes)

            flIconBg.backgroundTintList = ColorStateList.valueOf(badgeBgColor)
            ivIcon.setImageResource(estilo.iconRes)
            ivIcon.imageTintList = ColorStateList.valueOf(badgeTextColor)

            // Requisito HU-03.7: Mostrar cada coincidencia como "Leer Kotlin · Estudio"
            tvTitleWithCategory.text = tarea.textoConCategoria
            tvCategoryChip.text = tarea.categoriaNombre
            tvCategoryChip.backgroundTintList = ColorStateList.valueOf(badgeBgColor)
            tvCategoryChip.setTextColor(badgeTextColor)

            if (seleccionada) {
                card.strokeColor = ContextCompat.getColor(context, R.color.secondary)
                card.strokeWidth = (2 * context.resources.displayMetrics.density).toInt()
                tvSubtitle.text = "Seleccionada · Lista para iniciar cronómetro"
                flSelectBadge.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.secondary)
                )
                ivSelectIcon.setImageResource(R.drawable.ic_check)
                ivSelectIcon.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.on_secondary)
                )
            } else {
                card.strokeColor = ContextCompat.getColor(context, R.color.outline_variant)
                card.strokeWidth = (1 * context.resources.displayMetrics.density).toInt()
                tvSubtitle.text = "Categoría: ${tarea.categoriaNombre} · ${estilo.descripcionBreve}"
                flSelectBadge.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.surface_container)
                )
                ivSelectIcon.setImageResource(R.drawable.ic_arrow_forward)
                ivSelectIcon.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.on_surface_variant)
                )
            }

            itemView.setOnClickListener {
                onTareaSeleccionada(tarea)
            }
        }
    }
}

