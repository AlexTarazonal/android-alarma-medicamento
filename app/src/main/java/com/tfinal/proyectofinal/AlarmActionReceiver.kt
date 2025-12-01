package com.tfinal.proyectofinal

import android.content.*
import android.app.*
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
                        it.status = "Tomada"
                        db.medItemDao().update(it)
                    }
                }
                cancelAlarm(context, medId)
                NotificationManagerCompat.from(context).cancel(medId)
            }

            "ACTION_POSPONER" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val med = db.medItemDao().getById(medId)
                    med?.let {
                        it.status = "Olvidada"
                        db.medItemDao().update(it)
                    }
                }
                posponeAlarm(context, medId)
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

    private fun posponeAlarm(context: Context, medId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("med_id", medId)
        }
        val pending = PendingIntent.getBroadcast(
            context, medId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + 5 * 60 * 1000
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }
}
