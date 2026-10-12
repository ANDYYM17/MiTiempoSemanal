package com.senati.mitiemposemanal.ayovera

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.senati.mitiemposemanal.ayovera.data.AdminSQLiteOpenHelper
import com.senati.mitiemposemanal.ayovera.data.ResultadoOperacion
import com.senati.mitiemposemanal.ayovera.data.ValidationLogic
import com.senati.mitiemposemanal.ayovera.model.Categoria
import com.senati.mitiemposemanal.ayovera.model.Tarea
import com.senati.mitiemposemanal.ayovera.ui.DialogHelper

/**
 * Actividad para Crear Tarea (Pantalla 6 de Stitch) y Editar Tarea (Pantalla 7 de Stitch).
 * Cumple los requisitos de HU-02:
 * - Crear tareas seleccionando una categoría existente.
 * - Impedir guardar tareas sin nombre.
 * - Impedir nombres duplicados de tareas de forma consistente.
 * - Permitir editar el nombre y la categoría de una tarea.
 * - Permitir eliminar tareas con confirmación.
 */
class TaskFormActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_PRE_SELECTED_CATEGORY_ID = "extra_pre_selected_category_id"
        const val EXTRA_FEEDBACK_MESSAGE = "extra_feedback_message"
    }

    private lateinit var dbHelper: AdminSQLiteOpenHelper
    private var taskId: Long = -1L
    private var tareaEnEdicion: Tarea? = null
    private var categoriasDisponibles: List<Categoria> = emptyList()
    private var categoriaSeleccionada: Categoria? = null

    private lateinit var rootView: View
    private lateinit var tvToolbarTitle: TextView
    private lateinit var llCreateTaskHeader: LinearLayout
    private lateinit var llEditTaskHeader: LinearLayout
    private lateinit var tvEditTaskIdBadge: TextView

    private lateinit var llTaskNameInputBox: LinearLayout
    private lateinit var ivTaskNameIcon: ImageView
    private lateinit var etTaskName: EditText
    private lateinit var btnClearTaskName: ImageButton
    private lateinit var tvTaskCharCounter: TextView
    private lateinit var llTaskNameError: LinearLayout
    private lateinit var tvTaskNameError: TextView

    private lateinit var llCategorySelectorBox: LinearLayout
    private lateinit var spinnerTaskCategory: Spinner
    private lateinit var llTaskCategoryError: LinearLayout
    private lateinit var tvTaskCategoryError: TextView
    private lateinit var tvActiveCategoryChip: TextView
    private lateinit var llQuickCategoryChips: LinearLayout
    private lateinit var tvTaskFormatPreview: TextView

    private lateinit var btnSaveTask: MaterialButton
    private lateinit var btnCancelTask: MaterialButton
    private lateinit var btnDeleteTaskInEdit: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task_form)

        rootView = findViewById(R.id.rootTaskForm)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = AdminSQLiteOpenHelper(this)
        dbHelper.asegurarCategoriasInicialesSiVacio()

        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)

        inicializarVistas()
        cargarCategoriasYModo()
        configurarEventos()
    }

    private fun inicializarVistas() {
        tvToolbarTitle = findViewById(R.id.tvTaskFormToolbarTitle)
        llCreateTaskHeader = findViewById(R.id.llCreateTaskHeader)
        llEditTaskHeader = findViewById(R.id.llEditTaskHeader)
        tvEditTaskIdBadge = findViewById(R.id.tvEditTaskIdBadge)

        llTaskNameInputBox = findViewById(R.id.llTaskNameInputBox)
        ivTaskNameIcon = findViewById(R.id.ivTaskNameIcon)
        etTaskName = findViewById(R.id.etTaskName)
        btnClearTaskName = findViewById(R.id.btnClearTaskName)
        tvTaskCharCounter = findViewById(R.id.tvTaskCharCounter)
        llTaskNameError = findViewById(R.id.llTaskNameError)
        tvTaskNameError = findViewById(R.id.tvTaskNameError)

        llCategorySelectorBox = findViewById(R.id.llCategorySelectorBox)
        spinnerTaskCategory = findViewById(R.id.spinnerTaskCategory)
        llTaskCategoryError = findViewById(R.id.llTaskCategoryError)
        tvTaskCategoryError = findViewById(R.id.tvTaskCategoryError)
        tvActiveCategoryChip = findViewById(R.id.tvActiveCategoryChip)
        llQuickCategoryChips = findViewById(R.id.llQuickCategoryChips)
        tvTaskFormatPreview = findViewById(R.id.tvTaskFormatPreview)

        btnSaveTask = findViewById(R.id.btnSaveTask)
        btnCancelTask = findViewById(R.id.btnCancelTask)
        btnDeleteTaskInEdit = findViewById(R.id.btnDeleteTaskInEdit)
    }

    private fun cargarCategoriasYModo() {
        categoriasDisponibles = dbHelper.listarCategorias()

        // Construir lista del Spinner (en modo crear el índice 0 invita a seleccionar categoría)
        val opcionesSpinner = mutableListOf<String>()
        opcionesSpinner.add("Elige una categoría...")
        opcionesSpinner.addAll(categoriasDisponibles.map { it.nombre })

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opcionesSpinner
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerTaskCategory.adapter = spinnerAdapter

        spinnerTaskCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                categoriaSeleccionada = if (position > 0 && position <= categoriasDisponibles.size) {
                    categoriasDisponibles[position - 1]
                } else {
                    null
                }
                if (categoriaSeleccionada != null) {
                    limpiarErrorCategoria()
                }
                actualizarChipsYVistaPrevia()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                categoriaSeleccionada = null
                actualizarChipsYVistaPrevia()
            }
        }

        if (taskId > 0L) {
            tareaEnEdicion = dbHelper.obtenerTareaPorId(taskId)
            val tarea = tareaEnEdicion
            if (tarea != null) {
                // Modo Pantalla 7: Editar Tarea
                tvToolbarTitle.text = getString(R.string.title_edit_task)
                llCreateTaskHeader.visibility = View.GONE
                llEditTaskHeader.visibility = View.VISIBLE
                tvEditTaskIdBadge.text = "ID: TSK-${tarea.id.toString().padStart(3, '0')}"

                btnSaveTask.text = getString(R.string.btn_save_changes)
                btnDeleteTaskInEdit.visibility = View.VISIBLE

                etTaskName.setText(tarea.nombre)
                etTaskName.setSelection(tarea.nombre.length)
                tvTaskCharCounter.text = "${tarea.nombre.length}/50"
                btnClearTaskName.visibility = if (tarea.nombre.isNotEmpty()) View.VISIBLE else View.GONE

                val indexCat = categoriasDisponibles.indexOfFirst { it.id == tarea.categoriaId }
                if (indexCat >= 0) {
                    categoriaSeleccionada = categoriasDisponibles[indexCat]
                    spinnerTaskCategory.setSelection(indexCat + 1)
                }
            }
        } else {
            // Modo Pantalla 6: Crear Tarea
            tvToolbarTitle.text = getString(R.string.title_create_task)
            llCreateTaskHeader.visibility = View.VISIBLE
            llEditTaskHeader.visibility = View.GONE
            btnSaveTask.text = getString(R.string.btn_save_task)
            btnDeleteTaskInEdit.visibility = View.GONE

            val preSelectedId = intent.getLongExtra(EXTRA_PRE_SELECTED_CATEGORY_ID, -1L)
            val indexPre = categoriasDisponibles.indexOfFirst { it.id == preSelectedId }
            if (indexPre >= 0) {
                categoriaSeleccionada = categoriasDisponibles[indexPre]
                spinnerTaskCategory.setSelection(indexPre + 1)
            } else if (categoriasDisponibles.isNotEmpty()) {
                // Seleccionar Estudio por defecto para facilitar flujo rápido manteniendo opción de cambiar
                categoriaSeleccionada = categoriasDisponibles[0]
                spinnerTaskCategory.setSelection(1)
            }
        }

        renderizarChipsCategorias()
        actualizarChipsYVistaPrevia()
    }

    private fun renderizarChipsCategorias() {
        llQuickCategoryChips.removeAllViews()
        categoriasDisponibles.forEachIndexed { index, cat ->
            val btnChip = MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonTonalStyle
            ).apply {
                text = cat.nombre
                textSize = 12f
                isAllCaps = false
                cornerRadius = (10 * resources.displayMetrics.density).toInt()
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (40 * resources.displayMetrics.density).toInt()
                ).apply {
                    marginEnd = (8 * resources.displayMetrics.density).toInt()
                }
                layoutParams = params
                setOnClickListener {
                    categoriaSeleccionada = cat
                    spinnerTaskCategory.setSelection(index + 1)
                    limpiarErrorCategoria()
                    actualizarChipsYVistaPrevia()
                }
            }
            llQuickCategoryChips.addView(btnChip)
        }
    }

    private fun actualizarChipsYVistaPrevia() {
        val cat = categoriaSeleccionada
        tvActiveCategoryChip.text = cat?.nombre ?: "Sin seleccionar"

        for (i in 0 until llQuickCategoryChips.childCount) {
            val btn = llQuickCategoryChips.getChildAt(i) as? MaterialButton ?: continue
            val itemCat = categoriasDisponibles.getOrNull(i) ?: continue
            val seleccionado = cat?.id == itemCat.id
            if (seleccionado) {
                btn.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.primary_fixed)
                )
                btn.setTextColor(ContextCompat.getColor(this, R.color.primary))
            } else {
                btn.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.surface_container_high)
                )
                btn.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant))
            }
        }

        val nombreLimpio = ValidationLogic.normalizarNombre(etTaskName.text?.toString().orEmpty())
        val textoNombre = if (nombreLimpio.isNotEmpty()) nombreLimpio else "Leer Kotlin"
        val textoCategoria = cat?.nombre ?: "Estudio"
        tvTaskFormatPreview.text = "$textoNombre · $textoCategoria"
    }

    private fun configurarEventos() {
        findViewById<ImageButton>(R.id.btnBackTaskForm).setOnClickListener {
            finish()
        }

        btnCancelTask.setOnClickListener {
            finish()
        }

        btnClearTaskName.setOnClickListener {
            etTaskName.setText("")
            etTaskName.requestFocus()
        }

        etTaskName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val texto = s?.toString().orEmpty()
                tvTaskCharCounter.text = "${texto.length}/50"
                btnClearTaskName.visibility = if (texto.isNotEmpty()) View.VISIBLE else View.GONE
                if (ValidationLogic.normalizarNombre(texto).isNotEmpty()) {
                    limpiarErrorNombre()
                }
                actualizarChipsYVistaPrevia()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        // Botones del Simulador de Estados de Validación (Stitch Pantalla 6)
        findViewById<MaterialButton>(R.id.btnSimTaskEmpty).setOnClickListener {
            etTaskName.setText("")
            mostrarErrorNombre(getString(R.string.error_task_empty))
            etTaskName.requestFocus()
        }

        findViewById<MaterialButton>(R.id.btnSimTaskExample).setOnClickListener {
            val ejemplo = "Leer Kotlin"
            etTaskName.setText(ejemplo)
            etTaskName.setSelection(ejemplo.length)
            if (categoriasDisponibles.isNotEmpty()) {
                spinnerTaskCategory.setSelection(1)
            }
            limpiarErrorNombre()
            limpiarErrorCategoria()
            actualizarChipsYVistaPrevia()
        }

        btnSaveTask.setOnClickListener {
            guardarTarea()
        }

        btnDeleteTaskInEdit.setOnClickListener {
            val tarea = tareaEnEdicion ?: return@setOnClickListener
            DialogHelper.mostrarDialogoConfirmarEliminarTarea(this, tarea) {
                when (val res = dbHelper.eliminarTarea(tarea.id)) {
                    is ResultadoOperacion.Exito -> {
                        val data = Intent().apply {
                            putExtra(EXTRA_FEEDBACK_MESSAGE, res.mensaje)
                        }
                        setResult(Activity.RESULT_OK, data)
                        finish()
                    }
                    is ResultadoOperacion.Error -> {
                        DialogHelper.mostrarSnackbarError(rootView, res.mensaje)
                    }
                }
            }
        }
    }

    private fun mostrarErrorNombre(mensaje: String) {
        llTaskNameInputBox.setBackgroundResource(R.drawable.bg_input_error)
        ivTaskNameIcon.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.error)
        )
        tvTaskNameError.text = mensaje
        llTaskNameError.visibility = View.VISIBLE
    }

    private fun limpiarErrorNombre() {
        llTaskNameInputBox.setBackgroundResource(R.drawable.bg_input_normal)
        ivTaskNameIcon.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.outline)
        )
        llTaskNameError.visibility = View.GONE
    }

    private fun mostrarErrorCategoria(mensaje: String) {
        llCategorySelectorBox.setBackgroundResource(R.drawable.bg_input_error)
        tvTaskCategoryError.text = mensaje
        llTaskCategoryError.visibility = View.VISIBLE
    }

    private fun limpiarErrorCategoria() {
        llCategorySelectorBox.setBackgroundResource(R.drawable.bg_input_normal)
        llTaskCategoryError.visibility = View.GONE
    }

    private fun guardarTarea() {
        val rawName = etTaskName.text?.toString().orEmpty()
        val nombreLimpio = ValidationLogic.normalizarNombre(rawName)
        val catId = categoriaSeleccionada?.id

        var hayErrorLocal = false
        if (nombreLimpio.isEmpty()) {
            mostrarErrorNombre(getString(R.string.error_task_empty))
            hayErrorLocal = true
        } else {
            limpiarErrorNombre()
        }

        if (catId == null || catId <= 0L) {
            mostrarErrorCategoria(getString(R.string.error_task_no_category))
            hayErrorLocal = true
        } else {
            limpiarErrorCategoria()
        }

        if (hayErrorLocal) {
            DialogHelper.mostrarSnackbarError(rootView, "Por favor corrige los campos marcados.")
            return
        }

        val resultado = if (taskId > 0L) {
            dbHelper.actualizarTarea(taskId, nombreLimpio, catId)
        } else {
            dbHelper.crearTarea(nombreLimpio, catId)
        }

        when (resultado) {
            is ResultadoOperacion.Exito -> {
                val data = Intent().apply {
                    putExtra(EXTRA_FEEDBACK_MESSAGE, resultado.mensaje)
                }
                setResult(Activity.RESULT_OK, data)
                finish()
            }
            is ResultadoOperacion.Error -> {
                mostrarErrorNombre(resultado.mensaje)
                DialogHelper.mostrarSnackbarError(rootView, resultado.mensaje)
            }
        }
    }
}

