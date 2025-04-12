package com.example.grua.screens

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.grua.R
import com.google.firebase.auth.FirebaseAuth

class OperarioHomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operario_home)

        auth = FirebaseAuth.getInstance()

        // Verificar que el usuario está autenticado
        if (auth.currentUser == null) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Aquí puedes añadir la lógica para la pantalla del cliente
        Toast.makeText(this, "Bienvenido Cliente", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        // Evitar que el usuario regrese al login con el botón atrás
        moveTaskToBack(true)
    }
}