package com.example.grua

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.grua.screens.OperarioHomeActivity
import com.example.grua.screens.RegisterActivity2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity2 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var userLoginTextView: TextView
    private lateinit var registerOperatorTextView: TextView // Nuevo botón para registro
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Inicializar vistas
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        userLoginTextView = findViewById(R.id.userLoginTextView)
        registerOperatorTextView = findViewById(R.id.registerTextView) // Inicializar nuevo botón
        progressBar = findViewById(R.id.progressBar)

        // Configurar botón de login
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            when {
                email.isEmpty() -> showToast("Ingrese su correo electrónico")
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showToast("Email inválido")
                password.isEmpty() -> showToast("Ingrese su contraseña")
                else -> loginUser(email, password)
            }
        }

        // Botón para ir a login de usuario normal
        userLoginTextView.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Nuevo listener para botón de registro de operador
        registerOperatorTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity2::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        showLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    verifyUserType(auth.currentUser?.uid ?: "")
                } else {
                    showLoading(false)
                    showToast("Error al iniciar sesión: ${task.exception?.message ?: "Credenciales incorrectas"}")
                }
            }
    }

    private fun verifyUserType(userId: String) {
        showLoading(true)
        db.collection("usuarios").document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)
                if (document != null && document.exists() && document.getString("tipoUsuario") == "operador") {
                    redirectToOperatorHome()
                } else {
                    auth.signOut()
                    showToast("No tienes permisos como operador")
                }
            }
            .addOnFailureListener {
                showLoading(false)
                auth.signOut()
                showToast("Error al verificar usuario")
            }
    }

    private fun redirectToOperatorHome() {
        startActivity(Intent(this, OperarioHomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) ProgressBar.VISIBLE else ProgressBar.GONE
        loginButton.isEnabled = !show
        userLoginTextView.isEnabled = !show
        registerOperatorTextView.isEnabled = !show // También deshabilitar durante carga
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}