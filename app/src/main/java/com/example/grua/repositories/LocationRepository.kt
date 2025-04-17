package com.example.grua.repositories

import android.util.Log
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.google.firebase.database.FirebaseDatabase
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation

class LocationRepository(
    private val pointAnnotationManager: PointAnnotationManager



) {
    private var carMarker: PointAnnotation? = null
    fun addUserMarker(uid: String, point: Point, name: String, photoUrl: String?) {
        try {
            val annotationOptions = PointAnnotationOptions()
                .withPoint(point)
                // Asegurate de tener el recurso "user_marker_icon" en tu carpeta drawable o registrado en Mapbox
                //.withIconImage("car")
                .withIconSize(1.0)

            // Crear el marker en el mapa usando el manager
            pointAnnotationManager.create(annotationOptions)
        } catch (e: Exception) {
            Log.e("Mapbox", "Error al añadir marcador: $e")
        }
    }

    fun moveCarMarker(point: Point) {
        pointAnnotationManager.deleteAll() // Elimina cualquier marcador anterior

        val markerOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage("car")
            .withIconSize(1.5)

        pointAnnotationManager.create(markerOptions)
    }

    fun updateUserLocation(
        uid: String,
        latitude: Double,
        longitude: Double,
        onComplete: (Boolean) -> Unit
    ) {
        // Obtén la referencia al nodo "locations" para el uid dado
        val dbRef = FirebaseDatabase.getInstance().getReference("locations").child(uid)
        // Define el mapa de datos a actualizar
        val locationData = mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to System.currentTimeMillis()
        )
        dbRef.setValue(locationData)
            .addOnSuccessListener {
                Log.d("LocationRepository", "Ubicación actualizada exitosamente para $uid")
                onComplete(true)
            }
            .addOnFailureListener { exception ->
                Log.e("LocationRepository", "Error actualizando ubicación: ${exception.message}")
                onComplete(false)
            }
        }
    }
