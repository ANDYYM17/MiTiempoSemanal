package com.senati.mitiemposemanal.ayovera.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.senati.mitiemposemanal.ayovera.R
import com.senati.mitiemposemanal.ayovera.data.ResultadoEvento
import com.senati.mitiemposemanal.ayovera.data.ValidationLogic
import com.senati.mitiemposemanal.ayovera.model.Categoria
import com.senati.mitiemposemanal.ayovera.model.Evento
import com.senati.mitiemposemanal.ayovera.model.Tarea

/**
 * Implementa los diálogos modales, hojas inferiores y Snackbars especificados en las Pantallas 8, 12 y 14 de Stitch.
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
     * Pantalla 12 de Stitch (HU-04): Hoja inferior modal para crear una tarea que no existe
     * desde la pantalla Eventos, elegir una categoría existente e iniciar inmediatamente su evento.
     */
    fun mostrarHojaCrearTareaDesdeEventos(
        context: Context,
        nombreInicial: String,
        categorias: List<Categoria>,
        onCrearEIniciar: (nombreInput: String, categoriaIdSeleccionada: Long?) -> ResultadoEvento,
        onExito: (Evento, String) -> Unit
    ) {
        val bottomSheet = BottomSheetDialog(context)
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_create_task_from_event, null, false)
        bottomSheet.setContentView(view)

        val llInputBox = view.findViewById<LinearLayout>(R.id.llBottomSheetTaskNameInputBox)
        val etTaskName = view.findViewById<EditText>(R.id.etBottomSheetTaskName)
        val tvTaskHelper = view.findViewById<TextView>(R.id.tvBottomSheetTaskHelper)
        val tvSelectedCount = view.findViewById<TextView>(R.id.tvBottomSheetSelectedCount)
        val llCategoriesContainer = view.findViewById<LinearLayout>(R.id.llBottomSheetCategoriesContainer)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnBottomSheetConfirmCreateAndStart)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnBottomSheetCancel)

        val nombreLimpioInicial = ValidationLogic.normalizarNombre(nombreInicial)
        etTaskName.setText(nombreLimpioInicial)
        etTaskName.setSelection(etTaskName.text.length)

        var categoriaIdSeleccionada: Long? = categorias.firstOrNull()?.id

        fun renderizarGridCategorias() {
            llCategoriesContainer.removeAllViews()
            val density = context.resources.displayMetrics.density
            val total = categorias.size
            val selIndex = categorias.indexOfFirst { it.id == categoriaIdSeleccionada }
            tvSelectedCount.text = if (selIndex >= 0) {
                "1 de $total seleccionada"
            } else {
                "0 de $total seleccionada"
            }

            categorias.chunked(2).forEach { par ->
                val fila = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = (8 * density).toInt()
                    }
                }

                par.forEachIndexed { idx, cat ->
                    val seleccionada = cat.id == categoriaIdSeleccionada
                    val estilo = CategoryVisualStyle.obtenerEstilo(cat.nombre)

                    val card = MaterialCardView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        ).apply {
                            if (idx == 0 && par.size == 2) {
                                marginEnd = (4 * density).toInt()
                            } else if (idx == 1) {
                                marginStart = (4 * density).toInt()
                            }
                        }
                        radius = 12 * density
                        cardElevation = 1 * density
                        isClickable = true
                        isFocusable = true
                        if (seleccionada) {
                            setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_fixed))
                            strokeColor = ContextCompat.getColor(context, R.color.primary)
                            strokeWidth = (1.5f * density).toInt()
                        } else {
                            setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_container_low))
                            strokeColor = ContextCompat.getColor(context, R.color.outline_variant)
                            strokeWidth = (1 * density).toInt()
                        }
                    }

                    val inner = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        val pad = (10 * density).toInt()
                        setPadding(pad, pad, pad, pad)
                    }

                    val topRow = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val iconBox = FrameLayout(context).apply {
                        val size = (30 * density).toInt()
                        layoutParams = LinearLayout.LayoutParams(size, size)
                        setBackgroundResource(R.drawable.bg_rounded_12)
                        backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                context,
                                if (seleccionada) R.color.surface_container_lowest else R.color.surface_container_highest
                            )
                        )
                    }

                    val iv = ImageView(context).apply {
                        val s = (17 * density).toInt()
                        layoutParams = FrameLayout.LayoutParams(s, s, Gravity.CENTER)
                        setImageResource(estilo.iconRes)
                        imageTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                context,
                                if (seleccionada) R.color.primary else estilo.accentColorRes
                            )
                        )
                    }
                    iconBox.addView(iv)

                    val spacer = View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                    }

                    val checkCircle = FrameLayout(context).apply {
                        val s = (20 * density).toInt()
                        layoutParams = LinearLayout.LayoutParams(s, s)
                        setBackgroundResource(R.drawable.bg_circle)
                        backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                context,
                                if (seleccionada) R.color.primary else R.color.surface_container_highest
                            )
                        )
                    }

                    if (seleccionada) {
                        val ivCheck = ImageView(context).apply {
                            val s = (12 * density).toInt()
                            layoutParams = FrameLayout.LayoutParams(s, s, Gravity.CENTER)
                            setImageResource(R.drawable.ic_check)
                            imageTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(context, R.color.on_primary)
                            )
                        }
                        checkCircle.addView(ivCheck)
                    }

                    topRow.addView(iconBox)
                    topRow.addView(spacer)
                    topRow.addView(checkCircle)

                    val tvTitle = TextView(context).apply {
                        text = cat.nombre
                        textSize = 13f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(
                            ContextCompat.getColor(
                                context,
                                if (seleccionada) R.color.on_primary_fixed else R.color.on_surface
                            )
                        )
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = (6 * density).toInt()
                        }
                    }

                    val tvSub = TextView(context).apply {
                        text = estilo.descripcionBreve
                        textSize = 10f
                        maxLines = 1
                        setTextColor(
                            ContextCompat.getColor(
                                context,
                                if (seleccionada) R.color.on_primary_fixed_variant else R.color.on_surface_variant
                            )
                        )
                    }

                    inner.addView(topRow)
                    inner.addView(tvTitle)
                    inner.addView(tvSub)
                    card.addView(inner)

                    card.setOnClickListener {
                        categoriaIdSeleccionada = cat.id
                        renderizarGridCategorias()
                    }

                    fila.addView(card)
                }

                if (par.size == 1) {
                    fila.addView(
                        View(context).apply {
                            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                        }
                    )
                }

                llCategoriesContainer.addView(fila)
            }
        }

        renderizarGridCategorias()

        btnCancel.setOnClickListener {
            // Requisito HU-04.9: Si el usuario cancela, no se crea la tarea ni se inicia el evento
            bottomSheet.dismiss()
        }

        btnConfirm.setOnClickListener {
            val textoActual = etTaskName.text?.toString().orEmpty()
            when (val res = onCrearEIniciar(textoActual, categoriaIdSeleccionada)) {
                is ResultadoEvento.Exito -> {
                    bottomSheet.dismiss()
                    onExito(res.evento, res.mensaje)
                }
                is ResultadoEvento.Error -> {
                    llInputBox.setBackgroundResource(R.drawable.bg_input_error)
                    tvTaskHelper.text = res.mensaje
                    tvTaskHelper.setTextColor(ContextCompat.getColor(context, R.color.error))
                }
            }
        }

        bottomSheet.show()
    }

    /**
     * Pantalla 14 de Stitch (HU-05): Diálogo M3 de confirmación al finalizar un evento con éxito.
     * Muestra tarea, categoría, fecha, hora de inicio, hora de fin y duración calculada.
     */
    fun mostrarDialogoConfirmacionEventoFinalizado(
        context: Context,
        eventoFinalizado: Evento,
        onAceptar: () -> Unit = {}
    ) {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_event_finished, null, false)

        val tvTaskName = view.findViewById<TextView>(R.id.tvFinishedTaskName)
        val llCategoryBadge = view.findViewById<LinearLayout>(R.id.llFinishedCategoryBadge)
        val ivCategoryIcon = view.findViewById<ImageView>(R.id.ivFinishedCategoryIcon)
        val tvCategoryName = view.findViewById<TextView>(R.id.tvFinishedCategoryName)
        val tvDate = view.findViewById<TextView>(R.id.tvFinishedDate)
        val tvStartTime = view.findViewById<TextView>(R.id.tvFinishedStartTime)
        val tvEndTime = view.findViewById<TextView>(R.id.tvFinishedEndTime)
        val tvDuration = view.findViewById<TextView>(R.id.tvFinishedDuration)
        val btnAccept = view.findViewById<MaterialButton>(R.id.btnFinishedAccept)

        val estilo = CategoryVisualStyle.obtenerEstilo(eventoFinalizado.categoriaNombre)
        val badgeBgColor = ContextCompat.getColor(context, estilo.badgeBgColorRes)
        val badgeTextColor = ContextCompat.getColor(context, estilo.badgeTextColorRes)

        tvTaskName.text = eventoFinalizado.tareaNombre
        llCategoryBadge.backgroundTintList = ColorStateList.valueOf(badgeBgColor)
        ivCategoryIcon.setImageResource(estilo.iconRes)
        ivCategoryIcon.imageTintList = ColorStateList.valueOf(badgeTextColor)
        tvCategoryName.text = eventoFinalizado.categoriaNombre
        tvCategoryName.setTextColor(badgeTextColor)

        tvDate.text = eventoFinalizado.formatearFechaInicioCompleta()
        tvStartTime.text = eventoFinalizado.formatearHoraInicioConSegundos()
        tvEndTime.text = eventoFinalizado.formatearHoraFinConSegundos()
        tvDuration.text = eventoFinalizado.formatearDuracion()

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(view)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnAccept.setOnClickListener {
            dialog.dismiss()
            onAceptar()
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
