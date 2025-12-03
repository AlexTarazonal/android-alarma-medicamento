package com.tfinal.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.tfinal.proyectofinal.databinding.ActivityHistorialBinding
import com.tfinal.proyectofinal.databinding.ItemMedBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Historial : AppCompatActivity() {

    private lateinit var b: ActivityHistorialBinding
    private lateinit var db: AppDatabase

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

        b = ActivityHistorialBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = AppDatabase.getDatabase(this)

        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        b.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        b.bottomNavigation.selectedItemId = R.id.nav_historial
        b.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_medicamentos -> {
                    startActivity(Intent(this, Principal::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_historial -> true
                R.id.nav_config -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }

        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "No hay usuario logueado", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = currentUser.uid

        lifecycleScope.launch {
            try {
                val items = withContext(Dispatchers.IO) {
                    db.medItemDao().getHistory(userId)
                }

                b.containerHistory.removeAllViews()

                if (items.isEmpty()) {
                    Toast.makeText(this@Historial, "No hay historial disponible", Toast.LENGTH_SHORT).show()
                } else {
                    items.forEach { med ->
                        val card = ItemMedBinding.inflate(layoutInflater, b.containerHistory, false)

                        card.txtName.text = med.name
                        card.txtDose.text = med.dose
                        card.txtDescipcion.text = med.desc
                        card.txtSchedule.text = "Estado: ${med.status} • ${med.hours}h × ${med.days}d"

                        card.switchMed.isVisible = false
                        card.btnSonar.isVisible = false
                        card.imgEditar.isVisible = false
                        card.txtRemaining.isVisible = false

                        b.containerHistory.addView(card.root)
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(this@Historial, "Error al cargar los datos: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
