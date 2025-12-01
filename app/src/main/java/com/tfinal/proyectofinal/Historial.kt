package com.tfinal.proyectofinal

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.tfinal.proyectofinal.databinding.ActivityHistorialBinding
import com.tfinal.proyectofinal.databinding.ItemMedBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Historial : AppCompatActivity() {

    private lateinit var b: ActivityHistorialBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    private fun loadHistory() {
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) { db.medItemDao().getHistory() }
            b.containerHistory.removeAllViews()

            items.forEach { med ->
                val card = ItemMedBinding.inflate(layoutInflater, b.containerHistory, false).apply {
                    txtName.text = med.name
                    txtDose.text = med.dose
                    txtDescipcion.text = med.desc
                    txtSchedule.text = "Estado: ${med.status} • ${med.hours}h × ${med.days}d"

                    switchMed.isVisible = false
                    btnSonar.isVisible = false
                    imgEditar.isVisible = false
                }
                b.containerHistory.addView(card.root)
            }
        }
    }
}
