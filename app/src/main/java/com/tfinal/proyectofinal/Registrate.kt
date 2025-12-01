package com.tfinal.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Registrate : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registrate)

        val analytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle()
        bundle.putString("message", "Integración de Firebase completa")
        analytics.logEvent("InitScreen", bundle)

        setup()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) {
            v, insets -> val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setup() {
        title = "Autenticación"
        val btnRegistrate = findViewById<Button>(R.id.btnRegistrate)
        val txtUsuario = findViewById<TextInputEditText>(R.id.txtUsuario)
        val txtCorreo = findViewById<TextInputEditText>(R.id.txtCorreo)
        val txtContraseña = findViewById<TextInputEditText>(R.id.txtContraseña)
        val txtRepiteContraseña = findViewById<TextInputEditText>(R.id.txtRepitaContraseña)
        val layoutRepiteContraseña = findViewById<TextInputLayout>(R.id.txtRepiteLayout)
        val txtIniciarSesion = findViewById<TextView>(R.id.txtIniciarSesion)

        txtIniciarSesion.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish() }
        btnRegistrate.setOnClickListener {
            val usuario = txtUsuario.text?.toString()?.trim()
            val correo = txtCorreo.text?.toString()?.trim()
            val contraseña = txtContraseña.text?.toString()?.trim()
            val repiteContraseña = txtRepiteContraseña.text?.toString()?.trim()
            layoutRepiteContraseña.error = null

            if (usuario.isNullOrEmpty() || correo.isNullOrEmpty() || contraseña.isNullOrEmpty() || repiteContraseña.isNullOrEmpty()) {
                showAlert("Por favor complete todos los campos")
                return@setOnClickListener
            }

            if (contraseña != repiteContraseña) {
                layoutRepiteContraseña.error = "La contraseña no es igual"
                return@setOnClickListener
            }
            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(correo, contraseña)
                .addOnCompleteListener {
                    task ->
                    if (task.isSuccessful) {
                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                        val db = FirebaseFirestore.getInstance()
                        val user = hashMapOf( "usuario" to usuario, "correo" to correo )
                        if (userId != null) {
                            db.collection("usuarios")
                                .document(userId)
                                .set(user) .addOnSuccessListener {
                                    val intent = Intent(this, Login::class.java)
                                    startActivity(intent)
                                    finish() }
                                .addOnFailureListener {
                                    e -> showAlert("Error al guardar en Firestore: ${e.message}")
                                }
                        }
                    } else {
                        showAlert("Error al registrar usuario: ${task.exception?.message}")
                    }
                }
        }
    }

    private fun showAlert(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}
