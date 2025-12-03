package com.tfinal.proyectofinal

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getIntExtra("med_id", -1)
        val name = intent.getStringExtra("med_name") ?: "Medicamento"
        val isTest = intent.getBooleanExtra("test_only", false)

        val channelId = "med_alarm"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            nm.getNotificationChannel(channelId) == null
        ) {
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Alarmas de Medicamentos",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones de alarma de medicamentos"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
        }

        val canNotify = (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                )
        if (!canNotify) return

        val tapIntent = Intent(context, Principal::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            medId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val apagarIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = "ACTION_APAGAR"
            putExtra("med_id", medId)
            putExtra("test_only", isTest)
        }
        val apagarPending = PendingIntent.getBroadcast(
            context,
            medId + 1000,
            apagarIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val posponerIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = "ACTION_POSPONER"
            putExtra("med_id", medId)
            putExtra("test_only", isTest)
        }
        val posponerPending = PendingIntent.getBroadcast(
            context,
            medId + 2000,
            posponerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isTest) {
            "Prueba de alarma de medicamento"
        } else {
            "¡Es hora de tomar tu medicamento!"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(name)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(android.R.drawable.ic_media_pause, "Apagar", apagarPending)
            .addAction(android.R.drawable.ic_media_play, "Posponer", posponerPending)

        NotificationManagerCompat.from(context).notify(medId, builder.build())
    }
}
