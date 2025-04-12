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
import com.example.grua.screens.ClienteHomeActivity
import com.example.grua.screens.RegisterActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerTextView: TextView
    private lateinit var operatorLoginTextView: TextView  // Nuevo botón
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Forzar cierre de sesión al iniciar (opcional)
        auth.signOut()

        // Referencias a las vistas
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        registerTextView = findViewById(R.id.registerTextView)
        operatorLoginTextView = findViewById(R.id.operarioTextView)  // Nuevo botón
        progressBar = findViewById(R.id.progressBar)

        // Configurar listeners
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

        // Listener para Registro
        registerTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // 🔥 Nuevo Listener para Operario (Redirige a MainActivity2)
        operatorLoginTextView.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)

            // Opcional: Limpiar campos al cambiar de pantalla
            emailEditText.text.clear()
            passwordEditText.text.clear()
        }
    }

    override fun onStart() {
        super.onStart()
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        auth.currentUser?.let { user ->
            user.getIdToken(false).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    verifyUserType(user.uid)
                } else {
                    auth.signOut()
                    showLoginScreen()
                }
            }
        } ?: showLoginScreen()
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
                if (document != null && document.exists() && document.getString("tipoUsuario") == "users") {
                    redirectToHome()
                } else {
                    auth.signOut()
                    showToast("No tienes permisos como cliente")
                    showLoginScreen()
                }
            }
            .addOnFailureListener {
                showLoading(false)
                auth.signOut()
                showToast("Error al verificar usuario")
                showLoginScreen()
            }
    }

    private fun redirectToHome() {
        startActivity(
            Intent(this, ClienteHomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }

    private fun showLoginScreen() {
        emailEditText.text.clear()
        passwordEditText.text.clear()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) ProgressBar.VISIBLE else ProgressBar.GONE
        loginButton.isEnabled = !show
        operatorLoginTextView.isEnabled = !show  // También deshabilita el botón de operario
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}