package com.tfinal.proyectofinal

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.tfinal.proyectofinal.databinding.ActivityAgregarMedicamentoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AgregarMedicamento : AppCompatActivity() {

    private lateinit var b: ActivityAgregarMedicamentoBinding
    private lateinit var db: AppDatabase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAgregarMedicamentoBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = AppDatabase.getDatabase(this)
        auth = FirebaseAuth.getInstance()

        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        b.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val unidades = listOf("mg", "gr", "ml")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, unidades)
        b.txtUnidad.setAdapter(adapter)

        b.btnRegistrarMed.setOnClickListener {
            val nombre = b.txtMedicamento.text.toString().trim()
            val cantidad = b.txtCantidad.text.toString().trim()
            val unidad = b.txtUnidad.text.toString().trim()
            val descripcion = b.txtDescipcion.text.toString().trim()
            val dias = b.txtDias.text.toString().toIntOrNull() ?: 0
            val horas = b.txtHoras.text.toString().toIntOrNull() ?: 0

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "No hay usuario logueado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val userId = currentUser.uid

            if (nombre.isBlank() || cantidad.isBlank() || unidad.isBlank() ||
                descripcion.isBlank() || dias <= 0 || horas <= 0
            ) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dosis = "$cantidad $unidad"

            lifecycleScope.launch(Dispatchers.IO) {
                db.medItemDao().insert(
                    MedItemEntity(
                        name = nombre,
                        dose = dosis,
                        desc = descripcion,
                        days = dias,
                        hours = horas,
                        userId = userId,
                        status = "Pendiente"
                    )
                )
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@AgregarMedicamento,
                        "Medicamento registrado",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }
}
