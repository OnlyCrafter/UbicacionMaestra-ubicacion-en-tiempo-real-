package com.esime.ubicacionmaestra.Firstapp.ui.saveLocation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.esime.ubicacionmaestra.Firstapp.ui.preferences.preferenceUserActivity
import com.esime.ubicacionmaestra.Firstapp.ui.home.MenuPrincipalActivity
import com.esime.ubicacionmaestra.Firstapp.ui.consult1To1.ConsultAppR
import com.esime.ubicacionmaestra.Firstapp.ui.utilities.activitiesUseful.DetallesDelitosActivity
import com.esime.ubicacionmaestra.Firstapp.ui.utilities.broadcasts.GeofenceBroadcastReceiver
import com.esime.ubicacionmaestra.Firstapp.ui.utilities.activitiesUseful.MapActivity
import com.esime.ubicacionmaestra.Firstapp.ui.utilities.broadcasts.BatteryMapReceiver
import com.esime.ubicacionmaestra.Firstapp.ui.utilities.services.UbicacionGuardarService
import com.esime.ubicacionmaestra.R
import com.github.anastr.speedviewlib.SpeedView
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt
import java.io.Serializable

class SaveUbicacionReal : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener  {

    // Clase para manejar los datos de la geovalla
    data class GeofenceData(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val radius: Float = 100f,
        val transitionTypes: String
    )

    data class Delito(
        val latitud: Double,
        val longitud: Double,
        val categoriaDelito: String,
        val delito: String
    ) : Serializable


    data class User(
        val name: String? = "-",
        val latitud: String? = "-",
        val longitud: String? = "-",
        val radius: String? = "-",
        val transitionTypes: String? = "null"
    )

    private lateinit var map:GoogleMap
    private lateinit var geofenceContainer: LinearLayout
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var uid: String? = null
    private lateinit var vinetaDelictivo: androidx.cardview.widget.CardView
    private lateinit var overlayView: View

    companion object {
        const val TAG = "SaveUbicacionReal" // Definimos la variable TAG aqui
        const val PREFS_NAME = "SwitchPrefs"
        const val SWITCH_STATE = "switch_state"
        const val REQUEST_CODE_MAP = 1
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1002
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1003
    }

    private lateinit var database: DatabaseReference
    private lateinit var denunciasdatabase: DatabaseReference
    private lateinit var batteryMapReceiver: BatteryMapReceiver
    private lateinit var geofencingClient: GeofencingClient


    @SuppressLint("MissingInflatedId", "MissingPermission", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_ubicacion_real)

        requestNotificationPermission()
        requestLocationPermission()
        supportActionBar?.hide()
        val bundle = intent.extras
        //val email = bundle?.getString("Email")
        uid = bundle?.getString("UID")

        Log.d(TAG, "UID: $uid")
        createFragment()
        geofencingClient = LocationServices.getGeofencingClient(this)

