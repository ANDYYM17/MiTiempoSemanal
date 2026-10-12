package com.senati.mitiemposemanal.ayovera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.senati.mitiemposemanal.ayovera.data.ResultadoEvento
import com.senati.mitiemposemanal.ayovera.data.ResultadoOperacion
import com.senati.mitiemposemanal.ayovera.data.ValidationLogic
import com.senati.mitiemposemanal.ayovera.model.Categoria
import com.senati.mitiemposemanal.ayovera.model.Evento
import com.senati.mitiemposemanal.ayovera.model.Tarea
import com.senati.mitiemposemanal.ayovera.ui.CategoryAdapter
import com.senati.mitiemposemanal.ayovera.ui.CategoryVisualStyle
import com.senati.mitiemposemanal.ayovera.ui.DialogHelper
import com.senati.mitiemposemanal.ayovera.ui.EventHistoryAdapter
import com.senati.mitiemposemanal.ayovera.ui.EventTaskSearchAdapter
import com.senati.mitiemposemanal.ayovera.ui.TaskAdapter
import java.time.LocalDateTime

/**
 * Actividad principal de MiTiempoSemanal (Sprints 1 y 2 SENATI).
 * Integra la navegación entre:
 * - Pantalla 1: Inicio (Dashboard, nombre de la app, estudiante · v0.2.0 (2), accesos rápidos, categorías iniciales y vista previa de tareas)
 * - Pantalla 2: Mis Categorías (HU-01)
 * - Pantalla 5: Mis Tareas y Estado Vacío (HU-02)
 * - Pantallas 10, 11, 12, 13, 14 y 15: Eventos, Búsqueda en Tiempo Real (HU-03), Crear Tarea desde Eventos (HU-04),
 *   Evento en Curso e Inicio/Fin de Eventos con Persistencia en SQLite (HU-05) e Historial de Eventos.
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
    private lateinit var viewEvents: NestedScrollView
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

    // Pantallas 10, 11 y 13: Eventos Views
    private lateinit var llEventsSearchLockedBanner: LinearLayout
    private lateinit var llEventSearchInputBox: LinearLayout
    private lateinit var etEventTaskSearch: EditText
    private lateinit var btnClearEventSearch: ImageButton
    private lateinit var tvEventSearchHelper: TextView
    private lateinit var rvEventSearchMatches: RecyclerView
    private lateinit var cardEventSearchNoResults: MaterialCardView
    private lateinit var tvEventSearchNoResultsTitle: TextView
    private lateinit var btnQuickCreateFromSearch: MaterialButton
    private lateinit var cardSelectedTaskPreview: MaterialCardView
    private lateinit var tvSelectedTaskCategoryBadge: TextView
    private lateinit var tvSelectedTaskPreviewTitle: TextView
    private lateinit var btnStartEvent: MaterialButton

    private lateinit var tvEventLiveStatusBadge: TextView
    private lateinit var cardEventIdleState: MaterialCardView
    private lateinit var cardEventActiveState: MaterialCardView
    private lateinit var viewActiveEventStripe: View
    private lateinit var llActiveEventCategoryBadge: LinearLayout
    private lateinit var ivActiveEventCategoryIcon: ImageView
    private lateinit var tvActiveEventCategoryChip: TextView
    private lateinit var tvActiveEventIdBadge: TextView
    private lateinit var tvActiveEventTaskTitle: TextView
    private lateinit var tvActiveEventTaskSubtitle: TextView
    private lateinit var tvActiveEventTimerDisplay: TextView
    private lateinit var tvActiveEventStartDate: TextView
    private lateinit var tvActiveEventStartTime: TextView
    private lateinit var btnStopEvent: MaterialButton

    private lateinit var btnViewFullEventHistory: TextView
    private lateinit var rvRecentEvents: RecyclerView
    private lateinit var cardEmptyRecentEvents: MaterialCardView

    // Adapters & State
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var homePreviewTaskAdapter: TaskAdapter
    private lateinit var eventTaskSearchAdapter: EventTaskSearchAdapter
    private lateinit var recentEventsAdapter: EventHistoryAdapter

    private var filtroCategoriaIdTarea: Long? = null
    private var tareaSeleccionadaEnEventos: Tarea? = null
    private var actualizandoTextoPorSeleccion: Boolean = false

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            val activo = dbHelper.obtenerEventoActivo()
            if (activo != null) {
                val segundos = activo.obtenerSegundosTranscurridos(LocalDateTime.now())
                tvActiveEventTimerDisplay.text = Evento.formatearCronometro(segundos)
                timerHandler.postDelayed(this, 1000L)
            }
        }
    }

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
        configurarEventosYBusquedaEnTiempoReal()
        actualizarCabeceraEstudianteYVersion()
        recargarDatosYActualizarUI()
    }

    override fun onResume() {
        super.onResume()
        recargarDatosYActualizarUI()
    }

    override fun onPause() {
        super.onPause()
        // El evento activo NO se finaliza al pasar a segundo plano (Sección 7);
        // solo se pausa el refresco visual de la vista mientras no está en pantalla.
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun inicializarVistas() {
        tvTopAppSubtitle = findViewById(R.id.tvTopAppSubtitle)
        viewHome = findViewById(R.id.viewHome)
        viewCategories = findViewById(R.id.viewCategories)
        viewTasks = findViewById(R.id.viewTasks)
        viewEvents = findViewById(R.id.viewEvents)
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

        // Pantallas 10, 11 y 13 (Eventos)
        llEventsSearchLockedBanner = findViewById(R.id.llEventsSearchLockedBanner)
        llEventSearchInputBox = findViewById(R.id.llEventSearchInputBox)
        etEventTaskSearch = findViewById(R.id.etEventTaskSearch)
        btnClearEventSearch = findViewById(R.id.btnClearEventSearch)
        tvEventSearchHelper = findViewById(R.id.tvEventSearchHelper)
        rvEventSearchMatches = findViewById(R.id.rvEventSearchMatches)
        cardEventSearchNoResults = findViewById(R.id.cardEventSearchNoResults)
        tvEventSearchNoResultsTitle = findViewById(R.id.tvEventSearchNoResultsTitle)
        btnQuickCreateFromSearch = findViewById(R.id.btnQuickCreateFromSearch)
        cardSelectedTaskPreview = findViewById(R.id.cardSelectedTaskPreview)
        tvSelectedTaskCategoryBadge = findViewById(R.id.tvSelectedTaskCategoryBadge)
        tvSelectedTaskPreviewTitle = findViewById(R.id.tvSelectedTaskPreviewTitle)
        btnStartEvent = findViewById(R.id.btnStartEvent)

        tvEventLiveStatusBadge = findViewById(R.id.tvEventLiveStatusBadge)
        cardEventIdleState = findViewById(R.id.cardEventIdleState)
        cardEventActiveState = findViewById(R.id.cardEventActiveState)
        viewActiveEventStripe = findViewById(R.id.viewActiveEventStripe)
        llActiveEventCategoryBadge = findViewById(R.id.llActiveEventCategoryBadge)
        ivActiveEventCategoryIcon = findViewById(R.id.ivActiveEventCategoryIcon)
        tvActiveEventCategoryChip = findViewById(R.id.tvActiveEventCategoryChip)
        tvActiveEventIdBadge = findViewById(R.id.tvActiveEventIdBadge)
        tvActiveEventTaskTitle = findViewById(R.id.tvActiveEventTaskTitle)
        tvActiveEventTaskSubtitle = findViewById(R.id.tvActiveEventTaskSubtitle)
        tvActiveEventTimerDisplay = findViewById(R.id.tvActiveEventTimerDisplay)
        tvActiveEventStartDate = findViewById(R.id.tvActiveEventStartDate)
        tvActiveEventStartTime = findViewById(R.id.tvActiveEventStartTime)
        btnStopEvent = findViewById(R.id.btnStopEvent)

        btnViewFullEventHistory = findViewById(R.id.btnViewFullEventHistory)
        rvRecentEvents = findViewById(R.id.rvRecentEvents)
        cardEmptyRecentEvents = findViewById(R.id.cardEmptyRecentEvents)
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

        eventTaskSearchAdapter = EventTaskSearchAdapter(
            tareas = emptyList(),
            tareaSeleccionadaId = null,
            onTareaSeleccionada = { tarea -> seleccionarTareaEnEventos(tarea) }
        )
        rvEventSearchMatches.layoutManager = LinearLayoutManager(this)
        rvEventSearchMatches.itemAnimator = null
        rvEventSearchMatches.isFocusable = false
        rvEventSearchMatches.isFocusableInTouchMode = false
        rvEventSearchMatches.adapter = eventTaskSearchAdapter

        recentEventsAdapter = EventHistoryAdapter(
            eventos = emptyList(),
            onEventoClick = { abrirHistorialCompletoEventos() }
        )
        rvRecentEvents.layoutManager = LinearLayoutManager(this)
        rvRecentEvents.itemAnimator = null
        rvRecentEvents.isFocusable = false
        rvRecentEvents.isFocusableInTouchMode = false
        rvRecentEvents.adapter = recentEventsAdapter
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
                R.id.nav_events -> {
                    mostrarSeccion(SeccionActiva.EVENTOS)
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

    // =========================================================================
    // CONFIGURACIÓN DE EVENTOS, BÚSQUEDA EN TIEMPO REAL E INICIO/FIN (SPRINT 2)
    // =========================================================================

    private fun configurarEventosYBusquedaEnTiempoReal() {
        llEventSearchInputBox.setOnClickListener {
            etEventTaskSearch.requestFocus()
        }

        etEventTaskSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                val textoRaw = s?.toString().orEmpty()
                btnClearEventSearch.visibility = if (textoRaw.isNotEmpty()) View.VISIBLE else View.GONE

                if (actualizandoTextoPorSeleccion) return

                val sel = tareaSeleccionadaEnEventos
                if (sel != null && !ValidationLogic.sonNombresEquivalentes(textoRaw, sel.nombre)) {
                    tareaSeleccionadaEnEventos = null
                    cardSelectedTaskPreview.visibility = View.GONE
                }

                actualizarResultadosBusquedaEventos(textoRaw)
            }
        })

        btnClearEventSearch.setOnClickListener {
            tareaSeleccionadaEnEventos = null
            cardSelectedTaskPreview.visibility = View.GONE
            etEventTaskSearch.setText("")
            etEventTaskSearch.requestFocus()
        }

        btnStartEvent.setOnClickListener {
            manejarClickIniciarEvento()
        }

        btnQuickCreateFromSearch.setOnClickListener {
            manejarClickIniciarEvento()
        }

        btnStopEvent.setOnClickListener {
            manejarClickFinalizarEvento()
        }

        btnViewFullEventHistory.setOnClickListener {
            abrirHistorialCompletoEventos()
        }
    }

    /**
     * HU-03: Actualiza en tiempo real los resultados de búsqueda según el texto introducido.
     * - Con menos de 2 caracteres útiles, no muestra coincidencias de búsqueda.
     * - Desde 2 caracteres, consulta SQLite con LIKE ? parametrizado.
     */
    private fun actualizarResultadosBusquedaEventos(textoRaw: String) {
        val limpio = ValidationLogic.normalizarNombre(textoRaw)

        if (!ValidationLogic.esConsultaBusquedaValida(limpio)) {
            rvEventSearchMatches.visibility = View.GONE
            cardEventSearchNoResults.visibility = View.GONE
            eventTaskSearchAdapter.actualizarResultados(emptyList(), tareaSeleccionadaEnEventos?.id)
            tvEventSearchHelper.text = getString(R.string.events_search_helper_short)
            tvEventSearchHelper.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant))
            return
        }

        val coincidencias = dbHelper.buscarTareasPorTexto(limpio)
        if (coincidencias.isNotEmpty()) {
            rvEventSearchMatches.visibility = View.VISIBLE
            cardEventSearchNoResults.visibility = View.GONE
            eventTaskSearchAdapter.actualizarResultados(coincidencias, tareaSeleccionadaEnEventos?.id)
            val cant = coincidencias.size
            tvEventSearchHelper.text = if (cant == 1) {
                "1 coincidencia encontrada · Selecciona la tarea o pulsa Iniciar"
            } else {
                "$cant coincidencias encontradas · Selecciona una tarea"
            }
            tvEventSearchHelper.setTextColor(ContextCompat.getColor(this, R.color.secondary))
        } else {
            rvEventSearchMatches.visibility = View.GONE
            eventTaskSearchAdapter.actualizarResultados(emptyList(), null)
            cardEventSearchNoResults.visibility = View.VISIBLE
            tvEventSearchNoResultsTitle.text = "No se encontraron tareas con “$limpio”"
            tvEventSearchHelper.text = "0 coincidencias · Pulsa Iniciar para crear “$limpio” y elegir su categoría"
            tvEventSearchHelper.setTextColor(ContextCompat.getColor(this, R.color.error))
        }
    }

    /**
     * HU-03 puntos 8 y 9: Al seleccionar una tarea de los resultados, queda claramente identificada
     * en la pantalla Eventos lista para iniciar su cronómetro.
     */
    private fun seleccionarTareaEnEventos(tarea: Tarea) {
        tareaSeleccionadaEnEventos = tarea
        actualizandoTextoPorSeleccion = true
        etEventTaskSearch.setText(tarea.nombre)
        etEventTaskSearch.setSelection(etEventTaskSearch.text.length)
        actualizandoTextoPorSeleccion = false

        cardSelectedTaskPreview.visibility = View.VISIBLE
        tvSelectedTaskCategoryBadge.text = "Categoría: ${tarea.categoriaNombre}"
        tvSelectedTaskPreviewTitle.text = tarea.textoConCategoria

        actualizarResultadosBusquedaEventos(tarea.nombre)
    }

    /**
     * HU-04 y HU-05: Maneja la pulsación del botón Iniciar en la pantalla Eventos.
     * - Si ya hay un evento en curso, impide iniciar otro.
     * - Si hay una tarea seleccionada o el texto escrito coincide con una tarea existente en SQLite,
     *   inicia directamente el evento para esa tarea.
     * - Si el usuario escribió el nombre de una tarea que no existe, abre la hoja inferior (Pantalla 12)
     *   para elegir una categoría existente, guardar la tarea en SQLite e iniciar inmediatamente el evento.
     */
    private fun manejarClickIniciarEvento() {
        val activo = dbHelper.obtenerEventoActivo()
        if (activo != null) {
            DialogHelper.mostrarSnackbarError(
                rootMain,
                "Ya existe un evento en curso (“${activo.tareaNombre}”). Finalízalo antes de iniciar otro."
            )
            sincronizarEstadoEventoActivoYRecientes()
            return
        }

        val textoEscrito = ValidationLogic.normalizarNombre(etEventTaskSearch.text?.toString().orEmpty())
        val seleccionada = tareaSeleccionadaEnEventos

        // 1. Si el usuario seleccionó una tarea de la lista y el texto coincide (o no lo borró)
        if (seleccionada != null &&
            (textoEscrito.isEmpty() || ValidationLogic.sonNombresEquivalentes(textoEscrito, seleccionada.nombre))
        ) {
            iniciarEventoParaTareaExistente(seleccionada)
            return
        }

        // 2. Validar que haya escrito un nombre si no seleccionó ninguna tarea
        if (textoEscrito.isEmpty()) {
            DialogHelper.mostrarSnackbarError(
                rootMain,
                "Escribe o selecciona una tarea antes de pulsar Iniciar."
            )
            etEventTaskSearch.requestFocus()
            return
        }

        // 3. Requisito HU-04.10: Comprobar en la base de datos si ya existe una tarea con ese nombre
        //    aunque el usuario no haya tocado la lista de sugerencias
        val tareaExistenteEnDb = dbHelper.obtenerTareaPorNombreExacto(textoEscrito)
        if (tareaExistenteEnDb != null) {
            seleccionarTareaEnEventos(tareaExistenteEnDb)
            iniciarEventoParaTareaExistente(tareaExistenteEnDb)
            return
        }

        // 4. La tarea no existe: solicitar categoría con el componente de Stitch (Pantalla 12)
        val categorias = dbHelper.listarCategorias()
        DialogHelper.mostrarHojaCrearTareaDesdeEventos(
            context = this,
            nombreInicial = textoEscrito,
            categorias = categorias,
            onCrearEIniciar = { nombreInput, categoriaIdSeleccionada ->
                dbHelper.crearTareaEIniciarEvento(
                    nombreTareaInput = nombreInput,
                    categoriaId = categoriaIdSeleccionada,
                    fechaInicio = LocalDateTime.now()
                )
            },
            onExito = { eventoIniciado, mensaje ->
                limpiarBuscadorEventosTrasIniciar()
                recargarDatosYActualizarUI()
                DialogHelper.mostrarSnackbarExito(
                    rootMain,
                    "Tarea “${eventoIniciado.textoTareaConCategoria}” creada y evento iniciado."
                )
            }
        )
    }

    private fun iniciarEventoParaTareaExistente(tarea: Tarea) {
        when (val res = dbHelper.iniciarEvento(tarea.id, LocalDateTime.now())) {
            is ResultadoEvento.Exito -> {
                limpiarBuscadorEventosTrasIniciar()
                recargarDatosYActualizarUI()
                DialogHelper.mostrarSnackbarExito(rootMain, res.mensaje)
            }
            is ResultadoEvento.Error -> {
                DialogHelper.mostrarSnackbarError(rootMain, res.mensaje)
                sincronizarEstadoEventoActivoYRecientes()
            }
        }
    }

    private fun limpiarBuscadorEventosTrasIniciar() {
        tareaSeleccionadaEnEventos = null
        cardSelectedTaskPreview.visibility = View.GONE
        actualizandoTextoPorSeleccion = true
        etEventTaskSearch.setText("")
        actualizandoTextoPorSeleccion = false
        rvEventSearchMatches.visibility = View.GONE
        cardEventSearchNoResults.visibility = View.GONE
        tvEventSearchHelper.text = getString(R.string.events_search_helper_short)
        tvEventSearchHelper.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant))
    }

    /**
     * HU-05: Finaliza el evento actualmente en curso, guarda fecha_fin y duracion_segundos en SQLite,
     * actualiza el historial y muestra la confirmación de finalización (Pantalla 14 de Stitch).
     */
    private fun manejarClickFinalizarEvento() {
        btnStopEvent.isEnabled = false
        when (val res = dbHelper.finalizarEventoActivo(LocalDateTime.now())) {
            is ResultadoEvento.Exito -> {
                timerHandler.removeCallbacks(timerRunnable)
                recargarDatosYActualizarUI()
                DialogHelper.mostrarDialogoConfirmacionEventoFinalizado(
                    context = this,
                    eventoFinalizado = res.evento
                )
            }
            is ResultadoEvento.Error -> {
                DialogHelper.mostrarSnackbarError(rootMain, res.mensaje)
                sincronizarEstadoEventoActivoYRecientes()
            }
        }
    }

    private fun abrirHistorialCompletoEventos() {
        startActivity(Intent(this, EventHistoryActivity::class.java))
    }

    /**
     * Sincroniza la pestaña Eventos con SQLite (Sección 7 y Sección 9):
     * - Recupera si existe un evento en curso (`obtenerEventoActivo()`).
     * - Si existe: muestra la tarjeta activa (Pantalla 13), deshabilita Iniciar,
     *   habilita Finalizar y arranca el cronómetro en vivo calculado desde `fecha_inicio`.
     *   El campo de búsqueda y el botón "X" permanecen siempre habilitados para que el usuario
     *   pueda escribir, borrar caracteres o limpiar su búsqueda sin bloqueo.
     * - Si no existe: muestra el estado inactivo (Pantalla 10), habilita Iniciar
     *   y deshabilita Finalizar.
     * - Actualiza la lista de últimos eventos registrados.
     */
    private fun sincronizarEstadoEventoActivoYRecientes() {
        val eventoActivo = dbHelper.obtenerEventoActivo()
        timerHandler.removeCallbacks(timerRunnable)

        // El buscador y el botón de limpiar ("X") nunca se bloquean para permitir escribir y borrar libremente
        etEventTaskSearch.isEnabled = true
        btnClearEventSearch.isEnabled = true

        if (eventoActivo != null) {
            // Estado En Curso (Pantalla 13 de Stitch)
            tvEventLiveStatusBadge.text = "● EN VIVO"
            tvEventLiveStatusBadge.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.secondary_fixed)
            )
            tvEventLiveStatusBadge.setTextColor(
                ContextCompat.getColor(this, R.color.on_secondary_fixed)
            )

            cardEventIdleState.visibility = View.GONE
            cardEventActiveState.visibility = View.VISIBLE

            val estilo = CategoryVisualStyle.obtenerEstilo(eventoActivo.categoriaNombre)
            val accentColor = ContextCompat.getColor(this, estilo.accentColorRes)
            val badgeBgColor = ContextCompat.getColor(this, estilo.badgeBgColorRes)
            val badgeTextColor = ContextCompat.getColor(this, estilo.badgeTextColorRes)

            viewActiveEventStripe.setBackgroundColor(accentColor)
            llActiveEventCategoryBadge.backgroundTintList = ColorStateList.valueOf(badgeBgColor)
            ivActiveEventCategoryIcon.setImageResource(estilo.iconRes)
            ivActiveEventCategoryIcon.imageTintList = ColorStateList.valueOf(badgeTextColor)
            tvActiveEventCategoryChip.text = eventoActivo.categoriaNombre
            tvActiveEventCategoryChip.setTextColor(badgeTextColor)

            tvActiveEventIdBadge.text = "ID #EVT-${eventoActivo.id.toString().padStart(3, '0')}"
            tvActiveEventTaskTitle.text = eventoActivo.tareaNombre
            tvActiveEventTaskSubtitle.text = eventoActivo.textoTareaConCategoria
            tvActiveEventStartDate.text = eventoActivo.formatearFechaInicioCompleta()
            tvActiveEventStartTime.text = eventoActivo.formatearHoraInicioConSegundos()

            val segundosActuales = eventoActivo.obtenerSegundosTranscurridos(LocalDateTime.now())
            tvActiveEventTimerDisplay.text = Evento.formatearCronometro(segundosActuales)

            // Regla HU-05 y Sección 7: Mantener Iniciar deshabilitado y Finalizar habilitado
            llEventsSearchLockedBanner.visibility = View.VISIBLE
            btnStartEvent.isEnabled = false
            btnStartEvent.alpha = 0.45f
            btnQuickCreateFromSearch.isEnabled = false
            btnQuickCreateFromSearch.alpha = 0.45f
            btnStopEvent.isEnabled = true
            btnStopEvent.alpha = 1.0f

            timerHandler.postDelayed(timerRunnable, 1000L)
        } else {
            // Estado Inactivo (Pantalla 10 de Stitch)
            tvEventLiveStatusBadge.text = "● Inactivo"
            tvEventLiveStatusBadge.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.surface_container_high)
            )
            tvEventLiveStatusBadge.setTextColor(
                ContextCompat.getColor(this, R.color.on_surface_variant)
            )

            cardEventIdleState.visibility = View.VISIBLE
            cardEventActiveState.visibility = View.GONE

            llEventsSearchLockedBanner.visibility = View.GONE
            btnStartEvent.isEnabled = true
            btnStartEvent.alpha = 1.0f
            btnQuickCreateFromSearch.isEnabled = true
            btnQuickCreateFromSearch.alpha = 1.0f
            btnStopEvent.isEnabled = false
        }

        // Actualizar lista de últimos eventos registrados en Eventos
        val eventosRecientes = dbHelper.listarEventos(categoriaIdFiltro = null, limite = 5)
        if (eventosRecientes.isEmpty()) {
            rvRecentEvents.visibility = View.GONE
            cardEmptyRecentEvents.visibility = View.VISIBLE
        } else {
            rvRecentEvents.visibility = View.VISIBLE
            cardEmptyRecentEvents.visibility = View.GONE
            recentEventsAdapter.actualizarDatos(eventosRecientes)
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
        INICIO, CATEGORIAS, TAREAS, EVENTOS
    }

    private fun mostrarSeccion(seccion: SeccionActiva) {
        when (seccion) {
            SeccionActiva.INICIO -> {
                tvTopAppSubtitle.text = getString(R.string.nav_home)
                viewHome.visibility = View.VISIBLE
                viewCategories.visibility = View.GONE
                viewTasks.visibility = View.GONE
                viewEvents.visibility = View.GONE
                fabMain.visibility = View.GONE
            }
            SeccionActiva.CATEGORIAS -> {
                tvTopAppSubtitle.text = getString(R.string.nav_categories)
                viewHome.visibility = View.GONE
                viewCategories.visibility = View.VISIBLE
                viewTasks.visibility = View.GONE
                viewEvents.visibility = View.GONE
                fabMain.visibility = View.VISIBLE
                fabMain.contentDescription = "Crear nueva categoría"
            }
            SeccionActiva.TAREAS -> {
                tvTopAppSubtitle.text = getString(R.string.nav_tasks)
                viewHome.visibility = View.GONE
                viewCategories.visibility = View.GONE
                viewTasks.visibility = View.VISIBLE
                viewEvents.visibility = View.GONE
                fabMain.visibility = View.VISIBLE
                fabMain.contentDescription = "Agregar nueva tarea"
            }
            SeccionActiva.EVENTOS -> {
                tvTopAppSubtitle.text = getString(R.string.nav_events)
                viewHome.visibility = View.GONE
                viewCategories.visibility = View.GONE
                viewTasks.visibility = View.GONE
                viewEvents.visibility = View.VISIBLE
                fabMain.visibility = View.GONE
            }
        }
        recargarDatosYActualizarUI()
    }

    /**
     * Muestra el nombre del estudiante y la versión actual:
     * "Nombre Apellido · v0.2.0 (2)"
     */
    private fun actualizarCabeceraEstudianteYVersion() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nombreGuardado = prefs.getString(KEY_STUDENT_NAME, null)
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.student_name)

        val pkgInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = pkgInfo.versionName ?: "0.2.0"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode.toLong()
        }

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
            .setMessage("Personaliza el nombre que se muestra junto a la versión de la aplicación:")
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
        val todosLosEventos = dbHelper.listarEventos(categoriaIdFiltro = null)

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
        val totalEvents = todosLosEventos.size

        tvQuickCategoriesCount.text = if (totalCats == 1) "1 configurada" else "$totalCats configuradas"
        tvQuickTasksBadge.text = totalTasks.toString()
        tvQuickTasksCount.text = if (totalTasks == 1) "1 registrada" else "$totalTasks registradas"
        tvHeroSummary.text =
            "Resumen actual: $totalCats categorías, $totalTasks tareas y $totalEvents eventos"

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

        // 4. Actualizar Pantallas 10, 11 y 13: Eventos y recuperación de evento activo
        sincronizarEstadoEventoActivoYRecientes()
        val textoBusquedaActual = etEventTaskSearch.text?.toString().orEmpty()
        if (textoBusquedaActual.isNotEmpty()) {
            actualizarResultadosBusquedaEventos(textoBusquedaActual)
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
    // NAVEGACIÓN A FORMULARIOS Y DIÁLOGOS DE CONFIRMACIÓN / BLOQUEO (SPRINT 1)
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
            DialogHelper.mostrarDialogoBloqueoCategoriaConTareas(
                context = this,
                nombreCategoria = categoria.nombre,
                tareasAsociadas = tareasAsociadas
            )
            return
        }

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