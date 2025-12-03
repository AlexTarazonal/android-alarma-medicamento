package com.tfinal.proyectofinal

import android.content.*
import android.app.*
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getIntExtra("med_id", -1)
        val action = intent.action ?: return

        val db = AppDatabase.getDatabase(context)

        when (action) {
            "ACTION_APAGAR" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val med = db.medItemDao().getById(medId)
                    med?.let {
                        val historialMed = it.copy()
                        historialMed.status = "Tomada"

                        db.medItemDao().insert(historialMed)
                    }
                }
                cancelAlarm(context, medId)
                NotificationManagerCompat.from(context).cancel(medId)
            }

            "ACTION_POSPONER" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val med = db.medItemDao().getById(medId)
                    med?.let {
                        val historialMed = it.copy()
                        historialMed.status = "Pospuesta"

                        db.medItemDao().insert(historialMed)
                    }
                }
                posponerAlarma(context, medId)
                NotificationManagerCompat.from(context).cancel(medId)
            }
        }
    }



    private fun cancelAlarm(context: Context, medId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, medId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    private fun posponerAlarma(context: Context, medId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                programarAlarmaExacta(context, medId, alarmManager)
            } else {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                    Toast.makeText(context, "Habilita el permiso de 'Alarmas exactas' en la configuración.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "No se pudo abrir la configuración para el permiso de alarmas exactas.", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            programarAlarmaExacta(context, medId, alarmManager)
        }
    }

    private fun programarAlarmaExacta(context: Context, medId: Int, alarmManager: AlarmManager) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("med_id", medId)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            medId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = System.currentTimeMillis() + 5 * 60 * 1000 // Posponer por 5 minutos

        try {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            Toast.makeText(context, "⏰ Alarma programada.", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(context, "No se pudo programar la alarma exacta. Por favor, verifica tus permisos.", Toast.LENGTH_LONG).show()
        }
    }


}
