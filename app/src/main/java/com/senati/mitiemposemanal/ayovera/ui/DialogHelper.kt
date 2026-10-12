package com.senati.mitiemposemanal.ayovera.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.senati.mitiemposemanal.ayovera.R
import com.senati.mitiemposemanal.ayovera.model.Categoria
import com.senati.mitiemposemanal.ayovera.model.Tarea

/**
 * Implementa los diálogos modales y Snackbars especificados en la Pantalla 8 de Stitch
 * ("Confirmaciones y Mensajes").
 */
object DialogHelper {

    /**
     * Sección 1 de Pantalla 8: Confirmación destructiva antes de eliminar una categoría sin tareas.
     */
    fun mostrarDialogoConfirmarEliminarCategoria(
        context: Context,
        categoria: Categoria,
        onConfirmar: () -> Unit
    ) {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_confirm_delete_category, null, false)

        val tvMessage = view.findViewById<TextView>(R.id.tvDialogDeleteCatMessage)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnDialogCancelCat)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnDialogConfirmDeleteCat)

        tvMessage.text =
            "¿Estás seguro de que deseas eliminar “${categoria.nombre}”? Esta acción no se puede deshacer."

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(view)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            onConfirmar()
        }

        dialog.show()
    }

    /**
     * Sección 2 de Pantalla 8: Regla de bloqueo cuando una categoría tiene tareas asociadas (HU-01.7).
     */
    fun mostrarDialogoBloqueoCategoriaConTareas(
        context: Context,
        nombreCategoria: String,
        tareasAsociadas: List<Tarea>
    ) {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_blocked_category, null, false)

        val tvBadgeCount = view.findViewById<TextView>(R.id.tvBlockedBadgeCount)
        val tvHeader = view.findViewById<TextView>(R.id.tvBlockedCategoryHeader)
        val tvTasksList = view.findViewById<TextView>(R.id.tvBlockedTasksList)
        val btnUnderstood = view.findViewById<MaterialButton>(R.id.btnBlockedUnderstood)

        val cantidad = tareasAsociadas.size
        tvBadgeCount.text = cantidad.toString()
        val textoCantidad = if (cantidad == 1) "1 tarea asociada" else "$cantidad tareas asociadas"
        tvHeader.text = "La categoría “$nombreCategoria” tiene $textoCantidad:"

        val tareasFormateadas = tareasAsociadas.take(5).joinToString("\n") { tarea ->
            "• “${tarea.nombre}” (${tarea.categoriaNombre})"
        }
        val extra = if (cantidad > 5) "\n• ...y ${cantidad - 5} más" else ""
        tvTasksList.text = tareasFormateadas + extra

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(view)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnUnderstood.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    /**
     * Sección 3 de Pantalla 8: Confirmación antes de eliminar una tarea (HU-02.7).
     */
    fun mostrarDialogoConfirmarEliminarTarea(
        context: Context,
        tarea: Tarea,
        onConfirmar: () -> Unit
    ) {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_confirm_delete_task, null, false)

        val tvMessage = view.findViewById<TextView>(R.id.tvDialogDeleteTaskMessage)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnDialogCancelTask)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnDialogConfirmDeleteTask)

        tvMessage.text =
            "¿Deseas eliminar la tarea “${tarea.nombre}” (${tarea.categoriaNombre})? Se quitará de tu agenda semanal."

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(view)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            onConfirmar()
        }

        dialog.show()
    }

    /**
     * Sección 4 de Pantalla 8: Snackbar M3 de confirmación positiva (verde menta / tertiary).
     */
    fun mostrarSnackbarExito(anchorView: View, mensaje: String) {
        val snackbar = Snackbar.make(anchorView, mensaje, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(
            ContextCompat.getColor(anchorView.context, R.color.tertiary_container)
        )
        snackbar.setTextColor(
            ContextCompat.getColor(anchorView.context, R.color.tertiary_fixed)
        )
        snackbar.show()
    }

    /**
     * Sección 4 de Pantalla 8: Snackbar M3 de error / validación fallida.
     */
    fun mostrarSnackbarError(anchorView: View, mensaje: String) {
        val snackbar = Snackbar.make(anchorView, mensaje, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(
            ContextCompat.getColor(anchorView.context, R.color.inverse_surface)
        )
        snackbar.setTextColor(
            ContextCompat.getColor(anchorView.context, R.color.error_container)
        )
        snackbar.show()
    }
}

