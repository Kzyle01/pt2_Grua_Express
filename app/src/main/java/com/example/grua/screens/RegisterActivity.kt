package com.example.grua.screens

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.grua.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupUI()
    }

    private fun setupUI() {
        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

            // Validaciones
            when {
                email.isEmpty() -> showError("Ingrese un correo electrónico")
                !isValidEmail(email) -> showError("Correo electrónico no válido")
                password.isEmpty() -> showError("Ingrese una contraseña")
                password.length < 6 -> showError("La contraseña debe tener al menos 6 caracteres")
                password != confirmPassword -> showError("Las contraseñas no coinciden")
                else -> registerUser(email, password, "users") // Tipo de usuario fijo como "users"
            }
        }

        binding.backToLogin.setOnClickListener {
            finish()
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun registerUser(email: String, password: String, userType: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.registerButton.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: run {
                        showError("Error al obtener ID de usuario")
                        binding.progressBar.visibility = View.GONE
                        binding.registerButton.isEnabled = true
                        return@addOnCompleteListener
                    }

                    // Crear objeto con solo los datos requeridos
                    val userData = hashMapOf(
                        "correo" to email,
                        "tipoUsuario" to userType, // Siempre "users"
                        "fechaRegistro" to System.currentTimeMillis()
                    )

                    // Guardar en Firestore
                    db.collection("usuarios").document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Registro exitoso!",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            showError("Error al guardar datos: ${e.message}")
                            binding.progressBar.visibility = View.GONE
                            binding.registerButton.isEnabled = true
                        }
                } else {
                    showError("Error al registrar: ${task.exception?.message}")
                    binding.progressBar.visibility = View.GONE
                    binding.registerButton.isEnabled = true
                }
            }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}