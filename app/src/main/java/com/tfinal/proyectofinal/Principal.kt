package com.tfinal.proyectofinal

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.tfinal.proyectofinal.databinding.ActivityPrincipalBinding
import com.tfinal.proyectofinal.databinding.ItemMedBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Principal : AppCompatActivity() {

    private lateinit var b: ActivityPrincipalBinding
    private lateinit var db: AppDatabase
    private var items = mutableListOf<MedItemEntity>()
    private val REQUEST_CODE_NOTIFICATION_PERMISSION = 1001

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val prefs by lazy {
        val uid = auth.currentUser?.uid ?: "guest"
        getSharedPreferences("med_prefs_$uid", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

        b = ActivityPrincipalBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = AppDatabase.getDatabase(this)

        requestNotificationPermissionIfNeeded()

        setSupportActionBar(b.toolbar)
        setupBottomNavigation()

        loadMedications()

        b.fabAdd.setOnClickListener {
            startActivity(Intent(this, AgregarMedicamento::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadMedications()
    }

    private fun setupBottomNavigation() {
        b.bottomNavigation.selectedItemId = R.id.nav_medicamentos
        b.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_medicamentos -> true
                R.id.nav_historial -> {
                    startActivity(Intent(this, Historial::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_config -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    private fun loadMedications() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "No hay usuario logueado", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = currentUser.uid

        lifecycleScope.launch {
            items = withContext(Dispatchers.IO) {
                db.medItemDao().getActive(userId).toMutableList()
            }
            refreshMedList()
        }
    }

    private fun refreshMedList() {
        b.container.removeAllViews()

        items.forEach { med ->
            val card = ItemMedBinding.inflate(layoutInflater, b.container, false)

            card.txtName.text = med.name
            card.txtDose.text = med.dose
            card.txtDescipcion.text = med.desc
            card.txtSchedule.text = "Cada ${med.hours}h por ${med.days} días"

            val nextTrigger = getNextTrigger(med.id)
            card.txtRemaining.text = formatRemaining(nextTrigger)

            card.switchMed.isChecked = true
            updateSwitchColor(card.switchMed, card.switchMed.isChecked)

            card.switchMed.setOnCheckedChangeListener { button, isChecked ->
                updateSwitchColor(button as Switch, isChecked)
                if (isChecked) {
                    scheduleNotification(med)
                    val t = getNextTrigger(med.id)
                    card.txtRemaining.text = formatRemaining(t)
                } else {
                    cancelNotification(med)
                    clearNextTrigger(med.id)
                    card.txtRemaining.text = "Sin horario"
                }
            }

            card.imgEditar.setOnClickListener {
                val intent = Intent(this@Principal, EditarMedicamento::class.java)
                intent.putExtra("id", med.id)
                intent.putExtra("nombre", med.name)
                intent.putExtra("dosis", med.dose)
                intent.putExtra("descipcion", med.desc)
                intent.putExtra("dias", med.days)
                intent.putExtra("horas", med.hours)
                startActivity(intent)
            }

            card.btnSonar.setOnClickListener {
                val medId = med.id
                val triggerTime = System.currentTimeMillis() + 5000

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                val intent = Intent(this@Principal, AlarmReceiver::class.java).apply {
                    putExtra("med_id", medId)
                    putExtra("med_name", med.name)
                    putExtra("test_only", true)
                }

                val testRequestCode = medId + 100_000

                val pendingIntent = PendingIntent.getBroadcast(
                    this@Principal,
                    testRequestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                scheduleOrAskExactAlarm(triggerTime, pendingIntent)

                saveNextTrigger(medId, triggerTime)
                card.txtRemaining.text = formatRemaining(triggerTime)
            }

            card.root.setOnLongClickListener {
                confirmDelete(med)
                true
            }

            b.container.addView(card.root)
        }
    }

    private fun confirmDelete(med: MedItemEntity) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar medicamento")
            .setMessage("¿Seguro que deseas eliminar \"${med.name}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                cancelNotification(med)

                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        db.medItemDao().update(med.apply { status = "Eliminado" })
                    }
                    items.removeAll { it.id == med.id }

                    clearNextTrigger(med.id)

                    refreshMedList()
                    Toast.makeText(this@Principal, "Medicamento eliminado", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun scheduleNotification(med: MedItemEntity) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                scheduleExactAlarm(med, alarmManager)
            } else {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    Toast.makeText(this, "Activa el permiso para alarmas exactas.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "No se pudo abrir configuración de permisos.", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            scheduleExactAlarm(med, alarmManager)
        }
    }

    private fun scheduleExactAlarm(med: MedItemEntity, alarmManager: AlarmManager) {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("med_id", med.id)
            putExtra("med_name", med.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            med.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (med.hours * 60 * 60 * 1000L)

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )

            saveNextTrigger(med.id, triggerTime)

            Toast.makeText(this, "⏰ Alarma programada.", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "No se pudo programar la alarma exacta. Verifica permisos.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateSwitchColor(switch: Switch, isChecked: Boolean) {
        val color = if (isChecked)
            ContextCompat.getColor(this, R.color.switch_active)
        else
            ContextCompat.getColor(this, R.color.gray)
        switch.trackTintList = ColorStateList.valueOf(color)
        switch.thumbTintList = ColorStateList.valueOf(color)
    }

    private fun scheduleOrAskExactAlarm(
        triggerTime: Long,
        pendingIntent: PendingIntent
    ) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                )
                Toast.makeText(this, "⏰ Alarma programada.", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                    Toast.makeText(
                        this,
                        "Activa el permiso «Alarmas y recordatorios» para esta app.",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (_: Exception) {
                    Toast.makeText(
                        this,
                        "Ve a Ajustes > Apps > ${getString(R.string.app_name)} > Alarmas y recordatorios y actívalo.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
            )
            Toast.makeText(this, "⏰ Alarma programada.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelNotification(med: MedItemEntity) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            med.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun saveNextTrigger(medId: Int, triggerTime: Long) {
        prefs.edit()
            .putLong("next_trigger_$medId", triggerTime)
            .apply()
    }

    private fun clearNextTrigger(medId: Int) {
        prefs.edit()
            .remove("next_trigger_$medId")
            .apply()
    }

    private fun getNextTrigger(medId: Int): Long {
        return prefs.getLong("next_trigger_$medId", -1L)
    }

    private fun formatRemaining(triggerTime: Long): String {
        if (triggerTime <= 0L) return "Sin horario"

        val diff = triggerTime - System.currentTimeMillis()
        if (diff <= 0L) return "¡Ya toca!"

        val totalMin = diff / 60000L
        val horas = totalMin / 60
        val minutos = totalMin % 60

        return when {
            horas > 0 && minutos > 0 -> "Faltan ${horas}h ${minutos}min"
            horas > 0 -> "Faltan ${horas}h"
            else -> "Faltan ${minutos}min"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de notificaciones concedido ✅", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "El permiso de notificaciones es necesario para mostrar alertas.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
