package com.senati.mitiemposemanal.ayovera.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.senati.mitiemposemanal.ayovera.R
import com.senati.mitiemposemanal.ayovera.model.Evento

/**
 * Adaptador RecyclerView para el Historial de Eventos (Pantalla 10 y Pantalla 15 de Stitch).
 * Muestra tarea, categoría, fecha/hora de inicio, fecha/hora de fin (cuando exista), duración y estado.
 */
class EventHistoryAdapter(
    private var eventos: List<Evento>,
    private val onEventoClick: ((Evento) -> Unit)? = null
) : RecyclerView.Adapter<EventHistoryAdapter.EventViewHolder>() {

    fun actualizarDatos(nuevosEventos: List<Evento>) {
        eventos = nuevosEventos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_history, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(eventos[position])
    }

    override fun getItemCount(): Int = eventos.size

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewStripe: View = itemView.findViewById(R.id.viewEventStripe)
        private val tvTaskTitle: TextView = itemView.findViewById(R.id.tvEventTaskTitle)
        private val tvCategoryChip: TextView = itemView.findViewById(R.id.tvEventCategoryChip)
        private val tvTaskWithCategorySub: TextView = itemView.findViewById(R.id.tvEventTaskWithCategorySub)
        private val llStatusBadge: LinearLayout = itemView.findViewById(R.id.llEventStatusBadge)
        private val ivStatusIcon: ImageView = itemView.findViewById(R.id.ivEventStatusIcon)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tvEventStatusBadge)
        private val tvTimeRange: TextView = itemView.findViewById(R.id.tvEventTimeRange)
        private val tvFullDates: TextView = itemView.findViewById(R.id.tvEventFullDates)
        private val tvDurationValue: TextView = itemView.findViewById(R.id.tvEventDurationValue)
        private val tvDurationLabel: TextView = itemView.findViewById(R.id.tvEventDurationLabel)

        fun bind(evento: Evento) {
            val context = itemView.context
            val estilo = CategoryVisualStyle.obtenerEstilo(evento.categoriaNombre)

            val accentColor = ContextCompat.getColor(context, estilo.accentColorRes)
            val badgeBgColor = ContextCompat.getColor(context, estilo.badgeBgColorRes)
            val badgeTextColor = ContextCompat.getColor(context, estilo.badgeTextColorRes)

            viewStripe.setBackgroundColor(accentColor)
            tvTaskTitle.text = evento.tareaNombre
            tvCategoryChip.text = evento.categoriaNombre
            tvCategoryChip.backgroundTintList = ColorStateList.valueOf(badgeBgColor)
            tvCategoryChip.setTextColor(badgeTextColor)
            tvTaskWithCategorySub.text = evento.textoTareaConCategoria

            if (evento.enCurso) {
                llStatusBadge.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.primary_fixed)
                )
                ivStatusIcon.setImageResource(R.drawable.ic_timer)
                ivStatusIcon.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.on_primary_fixed)
                )
                tvStatusBadge.text = "En curso"
                tvStatusBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.on_primary_fixed)
                )
                tvDurationLabel.text = "Tiempo activo"
            } else {
                llStatusBadge.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.secondary_container)
                )
                ivStatusIcon.setImageResource(R.drawable.ic_check)
                ivStatusIcon.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.on_secondary_container)
                )
                tvStatusBadge.text = "Finalizado"
                tvStatusBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.on_secondary_container)
                )
                tvDurationLabel.text = "Duración"
            }

            tvTimeRange.text = evento.formatearRangoHorario()
            tvFullDates.text = evento.formatearDetalleFechasHistorial()
            tvDurationValue.text = evento.formatearDuracion()

            itemView.setOnClickListener {
                onEventoClick?.invoke(evento)
            }
        }
    }
}

