package com.example.grua.repositories

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore


class UserRepository {

    private val firestoreDb = FirebaseFirestore.getInstance()

    fun getUserProfile(
        uid: String,
        onSuccess: (name: String, photoUrl: String?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestoreDb.collection("cliente").document(uid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val name = documentSnapshot.getString("name") ?: "Usuario"
                    val photoUrl = documentSnapshot.getString("photoUrl")
                    onSuccess(name, photoUrl)
                } else {
                    onFailure(Exception("El usuario no existe"))
                }
            }
            .addOnFailureListener { exception ->
                Log.e("UserRepository", "Error obteniendo perfil: $exception")
                onFailure(exception)
            }
    }

    // Agrega aquí más funciones de acceso a datos según necesites.
}
