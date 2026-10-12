package com.senati.mitiemposemanal.ayovera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.senati.mitiemposemanal.ayovera.data.AdminSQLiteOpenHelper
import com.senati.mitiemposemanal.ayovera.data.ResultadoEliminacionCategoria
import com.senati.mitiemposemanal.ayovera.data.ResultadoOperacion
import com.senati.mitiemposemanal.ayovera.data.ValidationLogic
import com.senati.mitiemposemanal.ayovera.model.Categoria
import com.senati.mitiemposemanal.ayovera.model.Tarea
import com.senati.mitiemposemanal.ayovera.ui.CategoryAdapter
import com.senati.mitiemposemanal.ayovera.ui.DialogHelper
import com.senati.mitiemposemanal.ayovera.ui.TaskAdapter

/**
 * Actividad principal de MiTiempoSemanal (Sprint 1 SENATI).
 * Integra la navegación entre:
 * - Pantalla 1: Inicio (Dashboard, nombre de la app, estudiante · v0.1.0 (1), accesos rápidos, categorías iniciales y vista previa de tareas)
 * - Pantalla 2: Mis Categorías (HU-01: lista, creación, edición, confirmación de eliminación y bloqueo si tiene tareas)
 * - Pantalla 5: Mis Tareas y Estado Vacío (HU-02: lista "Tarea · Categoría", filtros por categoría, estado vacío, creación, edición y eliminación)
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "mi_tiempo_semanal_prefs"
        private const val KEY_STUDENT_NAME = "student_name"
    }

    private lateinit var dbHelper: AdminSQLiteOpenHelper
    private lateinit var rootMain: View

    // Top Bar & Navigation
    private lateinit var tvTopAppSubtitle: TextView
    private lateinit var viewHome: NestedScrollView
    private lateinit var viewCategories: NestedScrollView
    private lateinit var viewTasks: NestedScrollView
    private lateinit var fabMain: FloatingActionButton
    private lateinit var bottomNav: BottomNavigationView

    // Pantalla 1: Inicio Views
    private lateinit var tvHomeGreeting: TextView
    private lateinit var tvStudentVersion: TextView
    private lateinit var tvHeroSummary: TextView
    private lateinit var tvQuickCategoriesCount: TextView
    private lateinit var tvQuickTasksBadge: TextView
    private lateinit var tvQuickTasksCount: TextView
    private lateinit var tvHomeCountEstudio: TextView
    private lateinit var tvHomeCountTrabajo: TextView
    private lateinit var tvHomeCountDescanso: TextView
    private lateinit var tvHomeCountOcio: TextView
    private lateinit var tvHomeEmptyTasksHint: TextView
    private lateinit var rvHomePreviewTasks: RecyclerView

    // Pantalla 2: Mis Categorías Views
    private lateinit var tvCategoriesSubtitle: TextView
    private lateinit var rvCategories: RecyclerView

    // Pantalla 5: Mis Tareas Views
    private lateinit var tvTasksCounterSubtitle: TextView
    private lateinit var tvTasksSummaryDetail: TextView
    private lateinit var llCategoryFilterChips: LinearLayout
    private lateinit var rvTasks: RecyclerView
    private lateinit var cardEmptyTasksState: MaterialCardView
    private lateinit var tvEmptyStateTitle: TextView
    private lateinit var tvEmptyStateDesc: TextView

    // Adapters & State
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var homePreviewTaskAdapter: TaskAdapter
    private var filtroCategoriaIdTarea: Long? = null

    private val formActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        recargarDatosYActualizarUI()
        if (result.resultCode == Activity.RESULT_OK) {
            val msg = result.data?.getStringExtra("extra_feedback_message")
            if (!msg.isNullOrBlank()) {
                DialogHelper.mostrarSnackbarExito(rootMain, msg)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        rootMain = findViewById(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootMain) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = AdminSQLiteOpenHelper(this)
        dbHelper.asegurarCategoriasInicialesSiVacio()

        inicializarVistas()
        configurarRecyclerViews()
        configurarNavegacionYBotones()
        actualizarCabeceraEstudianteYVersion()
        recargarDatosYActualizarUI()
    }

    override fun onResume() {
        super.onResume()
        recargarDatosYActualizarUI()
    }

    private fun inicializarVistas() {
        tvTopAppSubtitle = findViewById(R.id.tvTopAppSubtitle)
        viewHome = findViewById(R.id.viewHome)
        viewCategories = findViewById(R.id.viewCategories)
        viewTasks = findViewById(R.id.viewTasks)
        fabMain = findViewById(R.id.fabMain)
        bottomNav = findViewById(R.id.bottomNav)

        // Pantalla 1
        tvHomeGreeting = findViewById(R.id.tvHomeGreeting)
        tvStudentVersion = findViewById(R.id.tvStudentVersion)
        tvHeroSummary = findViewById(R.id.tvHeroSummary)
        tvQuickCategoriesCount = findViewById(R.id.tvQuickCategoriesCount)
        tvQuickTasksBadge = findViewById(R.id.tvQuickTasksBadge)
        tvQuickTasksCount = findViewById(R.id.tvQuickTasksCount)
        tvHomeCountEstudio = findViewById(R.id.tvHomeCountEstudio)
        tvHomeCountTrabajo = findViewById(R.id.tvHomeCountTrabajo)
        tvHomeCountDescanso = findViewById(R.id.tvHomeCountDescanso)
        tvHomeCountOcio = findViewById(R.id.tvHomeCountOcio)
        tvHomeEmptyTasksHint = findViewById(R.id.tvHomeEmptyTasksHint)
        rvHomePreviewTasks = findViewById(R.id.rvHomePreviewTasks)

        // Pantalla 2
        tvCategoriesSubtitle = findViewById(R.id.tvCategoriesSubtitle)
        rvCategories = findViewById(R.id.rvCategories)

        // Pantalla 5
        tvTasksCounterSubtitle = findViewById(R.id.tvTasksCounterSubtitle)
        tvTasksSummaryDetail = findViewById(R.id.tvTasksSummaryDetail)
        llCategoryFilterChips = findViewById(R.id.llCategoryFilterChips)
        rvTasks = findViewById(R.id.rvTasks)
        cardEmptyTasksState = findViewById(R.id.cardEmptyTasksState)
        tvEmptyStateTitle = findViewById(R.id.tvEmptyStateTitle)
        tvEmptyStateDesc = findViewById(R.id.tvEmptyStateDesc)
    }

    private fun configurarRecyclerViews() {
        categoryAdapter = CategoryAdapter(
            categorias = emptyList(),
            onEditarClick = { categoria -> abrirFormularioEditarCategoria(categoria) },
            onEliminarClick = { categoria -> manejarIntentoEliminarCategoria(categoria) }
        )
        rvCategories.layoutManager = LinearLayoutManager(this)
        rvCategories.adapter = categoryAdapter

        taskAdapter = TaskAdapter(
            tareas = emptyList(),
            onEditarClick = { tarea -> abrirFormularioEditarTarea(tarea) },
            onEliminarClick = { tarea -> manejarIntentoEliminarTarea(tarea) }
        )
        rvTasks.layoutManager = LinearLayoutManager(this)
        rvTasks.adapter = taskAdapter

        homePreviewTaskAdapter = TaskAdapter(
            tareas = emptyList(),
            onEditarClick = { tarea -> abrirFormularioEditarTarea(tarea) },
            onEliminarClick = { tarea -> manejarIntentoEliminarTarea(tarea) }
        )
        rvHomePreviewTasks.layoutManager = LinearLayoutManager(this)
        rvHomePreviewTasks.adapter = homePreviewTaskAdapter
    }

    private fun configurarNavegacionYBotones() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    mostrarSeccion(SeccionActiva.INICIO)
                    true
                }
                R.id.nav_categories -> {
                    mostrarSeccion(SeccionActiva.CATEGORIAS)
                    true
                }
                R.id.nav_tasks -> {
                    mostrarSeccion(SeccionActiva.TAREAS)
                    true
                }
                else -> false
            }
        }

        // Accesos rápidos en Pantalla 1 (Inicio)
        findViewById<MaterialCardView>(R.id.cardQuickCategories).setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_categories
        }

        findViewById<MaterialCardView>(R.id.cardQuickTasks).setOnClickListener {
            filtroCategoriaIdTarea = null
            bottomNav.selectedItemId = R.id.nav_tasks
        }

        findViewById<TextView>(R.id.btnViewAllTasks).setOnClickListener {
            filtroCategoriaIdTarea = null
            bottomNav.selectedItemId = R.id.nav_tasks
        }

        findViewById<MaterialButton>(R.id.btnHomeNewTask).setOnClickListener {
            abrirFormularioCrearTarea()
        }

        findViewById<MaterialButton>(R.id.btnEmptyCreateFirstTask).setOnClickListener {
            abrirFormularioCrearTarea(filtroCategoriaIdTarea)
        }

        // Tarjetas 2x2 de Categorías Iniciales en Inicio: abren Tareas filtradas por esa categoría
        findViewById<MaterialCardView>(R.id.cardHomeEstudio).setOnClickListener {
            navegarATareasPorNombreCategoria("Estudio")
        }
        findViewById<MaterialCardView>(R.id.cardHomeTrabajo).setOnClickListener {
            navegarATareasPorNombreCategoria("Trabajo")
        }
        findViewById<MaterialCardView>(R.id.cardHomeDescanso).setOnClickListener {
            navegarATareasPorNombreCategoria("Descanso")
        }
        findViewById<MaterialCardView>(R.id.cardHomeOcio).setOnClickListener {
            navegarATareasPorNombreCategoria("Ocio")
        }

        // Botón FAB contextual
        fabMain.setOnClickListener {
            when (bottomNav.selectedItemId) {
                R.id.nav_categories -> abrirFormularioCrearCategoria()
                R.id.nav_tasks -> abrirFormularioCrearTarea(filtroCategoriaIdTarea)
                else -> abrirFormularioCrearTarea()
            }
        }

        // Botón para abrir Pantalla 8: Catálogo de Confirmaciones y Mensajes
        findViewById<ImageButton>(R.id.btnSystemMessagesCatalog).setOnClickListener {
            startActivity(Intent(this, SystemMessagesActivity::class.java))
        }

        // Botones para personalizar el nombre del estudiante
        findViewById<ImageButton>(R.id.btnEditStudentName).setOnClickListener {
            mostrarDialogoPersonalizarEstudiante()
        }
        findViewById<FrameLayout>(R.id.btnStudentProfile).setOnClickListener {
            mostrarDialogoPersonalizarEstudiante()
        }
    }

    private fun navegarATareasPorNombreCategoria(nombreCategoria: String) {
        val cat = dbHelper.listarCategorias().firstOrNull {
            it.nombre.equals(nombreCategoria, ignoreCase = true)
        }
        filtroCategoriaIdTarea = cat?.id
        bottomNav.selectedItemId = R.id.nav_tasks
    }

    private enum class SeccionActiva {
        INICIO, CATEGORIAS, TAREAS
    }

    private fun mostrarSeccion(seccion: SeccionActiva) {
        when (seccion) {
            SeccionActiva.INICIO -> {
                tvTopAppSubtitle.text = getString(R.string.nav_home)
                viewHome.visibility = View.VISIBLE
                viewCategories.visibility = View.GONE
                viewTasks.visibility = View.GONE
                fabMain.visibility = View.GONE
            }
            SeccionActiva.CATEGORIAS -> {
                tvTopAppSubtitle.text = getString(R.string.nav_categories)
                viewHome.visibility = View.GONE
                viewCategories.visibility = View.VISIBLE
                viewTasks.visibility = View.GONE
                fabMain.visibility = View.VISIBLE
                fabMain.contentDescription = "Crear nueva categoría"
            }
            SeccionActiva.TAREAS -> {
                tvTopAppSubtitle.text = getString(R.string.nav_tasks)
                viewHome.visibility = View.GONE
                viewCategories.visibility = View.GONE
                viewTasks.visibility = View.VISIBLE
                fabMain.visibility = View.VISIBLE
                fabMain.contentDescription = "Agregar nueva tarea"
            }
        }
        recargarDatosYActualizarUI()
    }

    /**
     * Muestra el nombre del estudiante y la versión en el formato obligatorio de la Sección 8:
     * "Nombre Apellido · v0.1.0 (1)"
     */
    private fun actualizarCabeceraEstudianteYVersion() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nombreGuardado = prefs.getString(KEY_STUDENT_NAME, null)
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.student_name)

        val pkgInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = pkgInfo.versionName ?: "0.1.0"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode.toLong()
        }

        // Formato exacto: "Nombre Apellido · v0.1.0 (1)"
        tvStudentVersion.text = "$nombreGuardado · v$versionName ($versionCode)"
        tvHomeGreeting.text = if (nombreGuardado == getString(R.string.student_name)) {
            "¡Hola, Estudiante SENATI! 👋"
        } else {
            "¡Hola, $nombreGuardado! 👋"
        }
    }

    private fun mostrarDialogoPersonalizarEstudiante() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val actual = prefs.getString(KEY_STUDENT_NAME, getString(R.string.student_name))
            ?: getString(R.string.student_name)

        val input = EditText(this).apply {
            setText(actual)
            hint = "Nombre Apellido"
            setSelection(text.length)
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Nombre del Estudiante")
            .setMessage("Personaliza el nombre que se muestra junto a la versión v0.1.0 (1):")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevo = ValidationLogic.normalizarNombre(input.text?.toString().orEmpty())
                if (nuevo.isNotEmpty()) {
                    prefs.edit().putString(KEY_STUDENT_NAME, nuevo).apply()
                    actualizarCabeceraEstudianteYVersion()
                    DialogHelper.mostrarSnackbarExito(rootMain, "Nombre actualizado a “$nuevo”")
                }
            }
            .show()
    }

    private fun recargarDatosYActualizarUI() {
        val categorias = dbHelper.listarCategorias()
        val todasLasTareas = dbHelper.listarTareas(categoriaIdFiltro = null)

        // Si el filtro actual apunta a una categoría que fue eliminada, reiniciar filtro
        if (filtroCategoriaIdTarea != null && categorias.none { it.id == filtroCategoriaIdTarea }) {
            filtroCategoriaIdTarea = null
        }

        val tareasFiltradas = if (filtroCategoriaIdTarea != null) {
            dbHelper.listarTareas(categoriaIdFiltro = filtroCategoriaIdTarea)
        } else {
            todasLasTareas
        }

        // 1. Actualizar Pantalla 1: Inicio
        val totalCats = categorias.size
        val totalTasks = todasLasTareas.size

        tvQuickCategoriesCount.text = if (totalCats == 1) "1 configurada" else "$totalCats configuradas"
        tvQuickTasksBadge.text = totalTasks.toString()
        tvQuickTasksCount.text = if (totalTasks == 1) "1 registrada" else "$totalTasks registradas"
        tvHeroSummary.text = "Resumen actual: $totalCats categorías y $totalTasks tareas registradas"

        fun textoTareasChip(nombreCat: String): String {
            val count = categorias.firstOrNull {
                it.nombre.equals(nombreCat, ignoreCase = true)
            }?.cantidadTareas ?: 0
            return if (count == 1) "1 tarea" else "$count tareas"
        }

        tvHomeCountEstudio.text = textoTareasChip("Estudio")
        tvHomeCountTrabajo.text = textoTareasChip("Trabajo")
        tvHomeCountDescanso.text = textoTareasChip("Descanso")
        tvHomeCountOcio.text = textoTareasChip("Ocio")

        if (todasLasTareas.isEmpty()) {
            tvHomeEmptyTasksHint.visibility = View.VISIBLE
            rvHomePreviewTasks.visibility = View.GONE
        } else {
            tvHomeEmptyTasksHint.visibility = View.GONE
            rvHomePreviewTasks.visibility = View.VISIBLE
            homePreviewTaskAdapter.actualizarDatos(todasLasTareas.take(3))
        }

        // 2. Actualizar Pantalla 2: Mis Categorías
        tvCategoriesSubtitle.text = if (totalCats == 1) {
            "1 categoría disponible para organizar tus tareas"
        } else {
            "$totalCats categorías disponibles para organizar tus tareas"
        }
        categoryAdapter.actualizarDatos(categorias)

        // 3. Actualizar Pantalla 5: Mis Tareas y Estado Vacío
        tvTasksCounterSubtitle.text = if (totalTasks == 1) {
            "1 tarea programada esta semana"
        } else {
            "$totalTasks tareas programadas esta semana"
        }
        tvTasksSummaryDetail.text = "$totalTasks tareas distribuidas en $totalCats categorías"

        renderizarChipsFiltroTareas(categorias, totalTasks)

        if (tareasFiltradas.isEmpty()) {
            rvTasks.visibility = View.GONE
            cardEmptyTasksState.visibility = View.VISIBLE
            val catFiltro = categorias.firstOrNull { it.id == filtroCategoriaIdTarea }
            if (catFiltro != null) {
                tvEmptyStateTitle.text = "Sin tareas en “${catFiltro.nombre}”"
                tvEmptyStateDesc.text =
                    "Aún no has registrado tareas en la categoría “${catFiltro.nombre}”. ¡Crea la primera ahora!"
            } else {
                tvEmptyStateTitle.text = getString(R.string.empty_tasks_title)
                tvEmptyStateDesc.text = getString(R.string.empty_tasks_desc)
            }
        } else {
            rvTasks.visibility = View.VISIBLE
            cardEmptyTasksState.visibility = View.GONE
            taskAdapter.actualizarDatos(tareasFiltradas)
        }
    }

    private fun renderizarChipsFiltroTareas(categorias: List<Categoria>, totalTareas: Int) {
        llCategoryFilterChips.removeAllViews()

        fun crearBotonFiltro(titulo: String, seleccionado: Boolean, onClick: () -> Unit): MaterialButton {
            return MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonTonalStyle
            ).apply {
                text = titulo
                textSize = 12f
                isAllCaps = false
                cornerRadius = (10 * resources.displayMetrics.density).toInt()
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (38 * resources.displayMetrics.density).toInt()
                ).apply {
                    marginEnd = (8 * resources.displayMetrics.density).toInt()
                }
                layoutParams = params
                if (seleccionado) {
                    backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(this@MainActivity, R.color.primary)
                    )
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.on_primary))
                } else {
                    backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(this@MainActivity, R.color.surface_container)
                    )
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.on_surface_variant))
                }
                setOnClickListener { onClick() }
            }
        }

        // Chip "Todas (X)"
        llCategoryFilterChips.addView(
            crearBotonFiltro(
                titulo = "Todas ($totalTareas)",
                seleccionado = filtroCategoriaIdTarea == null
            ) {
                filtroCategoriaIdTarea = null
                recargarDatosYActualizarUI()
            }
        )

        // Un chip por cada categoría existente en SQLite
        categorias.forEach { cat ->
            llCategoryFilterChips.addView(
                crearBotonFiltro(
                    titulo = "${cat.nombre} (${cat.cantidadTareas})",
                    seleccionado = filtroCategoriaIdTarea == cat.id
                ) {
                    filtroCategoriaIdTarea = cat.id
                    recargarDatosYActualizarUI()
                }
            )
        }
    }

    // =========================================================================
    // NAVEGACIÓN A FORMULARIOS Y DIÁLOGOS DE CONFIRMACIÓN / BLOQUEO
    // =========================================================================

    private fun abrirFormularioCrearCategoria() {
        val intent = Intent(this, CategoryFormActivity::class.java)
        formActivityLauncher.launch(intent)
    }

    private fun abrirFormularioEditarCategoria(categoria: Categoria) {
        val intent = Intent(this, CategoryFormActivity::class.java).apply {
            putExtra(CategoryFormActivity.EXTRA_CATEGORY_ID, categoria.id)
        }
        formActivityLauncher.launch(intent)
    }

    private fun manejarIntentoEliminarCategoria(categoria: Categoria) {
        val tareasAsociadas = dbHelper.listarTareas(categoriaIdFiltro = categoria.id)
        if (tareasAsociadas.isNotEmpty()) {
            // Regla HU-01.7: Impedir eliminar categoría con tareas asociadas y mostrar explicación (Pantalla 8 - Sección 2)
            DialogHelper.mostrarDialogoBloqueoCategoriaConTareas(
                context = this,
                nombreCategoria = categoria.nombre,
                tareasAsociadas = tareasAsociadas
            )
            return
        }

        // Regla HU-01.6: Solicitar confirmación antes de eliminar categoría (Pantalla 8 - Sección 1)
        DialogHelper.mostrarDialogoConfirmarEliminarCategoria(this, categoria) {
            when (val res = dbHelper.eliminarCategoria(categoria.id)) {
                is ResultadoEliminacionCategoria.Eliminada -> {
                    recargarDatosYActualizarUI()
                    DialogHelper.mostrarSnackbarExito(rootMain, res.mensaje)
                }
                is ResultadoEliminacionCategoria.BloqueadaConTareas -> {
                    DialogHelper.mostrarDialogoBloqueoCategoriaConTareas(
                        context = this,
                        nombreCategoria = res.categoriaNombre,
                        tareasAsociadas = res.tareasAsociadas
                    )
                }
                is ResultadoEliminacionCategoria.Error -> {
                    DialogHelper.mostrarSnackbarError(rootMain, res.mensaje)
                }
            }
        }
    }

    private fun abrirFormularioCrearTarea(preSelectedCategoryId: Long? = null) {
        val intent = Intent(this, TaskFormActivity::class.java).apply {
            if (preSelectedCategoryId != null && preSelectedCategoryId > 0L) {
                putExtra(TaskFormActivity.EXTRA_PRE_SELECTED_CATEGORY_ID, preSelectedCategoryId)
            }
        }
        formActivityLauncher.launch(intent)
    }

    private fun abrirFormularioEditarTarea(tarea: Tarea) {
        val intent = Intent(this, TaskFormActivity::class.java).apply {
            putExtra(TaskFormActivity.EXTRA_TASK_ID, tarea.id)
        }
        formActivityLauncher.launch(intent)
    }

    private fun manejarIntentoEliminarTarea(tarea: Tarea) {
        // Regla HU-02.7: Permitir eliminar tareas con confirmación (Pantalla 8 - Sección 3)
        DialogHelper.mostrarDialogoConfirmarEliminarTarea(this, tarea) {
            when (val res = dbHelper.eliminarTarea(tarea.id)) {
                is ResultadoOperacion.Exito -> {
                    recargarDatosYActualizarUI()
                    DialogHelper.mostrarSnackbarExito(rootMain, res.mensaje)
                }
                is ResultadoOperacion.Error -> {
                    DialogHelper.mostrarSnackbarError(rootMain, res.mensaje)
                }
            }
        }
    }
}