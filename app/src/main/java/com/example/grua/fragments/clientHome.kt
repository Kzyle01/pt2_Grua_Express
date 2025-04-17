package com.example.grua.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location

class clientHome : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var realtimeDbRef: DatabaseReference

    private val userRepository = UserRepository()
    private lateinit var locationRepository: LocationRepository

    // Marker personalizado para el usuario actual
    private var carMarker: PointAnnotation? = null

    // Última ubicación recibida
    private var lastLocation: Point? = null

    // Handler para el envío periódico
    private val handler = Handler(Looper.getMainLooper())
    private val uploadRunnable = object : Runnable {
        override fun run() {
            lastLocation?.let { point ->
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                uid?.let {
                    locationRepository.updateUserLocation(
                        it,
                        point.latitude(),
                        point.longitude()
                    ) { success ->
                        if (success) {
                            Log.d("ClientHome", "Ubicación subida correctamente para $it")
                        } else {
                            Log.e("ClientHome", "Fallo al subir ubicación para $it")
                        }
                    }
                }
            }
            // Reprogramar en 10 segundos
            handler.postDelayed(this, 10_000)
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_client_home, container, false)

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
        mapView.getMapboxMap().loadStyleUri(styleUri) {style ->

            // Verifica si la imagen "car" existe en el estilo
            val imageExists = style.getStyleImage("car") != null
            Log.d("Mapbox", "¿La imagen 'car' existe?: $imageExists")

            if (!imageExists) {
                Log.e("Mapbox", "La imagen 'car' no se encontró en el estilo.")
            }
            // Inicializamos el PointAnnotationManager y repositorios
            pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
            locationRepository = LocationRepository(pointAnnotationManager)

            enableUserLocation()
            listenLocations()

            // Iniciar ciclo de subida cada 10s
            handler.post(uploadRunnable)
        }
    }

    private fun enableUserLocation() {
        val locationComponentPlugin = mapView.location

        locationComponentPlugin.updateSettings {
            enabled = true
            pulsingEnabled = false
            locationPuck = LocationPuck2D(
                topImage = null,
                bearingImage = null,
                shadowImage = null
            )
        }

        // Listener que guarda la última ubic. y mueve el marker
        locationComponentPlugin.addOnIndicatorPositionChangedListener { point ->
            lastLocation = point
            // Mover cámara
            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(14.0)
                    .build()
            )
            moveCarMarker(point)
        }
    }

    /** Crea o mueve el marker del coche del usuario actual */
    private fun moveCarMarker(point: Point) {
        if (carMarker == null) {
            // Primera creación
            val options = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage("car")   // nombre del recurso registrado en tu estilo
                .withIconSize(1.0)
            carMarker = pointAnnotationManager.create(options)
        } else {
            // Actualizar posición
            carMarker?.point = point
            pointAnnotationManager.update(carMarker!!)
        }
    }

    /** Escucha ubicaciones de otros usuarios y añade sus markers */
    private fun listenLocations() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        realtimeDbRef = FirebaseDatabase.getInstance().getReference("locations")
        realtimeDbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { userSnap ->
                    val uid = userSnap.key
                    if (uid != null && uid != currentUid) {
                        val lat = userSnap.child("latitude").getValue(Double::class.java)
                        val lng = userSnap.child("longitude").getValue(Double::class.java)
                        if (lat != null && lng != null) {
                            val pt = Point.fromLngLat(lng, lat)
                            Log.d("ClientHome", "Ubicación de $uid en $pt")
                            userRepository.getUserProfile(uid,
                                onSuccess = { name, photoUrl ->
                                    locationRepository.addUserMarker(uid, pt, name, photoUrl)
                                },
                                onFailure = { ex ->
                                    Log.e("Firestore", "Perfil fallido: $ex")
                                }
                            )
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseDB", "Error lectura: ${error.message}")
            }
        })
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

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
        handler.removeCallbacks(uploadRunnable)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE
            && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            initMap()
        } else {
            Log.e("ClientHome", "Permiso de ubicación denegado")
        }
    }
}
