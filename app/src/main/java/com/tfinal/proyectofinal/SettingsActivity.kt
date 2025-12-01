package com.tfinal.proyectofinal

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.tfinal.proyectofinal.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var selectedSoundUri: Uri? = null
    private lateinit var vibrator: Vibrator

    private val prefs by lazy {
        getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }

    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                selectedSoundUri = uri
                val ringtone = RingtoneManager.getRingtone(this, uri)
                val title = ringtone?.getTitle(this)
                binding.buttonChangeSound.text = title ?: "Sonido seleccionado"
                prefs.edit().putString("notification_sound", uri.toString()).apply()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        loadPreferences()

        binding.buttonChangeSound.setOnClickListener {
            openRingtonePicker()
        }

        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibration_enabled", isChecked).apply()
            if (isChecked) vibrateShort()
        }

        binding.txtDuracionProp.setOnClickListener {
            showOptionDialog(
                title = "Selecciona duración del recordatorio",
                options = arrayOf("5m", "10m", "15m"),
                preferenceKey = "reminder_duration",
                targetViewId = R.id.txtDuracionProp
            )
        }

        binding.txtDiasAnt.setOnClickListener {
            showOptionDialog(
                title = "Selecciona tiempo de aviso",
                options = arrayOf("8H", "12H", "24H"),
                preferenceKey = "advance_time",
                targetViewId = R.id.txtDiasAnt
            )
        }

        binding.buttonLogout.setOnClickListener {
            confirmLogoutAndDelete()
        }

        setupBottomMenu()
    }

    private fun setupBottomMenu() {
        binding.bottomNavigation.selectedItemId = R.id.nav_config
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_medicamentos -> {
                    startActivity(Intent(this, Principal::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_historial -> {
                    startActivity(Intent(this, Historial::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_config -> true
                else -> false
            }
        }
    }

    private fun showOptionDialog(title: String, options: Array<String>, preferenceKey: String, targetViewId: Int) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(options) { _, which ->
                val selected = options[which]
                prefs.edit().putString(preferenceKey, selected).apply()
                when (targetViewId) {
                    R.id.txtDuracionProp -> binding.txtDuracionProp.text = selected
                    R.id.txtDiasAnt -> binding.txtDiasAnt.text = selected
                }
            }
            .show()
    }

    private fun loadPreferences() {
        val soundUri = prefs.getString("notification_sound", null)
        val vibrationEnabled = prefs.getBoolean("vibration_enabled", true)
        val duration = prefs.getString("reminder_duration", "10m")
        val advanceTime = prefs.getString("advance_time", "24H")

        selectedSoundUri = soundUri?.let { Uri.parse(it) }
        binding.switchVibration.isChecked = vibrationEnabled
        binding.txtDuracionProp.text = duration
        binding.txtDiasAnt.text = advanceTime

        if (selectedSoundUri != null) {
            val ringtone = RingtoneManager.getRingtone(this, selectedSoundUri)
            binding.buttonChangeSound.text = ringtone?.getTitle(this) ?: "Sonido personalizado"
        }
    }

    private fun openRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Selecciona un tono de notificación")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            selectedSoundUri?.let {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it)
            }
        }
        ringtonePickerLauncher.launch(intent)
    }

    private fun vibrateShort() {
        if (vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        }
    }

    private fun confirmLogoutAndDelete() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(this, Login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    companion object {
        fun getSelectedSound(context: Context): Uri? {
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val uriString = prefs.getString("notification_sound", null)
            return uriString?.let { Uri.parse(it) }
        }

        fun isVibrationEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            return prefs.getBoolean("vibration_enabled", true)
        }

        fun getReminderDuration(context: Context): String {
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            return prefs.getString("reminder_duration", "10m")!!
        }

        fun getAdvanceTime(context: Context): String {
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            return prefs.getString("advance_time", "24H")!!
        }
    }
}
