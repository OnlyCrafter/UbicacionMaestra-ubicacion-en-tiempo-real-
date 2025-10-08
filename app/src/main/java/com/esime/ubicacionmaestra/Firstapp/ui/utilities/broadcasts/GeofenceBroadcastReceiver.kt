package com.esime.ubicacionmaestra.Firstapp.ui.utilities.broadcasts
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.Geofence
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofenceStatusCodes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "GeofenceReceiver"
        const val EXTRA_GEOFENCE_NAME = "Nombre" // Asegúrate de usar la misma clave
        const val TELEGRAM_BOT_TOKEN = "6861684395:AAHvcC2wktHTsjL1104a7AoM88A6I74yS3E"
    }

    private var uid: String? = null

    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore

    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras
        uid = bundle?.getString("UID")

        database = FirebaseDatabase.getInstance().reference
        firestore = FirebaseFirestore.getInstance()

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        Log.e(TAG, "Entrando a onReceive")

        if (geofencingEvent!!.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Error de geovalla: $errorMessage")
            return
        }

        val name = intent.getStringExtra(EXTRA_GEOFENCE_NAME)

        Log.e(TAG, "Nombre de geovalla recibido: $name")

        // Obtener el tipo de transición
        val geofenceTransition = geofencingEvent.geofenceTransition
        val location = geofencingEvent.triggeringLocation
        val latitude = location?.latitude
        val longitude = location?.longitude
        val timestamp = SimpleDateFormat("EEEE, dd MMMM yyyy hh:mm:ss a", Locale.getDefault()).format(Date())

        if (name != null) {
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                val update = mapOf("transitionTypes" to "true")
                database.child("users").child(uid!!).child("Geovallas").child(name).updateChildren(update)
                Log.i(TAG, "Entrando en la geovalla: $name")
                Toast.makeText(context, "Entrando en la geovalla: $name", Toast.LENGTH_SHORT).show()

                // Obtener el grupo del usuario y enviar alertas a todos los usuarios en ese grupo
                sendGroupAlert(uid!!, "ha entrado en la geovalla '$name' el $timestamp.",latitude!!, longitude!!)

            } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                val update = mapOf("transitionTypes" to "false")
                database.child("users").child(uid!!).child("Geovallas").child(name).updateChildren(update)
                Log.i(TAG, "Saliendo de la geovalla: $name")
                Toast.makeText(context, "Saliendo de la geovalla: $name", Toast.LENGTH_SHORT).show()

                // Obtener el grupo del usuario y enviar alertas a todos los usuarios en ese grupo
                sendGroupAlert(uid!!, "ha salido de la geovalla '$name' el $timestamp.",latitude!!, longitude!!)
            } else {
                Log.e(TAG, "Transición no válida")
            }
        } else {
            Log.e(TAG, "Nombre de geovalla no encontrado en el Intent")
        }
    }

    private fun sendGroupAlert(uid: String, messageSuffix: String,latitude: Double, longitude: Double) {
        firestore.collection("users").document(uid).get()
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
                                                val userName = userDocument.getString("Nombres") ?: "Usuario"
                                                val googleMapsLink = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
                                                val message = "El usuario $userName $messageSuffix Puedes ver la ubicación aquí: $googleMapsLink"
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
                    Log.e(TAG, "No se encontró el grupoId para el usuario $uid")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al obtener el usuario: ${e.message}")
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
