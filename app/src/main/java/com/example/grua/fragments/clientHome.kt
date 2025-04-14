package com.example.grua.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.grua.R
import com.mapbox.maps.MapView
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location



class clientHome : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var locationListener: (Point) -> Unit

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el layout del fragment
        return inflater.inflate(R.layout.fragment_client_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa el MapView desde la vista inflada
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

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun initMap() {
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
            enableUserLocation()
            pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        }
    }

    private fun enableUserLocation() {
        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            enabled = true
            pulsingEnabled = false
        }

        locationListener = { point ->
            val cameraOptions = CameraOptions.Builder()
                .center(point)
                .zoom(14.0)
                .build()
            mapView.getMapboxMap().setCamera(cameraOptions)
            addMarker(point)
            locationComponentPlugin.removeOnIndicatorPositionChangedListener(locationListener)
        }

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

    // Usa esto si estás solicitando permisos desde un Fragment (recomendado)
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
