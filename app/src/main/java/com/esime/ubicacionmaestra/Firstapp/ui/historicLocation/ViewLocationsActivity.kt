package com.esime.ubicacionmaestra.Firstapp.ui.historicLocation

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.esime.ubicacionmaestra.Firstapp.ui.consult1To1.ConsultAppR
import com.esime.ubicacionmaestra.Firstapp.ui.consult1To1.ConsultAppR.Companion
import com.esime.ubicacionmaestra.Firstapp.ui.home.HomeActivity
import com.esime.ubicacionmaestra.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date


class ViewLocationsActivity : AppCompatActivity(), OnMapReadyCallback {
    // Definimos el objeto GoogleMap para usar el mapa
    private lateinit var map: GoogleMap

    private var uid: String? = null

    //private var emailPropio: String? = null
    private var emailCon: String? = null

    private lateinit var bitmap: Bitmap // Declaración global del bitmap
    private lateinit var selectDates: TextView

    // Definimos la instancia de FirebaseFirestore para acceder a la base de datos Firestore
    private val db = FirebaseFirestore.getInstance()

    // Definimos la etiqueta para el registro de logcat
    private val TAG = "ViewLocationsActivity"

    // Funcion que se ejecuta al inicio de la activity
    @SuppressLint("MissingInflatedId", "WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_locations)

        supportActionBar?.hide()    // Oculta la barra de acción

        val bundle = intent.extras
        //emailPropio = bundle?.getString("Email")  // Obtiene el email del intent
        uid = bundle?.getString("UID")

        // Inicializamos los elementos de la interfaz de usuario (Botones, EditText)
        //val ButtonHistorialUbicacion = findViewById<EditText>(R.id.eTHisUbicacion)
        val spinnerHisUbi = findViewById<Spinner>(R.id.emailHisSpinner)
        val selectDateButton = findViewById<Button>(R.id.selectDateButton)

        var grupoID: String? = null
        val docRefHis = db.collection("users").document(uid!!)
        val emailsList = mutableListOf<String>()
        selectDates = findViewById(R.id.selectDate)
        val uidList = mutableListOf<String>()

        val nameUidMap = mutableMapOf<String, String>()  // Para ligar nombres con UIDs
        val nombresList =
            mutableListOf<String>()  // Lista para nombres que se mostrará en el Spinner

        //Log.i(TAG, emailPropio!!)

        docRefHis.get().addOnSuccessListener { document ->
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

                                    Log.i(ConsultAppR.TAG, "Nombre: $nombre, UID: $uid")
                                }

                                // Cuando hayamos terminado de obtener todos los nombres, configuramos el Spinner
                                if (nombresList.size == uidList.size) {  // Asegúrate de que tenemos todos los nombres
                                    val adapter = ArrayAdapter(
                                        this, android.R.layout.simple_spinner_item, nombresList
                                    )
                                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                    spinnerHisUbi.adapter = adapter
                                }
                            }.addOnFailureListener { e ->
                                Log.w(
                                    ConsultAppR.TAG,
                                    "Error al obtener el usuario con UID: $uid",
                                    e
                                )
                            }
                    }

                } else {
                    Toast.makeText(this, "No se encontró el grupo", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Log.w(ConsultAppR.TAG, "Error al obtener el documento", e)
            }


            selectDateButton.setOnClickListener {   // Cuando se hace click en el boton de la seleccion de fecha

                val selectedName = spinnerHisUbi.selectedItem.toString()
                if (selectedName.isNullOrEmpty()) {
                    map.clear()
                    Toast.makeText(this, "Seleccione un nombre", Toast.LENGTH_LONG).show()
                } else {
                    map.clear()
                    val emailUbiHist =
                        nameUidMap[selectedName]  // Obtenemos el UID asociado al nombre

                    Log.i(TAG, "Email a consultar: $emailUbiHist")


                    if (emailUbiHist!!.isEmpty()) { // Si el EditText no esta vacio
                        Toast.makeText(this, "Seleccione un email valido!", Toast.LENGTH_LONG)
                            .show()

                    } else {
                        val docRef = db.collection("users").document(emailUbiHist!!)
                        docRef.get().addOnSuccessListener { document1 ->
                            val nuevaPhoto = document1.getString("photoUrl")
                            if (nuevaPhoto != null) {
                                cargarFotoEnMarker(nuevaPhoto)
                            }
                        }

                        showDatePickerDialog(emailUbiHist)   // Muestra el Selector del dia
                    }
                }
            }
            // Llamamos a la funcion para crear el mapa
            createMapFragment()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        finish() // Cierra la Activity
    }

    private fun cargarFotoEnMarker(photoUrl: String) {
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(photoUrl)
        val localFile = File.createTempFile("tempImage", "jpg")

        storageRef.getFile(localFile).addOnSuccessListener {
            bitmap = BitmapFactory.decodeFile(localFile.absolutePath) // Actualiza el bitmap global
        }.addOnFailureListener {
            Log.e(TAG, "Error al cargar la imagen: ${it.message}")
        }
    }

    ///////////////////////////////////////////// FUNCIONES DEL MAPA ////////////////////////////////////////////////////
    private fun createMapFragment() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val mexicoCity = LatLng(19.432608, -99.133209)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(mexicoCity, 5f))
        setupMap()
    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun setupMap() {
        val sharedPreferences = getSharedPreferences("MapSettings", Context.MODE_PRIVATE)
        val mapType = sharedPreferences.getInt("map_type", GoogleMap.MAP_TYPE_NORMAL)
        val trafficEnabled = sharedPreferences.getBoolean("traffic_enabled", false)

        map.mapType = mapType
        map.isTrafficEnabled = trafficEnabled
    }

    // Función para mostrar el Selector del Mapa
    private fun showDatePickerDialog(emailhis: String?) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                // Formato para buscar en la base de datos
                val selectedDateForSearch =
                    String.format(Locale.getDefault(), "%02d-%02d-%d", dayOfMonth, month + 1, year)

                // Formato para mostrar al usuario
                val calendarSelected = Calendar.getInstance()
                calendarSelected.set(year, month, dayOfMonth)
                val displayDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("es", "MX"))
                val displayDate = displayDateFormat.format(calendarSelected.time)

                // Actualiza la vista con el formato deseado
                selectDates.text = displayDate

                // Carga las ubicaciones usando el formato para búsqueda
                loadLocationsForDate(selectedDateForSearch, emailhis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    // Funcion para cargar las ubicaciones guardadas en la base de datos
    private fun loadLocationsForDate(date: String, emailhis: String?) {

        val NombreView = findViewById<TextView>(R.id.NombreUsuario)

        db.collection("users").document(emailhis!!).get().addOnSuccessListener {
            NombreView.text = it.getString("Nombres")
        }

        // Conexiona a la base de datos Firestore
        db.collection("users").document(emailhis!!).collection(date)
            .orderBy("timestamp")   // Ordena por fecha en orden ascendente deacuerdo con el timestamp
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "No se encontraron datos de la fecha seleccionada", Toast.LENGTH_LONG).show()
                    Log.d(TAG, "No se encontraron documentos para la fecha: $date")
                    selectDates.text = "00/00/0000"
                    return@addOnSuccessListener
                } else {
                    val FotoPerfil = findViewById<ImageView>(R.id.PhotoPerfil)
                    FotoPerfil.setImageBitmap(bitmap)
                    bitmap = createCustomMarker(bitmap)
                    val latLngList = mutableListOf<LatLng>()
                    val timestampList = mutableListOf<Long>()
                    for (document in documents) {
                        val lat = document.getDouble("latitude")
                        val lng = document.getDouble("longitude")
                        val timestamp = document.getLong("timestamp")
                        if (lat != null && lng != null && timestamp != null) {
                            latLngList.add(LatLng(lat, lng))

                            timestampList.add(timestamp)
                            Log.d(TAG, "Ubicacion agregada: $lat, $lng")

                        }
                        Log.d(TAG, "Documento recuperado: $document")
                    }
                    showLocationHistoryOnMap(
                        latLngList,
                        timestampList
                    )    // Muestra la ubicacion en el mapa
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting documents: ", e)
            }
        Log.d(TAG, "loadLocationsForDate: $date")

    }

    // Funcion para mostrar las ubicaciones en el mapa
    private fun showLocationHistoryOnMap(
        latLngList: List<LatLng>,
        timestampList: MutableList<Long>
    ) {
        map.clear() // Limpia el mapa antes de agregar la ruta

        if (latLngList.isNotEmpty()) {
            val options = PolylineOptions().width(5f).color(Color.BLUE).geodesic(true)
            for (latLng in latLngList) {
                options.add(latLng)
            }
            map.addPolyline(options)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngList.first(), 15f))

            // Opcional: agrega marcadores en cada punto
            for (i in latLngList.indices) {
                val horaFormateada = convertirTimestamp(timestampList[i])
                map.addMarker(
                    MarkerOptions().position(latLngList[i]).title(horaFormateada)
                        .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                )
            }
        }
    }

    private fun convertirTimestamp(timestamp: Long): String {
        return try {
            // Crea una instancia de Date usando el timestamp en milisegundos
            val date = Date(timestamp)

            // Define el formato de salida que será HH:mm:ss
            val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            // Retorna la fecha formateada
            format.format(date)
        } catch (e: Exception) {
            "Hora no disponible" // En caso de error
        }
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