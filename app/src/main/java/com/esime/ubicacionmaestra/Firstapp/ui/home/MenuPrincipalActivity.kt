package com.esime.ubicacionmaestra.Firstapp.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.esime.ubicacionmaestra.Firstapp.ui.preferences.preferenceUserActivity
import com.esime.ubicacionmaestra.Firstapp.ui.historicLocation.ViewLocationsActivity
import com.esime.ubicacionmaestra.Firstapp.ui.consult1To1.ConsultAppR
import com.esime.ubicacionmaestra.Firstapp.ui.consultGroup.ConsultGroupAcivity
import com.esime.ubicacionmaestra.Firstapp.ui.saveLocation.SaveUbicacionReal
import com.esime.ubicacionmaestra.Firstapp.ui.utilities.services.EarthquakeMonitoringService
import com.esime.ubicacionmaestra.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.annotations.SerializedName
import com.squareup.picasso.Picasso
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("data/2.5/weather")
    fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric" // Usar unidades métricas
    ): Call<WeatherResponse>
}
data class WeatherResponse(
    @SerializedName("main") val main: Main,
    @SerializedName("weather") val weather: List<Weather>,
    @SerializedName("name") val name: String
)

data class Main(
    @SerializedName("temp") val temp: Float,
    @SerializedName("humidity") val humidity: Int
)

data class Weather(
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String
)

object RetrofitInstance {
    private const val BASE_URL = "https://api.openweathermap.org/"

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val weatherService: WeatherService = retrofit.create(WeatherService::class.java)
}

private lateinit var database: DatabaseReference
// ...

class MenuPrincipalActivity : AppCompatActivity() {

    val TAG = "MenuPrincipalActivity"

    companion object {
        const val REQUEST_CODE_LOCATION = 0
        const val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1002
        const val ALL_PERMISSIONS_REQUEST_CODE = 1001
    }

    // Replace with your OpenWeather API key
    private var apiKey: String? = null

    @SuppressLint("MissingInflatedId", "LongLogTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_principal)

        apiKey = getString(R.string.api_key)

        // Aquí se llama a la función para obtener el clima
        fetchWeather()

        requestAllPermissions()


        supportActionBar?.hide()    // Oculta la barra de título

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Obtener el email del intent
        val bundle = intent.extras
        val email = bundle?.getString("Email1")
        val uid = bundle?.getString("UID")

        // Definimos los botones para navegar a otras activitys
        val consultButton = findViewById<Button>(R.id.consultButton)
        val saveUbi = findViewById<Button>(R.id.saveUbi)
        val viewLocationsButton = findViewById<Button>(R.id.viewsLocationsButton)
        val consultGruopButton = findViewById<Button>(R.id.consultUbiGroup)
        val panicButton = findViewById<Button>(R.id.panicButton)

        database = Firebase.database.reference

        consultButton.setOnClickListener {
            Log.d(TAG, "to ConsultAppR Email: $email")
            val intent1 = Intent(this, ConsultAppR::class.java).apply {
                putExtra("Email", email)    // Pasamos el email al intent
                putExtra("UID", uid)
            }
            startActivity(intent1)  // Lanzamos la activity
        }

        saveUbi.setOnClickListener {
            Log.d(TAG, "to SaveUbicacionReal Email: $email")
            val intent = Intent(this, SaveUbicacionReal::class.java).apply {
                putExtra("Email", email)    // Pasamos el email al intent
                putExtra("UID", uid)
            }
            startActivity(intent)   // Lanzamos la activity
        }

        viewLocationsButton.setOnClickListener {
            val intent = Intent(this, ViewLocationsActivity::class.java).apply {
                putExtra("Email", email)    // Pasamos el email al intent
                putExtra("UID", uid)
            }
            startActivity(intent)   // Lanzamos la activity
        }

        consultGruopButton.setOnClickListener {
            val intent = Intent(this, ConsultGroupAcivity::class.java).apply {
                putExtra("Email", email)    // Pasamos el email al intent
                putExtra("UID", uid)
            }
            startActivity(intent)   // Lanzamos la activity
        }
        panicButton.setOnClickListener {
            val intent = Intent(this, preferenceUserActivity::class.java).apply {
            }
            startActivity(intent)   // Lanzamos la activity
        }
    }
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Función para obtener la ubicación actual (dummy para ejemplificar)
    private fun getCurrentLocation(): Pair<Double, Double> {
        // Reemplaza esto con la lógica para obtener la ubicación real
        val latitude = 19.4326 // Por ejemplo, CDMX
        val longitude = -99.1332
        return Pair(latitude, longitude)
    }

    // Función para obtener y mostrar la información del clima
    private fun fetchWeather() {
        val (latitude, longitude) = getCurrentLocation()
        val weatherService = RetrofitInstance.weatherService

        weatherService.getCurrentWeather(latitude, longitude, apiKey!!).enqueue(object : Callback<WeatherResponse> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weatherResponse = response.body()
                    weatherResponse?.let {
                        val temperature = it.main.temp
                        val description = it.weather[0].description
                        val iconCode = it.weather[0].icon
                        val city = it.name

                        // Actualizar el TextView con la información del clima
                        val weatherTextView: TextView = findViewById(R.id.textViewWeather)
                        weatherTextView.text = "Clima en $city: $temperature°C"

                        // Actualizar el ImageView con el icono
                        val weatherIcon: ImageView = findViewById(R.id.imageViewWeatherIcon)
                        val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
                        Picasso.get().load(iconUrl).into(weatherIcon)
                    }
                } else {
                    Log.e(TAG, "Error en la respuesta: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e(TAG, "Error en la llamada: ${t.message}")
            }
        })
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf<String>()

        // Verifica permisos de ubicación en primer plano
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Verifica permisos de notificaciones (solo para Android Tiramisu y superior)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Si hay permisos pendientes, solicitarlos
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                ALL_PERMISSIONS_REQUEST_CODE
            )
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isBackgroundLocationPermissionGranted() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!isBackgroundLocationPermissionGranted()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            ALL_PERMISSIONS_REQUEST_CODE -> {
                permissions.forEachIndexed { index, permission ->
                    when (permission) {
                        Manifest.permission.ACCESS_FINE_LOCATION -> {
                            if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                                // Permiso de ubicación en primer plano concedido
                                Toast.makeText(this, "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show()
                                // Ahora solicita la ubicación en segundo plano
                                requestBackgroundLocationPermission()
                            } else {
                                // Permiso de ubicación en primer plano denegado
                                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
                            }
                        }

                        Manifest.permission.POST_NOTIFICATIONS -> {
                            if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                                // Permiso de notificaciones concedido
                                Toast.makeText(this, "Permiso de notificaciones concedido", Toast.LENGTH_SHORT).show()
                            } else {
                                // Permiso de notificaciones denegado
                                Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE -> {
                // Maneja la respuesta del permiso de ubicación en segundo plano
                permissions.forEachIndexed { index, permission ->
                    if (permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION) {
                        if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "Permiso de ubicación en segundo plano concedido", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Permiso de ubicación en segundo plano denegado", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }



}
