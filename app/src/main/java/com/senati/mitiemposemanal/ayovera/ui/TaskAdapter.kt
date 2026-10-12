package com.senati.mitiemposemanal.ayovera.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.senati.mitiemposemanal.ayovera.R
import com.senati.mitiemposemanal.ayovera.model.Tarea

/**
 * Adaptador RecyclerView para la lista de tareas en Pantalla 5 ("Mis Tareas") y vista previa en Inicio.
 * Muestra cada tarea con su categoría asociada en el formato requerido por HU-02.5:
 * Ejemplo: "Leer Kotlin · Estudio"
 */
class TaskAdapter(
    private var tareas: List<Tarea>,
    private val onEditarClick: (Tarea) -> Unit,
    private val onEliminarClick: (Tarea) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    fun actualizarDatos(nuevasTareas: List<Tarea>) {
        tareas = nuevasTareas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tareas[position])
    }

    override fun getItemCount(): Int = tareas.size

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewStripe: View = itemView.findViewById(R.id.viewTaskStripe)
        private val llBadge: LinearLayout = itemView.findViewById(R.id.llTaskCategoryBadge)
        private val ivCatIcon: ImageView = itemView.findViewById(R.id.ivTaskCategoryIcon)
        private val tvCatChip: TextView = itemView.findViewById(R.id.tvTaskCategoryChip)
        private val tvTaskCode: TextView = itemView.findViewById(R.id.tvTaskCode)
        private val tvTitleWithCategory: TextView = itemView.findViewById(R.id.tvTaskTitleWithCategory)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvTaskSubtitle)
        private val viewStatusDot: View = itemView.findViewById(R.id.viewTaskStatusDot)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditTask)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteTask)

        fun bind(tarea: Tarea) {
            val context = itemView.context
            val estilo = CategoryVisualStyle.obtenerEstilo(tarea.categoriaNombre)

            val accentColor = ContextCompat.getColor(context, estilo.accentColorRes)
            val badgeBgColor = ContextCompat.getColor(context, estilo.badgeBgColorRes)
            val badgeTextColor = ContextCompat.getColor(context, estilo.badgeTextColorRes)

            viewStripe.setBackgroundColor(accentColor)
            llBadge.backgroundTintList = ColorStateList.valueOf(badgeBgColor)
            ivCatIcon.setImageResource(estilo.iconRes)
            ivCatIcon.imageTintList = ColorStateList.valueOf(badgeTextColor)
            tvCatChip.text = tarea.categoriaNombre
            tvCatChip.setTextColor(badgeTextColor)

            tvTaskCode.text = "ID: TSK-${tarea.id.toString().padStart(3, '0')}"

            // Formato obligatorio HU-02 punto 5: "Leer Kotlin · Estudio"
            tvTitleWithCategory.text = tarea.textoConCategoria
            tvSubtitle.text = "Categoría asociada: ${tarea.categoriaNombre} · ${estilo.descripcionBreve}"

            viewStatusDot.backgroundTintList = ColorStateList.valueOf(accentColor)

            btnEdit.setOnClickListener { onEditarClick(tarea) }
            btnDelete.setOnClickListener { onEliminarClick(tarea) }
            itemView.setOnClickListener { onEditarClick(tarea) }
        }
    }
}

