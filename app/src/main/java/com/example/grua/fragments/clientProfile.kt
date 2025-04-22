package com.example.grua.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.grua.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import com.example.grua.MainActivity

class clientProfile : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_client_profile, container, false)

        // Vistas del layout
        val tvProfileNameTop = view.findViewById<TextView>(R.id.tvProfileNameTop)
        val tvProfileRating = view.findViewById<TextView>(R.id.tvProfileRating)
        val tvProfileYears = view.findViewById<TextView>(R.id.tvProfileYears)
        val tvProfileName = view.findViewById<TextView>(R.id.tvProfileName)
        val tvProfileEmail = view.findViewById<TextView>(R.id.tvProfileEmail)
        val imgProfilePicture = view.findViewById<ImageView>(R.id.imgProfilePicture)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val uid = auth.currentUser?.uid

        // Cargar datos del usuario desde Firestore
        if (uid != null) {
            db.collection("cliente").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val nombre = document.getString("nombre") ?: "Sin nombre"
                        val correo = document.getString("correo") ?: "Sin correo"
                        val calificacion = document.getDouble("calificacion") ?: 0.0
                        val anios = document.getLong("anios") ?: 0
                        val fotoUrl = document.getString("fotoPerfilUrl")

                        tvProfileNameTop.text = nombre
                        tvProfileName.text = nombre
                        tvProfileEmail.text = correo
                        tvProfileRating.text = "${calificacion} Calificación"
                        tvProfileYears.text = "$anios Años"

                        // Imagen circular con Glide
                        if (!fotoUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(fotoUrl)
                                .circleCrop()
                                .into(imgProfilePicture)
                        }
                    } else {
                        tvProfileNameTop.text = "Usuario no encontrado"
                    }
                }
                .addOnFailureListener {
                    tvProfileNameTop.text = "Error al cargar datos"
                }
        } else {
            tvProfileNameTop.text = "Usuario no autenticado"
        }

        // Cerrar sesión (solo si tienes pantalla de login configurada)
        btnLogout.setOnClickListener {
            auth.signOut()
            // Redirige a MainActivity
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish() // Cierra la actividad actual
        }

        return view
    }
}
