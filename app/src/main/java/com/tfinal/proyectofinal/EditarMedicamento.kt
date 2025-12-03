package com.tfinal.proyectofinal

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tfinal.proyectofinal.databinding.ActivityAgregarMedicamentoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditarMedicamento : AppCompatActivity() {

    private lateinit var b: ActivityAgregarMedicamentoBinding
    private lateinit var db: AppDatabase
    private var medId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityAgregarMedicamentoBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = AppDatabase.getDatabase(this)

        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Editar medicamento"

        b.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val unidades = listOf("mg", "gr", "ml")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, unidades)
        b.txtUnidad.setAdapter(adapter)

        medId = intent.getIntExtra("id", -1)
        val nombre = intent.getStringExtra("nombre") ?: ""
        val dosis = intent.getStringExtra("dosis") ?: ""
        val desc = intent.getStringExtra("descipcion") ?: ""
        val dias = intent.getIntExtra("dias", 0)
        val horas = intent.getIntExtra("horas", 0)

        if (medId == -1) {
            Toast.makeText(this, "Error: id de medicamento inválido", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        b.txtMedicamento.setText(nombre)

        val partesDosis = dosis.split(" ")
        if (partesDosis.size >= 2) {
            b.txtCantidad.setText(partesDosis[0])
            b.txtUnidad.setText(partesDosis[1], false)
        } else {
            b.txtCantidad.setText(dosis)
        }

        b.txtDescipcion.setText(desc)
        b.txtDias.setText(dias.toString())
        b.txtHoras.setText(horas.toString())

        b.btnRegistrarMed.text = "Guardar cambios"

        b.btnRegistrarMed.setOnClickListener {
            val nuevoNombre = b.txtMedicamento.text.toString().trim()
            val cantidad = b.txtCantidad.text.toString().trim()
            val unidad = b.txtUnidad.text.toString().trim()
            val nuevaDesc = b.txtDescipcion.text.toString().trim()
            val nuevosDias = b.txtDias.text.toString().toIntOrNull() ?: 0
            val nuevasHoras = b.txtHoras.text.toString().toIntOrNull() ?: 0

            if (nuevoNombre.isBlank() ||
                cantidad.isBlank() ||
                unidad.isBlank() ||
                nuevaDesc.isBlank() ||
                nuevosDias <= 0 ||
                nuevasHoras <= 0
            ) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nuevaDosis = "$cantidad $unidad"

            lifecycleScope.launch(Dispatchers.IO) {
                val dao = db.medItemDao()
                val medActual = dao.getById(medId)

                if (medActual == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@EditarMedicamento,
                            "No se encontró el medicamento en la base de datos",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@launch
                }

                val medEditado = medActual.copy(
                    name = nuevoNombre,
                    dose = nuevaDosis,
                    desc = nuevaDesc,
                    days = nuevosDias,
                    hours = nuevasHoras
                )

                dao.update(medEditado)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@EditarMedicamento,
                        "Medicamento actualizado",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }
}
