package com.example.grua.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.grua.R
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location

class ClienteHomeActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var locationListener: (Point) -> Unit

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cliente_home)

        mapView = findViewById(R.id.mapView)

        if (hasLocationPermission()) {
            initMap()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun initMap() {
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { _ ->
            enableUserLocation()
            pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        }
    }

    private fun enableUserLocation() {
        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            enabled = true
            pulsingEnabled = false // Deshabilitar el parpadeo
        }

        // Definir y almacenar el listener
        locationListener = { point ->
            val cameraOptions = CameraOptions.Builder()
                .center(point)
                .zoom(14.0)
                .build()
            mapView.getMapboxMap().setCamera(cameraOptions)

            addMarker(point)

            // Quitar el listener después de usarlo una vez
            locationComponentPlugin.removeOnIndicatorPositionChangedListener(locationListener)
        }

        // Agregar el listener
        locationComponentPlugin.addOnIndicatorPositionChangedListener(locationListener)
    }

    private fun addMarker(point: Point) {
        try {
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconSize(1.0)

            pointAnnotationManager.create(pointAnnotationOptions)
        } catch (e: Exception) {
            Log.e("Mapbox", "Error al añadir marcador", e)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            initMap()
        } else {
            Log.e("Mapbox", "Permiso de ubicación denegado")
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}
