package com.tfinal.proyectofinal

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tfinal.proyectofinal.databinding.ActivityAlarmBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.KeyguardManager
import android.view.WindowManager
class AlarmActivity : AppCompatActivity() {

    private lateinit var b: ActivityAlarmBinding
    private lateinit var ringtone: Ringtone
    private lateinit var db: AppDatabase
    private var medId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(b.root)
        val medName = intent.getStringExtra("med_name") ?: "Medicamento"
        b.txtTitulo.text = "⏰ Hora de tu medicamento: $medName"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java)
                ?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        db = AppDatabase.getDatabase(this)

        medId = intent.getIntExtra("med_id", -1)
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, uri)
        ringtone.play()

        b.btnApagar.setOnClickListener {
            stopRingtone()
            markAs("Tomada")
            cancelReminder()
            finish()
        }

        b.btnPosponer.setOnClickListener {
            stopRingtone()
            markAs("Pospuesta")
            posponeAlarm()
            finish()
        }

        lifecycleScope.launch {
            kotlinx.coroutines.delay(5 * 60 * 1000)
            if (!isFinishing) {
                markAs("Olvidada")
                finish()
            }
        }
    }

    private fun markAs(status: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val med = db.medItemDao().getById(medId)
            med?.let {
                it.status = status
                db.medItemDao().update(it)
            }
        }
    }

    private fun posponeAlarm() {
        lifecycleScope.launch(Dispatchers.IO) {
            val med = db.medItemDao().getById(medId) ?: return@launch
            withContext(Dispatchers.Main) {
                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

                val intent = Intent(this@AlarmActivity, AlarmReceiver::class.java).apply {
                    putExtra("med_id", medId)
                    putExtra("med_name", med.name)
                }

                val pending = PendingIntent.getBroadcast(
                    this@AlarmActivity,
                    medId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val triggerAt = System.currentTimeMillis() + 5 * 60 * 1000L // 5 minutos
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                    } else {
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:$packageName")
                        })
                        Toast.makeText(this@AlarmActivity, "Activa permiso para alarmas exactas", Toast.LENGTH_LONG).show()
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                }

                Toast.makeText(this@AlarmActivity, "⏰ Pospuesto 5 min", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun cancelReminder() {
        val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
        val intent = android.content.Intent(this, AlarmReceiver::class.java)
        val pending = android.app.PendingIntent.getBroadcast(
            this,
            medId,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    private fun stopRingtone() {
        if (ringtone.isPlaying) ringtone.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
    }
}

