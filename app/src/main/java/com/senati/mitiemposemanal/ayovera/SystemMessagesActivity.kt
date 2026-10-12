package com.senati.mitiemposemanal.ayovera

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.senati.mitiemposemanal.ayovera.model.Categoria
import com.senati.mitiemposemanal.ayovera.model.Tarea
import com.senati.mitiemposemanal.ayovera.ui.DialogHelper

/**
 * Actividad dedicada para visualizar e interactuar con la Pantalla 8 de Stitch
 * ("Confirmaciones y Mensajes" — Catálogo UI / Specs M3).
 */
class SystemMessagesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_system_messages)

        val root = findViewById<View>(R.id.rootSystemMessages)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBackMessages).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnDemoDialogDeleteCat).setOnClickListener {
            val catDemo = Categoria(id = 4L, nombre = "Ocio", cantidadTareas = 0)
            DialogHelper.mostrarDialogoConfirmarEliminarCategoria(this, catDemo) {
                DialogHelper.mostrarSnackbarExito(root, "Demostración: Categoría “Ocio” eliminada")
            }
        }

        findViewById<MaterialButton>(R.id.btnDemoDialogBlockedCat).setOnClickListener {
            val tareasDemo = listOf(
                Tarea(id = 1L, nombre = "Leer Kotlin", categoriaId = 1L, categoriaNombre = "Estudio"),
                Tarea(id = 2L, nombre = "Mantenimiento CNC", categoriaId = 1L, categoriaNombre = "Estudio")
            )
            DialogHelper.mostrarDialogoBloqueoCategoriaConTareas(this, "Estudio", tareasDemo)
        }

        findViewById<MaterialButton>(R.id.btnDemoDialogDeleteTask).setOnClickListener {
            val tareaDemo = Tarea(
                id = 1L,
                nombre = "Leer Kotlin",
                categoriaId = 1L,
                categoriaNombre = "Estudio"
            )
            DialogHelper.mostrarDialogoConfirmarEliminarTarea(this, tareaDemo) {
                DialogHelper.mostrarSnackbarExito(root, "Demostración: Tarea “Leer Kotlin” eliminada")
            }
        }

        findViewById<MaterialButton>(R.id.btnDemoSnackbarSuccess).setOnClickListener {
            DialogHelper.mostrarSnackbarExito(root, "Tarea guardada con éxito")
        }

        findViewById<MaterialButton>(R.id.btnDemoSnackbarError).setOnClickListener {
            DialogHelper.mostrarSnackbarError(root, "Error: Ya existe una categoría con este nombre")
        }
    }
}

