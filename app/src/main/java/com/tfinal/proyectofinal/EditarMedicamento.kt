package com.tfinal.proyectofinal

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tfinal.proyectofinal.databinding.ActivityEditarMedicamentoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditarMedicamento : AppCompatActivity() {

    private lateinit var b: ActivityEditarMedicamentoBinding
    private lateinit var db: AppDatabase
    private var medId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityEditarMedicamentoBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = AppDatabase.getDatabase(this)

        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        b.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        medId = intent.getIntExtra("id", -1)
        val nombre = intent.getStringExtra("nombre") ?: ""
        val dosis = intent.getStringExtra("dosis") ?: ""
        val descripcion = intent.getStringExtra("descipcion") ?: ""
        val dias = intent.getIntExtra("dias", 0)
        val horas = intent.getIntExtra("horas", 0)

        b.txtMedicamento.setText(nombre)
        val parts = dosis.split(" ")
        if (parts.size == 2) {
            b.txtCantidad.setText(parts[0])
            b.txtUnidad.setText(parts[1])
        }
        b.txtDescripcion.setText(descripcion)
        b.txtDias.setText(dias.toString())
        b.txtHoras.setText(horas.toString())

        val unidades = arrayOf("mg", "g", "ml")
        b.txtUnidad.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, unidades))

        b.btnGuardar.setOnClickListener {
            val nuevoNombre = b.txtMedicamento.text.toString()
            val nuevaCantidad = b.txtCantidad.text.toString()
            val nuevaUnidad = b.txtUnidad.text.toString()
            val nuevoDescipcion = b.txtDescripcion.text.toString()
            val nuevosDias = b.txtDias.text.toString().toIntOrNull() ?: 0
            val nuevasHoras = b.txtHoras.text.toString().toIntOrNull() ?: 0

            if (nuevoNombre.isBlank() || nuevaCantidad.isBlank() || nuevaUnidad.isBlank() || nuevosDias <= 0 || nuevasHoras <= 0) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val med = MedItemEntity(
                    id = medId,
                    name = nuevoNombre,
                    dose = "$nuevaCantidad $nuevaUnidad",
                    desc = nuevoDescipcion,
                    days = nuevosDias,
                    hours = nuevasHoras
                )
                withContext(Dispatchers.IO) {
                    if (medId == -1) {
                        db.medItemDao().insert(med)
                    } else {
                        db.medItemDao().update(med)
                    }
                }
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }
}
