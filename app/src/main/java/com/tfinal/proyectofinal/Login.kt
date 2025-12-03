package com.tfinal.proyectofinal

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class Login : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupGoogleSignIn()
        setupUI()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupUI() {
        title = "Inicio de Sesión"

        val btnIniciarSesion = findViewById<Button>(R.id.btnIniciarSesion)
        val txtUsuarioCorreo = findViewById<TextInputEditText>(R.id.txtUsuario)
        val txtContraseña = findViewById<TextInputEditText>(R.id.txtContraseña)
        val txtRegistrate = findViewById<TextView>(R.id.txtRegistrar)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)

        txtRegistrate.setOnClickListener {
            startActivity(Intent(this, Registrate::class.java))
            finish()
        }

        btnIniciarSesion.setOnClickListener {
            val usuarioCorreo = txtUsuarioCorreo.text?.toString()?.trim()
            val contraseña = txtContraseña.text?.toString()?.trim()

            if (usuarioCorreo.isNullOrEmpty() || contraseña.isNullOrEmpty()) {
                showAlert("Por favor ingrese su usuario o correo y contraseña")
                return@setOnClickListener
            }

            if (usuarioCorreo.contains("@")) {
                // Login usando correo
                loginWithEmail(usuarioCorreo, contraseña)
            } else {
                // Login usando nombre de usuario (busca correo en Firestore)
                db.collection("usuarios")
                    .whereEqualTo("usuario", usuarioCorreo)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val correo = documents.documents[0].getString("correo")
                            if (!correo.isNullOrEmpty()) {
                                loginWithEmail(correo, contraseña)
                            } else {
                                showAlert("No se encontró un correo para este usuario")
                            }
                        } else {
                            showAlert("Usuario no encontrado")
                        }
                    }
                    .addOnFailureListener {
                        showAlert("Error al buscar usuario: ${it.message}")
                    }
            }
        }

        btnGoogle.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account.idToken!!)
                }
            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Google sign in failed", e)
                showAlert("Error al iniciar sesión con Google: ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    ensureUserDocument {
                        goToPrincipal()
                    }
                } else {
                    showAlert("Error con Firebase: ${task.exception?.message}")
                }
            }
    }

    private fun loginWithEmail(correo: String, contraseña: String) {
        auth.signInWithEmailAndPassword(correo, contraseña)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    ensureUserDocument {
                        goToPrincipal()
                    }
                } else {
                    showAlert("Error al iniciar sesión: ${it.exception?.message}")
                }
            }
    }

    private fun ensureUserDocument(onComplete: () -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            showAlert("No se encontró sesión de usuario.")
            return
        }

        val userDocRef = db.collection("usuarios").document(user.uid)

        userDocRef.get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    val data = hashMapOf(
                        "uid" to user.uid,
                        "correo" to (user.email ?: ""),
                        "nombre" to (user.displayName ?: ""),
                        "creadoEn" to com.google.firebase.Timestamp.now()
                    )
                    userDocRef.set(data)
                        .addOnSuccessListener { onComplete() }
                        .addOnFailureListener { e ->
                            showAlert("Error al guardar datos del usuario: ${e.message}")
                        }
                } else {
                    onComplete()
                }
            }
            .addOnFailureListener { e ->
                showAlert("Error al leer datos del usuario: ${e.message}")
            }
    }

    private fun goToPrincipal() {
        val intent = Intent(this, Principal::class.java)
        startActivity(intent)
        finish()
    }

    private fun showAlert(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar", null)
        builder.create().show()
    }
}
