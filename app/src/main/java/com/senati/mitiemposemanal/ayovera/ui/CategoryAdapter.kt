package com.senati.mitiemposemanal.ayovera.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.senati.mitiemposemanal.ayovera.R
import com.senati.mitiemposemanal.ayovera.model.Categoria

/**
 * Adaptador RecyclerView para la lista de categorías en Pantalla 2 ("Mis Categorías").
 */
class CategoryAdapter(
    private var categorias: List<Categoria>,
    private val onEditarClick: (Categoria) -> Unit,
    private val onEliminarClick: (Categoria) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    fun actualizarDatos(nuevasCategorias: List<Categoria>) {
        categorias = nuevasCategorias
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categorias[position])
    }

    override fun getItemCount(): Int = categorias.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewStripe: View = itemView.findViewById(R.id.viewCategoryStripe)
        private val flIconBg: FrameLayout = itemView.findViewById(R.id.flCategoryIconBg)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        private val tvName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val viewDot: View = itemView.findViewById(R.id.viewCategoryDot)
        private val tvTaskCount: TextView = itemView.findViewById(R.id.tvCategoryTaskCount)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditCategory)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteCategory)

        fun bind(categoria: Categoria) {
            val context = itemView.context
            val estilo = CategoryVisualStyle.obtenerEstilo(categoria.nombre)

            val accentColor = ContextCompat.getColor(context, estilo.accentColorRes)
            val badgeBgColor = ContextCompat.getColor(context, estilo.badgeBgColorRes)
            val badgeTextColor = ContextCompat.getColor(context, estilo.badgeTextColorRes)

            viewStripe.setBackgroundColor(accentColor)
            flIconBg.backgroundTintList = ColorStateList.valueOf(badgeBgColor)
            ivIcon.setImageResource(estilo.iconRes)
            ivIcon.imageTintList = ColorStateList.valueOf(badgeTextColor)

            tvName.text = categoria.nombre
            viewDot.backgroundTintList = ColorStateList.valueOf(accentColor)

            val count = categoria.cantidadTareas
            tvTaskCount.text = if (count == 1) {
                "1 tarea asignada"
            } else {
                "$count tareas asignadas"
            }

            // Si tiene tareas asociadas, el botón de eliminar se muestra en tono protegido pero sigue activo
            // para disparar la explicación de bloqueo (HU-01.7)
            val deleteTint = if (count > 0) {
                ContextCompat.getColor(context, R.color.outline)
            } else {
                ContextCompat.getColor(context, R.color.error)
            }
            btnDelete.imageTintList = ColorStateList.valueOf(deleteTint)

            btnEdit.setOnClickListener { onEditarClick(categoria) }
            btnDelete.setOnClickListener { onEliminarClick(categoria) }
            itemView.setOnClickListener { onEditarClick(categoria) }
        }
    }
}

