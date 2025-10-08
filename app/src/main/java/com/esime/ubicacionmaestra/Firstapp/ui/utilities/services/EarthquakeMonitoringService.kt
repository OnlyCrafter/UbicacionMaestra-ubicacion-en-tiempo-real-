package com.esime.ubicacionmaestra.Firstapp.ui.utilities.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.esime.ubicacionmaestra.Firstapp.ui.panic.panicBttonActivity
import com.esime.ubicacionmaestra.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class EarthquakeMonitoringService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val TAG = "EarthquakeService"

    // Variables para la corrutina del servicio
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    // Retrofit para conectarse con la API de USGS
    val retrofit = Retrofit.Builder()
        .baseUrl("https://earthquake.usgs.gov/earthquakes/feed/v1.0/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Interface para la API
    val earthquakeApi = retrofit.create(EarthquakeApiService::class.java)

    // Cuando se crea el servicio
    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel() // Crear canal de notificaciones
    }

    // Método llamado cuando se inicia el servicio
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Servicio de monitoreo de sismos iniciado")
        startForegroundNotification()

        // Iniciar las actualizaciones de ubicación
        startLocationUpdates()

        // Devolver START_STICKY para mantener el servicio corriendo en segundo plano
        return START_STICKY
    }


    // Método onBind (para completar los métodos abstractos de la clase Service)
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel() // Cancelar cualquier tarea en segundo plano para evitar fugas de memoria
        Log.d(TAG, "Servicio de monitoreo de sismos detenido")
        stopLocationUpdates(object : LocationCallback() {})
    }

    private suspend fun fetchEarthquakeData(userLat: Double, userLng: Double) {
        try {
            val response = earthquakeApi.getRecentEarthquakes()
            if (response.isSuccessful) {
                val earthquakes = response.body()?.features ?: emptyList()
                checkIfEarthquakeNearby(earthquakes, userLat, userLng)
            } else {
                Log.e(TAG, "Error en la respuesta: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener datos de sismos: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkIfEarthquakeNearby(earthquakes: List<EarthquakeApiService.EarthquakeFeature>, userLat: Double, userLng: Double) {
        val currentTime = System.currentTimeMillis() // Hora actual en milisegundos
        val fiveMinutesInMillis = 5 * 60 * 1000 // Cinco minutos en milisegundos

        earthquakes.forEach { earthquake ->
            val earthquakeTime = earthquake.properties.time // Tiempo del terremoto en milisegundos
            val lat = earthquake.geometry.coordinates[1]
            val lng = earthquake.geometry.coordinates[0]

            // Calcular la distancia entre el terremoto y la ubicación del usuario
            val distance = calculateDistance(userLat, userLng, lat, lng)

            // Filtrar por magnitud, distancia y tiempo
            if (earthquake.properties.mag >= 6.0 &&
                distance <= 100 && // Distancia máxima de 100 km
                currentTime - earthquakeTime <= fiveMinutesInMillis // Terremotos de los últimos 5 minutos
            ) {
                Log.d(TAG, "Sismo detectado cerca de ti: ${earthquake.properties.place}")
                notifyUser(earthquake) // Notificar al usuario
            }
        }
    }

    // Iniciar el servicio de ubicación (UbicacionGuardarService)
    private fun startLocationService() {
        val intent = Intent(this, UbicacionGuardarService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    // Calcular la distancia entre dos puntos geográficos
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371 // Radio de la tierra en kilómetros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c // Distancia en kilómetros
    }

    // Notificar al usuario sobre el sismo
    @SuppressLint("MissingPermission")
    private fun notifyUser(earthquake: EarthquakeApiService.EarthquakeFeature) {
        val intent = Intent(this, panicBttonActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("magnitude", earthquake.properties.mag)
            putExtra("place", earthquake.properties.place)
        }

        // Añadir FLAG_IMMUTABLE al PendingIntent
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE // Cambiado aquí
        )

        val notification = NotificationCompat.Builder(this, "EarthquakeChannel")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("\u00a1Sismo detectado cerca de ti!")
            .setContentText("Magnitud: ${earthquake.properties.mag}, Lugar: ${earthquake.properties.place}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // Establecer el PendingIntent para abrir la actividad
            .setAutoCancel(true) // Cerrar la notificación al tocarla
            .build()

        NotificationManagerCompat.from(this).notify(1, notification)
    }


    // Crear el canal de notificaciones para el servicio
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "EarthquakeChannel",
                "Earthquake Monitoring Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    // Crear la notificación persistente para el servicio en primer plano
    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, "EarthquakeChannel")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Monitoreo de Sismos Activo")
            .setContentText("Estamos monitoreando posibles sismos en tu área.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 600000) // Cada 10 segundos
            .setMinUpdateIntervalMillis(300000) // Mínimo 5 segundos entre actualizaciones
            .setMaxUpdates(100) // Sin límite de actualizaciones
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val currentLocation = locationResult.lastLocation
                if (currentLocation != null) {
                    Log.d(TAG, "Nueva ubicación: ${currentLocation.latitude}, ${currentLocation.longitude}")

                    // Llamar a fetchEarthquakeData dentro de una corrutina
                    serviceScope.launch {
                        fetchEarthquakeData(currentLocation.latitude, currentLocation.longitude)
                    }
                } else {
                    Log.e(TAG, "No se pudo obtener la ubicación en la actualización.")
                }
            }
        }

        // Comienza a recibir actualizaciones de ubicación
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }


    private fun stopLocationUpdates(locationCallback: LocationCallback) {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

}

