package com.esime.ubicacionmaestra.Firstapp.ui.consult1To1

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.esime.ubicacionmaestra.Firstapp.ui.consultGroup.ConsultGroupAcivity
import com.esime.ubicacionmaestra.Firstapp.ui.consultGroup.ConsultGroupAcivity.Companion
import com.esime.ubicacionmaestra.Firstapp.ui.home.MenuPrincipalActivity
import com.esime.ubicacionmaestra.Firstapp.ui.profile.PerfilActivity
import com.esime.ubicacionmaestra.R
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConsultAppR : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {

    // Variables para el mapa
    private lateinit var map: GoogleMap
    private var currentMarker: Marker? = null

    // Variables para almacenar los ValueEventListener
    private var geofenceListener: ValueEventListener? = null
    private var locationListener: ValueEventListener? = null

    private lateinit var bitmap: Bitmap // Declaración global del bitmap
    private lateinit var bitmap2: Bitmap // Declaración global del bitmap

    private lateinit var nombre: String

    private lateinit var photoPerfil: ImageView
    private lateinit var nombresubi: TextView
    private lateinit var dates: TextView
    private lateinit var hours: TextView

    private var uid: String? = null
    private var uidToConsult: String? = null

    //private var emailPropio: String? = null
    private var emailCon: String? = null


    // Variables para la base de datos
    private val db = FirebaseFirestore.getInstance()

    private lateinit var database: DatabaseReference

    // Definimos la variable TAG para ubicar mas facil en el Logcat
    companion object {
        const val TAG = "ConsultarUbicacionReal" // Definimos la variable TAG aqui
    }

    private lateinit var geofencingClient: GeofencingClient


    // Funcion que se ejecuta al entrar a la activity
    @SuppressLint("MissingInflatedId", "WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consult_app_r)
        supportActionBar?.hide()

        // Obtener el email del intent
        val bundle = intent.extras
        //emailPropio = bundle?.getString("Email")
        uid = bundle?.getString("UID")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Obtener referencias a los elementos de la interfaz de usuario (botones,EditText,etc)
        val switchConsultar =
            findViewById<SwitchMaterial>(R.id.ConsultarUbicacion) as SwitchMaterial

        val spinnerUbi = findViewById<Spinner>(R.id.emailSpinner)
        val uidList = mutableListOf<String>()
        nombresubi = findViewById(R.id.nombreUser)
        photoPerfil = findViewById(R.id.photoPerfil)
        dates = findViewById(R.id.date)
        hours = findViewById(R.id.hour)
        Log.d(TAG, "onCreate")

        // El mapa
        createFragment()

        var grupoID: String? = null
        val nameUidMap = mutableMapOf<String, String>()  // Para ligar nombres con UIDs
        val nombresList =
            mutableListOf<String>()  // Lista para nombres que se mostrará en el Spinner

        geofencingClient = LocationServices.getGeofencingClient(this)
        database = FirebaseDatabase.getInstance().reference


        val docRef2 = db.collection("users").document(uid!!)

        //Log.i(TAG, emailPropio!!)

        docRef2.get().addOnSuccessListener { document ->
            val GrupoID = document.getString("GrupoID")
            if (GrupoID != "-") {
                grupoID = GrupoID!!
            }

            Log.i(TAG, "GrupoID: $grupoID")

            val docRef = db.collection("grupos").document(grupoID!!)
            docRef.get().addOnSuccessListener { document ->
                if (document != null) {
                    for (field in document.data?.keys.orEmpty()) {
                        val uid = document.getString(field)
                        if (!uid.isNullOrEmpty()) {
                            uidList.add(uid)
                        }
                    }

                    // Iteramos por cada UID para obtener el nombre del usuario
                    uidList.forEach { uid ->
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { userDocument ->
                                val nombre =
                                    userDocument.getString("Nombres")  // Asegúrate de que el campo sea correcto
                                if (!nombre.isNullOrEmpty()) {
                                    nombresList.add(nombre)  // Agrega el nombre a la lista
                                    nameUidMap[nombre] =
                                        uid  // Asocia el nombre con el UID en el map

                                    Log.i(TAG, "Nombre: $nombre, UID: $uid")
                                }

                                // Cuando hayamos terminado de obtener todos los nombres, configuramos el Spinner
                                if (nombresList.size == uidList.size) {  // Asegúrate de que tenemos todos los nombres
                                    val adapter = ArrayAdapter(
                                        this, android.R.layout.simple_spinner_item, nombresList
                                    )
                                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                    spinnerUbi.adapter = adapter
                                }
                            }.addOnFailureListener { e ->
                            Log.w(TAG, "Error al obtener el usuario con UID: $uid", e)
                        }
                    }

                } else {
                    Toast.makeText(this, "No se encontró el grupo", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Log.w(TAG, "Error al obtener el documento", e)
            }


            switchConsultar.setOnCheckedChangeListener { _, isChecked ->
                val selectedName = spinnerUbi.selectedItem?.toString()

                if (selectedName != null) {
                    uidToConsult = nameUidMap[selectedName] // Obtenemos el UID asociado al nombre

                    Log.i(TAG, "Nombre seleccionado: $selectedName con UID: $uidToConsult")

                    val docRef4 = db.collection("users").document(uidToConsult!!)

                    docRef4.get().addOnSuccessListener { document4 ->
                        val photoUrl = document4.getString("photoUrl")

                        Log.i(TAG, "PhotoUrl: $photoUrl")

                        if (photoUrl != null) {
                            cargarFoto2(photoUrl, photoPerfil) { bitmap ->
                                if (isChecked) {
                                    // Inicia la escucha de geovallas si no hay un listener activo
                                    if (geofenceListener == null) {
                                        val postReferenceGeofences =
                                            database.child("users").child(uidToConsult!!).child("Geovallas")
                                        geofenceListener = object : ValueEventListener {
                                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                for (geovallaSnapshot in dataSnapshot.children) {
                                                    val nombre = geovallaSnapshot.child("name")
                                                        .getValue(String::class.java)
                                                    val latitud = geovallaSnapshot.child("latitud")
                                                        .getValue(String::class.java)?.toDoubleOrNull()
                                                    val longitud = geovallaSnapshot.child("longitud")
                                                        .getValue(String::class.java)?.toDoubleOrNull()
                                                    val radio = geovallaSnapshot.child("radius")
                                                        .getValue(String::class.java)?.toFloatOrNull()
                                                    val transitionType = geovallaSnapshot.child("transitionTypes")
                                                        .getValue(String::class.java)

                                                    if (nombre != null && latitud != null && longitud != null && radio != null) {
                                                        Log.i(
                                                            TAG,
                                                            "Geovalla: $nombre, Latitud: $latitud, Longitud: $longitud, Radio: $radio, Transition: $transitionType"
                                                        )
                                                        if (transitionType == "true") {
                                                            val tranTT = "Dentro"
                                                            Toast.makeText(
                                                                this@ConsultAppR,
                                                                "En la Geovalla: $nombre, el usuario esta: $tranTT",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }else{
                                                            val tranTF = "Fuera"
                                                            Toast.makeText(
                                                                this@ConsultAppR,
                                                                "En la Geovalla: $nombre, el usuario esta: $tranTF",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }

                                                    }
                                                }
                                            }

                                            override fun onCancelled(databaseError: DatabaseError) {
                                                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                                            }
                                        }
                                        postReferenceGeofences.addValueEventListener(geofenceListener!!)
                                    }

                                    // Inicia el listener de ubicación si no hay uno activo
                                    if (locationListener == null) {
                                        Log.i(TAG, "Listener de ubicación activado para UID: $uidToConsult")
                                        val postReferenceLocation = database.child("users").child(uidToConsult!!)
                                        locationListener = object : ValueEventListener {
                                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                val latitud = dataSnapshot.child("latitud")
                                                    .getValue(String::class.java)?.toDoubleOrNull()
                                                val longitud = dataSnapshot.child("longitud")
                                                    .getValue(String::class.java)?.toDoubleOrNull()
                                                val date = dataSnapshot.child("date").getValue(String::class.java)
                                                val hour = dataSnapshot.child("timestamp").getValue(Long::class.java)
                                                val name = dataSnapshot.child("Nombre").getValue(String::class.java)

                                                val hourFormated = convertirTimestamp(hour!!)

                                                if (latitud != null && longitud != null && date != null) {
                                                    Log.i(TAG, "Latitud: $latitud, Longitud: $longitud")
                                                    val coordinates = LatLng(latitud, longitud)
                                                    dates.text = date
                                                    hours.text = hourFormated
                                                    nombresubi.text = name

                                                    currentMarker?.remove()

                                                    // Crear el marcador personalizado
                                                    val customMarkerBitmap = createCustomMarker(bitmap)

                                                    currentMarker = map.addMarker(
                                                        MarkerOptions().position(coordinates)
                                                            .title(selectedName)
                                                            .icon(BitmapDescriptorFactory.fromBitmap(customMarkerBitmap))
                                                            .flat(true)
                                                    )
                                                    map.animateCamera(
                                                        CameraUpdateFactory.newLatLngZoom(
                                                            coordinates,
                                                            18f
                                                        ), 5000, null
                                                    )
                                                } else {
                                                    Log.i(TAG, "Latitud y Longitud son nulos")
                                                }
                                            }

                                            override fun onCancelled(databaseError: DatabaseError) {
                                                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                                            }
                                        }
                                        postReferenceLocation.addValueEventListener(locationListener!!)
                                    }
                                }
                            }
                        }
                    }

                    if (uidToConsult!!.isEmpty()) {
                        Toast.makeText(
                            this,
                            "Ingresa una dirección de correo válida",
                            Toast.LENGTH_LONG
                        ).show()
                        currentMarker?.remove()
                        switchConsultar.isChecked = false
                    } else if (!isChecked) {
                        // Si el switch está apagado, elimina los listeners
                        geofenceListener?.let { listener ->
                            database.child("users").child(uidToConsult!!).child("Geovallas")
                                .removeEventListener(listener)
                            geofenceListener = null
                        }

                        locationListener?.let { listener ->
                            database.child("users").child(uidToConsult!!).removeEventListener(listener)
                            locationListener = null
                        }
                        currentMarker?.remove()
                        switchConsultar.isChecked = false
                    }
                } else {
                    Toast.makeText(this, "Seleccione un usuario", Toast.LENGTH_SHORT).show()
                }
            }



        }
    }

    private fun cargarFoto2(photoUrl: String, imageView: ImageView, onComplete: (Bitmap) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(photoUrl)
        val localFile = File.createTempFile("tempImage", "jpg")

        storageRef.getFile(localFile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
            imageView.setImageBitmap(bitmap) // Actualiza el ImageView correspondiente

            // Llama al callback después de que la imagen se haya descargado
            onComplete(bitmap)
        }.addOnFailureListener {
            Log.e(ConsultGroupAcivity.TAG, "Error al cargar la imagen: ${it.message}")
        }
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
    private fun convertirTimestamp(timestamp: Long): String {
        return try {
            // Crea una instancia de Date usando el timestamp en milisegundos
            val date = Date(timestamp)

            // Define el formato de salida que será hh:mm:ss
            val format = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())

            // Retorna la fecha formateada
            format.format(date)
        } catch (e: Exception) {
            "Hora no disponible" // En caso de error
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        finish() // Cierra la Activity
    }

    private fun setupMap() {
        val sharedPreferences = getSharedPreferences("MapSettings", Context.MODE_PRIVATE)
        val mapType = sharedPreferences.getInt("map_type", GoogleMap.MAP_TYPE_NORMAL)
        val trafficEnabled = sharedPreferences.getBoolean("traffic_enabled", false)

        map.mapType = mapType
        map.isTrafficEnabled = trafficEnabled
    }

    ////////////////////////////////// COSAS QUE HACEN QUE FUNCIONE EL MAPA ///////////////////////////////////////////////
    private fun createFragment() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        //enableLocation()
        //map.setOnMyLocationButtonClickListener(this)
        //map.setOnMyLocationClickListener(this)
        setupMap()
        val mexicoCity = LatLng(19.432608, -99.133209)
        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                mexicoCity,
                5f
            )
        )
        consultaGeofence()
    }

    private fun consultaGeofence() {
        val mDatabase = Firebase.database.reference
        Log.d(TAG, "UID: $uid")
        mDatabase.child("users").child(uid!!).child("Geovallas").get()
            .addOnSuccessListener { snapshot ->
                snapshot.children.forEach { geovalla ->
                    val latitud =
                        geovalla.child("latitud").getValue(String::class.java)?.toDoubleOrNull()
                    val longitud =
                        geovalla.child("longitud").getValue(String::class.java)?.toDoubleOrNull()
                    val nombre = geovalla.child("name").getValue(String::class.java)
                    val radio =
                        geovalla.child("radius").getValue(String::class.java)?.toFloatOrNull()

                    // Verificar si alguno de los valores es nulo
                    if (latitud != null && longitud != null && nombre != null && radio != null) {
                        Log.i(
                            TAG,
                            "Geovalla: $nombre, Latitud: $latitud, Longitud: $longitud, Radio: $radio"
                        )
                        mostrarGeovalla(nombre, latitud, longitud, radio)
                    } else {
                        Log.w(
                            TAG,
                            "Geovalla incompleta: latitud: $latitud, longitud: $longitud, nombre: $nombre, radio: $radio"
                        )
                    }
                }
            }.addOnFailureListener {
                Log.e(TAG, "Error getting data", it)
            }
    }
    // Función para redimensionar un Bitmap
    private fun resizeBitmap(resourceId: Int, width: Int, height: Int): Bitmap {
        val bitmap = BitmapFactory.decodeResource(resources, resourceId)
        return Bitmap.createScaledBitmap(bitmap, width, height, false)
    }

    private fun mostrarGeovalla(
        nombre: String?,
        latitud: Double?,
        longitud: Double?,
        radio: Float?
    ) {
        val circleOptions = CircleOptions()
            .center(LatLng(latitud!!, longitud!!))
            .radius(radio!!.toDouble())
            .strokeWidth(2f)
            .fillColor(0x40ff0000)
            .strokeColor(Color.BLUE)
        val resizedBitmap = resizeBitmap(R.drawable.ic_geovalla, 120, 120)
        map.addMarker(MarkerOptions().position(LatLng(latitud, longitud)).title(nombre).icon(BitmapDescriptorFactory.fromBitmap(createCustomMarker(resizedBitmap))))
        map.addCircle(circleOptions)
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Verificar el estado del permiso de ubicación
    private fun isLocationPermissionGranted() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // Solicitar si la ubicacion esta activa
    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        if (!::map.isInitialized) return
        if (isLocationPermissionGranted()) {
            map.isMyLocationEnabled = true
        } else {
            requestLocationPermission()
        }
    }

    // Solicitar el permiso de ubicación
    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            Toast.makeText(
                this,
                "Activa el permiso de ubicacion para poder usar esta caracteristica",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MenuPrincipalActivity.REQUEST_CODE_LOCATION
            )
        }
    }

    // Respuesta del permiso de ubicación
    @SuppressLint("MissingPermission", "MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MenuPrincipalActivity.REQUEST_CODE_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                map.isMyLocationEnabled = true
            } else {
                Toast.makeText(
                    this,
                    "Porfavor activa el permiso de ubicacion para poder usar esta caracteristica",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {}
        }
    }

    //////////////////////////////////////////////// MAS PARA EL MAPA ////////////////////////////////////////////////////////////////
    @SuppressLint("MissingPermission")
    override fun onResumeFragments() {
        super.onResumeFragments()
        if (!::map.isInitialized) return
        if (!isLocationPermissionGranted()) {
            map.isMyLocationEnabled = false
            Toast.makeText(
                this,
                "Porfavor activa el permiso de ubicacion para poder usar esta caracteristica",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Boton de ubicacion para saber tu ubicacion
    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(
            this,
            "Ubicación Aproximada",
            Toast.LENGTH_SHORT
        ).show()
        return false
    }

    // Cuando se hace click en la ubicacion muestra las coordenadas
    override fun onMyLocationClick(p0: Location) {
        Toast.makeText(
            this,
            "Estas en ${p0.latitude}, ${p0.longitude}",
            Toast.LENGTH_SHORT
        ).show()
    }
}