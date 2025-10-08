package com.esime.ubicacionmaestra.Firstapp.ui.utilities.services

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine

class LocationService {

    // FusedLocationProviderClient para obtener la ubicación del dispositivo en tiempo real
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null

    // Método para obtener la ubicación del dispositivo con corrutinas
    @SuppressLint("MissingPermission")
    suspend fun getUserLocation(context: Context): Location? {
        if (fusedLocationProviderClient == null) {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGPSEnable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGPSEnable) {
            return null
        }

        return suspendCancellableCoroutine { cont ->
            locationRequest = LocationRequest.create().apply {
                interval = 10000 // 10 segundos
                fastestInterval = 5000 // 5 segundos
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    p0 ?: return
                    for (location in p0.locations) {
                        cont.resume(location) {}
                        fusedLocationProviderClient?.removeLocationUpdates(locationCallback!!)
                        return
                    }
                }

                override fun onLocationAvailability(p0: LocationAvailability) {
                    if (p0 != null && !p0.isLocationAvailable) {
                        cont.resume(null) {}
                        fusedLocationProviderClient?.removeLocationUpdates(locationCallback!!)
                    }
                }
            }


            fusedLocationProviderClient?.requestLocationUpdates(locationRequest!!,
                locationCallback as LocationCallback, context.mainLooper)
        }
    }
}