        // Registrar el BroadcastReceiver para el nivel de batería (cambiar tipo de mapa)
        batteryMapReceiver = BatteryMapReceiver()
        val batteryStatusIntentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryMapReceiver, batteryStatusIntentFilter)

        denunciasdatabase = FirebaseDatabase.getInstance().reference.child("denuncias")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val btnIndice = findViewById<Button>(R.id.btn_indice_delictivo)
        vinetaDelictivo = findViewById<androidx.cardview.widget.CardView>(R.id.vineta_delictivo)
        val speedView = findViewById<SpeedView>(R.id.termometro_delictivo)
        val btnMasInformacion = findViewById<Button>(R.id.btn_mas_informacion)
        val ConfiButton = findViewById<Button>(R.id.AjustesButton)
        val switchUbicacionReal = findViewById<SwitchMaterial>(R.id.UbicacionReal) as SwitchMaterial
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val btnAjustes = findViewById<Button>(R.id.btnAjustes)
        switchUbicacionReal.isChecked = sharedPrefs.getBoolean(SWITCH_STATE, false)
        overlayView = findViewById<View>(R.id.overlayView)

        btnIndice.setOnClickListener {
            // Obtener referencia al ProgressBar
            val progressBarCargando = findViewById<ProgressBar>(R.id.progressBarCargando)
            overlayView.visibility = View.VISIBLE
            // Mostrar el ProgressBar mientras se cargan los datos
            progressBarCargando.visibility = View.VISIBLE

            // Ocultar la viñeta inicialmente
            vinetaDelictivo.visibility = View.GONE

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latitud = location.latitude
                    val longitud = location.longitude
                    Log.d(TAG, "Latitud: $latitud, Longitud: $longitud")

                    // Consultar Firebase y calcular el índice delictivo
                    lifecycleScope.launch(Dispatchers.IO) {
                        val (indiceDelictivo, _) = calcularIndiceDelictivo(latitud, longitud)
                        withContext(Dispatchers.Main) {
                            // Ocultar el ProgressBar después de cargar los datos
                            progressBarCargando.visibility = View.GONE
                            overlayView.visibility = View.GONE
                            // Mostrar la viñeta después de que se tengan los datos
                            vinetaDelictivo.visibility = View.VISIBLE
                            vinetaDelictivo.bringToFront()

                            // Convertir a Float y actualizar el valor del galvanómetro con animación
                            val indiceFloat = indiceDelictivo.toFloat()
                            speedView.speedTo(indiceFloat, 200)

                            if (indiceDelictivo > 0) {
                                Log.d(TAG, "Índice delictivo calculado: $indiceDelictivo")
                            } else {
                                Toast.makeText(
                                    this@SaveUbicacionReal,
                                    "No hay delitos cercanos para mostrar",
                                    Toast.LENGTH_LONG
                                ).show()
                                Toast.makeText(
                                    this@SaveUbicacionReal,
                                    "Nota: Función limitada a Ciudad de México",
                                    Toast.LENGTH_LONG
                                ).show()
                                Log.w(TAG, "No se encontraron delitos cercanos para mostrar el índice.")
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "No se pudo obtener la ubicación del usuario.")
                    progressBarCargando.visibility = View.GONE // Ocultar el ProgressBar si no se encuentra la ubicación
                }
            }
        }


        btnMasInformacion.setOnClickListener {
            // Verificar si la viñeta es visible antes de continuar
            if (vinetaDelictivo.visibility == View.VISIBLE) {

                // Obtener referencia al ProgressBar
                val progressBarCargando = findViewById<ProgressBar>(R.id.progressBarCargando)
                overlayView = findViewById<View>(R.id.overlayView)
                overlayView.visibility = View.VISIBLE
                // Mostrar el ProgressBar mientras se cargan los datos
                progressBarCargando.visibility = View.VISIBLE
                // Ocultar la viñeta inicialmente
                vinetaDelictivo.visibility = View.GONE

                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val latitud = location.latitude
                        val longitud = location.longitude

                        // Consultar los delitos cercanos y el índice delictivo
                        lifecycleScope.launch(Dispatchers.IO) {
                            val (_, delitosCercanos) = calcularIndiceDelictivo(latitud, longitud)
                            Log.d(TAG, "Delitos cercanos: $delitosCercanos")
                            withContext(Dispatchers.Main) {
                                // Ocultar el ProgressBar después de obtener los datos
                                progressBarCargando.visibility = View.GONE
                                overlayView.visibility = View.GONE

                                if (delitosCercanos.isNotEmpty()) {
                                    val intent = Intent(
                                        this@SaveUbicacionReal,
                                        DetallesDelitosActivity::class.java
                                    ).apply { }
                                    intent.putExtra("delitosCercanos", ArrayList(delitosCercanos))
                                    startActivity(intent)
                                } else {
                                    Log.w(TAG, "No hay delitos cercanos para mostrar.")
                                    Toast.makeText(
                                        this@SaveUbicacionReal,
                                        "No hay delitos cercanos para mostrar",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "No se pudo obtener la ubicación del usuario.")
                        progressBarCargando.visibility = View.GONE // Ocultar ProgressBar si no se pudo obtener la ubicación
                    }
                }
            } else {
                Log.w(TAG, "La viñeta no es visible. El botón 'Más Información' no tiene acción.")
            }
        }


        ConfiButton.setOnClickListener {view ->
            showGeofenceDialog()
            Log.d(TAG, "Boton Ajustes pulsado")
        }
        btnAjustes.setOnClickListener {view ->
            val intent = Intent(this, preferenceUserActivity::class.java)
            startActivity(intent)
            finish()
            Log.d(TAG, "Boton Ajustes pulsado")
        }
        switchUbicacionReal.setOnCheckedChangeListener {  _, isChecked ->
            if (isChecked) {
                Log.d(TAG, "Switch activo")
                val intent = Intent(this, UbicacionGuardarService::class.java).apply {
                    putExtra("Flag", true)
                    //putExtra("Email", email)
                    putExtra("UID", uid)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    startForegroundService(intent)
                }
                else{
                    startService(intent)
                }
            }
            else{
                Log.d(TAG, "Switch desactivado")
                val intent = Intent(this, UbicacionGuardarService::class.java)
                stopService(intent)
            }
            with(sharedPrefs.edit()){
                putBoolean(SWITCH_STATE, isChecked)
                apply()
            }
        }
    }
    // Función para redimensionar un Bitmap
    private fun resizeBitmap(resourceId: Int, width: Int, height: Int): Bitmap {
        val bitmap = BitmapFactory.decodeResource(resources, resourceId)
        return Bitmap.createScaledBitmap(bitmap, width, height, false)
    }
    private fun removeAllGeofences() {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
            .addOnSuccessListener {
                Log.d(TAG, "Todas las geovallas eliminadas correctamente")
                map.clear() // Limpia el mapa
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al eliminar geovallas: ${e.message}", e)
            }
    }
    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
    private suspend fun calcularIndiceDelictivo(
        latitudUsuario: Double,
        longitudUsuario: Double,
        radioKm: Double = 0.7
    ): Pair<Int, List<Delito>> {
        return withContext(Dispatchers.IO) {
            var totalDelitos = 0.0
            val delitosCercanos = mutableListOf<Delito>()

            // Definir el rango de latitud y longitud basado en el radio en km
            val latitudMin = latitudUsuario - kmToLatitudeDegrees(radioKm)
            val latitudMax = latitudUsuario + kmToLatitudeDegrees(radioKm)
            val longitudMin = longitudUsuario - kmToLongitudeDegrees(radioKm, latitudUsuario)
            val longitudMax = longitudUsuario + kmToLongitudeDegrees(radioKm, latitudUsuario)

            val query = denunciasdatabase.orderByChild("latitud").startAt(latitudMin).endAt(latitudMax)
            val snapshot = query.get().await()

            if (snapshot.exists()) {
                for (delitoSnapshot in snapshot.children) {
                    val latitudDelito = delitoSnapshot.child("latitud").getValue(Double::class.java)
                    val longitudDelito = delitoSnapshot.child("longitud").getValue(Double::class.java)
                    val categoriaDelito = delitoSnapshot.child("categoria_delito").getValue(String::class.java)
                    val delito = delitoSnapshot.child("delito").getValue(String::class.java)

                    if (latitudDelito != null && longitudDelito != null && categoriaDelito != null && delito != null && categoriaDelito != "Hecho no delictivo") {
                        // Verificar si la longitud está dentro del rango
                        if (latitudDelito in latitudMin..latitudMax && longitudDelito in longitudMin..longitudMax) {
                            val distancia = calcularDistancia(latitudUsuario, longitudUsuario, latitudDelito, longitudDelito)
                            if (distancia <= radioKm) {
                                // Contar los delitos según su categoría
                                if (categoriaDelito == "DELITO DE BAJO IMPACTO") {
                                    totalDelitos += 0.1
                                } else {
                                    totalDelitos += 1.0
                                }
                                // Agregar a la lista de delitos cercanos
                                delitosCercanos.add(Delito(latitudDelito, longitudDelito, categoriaDelito, delito))
                            }
                        }
                    } else {
                        Log.w(TAG, "Registro con datos incompletos encontrado, saltándolo.")
                    }
                }
            } else {
                Log.w(TAG, "No se encontraron datos dentro del rango especificado.")
                totalDelitos = 0.0
            }

            // Definir la población en riesgo para el área (valor simulado para este ejemplo)
            val poblacionEnRiesgo = 100000  // Esto puede cambiar dependiendo de la alcaldía o datos reales

            // Cálculo de la tasa de criminalidad
            val tasaCriminalidad = if (poblacionEnRiesgo > 0) {
                ((totalDelitos / poblacionEnRiesgo) * 100000).toInt()
            } else {
                0
            }

            Log.d(TAG, "Número de delitos: $totalDelitos, Población en riesgo: $poblacionEnRiesgo, Tasa de criminalidad: $tasaCriminalidad")

            // Devolver la tasa de criminalidad como el índice delictivo y la lista de delitos cercanos
            return@withContext Pair(tasaCriminalidad.coerceAtMost(100), delitosCercanos)
        }
    }
    private fun kmToLatitudeDegrees(km: Double): Double {
        val earthRadius = 6371.0
        return (km / earthRadius) * (180 / Math.PI)
    }
    private fun kmToLongitudeDegrees(km: Double, latitude: Double): Double {
        val earthRadius = 6371.0
        val radiusAtLatitude = earthRadius * Math.cos(Math.toRadians(latitude))
        return (km / radiusAtLatitude) * (180 / Math.PI)
    }
    private fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radioTierra = 6371.0 // Radio de la Tierra en km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).pow(2.0) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).pow(2.0)
        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
        return radioTierra * c
    }
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryMapReceiver) // Importante: liberar el receptor para evitar pérdidas de memoria
    }
    private fun setupMap() {
        val sharedPreferences = getSharedPreferences("MapSettings", Context.MODE_PRIVATE)
        val mapType = sharedPreferences.getInt("map_type", GoogleMap.MAP_TYPE_NORMAL)
        val trafficEnabled = sharedPreferences.getBoolean("traffic_enabled", false)

        map.mapType = mapType
        map.isTrafficEnabled = trafficEnabled
        map.setOnMapClickListener {
            vinetaDelictivo.visibility = View.GONE
        }
    }
    @SuppressLint("MissingPermission")
    private fun addGeofence(nombre: String?, latitud: Double?, longitud: Double?, radio: Float?) {

            val geofence = Geofence.Builder()
                .setRequestId(nombre!!)
                .setCircularRegion(
                    latitud!!,
                    longitud!!,
                    radio!!
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            Log.d(ConsultAppR.TAG, "Geofence in process to add")

            val intent = Intent(this, GeofenceBroadcastReceiver::class.java).apply {
                putExtra("Nombre", nombre) // Añade el nombre al Intent
                putExtra("UID", uid)
            }

            val geofencePendingIntent = PendingIntent.getBroadcast(
                this,
                nombre.hashCode(),
                intent,
               PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    Toast.makeText(this, "Geovalla añadida correctamente", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    val errorMessage = when (e) {
                        is ApiException -> {
                            when (e.statusCode) {
                                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> "Geofence no disponible"
                                GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> "Demasiadas geovallas"
                                GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> "Demasiados PendingIntents"
                                else -> "Error desconocido: ${e.statusCode}"
                            }
                        }
                        else -> "Error desconocido: ${e.localizedMessage}"
                    }
                    Toast.makeText(this, "Error añadiendo geovalla: $errorMessage", Toast.LENGTH_LONG).show()
                    Log.e("Geofence", errorMessage, e)
                }
    }
    private fun consultaGeofence() {
        removeAllGeofences()
        val mDatabase = Firebase.database.reference
        Log.i(TAG, "Consulta de geovallas id: $uid")
        if (uid != null) {
            mDatabase.child("users").child(uid!!).child("Geovallas").get().addOnSuccessListener { snapshot ->
                snapshot.children.forEach { geovalla ->
                    val latitud = geovalla.child("latitud").getValue(String::class.java)?.toDoubleOrNull()
                    val longitud = geovalla.child("longitud").getValue(String::class.java)?.toDoubleOrNull()
                    val nombre = geovalla.child("name").getValue(String::class.java)
                    val radio = geovalla.child("radius").getValue(String::class.java)?.toFloatOrNull()

                    if (latitud != null && longitud != null && radio != null) {
                        Log.i(TAG, "Geovalla: $nombre, Latitud: $latitud, Longitud: $longitud, Radio: $radio")
                        addGeofence(nombre, latitud, longitud, radio)
                        mostrarGeovalla(nombre, latitud, longitud, radio)
                    } else {
                        Log.e(TAG, "Error en los datos de la geovalla: $nombre")
                    }
                }
            }.addOnFailureListener {
                Log.e(TAG, "Error getting data", it)
            }
        } else {
            Log.e(TAG, "UID es null")
        }
    }
    private fun mostrarGeovalla(nombre: String?, latitud: Double?, longitud: Double?, radio: Float?) {
        val circleOptions = CircleOptions()
            .center(LatLng(latitud!!, longitud!!))
            .radius(radio!!.toDouble())
            .strokeWidth(2f)
            .fillColor(0x40ff0000)
            .strokeColor(Color.BLUE)
        val resizedBitmap = resizeBitmap(R.drawable.ic_geovalla, 120, 120)
        map.addMarker(MarkerOptions().position(LatLng(latitud, longitud)).title(nombre).icon(
            BitmapDescriptorFactory.fromBitmap(createCustomMarker(resizedBitmap))))
        map.addCircle(circleOptions)
    }
    // Función para crear un marcador personalizado
    private fun createCustomMarker(bitmap: Bitmap): Bitmap {
        val markerSize = 100 // Tamaño del marcador en píxeles
        val markerBitmap = Bitmap.createBitmap(markerSize, markerSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(markerBitmap)
        val paint = Paint()

        // Dibuja el círculo de fondo del marker
        paint.color = Color.WHITE
        paint.isAntiAlias = true
        canvas.drawCircle(
            (markerSize / 2).toFloat(),
            (markerSize / 2).toFloat(),
            (markerSize / 2).toFloat(),
            paint
        )

        // Recortar la imagen en forma circular
        val roundedBitmap = Bitmap.createBitmap(markerSize, markerSize, Bitmap.Config.ARGB_8888)
        val roundedCanvas = Canvas(roundedBitmap)
        val path = Path()
        path.addCircle(
            (markerSize / 2).toFloat(),
            (markerSize / 2).toFloat(),
            (markerSize / 2).toFloat(),
            Path.Direction.CCW
        )
        roundedCanvas.clipPath(path)

        roundedCanvas.drawBitmap(
            bitmap,
            Rect(0, 0, bitmap.width, bitmap.height),
            Rect(0, 0, markerSize, markerSize),
            null
        )

        // Dibuja la imagen recortada en el centro del marcador
        canvas.drawBitmap(roundedBitmap, 0f, 0f, null)

        return markerBitmap
    }
    // Modificación de la función showGeofenceDialog() para abrir el mapa
    @SuppressLint("InflateParams")
    private fun showGeofenceDialog() {
        val inflater: LayoutInflater = LayoutInflater.from(this)
        val dialogView: View = inflater.inflate(R.layout.dialog_geofence_menu, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)

        val dialog = builder.create()
        val numGeofencesEditText: EditText = dialogView.findViewById(R.id.numGeofences)
        geofenceContainer = dialogView.findViewById(R.id.geofenceContainer)
        val btnAddGeofences: Button = dialogView.findViewById(R.id.btnAddGeofences)
        val btnGuardarGeofences = dialogView.findViewById<Button>(R.id.btnGuardarGeofences)

        btnAddGeofences.setOnClickListener {
            val numGeofences = numGeofencesEditText.text.toString().toIntOrNull()

            if (numGeofences != null && numGeofences > 0) {
                geofenceContainer.removeAllViews()
                for (i in 1..numGeofences) {
                    addGeofenceFields(i, geofenceContainer)
                }
            }
        }

        btnGuardarGeofences.setOnClickListener {
            val geofences = mutableListOf<GeofenceData>()
            for (i in 0 until geofenceContainer.childCount step 5) {
                val name = (geofenceContainer.getChildAt(i) as EditText).text.toString()
                val lat = (geofenceContainer.getChildAt(i + 1) as EditText).text.toString().toDoubleOrNull()
                val lng = (geofenceContainer.getChildAt(i + 2) as EditText).text.toString().toDoubleOrNull()
                val radius = (geofenceContainer.getChildAt(i + 3) as EditText).text.toString().toFloatOrNull()
                val btnSelectLocation = geofenceContainer.getChildAt(i + 4) as Button

                if (lat != null && lng != null && radius != null) {
                    geofences.add(GeofenceData(name, lat, lng, radius, "ENTER_EXIT"))
                }
            }
            guardarGeofencesEnBaseDeDatos(geofences)
            dialog.dismiss()
            consultaGeofence()
        }
        dialog.show()
    }
    private fun addGeofenceFields(index: Int, container: LinearLayout) {
        val context = this
        // Texto para el nombre de la geovalla
        val geofenceName = EditText(context).apply {
            hint = "Nombre de geovalla $index"
        }
        // Campo para la latitud
        val latField = EditText(context).apply {
            hint = "Latitud geovalla $index"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            tag = "latField_$index"
        }
        // Campo para la longitud
        val lngField = EditText(context).apply {
            hint = "Longitud geovalla $index"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            tag = "lngField_$index"
        }
        // Campo para el radio
        val radioField = EditText(context).apply {
            hint = "Radio geovalla $index"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            tag = "radiusField_$index"
        }
        // Botón para seleccionar ubicación en el mapa
        val btnSelectLocation = Button(context).apply {
            text = "Seleccionar ubicación para geovalla $index"
            tag = "btnSelectLocation_$index"
            setOnClickListener {
                val mapIntent = Intent(context, MapActivity::class.java)
                mapIntent.putExtra("geofenceIndex", index)
                startActivityForResult(mapIntent, REQUEST_CODE_MAP)
            }
        }
        // Agregar los campos al contenedor
        container.addView(geofenceName)
        container.addView(latField)
        container.addView(lngField)
        container.addView(radioField)
        container.addView(btnSelectLocation)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MAP && resultCode == Activity.RESULT_OK) {
            val latitude = data?.getDoubleExtra("latitude", 0.0)
            val longitude = data?.getDoubleExtra("longitude", 0.0)
            val radius = data?.getFloatExtra("radius", 100f) ?: 100f
            val geofenceIndex = data?.getIntExtra("geofenceIndex", -1) ?: -1
            Log.d(TAG, "Latitud: $latitude, Longitud: $longitude, GeofenceIndex: $geofenceIndex, Radius: $radius")

            if (latitude != null && longitude != null && geofenceIndex != -1) {
                geofenceContainer.post {
                    val latField = geofenceContainer.findViewWithTag<EditText>("latField_$geofenceIndex")
                    val lngField = geofenceContainer.findViewWithTag<EditText>("lngField_$geofenceIndex")
                    val radiusField = geofenceContainer.findViewWithTag<EditText>("radiusField_$geofenceIndex")
                    latField?.setText(latitude.toString())
                    lngField?.setText(longitude.toString())
                    radiusField?.setText(radius.toString())
                }
            }
        }
    }
    private fun guardarGeofencesEnBaseDeDatos(geofences: List<GeofenceData>) {
        // Aquí puedes agregar tu lógica para guardar en Firebase u otro almacenamiento
        geofences.forEach { geofence ->
            Log.d(TAG, "Guardando geovalla: ${geofence.name}, Latitud: ${geofence.latitude}, Longitud: ${geofence.longitude}, Radio: ${geofence.radius}, Transiciones: ${geofence.transitionTypes}")
            // Lógica para guardar en base de datos
            if (geofence.name != null || geofence.name != "") {
                writeNewUser(geofence.name, geofence.latitude.toString(), geofence.longitude.toString(), geofence.radius.toString())
            }else{
                Toast.makeText(this, "Error al guardar geovalla, intente de nuevo y verifique los datos", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Sin nombre error al guardar geovalla")
            }
        }
    }
    fun writeNewUser(name: String, latitud: String, longitud: String, radius: String) {
        database = Firebase.database.reference
        val user = User(name, latitud, longitud, radius)
        if (name != "" && latitud != null && longitud != null && radius != null) {
            database.child("users").child(uid!!).child("Geovallas").child(name).setValue(user)
        }else{
            Toast.makeText(this, "Error al guardar geovalla, intente de nuevo y verifique los datos", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error al guardar geovalla")
        }
    }
    private fun createFragment(){
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        enableLocation()
        setupMap()
        map.uiSettings.isZoomControlsEnabled = true
        val mexicoCity = LatLng(19.432608, -99.133209)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(mexicoCity, 5f))
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMyLocationClickListener(this)
        consultaGeofence()
        map.setOnMapClickListener {
            vinetaDelictivo.visibility = View.GONE
            overlayView.visibility = View.GONE
        }
    }
    private fun isLocationPermissionGranted() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    private fun isBackgroundLocationPermissionGranted() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    @SuppressLint("MissingPermission")
    private fun enableLocation(){
        if(!::map.isInitialized) return
        if(isLocationPermissionGranted()){
            map.isMyLocationEnabled = true
        }else{
            requestLocationPermission()
        }
    }
    private fun requestLocationPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(
                this,
                "Activa el permiso de ubicacion para poder usar esta caracteristica",
                Toast.LENGTH_SHORT
            ).show()
        }else{
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MenuPrincipalActivity.REQUEST_CODE_LOCATION
            )
        }
    }
    @SuppressLint("MissingPermission", "MissingSuperCall")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        requestBackgroundLocationPermission()
                    } else {
                        map.isMyLocationEnabled = true
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Porfavor activa el permiso de ubicacion para poder usar esta caracteristica",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    map.isMyLocationEnabled = true
                } else {
                    Toast.makeText(
                        this,
                        "Porfavor activa el permiso de ubicacion para poder usar esta caracteristica",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "Notificaciones activadas",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
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
    @SuppressLint("MissingPermission")
    override fun onResumeFragments() {
        super.onResumeFragments()
        if(!::map.isInitialized) return
        if(!isLocationPermissionGranted()){
            map.isMyLocationEnabled = false
            Toast.makeText(
                this,
                "Porfavor activa el permiso de ubicacion para poder usar esta caracteristica",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(
            this,
            "Ubicacion aproximada",
            Toast.LENGTH_SHORT
        ).show()
        return false
    }
    override fun onMyLocationClick(p0: Location) {
        Toast.makeText(
            this,
            "Estas en ${p0.latitude}, ${p0.longitude}",
            Toast.LENGTH_SHORT
        ).show()
    }
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    // Función para cambiar el tipo de mapa a NONE
    fun changeMapToNone() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            googleMap.mapType = GoogleMap.MAP_TYPE_NONE
        }
    }
}
