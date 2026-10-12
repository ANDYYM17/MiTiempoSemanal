package com.senati.mitiemposemanal.ayovera

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.senati.mitiemposemanal.ayovera.ui.CategoryVisualStyle
import com.senati.mitiemposemanal.ayovera.ui.DialogHelper

/**
 * Actividad para Crear Categoría (Pantalla 3 de Stitch) y Editar Categoría (Pantalla 4 de Stitch).
 * Cumple los requisitos de HU-01:
 * - Crear categorías.
 * - Editar el nombre de una categoría.
 * - Rechazar nombres duplicados y mostrar un mensaje claro.
 * - Validar nombres vacíos y espacios innecesarios.
 */
class CategoryFormActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORY_ID = "extra_category_id"
        const val EXTRA_FEEDBACK_MESSAGE = "extra_feedback_message"
    }

    private lateinit var dbHelper: AdminSQLiteOpenHelper
    private var categoryId: Long = -1L
    private var categoriaEnEdicion: Categoria? = null
    private var nombresExistentes: List<String> = emptyList()

    private lateinit var rootView: View
    private lateinit var tvToolbarTitle: TextView
    private lateinit var llEditContextHeader: LinearLayout
    private lateinit var tvEditIdChip: TextView
    private lateinit var flEditBadgeIcon: FrameLayout
    private lateinit var ivEditPreviewIcon: ImageView
    private lateinit var tvEditPreviewName: TextView
    private lateinit var tvEditTaskCount: TextView

    private lateinit var llInputContainer: LinearLayout
    private lateinit var ivInputLeadingIcon: ImageView
    private lateinit var etCategoryName: EditText
    private lateinit var btnClearName: ImageButton
    private lateinit var tvCharCounter: TextView
    private lateinit var ivFeedbackIcon: ImageView
    private lateinit var tvFeedbackMessage: TextView

    private lateinit var llEditImpactSection: LinearLayout
    private lateinit var tvEditImpactDesc: TextView
    private lateinit var tvDependentTasksCount: TextView
    private lateinit var tvDependentTasksList: TextView

    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_category_form)

        rootView = findViewById(R.id.rootCategoryForm)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = AdminSQLiteOpenHelper(this)
        categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1L)

        inicializarVistas()
        cargarContextoModo()
        configurarEventos()
    }

    private fun inicializarVistas() {
        tvToolbarTitle = findViewById(R.id.tvCategoryFormToolbarTitle)
        llEditContextHeader = findViewById(R.id.llEditCategoryContextHeader)
        tvEditIdChip = findViewById(R.id.tvEditCategoryIdChip)
        flEditBadgeIcon = findViewById(R.id.flEditCategoryBadgeIcon)
        ivEditPreviewIcon = findViewById(R.id.ivEditCategoryPreviewIcon)
        tvEditPreviewName = findViewById(R.id.tvEditCategoryPreviewName)
        tvEditTaskCount = findViewById(R.id.tvEditCategoryTaskCount)

        llInputContainer = findViewById(R.id.llCategoryInputContainer)
        ivInputLeadingIcon = findViewById(R.id.ivCategoryInputLeadingIcon)
        etCategoryName = findViewById(R.id.etCategoryName)
        btnClearName = findViewById(R.id.btnClearCategoryName)
        tvCharCounter = findViewById(R.id.tvCategoryCharCounter)
        ivFeedbackIcon = findViewById(R.id.ivCategoryFeedbackIcon)
        tvFeedbackMessage = findViewById(R.id.tvCategoryFeedbackMessage)

        llEditImpactSection = findViewById(R.id.llEditCategoryImpactSection)
        tvEditImpactDesc = findViewById(R.id.tvEditCategoryImpactDesc)
        tvDependentTasksCount = findViewById(R.id.tvDependentTasksCount)
        tvDependentTasksList = findViewById(R.id.tvDependentTasksList)

        btnSave = findViewById(R.id.btnSaveCategory)
        btnCancel = findViewById(R.id.btnCancelCategory)
    }

    private fun cargarContextoModo() {
        val todas = dbHelper.listarCategorias()
        nombresExistentes = todas.map { it.nombre }

        if (categoryId > 0L) {
            categoriaEnEdicion = dbHelper.obtenerCategoriaPorId(categoryId)
            val cat = categoriaEnEdicion
            if (cat != null) {
                // Configurar modo Pantalla 4: Editar Categoría
                tvToolbarTitle.text = getString(R.string.title_edit_category)
                btnSave.text = getString(R.string.btn_save_changes)

                llEditContextHeader.visibility = View.VISIBLE
                llEditImpactSection.visibility = View.VISIBLE

                val codigoFormateado = "CAT-${cat.id.toString().padStart(3, '0')}"
                tvEditIdChip.text = "● ID: $codigoFormateado · Activa"
                tvEditPreviewName.text = cat.nombre

                val estilo = CategoryVisualStyle.obtenerEstilo(cat.nombre)
                flEditBadgeIcon.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this, estilo.accentColorRes)
                )
                ivEditPreviewIcon.setImageResource(estilo.iconRes)

                val cant = cat.cantidadTareas
                tvEditTaskCount.text = if (cant == 1) "1 tarea" else "$cant tareas"

                val tareasVinculadas = dbHelper.listarTareas(categoriaIdFiltro = cat.id)
                tvDependentTasksCount.text = if (tareasVinculadas.size == 1) {
                    "1 registro"
                } else {
                    "${tareasVinculadas.size} registros"
                }

                if (tareasVinculadas.isEmpty()) {
                    tvDependentTasksList.text = "No hay tareas vinculadas a esta categoría actualmente."
                } else {
                    tvDependentTasksList.text = tareasVinculadas.joinToString("\n") { t ->
                        "• ${t.nombre} (${t.categoriaNombre})"
                    }
                }

                etCategoryName.setText(cat.nombre)
                etCategoryName.setSelection(cat.nombre.length)
                actualizarEstadoValidacionEnVivo(cat.nombre, mostrarErrorVacio = false)
            }
        } else {
            // Configurar modo Pantalla 3: Crear Categoría
            tvToolbarTitle.text = getString(R.string.title_create_category)
            btnSave.text = getString(R.string.btn_save_category)
            llEditContextHeader.visibility = View.GONE
            llEditImpactSection.visibility = View.GONE
            actualizarEstadoValidacionEnVivo("", mostrarErrorVacio = false)
        }
    }

    private fun configurarEventos() {
        findViewById<ImageButton>(R.id.btnBackCategoryForm).setOnClickListener {
            finish()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnClearName.setOnClickListener {
            etCategoryName.setText("")
            etCategoryName.requestFocus()
        }

        etCategoryName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val texto = s?.toString().orEmpty()
                actualizarEstadoValidacionEnVivo(texto, mostrarErrorVacio = false)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        // Botones de prueba rápida de validación (Stitch Pantallas 3 y 4)
        findViewById<MaterialButton>(R.id.btnTestCategoryEmpty).setOnClickListener {
            etCategoryName.setText("   ")
            actualizarEstadoValidacionEnVivo("   ", mostrarErrorVacio = true)
        }

        findViewById<MaterialButton>(R.id.btnTestCategoryDuplicate).setOnClickListener {
            val ejemploDuplicado = nombresExistentes.firstOrNull {
                !it.equals(categoriaEnEdicion?.nombre, ignoreCase = true)
            } ?: "Estudio"
            etCategoryName.setText(ejemploDuplicado)
            etCategoryName.setSelection(ejemploDuplicado.length)
            actualizarEstadoValidacionEnVivo(ejemploDuplicado, mostrarErrorVacio = true)
        }

        btnSave.setOnClickListener {
            guardarCategoria()
        }
    }

    private fun actualizarEstadoValidacionEnVivo(textoRaw: String, mostrarErrorVacio: Boolean) {
        tvCharCounter.text = "${textoRaw.length}/30"
        btnClearName.visibility = if (textoRaw.isNotEmpty()) View.VISIBLE else View.GONE

        val limpio = ValidationLogic.normalizarNombre(textoRaw)

        categoriaEnEdicion?.let { cat ->
            tvEditPreviewName.text = if (limpio.isNotEmpty()) limpio else "Sin nombre"
            val cant = cat.cantidadTareas
            val tareasTexto = if (cant == 1) "1 tarea vinculada" else "$cant tareas vinculadas"
            tvEditImpactDesc.text = if (limpio.isNotEmpty()) {
                "Al cambiar el nombre a “$limpio”, las $tareasTexto mostrarán automáticamente el nuevo nombre."
            } else {
                "Especifica un nombre válido para mantener la consistencia con las $tareasTexto."
            }
        }

        if (limpio.isEmpty()) {
            if (mostrarErrorVacio) {
                aplicarEstiloInputError(getString(R.string.error_category_empty))
            } else {
                aplicarEstiloInputNormal(getString(R.string.category_helper_default))
            }
            return
        }

        val error = ValidationLogic.validarNombreCategoria(
            nombreRaw = limpio,
            nombresExistentes = nombresExistentes,
            nombreActualEnEdicion = categoriaEnEdicion?.nombre
        )

        if (error != null) {
            aplicarEstiloInputError(error)
        } else {
            aplicarEstiloInputDisponible("Nombre disponible: “$limpio”")
        }
    }

    private fun aplicarEstiloInputNormal(mensaje: String) {
        llInputContainer.setBackgroundResource(R.drawable.bg_input_normal)
        val colorOutline = ContextCompat.getColor(this, R.color.outline)
        ivInputLeadingIcon.imageTintList = ColorStateList.valueOf(colorOutline)
        ivFeedbackIcon.setImageResource(R.drawable.ic_info)
        ivFeedbackIcon.imageTintList = ColorStateList.valueOf(colorOutline)
        tvFeedbackMessage.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant))
        tvFeedbackMessage.text = mensaje
    }

    private fun aplicarEstiloInputDisponible(mensaje: String) {
        llInputContainer.setBackgroundResource(R.drawable.bg_input_normal)
        val colorSecondary = ContextCompat.getColor(this, R.color.secondary)
        ivInputLeadingIcon.imageTintList = ColorStateList.valueOf(colorSecondary)
        ivFeedbackIcon.setImageResource(R.drawable.ic_check)
        ivFeedbackIcon.imageTintList = ColorStateList.valueOf(colorSecondary)
        tvFeedbackMessage.setTextColor(colorSecondary)
        tvFeedbackMessage.text = mensaje
    }

    private fun aplicarEstiloInputError(mensajeError: String) {
        llInputContainer.setBackgroundResource(R.drawable.bg_input_error)
        val colorError = ContextCompat.getColor(this, R.color.error)
        ivInputLeadingIcon.imageTintList = ColorStateList.valueOf(colorError)
        ivFeedbackIcon.setImageResource(R.drawable.ic_warning)
        ivFeedbackIcon.imageTintList = ColorStateList.valueOf(colorError)
        tvFeedbackMessage.setTextColor(colorError)
        tvFeedbackMessage.text = mensajeError
    }

    private fun guardarCategoria() {
        val rawInput = etCategoryName.text?.toString().orEmpty()
        val resultado = if (categoryId > 0L) {
            dbHelper.actualizarCategoria(categoryId, rawInput)
        } else {
            dbHelper.crearCategoria(rawInput)
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
                aplicarEstiloInputError(resultado.mensaje)
                DialogHelper.mostrarSnackbarError(rootView, resultado.mensaje)
            }
        }
    }
}

