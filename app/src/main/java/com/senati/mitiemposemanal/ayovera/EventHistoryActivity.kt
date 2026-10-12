package com.senati.mitiemposemanal.ayovera

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.senati.mitiemposemanal.ayovera.data.AdminSQLiteOpenHelper
import com.senati.mitiemposemanal.ayovera.model.Categoria
import com.senati.mitiemposemanal.ayovera.ui.EventHistoryAdapter

/**
 * Pantalla 15 de Stitch: Historial de Eventos (Sprint 2 — Sección 9).
 * Muestra todos los eventos registrados en SQLite con filtros por categoría,
 * estado en curso o finalizado, fechas/horas de inicio y fin y duración.
 */
class EventHistoryActivity : AppCompatActivity() {

    private lateinit var dbHelper: AdminSQLiteOpenHelper
    private lateinit var tvTotalCountBadge: TextView
    private lateinit var llCategoryChips: LinearLayout
    private lateinit var rvHistoryEvents: RecyclerView
    private lateinit var cardEmptyHistoryState: MaterialCardView
    private lateinit var tvEmptyHistoryTitle: TextView
    private lateinit var tvEmptyHistoryDesc: TextView
    private lateinit var historyAdapter: EventHistoryAdapter

    private var filtroCategoriaId: Long? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val liveHistoryTick = object : Runnable {
        override fun run() {
            if (dbHelper.obtenerEventoActivo() != null) {
                cargarHistorial()
                mainHandler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_event_history)

        val root = findViewById<View>(R.id.rootEventHistory)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = AdminSQLiteOpenHelper(this)
        dbHelper.asegurarCategoriasInicialesSiVacio()

        tvTotalCountBadge = findViewById(R.id.tvHistoryTotalCountBadge)
        llCategoryChips = findViewById(R.id.llHistoryCategoryChips)
        rvHistoryEvents = findViewById(R.id.rvHistoryEvents)
        cardEmptyHistoryState = findViewById(R.id.cardEmptyHistoryState)
        tvEmptyHistoryTitle = findViewById(R.id.tvEmptyHistoryTitle)
        tvEmptyHistoryDesc = findViewById(R.id.tvEmptyHistoryDesc)

        historyAdapter = EventHistoryAdapter(emptyList())
        rvHistoryEvents.layoutManager = LinearLayoutManager(this)
        rvHistoryEvents.adapter = historyAdapter

        findViewById<ImageButton>(R.id.btnBackFromHistory).setOnClickListener {
            finish()
        }
        findViewById<MaterialButton>(R.id.btnEmptyGoToEvents).setOnClickListener {
            finish()
        }
        findViewById<FloatingActionButton>(R.id.fabHistoryNewEvent).setOnClickListener {
            finish()
        }

        cargarHistorial()
    }

    override fun onResume() {
        super.onResume()
        cargarHistorial()
        mainHandler.removeCallbacks(liveHistoryTick)
        if (dbHelper.obtenerEventoActivo() != null) {
            mainHandler.postDelayed(liveHistoryTick, 1000L)
        }
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(liveHistoryTick)
    }

    private fun cargarHistorial() {
        val categorias = dbHelper.listarCategorias()
        val todosEventos = dbHelper.listarEventos(categoriaIdFiltro = null)

        if (filtroCategoriaId != null && categorias.none { it.id == filtroCategoriaId }) {
            filtroCategoriaId = null
        }

        val eventosFiltrados = if (filtroCategoriaId != null) {
            dbHelper.listarEventos(categoriaIdFiltro = filtroCategoriaId)
        } else {
            todosEventos
        }

        val total = eventosFiltrados.size
        tvTotalCountBadge.text = if (total == 1) "1 registro" else "$total registros"

        renderizarChipsFiltro(categorias, todosEventos.size)

        if (eventosFiltrados.isEmpty()) {
            rvHistoryEvents.visibility = View.GONE
            cardEmptyHistoryState.visibility = View.VISIBLE
            val catFiltro = categorias.firstOrNull { it.id == filtroCategoriaId }
            if (catFiltro != null) {
                tvEmptyHistoryTitle.text = "Sin eventos en “${catFiltro.nombre}”"
                tvEmptyHistoryDesc.text =
                    "Aún no has registrado bloques de tiempo para tareas de la categoría “${catFiltro.nombre}”."
            } else {
                tvEmptyHistoryTitle.text = getString(R.string.empty_events_title)
                tvEmptyHistoryDesc.text =
                    "Ve a la pestaña Eventos, elige una tarea y pulsa Iniciar para comenzar a registrar tu tiempo."
            }
        } else {
            rvHistoryEvents.visibility = View.VISIBLE
            cardEmptyHistoryState.visibility = View.GONE
            historyAdapter.actualizarDatos(eventosFiltrados)
        }
    }

    private fun renderizarChipsFiltro(categorias: List<Categoria>, totalEventos: Int) {
        llCategoryChips.removeAllViews()

        fun crearChip(titulo: String, seleccionado: Boolean, onClick: () -> Unit): MaterialButton {
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
                    (36 * resources.displayMetrics.density).toInt()
                ).apply {
                    marginEnd = (8 * resources.displayMetrics.density).toInt()
                }
                layoutParams = params
                if (seleccionado) {
                    backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(this@EventHistoryActivity, R.color.primary_fixed)
                    )
                    setTextColor(
                        ContextCompat.getColor(this@EventHistoryActivity, R.color.on_primary_fixed)
                    )
                } else {
                    backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(this@EventHistoryActivity, R.color.surface_container_low)
                    )
                    setTextColor(
                        ContextCompat.getColor(this@EventHistoryActivity, R.color.on_surface_variant)
                    )
                }
                setOnClickListener { onClick() }
            }
        }

        llCategoryChips.addView(
            crearChip(
                titulo = "Todos ($totalEventos)",
                seleccionado = filtroCategoriaId == null
            ) {
                filtroCategoriaId = null
                cargarHistorial()
            }
        )

        categorias.forEach { cat ->
            llCategoryChips.addView(
                crearChip(
                    titulo = cat.nombre,
                    seleccionado = filtroCategoriaId == cat.id
                ) {
                    filtroCategoriaId = cat.id
                    cargarHistorial()
                }
            )
        }
    }
}

