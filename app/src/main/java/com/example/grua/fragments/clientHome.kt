package com.example.grua.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.grua.R
import com.example.grua.repositories.LocationRepository
import com.example.grua.repositories.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.LocationPuck2D

class clientHome : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var locationListener: (Point) -> Unit
    private lateinit var realtimeDbRef: DatabaseReference
    private var carMarker: PointAnnotation? = null

    private val userRepository = UserRepository()
    private lateinit var locationRepository: LocationRepository

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_client_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)

        if (hasLocationPermission()) {
            initMap()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun initMap() {
        val styleUri = "mapbox://styles/kzyle/cm9hsmsgv00ku01r3d9dp2n97"
        mapView.getMapboxMap().loadStyleUri(styleUri) { style ->

            // Verifica si la imagen "car" existe en el estilo
            val imageExists = style.getStyleImage("car") != null
            Log.d("Mapbox", "¿La imagen 'car' existe?: $imageExists")

            if (!imageExists) {
                Log.e("Mapbox", "La imagen 'car' no se encontró en el estilo.")
            }

            pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
            locationRepository = LocationRepository(pointAnnotationManager)

            enableUserLocation()
            listenLocations()
        }
    }

    private fun enableUserLocation() {
        val locationComponentPlugin = mapView.location

        // Ocultar el punto azul
        locationComponentPlugin.updateSettings {
            enabled = true
            pulsingEnabled = false
            locationPuck = LocationPuck2D(
                topImage = null,
                bearingImage = null,
                shadowImage = null
            )
        }

        locationListener = { point ->
            // Mover la cámara
            val cameraOptions = CameraOptions.Builder()
                .center(point)
                .zoom(14.0)
                .build()
            mapView.getMapboxMap().setCamera(cameraOptions)

            // Mover o crear el marcador personalizado "car"
            locationRepository.moveCarMarker(point)

            // Actualizar ubicación en Firebase
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                locationRepository.updateUserLocation(
                    uid,
                    point.latitude(),
                    point.longitude()
                ) { success ->
                    if (success) {
                        Log.d("ClientHome", "Ubicación actualizada correctamente para $uid")
                    } else {
                        Log.e("ClientHome", "Error al actualizar ubicación para $uid")
                    }
                }
            }
        }

        locationComponentPlugin.addOnIndicatorPositionChangedListener(locationListener)
    }



    private fun listenLocations() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        realtimeDbRef = FirebaseDatabase.getInstance().getReference("locations")
        realtimeDbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { userSnapshot ->
                    val uid = userSnapshot.key
                    if (uid != null && uid != currentUid) {
                        val lat = userSnapshot.child("latitude").getValue(Double::class.java)
                        val lng = userSnapshot.child("longitude").getValue(Double::class.java)
                        if (lat != null && lng != null) {
                            val point = Point.fromLngLat(lng, lat)
                            Log.d("ClientHome", "Ubicación recibida de $uid en $point")

                            userRepository.getUserProfile(uid,
                                onSuccess = { name, photoUrl ->
                                    locationRepository.addUserMarker(uid, point, name, photoUrl)
                                },
                                onFailure = { exception ->
                                    Log.e("Firestore", "Error obteniendo perfil: $exception")
                                }
                            )
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseDB", "Error al leer ubicaciones: ${error.message}")
            }
        })
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            initMap()
        } else {
            Log.e("Mapbox", "Permiso de ubicación denegado")
        }
    }
}
