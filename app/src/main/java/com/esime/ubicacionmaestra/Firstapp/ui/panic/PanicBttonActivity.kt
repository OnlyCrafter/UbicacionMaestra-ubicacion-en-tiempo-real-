package com.esime.ubicacionmaestra.Firstapp.ui.panic

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.esime.ubicacionmaestra.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class panicBttonActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var timeTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var firestore: FirebaseFirestore
    private var uid: String? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var locationCallback: LocationCallback
    companion object {
        const val TAG = "PanicBttonActivity"
        const val TELEGRAM_BOT_TOKEN = "6861684395:AAHvcC2wktHTsjL1104a7AoM88A6I74yS3E"
        const val LOCATION_REQUEST_CODE = 1001
    }

    @SuppressLint("MissingInflatedId", "MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()    // Oculta la barra de título
        setContentView(R.layout.activity_panic_btton)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        uid = auth.currentUser?.uid // obtener el id del usuario actual

        val panicButton = findViewById<Button>(R.id.panicButton)
        timeTextView = findViewById(R.id.currentTime)
        dateTextView = findViewById(R.id.currentDate)

        // Iniciar la actualización de hora y fecha en tiempo real
        updateTimeAndDate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Configuración del LocationCallback para obtener la ubicación actualizada
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    // Aquí puedes enviar la ubicación actualizada
                    sendPanicAlertWithLocation(latitude, longitude)
                    // Una vez obtenida la ubicación, puedes detener las actualizaciones si no son necesarias continuamente
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }
        panicButton.setOnClickListener {
            // Mostrar mensaje de pánico activado
            Toast.makeText(this, "Pánico activado!", Toast.LENGTH_SHORT).show()
            requestUpdatedLocation() // Solicitar una ubicación actualizada
        }
    }
    // Método para solicitar una ubicación actualizada
    private fun requestUpdatedLocation() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000 // Intervalo de actualización (en milisegundos)
            fastestInterval = 500 // Intervalo más rápido aceptado
            numUpdates = 1 // Solo queremos una actualización
        }

        // Asegúrate de que los permisos de ubicación están concedidos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            // Solicitar permisos si no están concedidos
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
        }
    }

    //Método para enviar la alerta de pánico con la ubicación actualizada
    private fun sendPanicAlertWithLocation(latitude: Double, longitude: Double) {
        if (uid != null) {
            firestore.collection("users").document(uid!!).get()
                .addOnSuccessListener { userDocument ->
                    val groupId = userDocument.getString("GrupoID")
                    if (groupId != null) {
                        firestore.collection("grupos").document(groupId).get()
                            .addOnSuccessListener { groupDocument ->
                                for (i in 1..7) {
                                    val userId = groupDocument.getString("email$i")
                                    if (userId != null) {
                                        firestore.collection("users").document(userId).get()
                                            .addOnSuccessListener { document ->
                                                val chatId = document.get("chat_id")?.toString()
                                                if (chatId != null) {
                                                    val userName = userDocument.getString("Nombres") ?: "Usuario desconocido"
                                                    val mapsLink = "https://www.google.com/maps?q=$latitude,$longitude"
                                                    val message = "El usuario $userName necesita ayuda. Ubicación: $mapsLink"
                                                    sendTelegramMessage(TELEGRAM_BOT_TOKEN, chatId, message)
                                                } else {
                                                    Log.e(TAG, "No se encontró el chat_id para el usuario $userId")
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e(TAG, "Error al obtener el chat_id: ${e.message}")
                                            }
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error al obtener el grupo: ${e.message}")
                            }
                    } else {
                        Log.e(TAG, "No se encontró el GrupoID para el usuario $uid")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al obtener el usuario: ${e.message}")
                }
        } else {
            Log.e(TAG, "UID es nulo, no se puede enviar el mensaje de alerta.")
        }
    }
    private fun sendTelegramMessage(botToken: String, chatId: String, message: String) {
        // Verificar que el chatId sea un número válido
        if (!chatId.matches(Regex("^-?\\d+\$"))) {
            Log.e(TAG, "chat_id no es un número válido: $chatId")
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.telegram.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val telegramApi = retrofit.create(TelegramApi::class.java)
        val telegramMessage = TelegramMessage(chatId, message)

        val call = telegramApi.sendMessage("https://api.telegram.org/bot$botToken/sendMessage", telegramMessage)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.i(TAG, "Mensaje enviado con éxito")
                } else {
                    Log.e(TAG, "Error al enviar el mensaje: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e(TAG, "Fallo en la conexión: ${t.message}")
            }
        })
    }

    private fun updateTimeAndDate() {
        // Formato para la hora en formato de 12 horas con AM/PM
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())

        // Formato para la fecha con día de la semana, mes en texto y año
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())

        // Handler para actualizar la fecha y hora en tiempo real
        val handler = Handler(Looper.getMainLooper())

        handler.post(object : Runnable {
            override fun run() {
                // Obtener la hora y fecha actuales
                val currentTime = Calendar.getInstance().time
                val time = timeFormat.format(currentTime)
                val date = dateFormat.format(currentTime)

                // Actualizar los TextViews
                timeTextView.text = time
                dateTextView.text = date

                // Volver a ejecutar el Runnable después de 1 segundo
                handler.postDelayed(this, 1000)
            }
        })
    }

    interface TelegramApi {
        @POST
        fun sendMessage(
            @Url url: String,
            @Body message: TelegramMessage
        ): Call<Void>
    }

    data class TelegramMessage(
        val chat_id: String,
        val text: String
    )
}
