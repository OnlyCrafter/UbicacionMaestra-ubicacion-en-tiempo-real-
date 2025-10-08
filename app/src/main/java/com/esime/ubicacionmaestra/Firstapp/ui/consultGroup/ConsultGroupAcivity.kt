package com.esime.ubicacionmaestra.Firstapp.ui.consultGroup

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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.esime.ubicacionmaestra.Firstapp.ui.home.MenuPrincipalActivity
import com.esime.ubicacionmaestra.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConsultGroupAcivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {

    companion object {
        const val TAG = "ConsultaGroupActivity"
    }

    private lateinit var map: GoogleMap
    private var currentMarkers: MutableList<Marker?> = mutableListOf()

    private var uidList = arrayOfNulls<String>(7)
    private var locationListeners = arrayOfNulls<ValueEventListener>(7)

    private var grupoID: String? = null

    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var bitmap: Bitmap // Declaración global del bitmap

    private var urllist = arrayOfNulls<String>(7)
    private lateinit var imageViewList: List<ImageView>
    private var namelist = arrayOfNulls<String>(7)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consult_group_acivity)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        supportActionBar?.hide()

        createFragment()

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val switchConsult = findViewById<SwitchMaterial>(R.id.ConsultarUbicacion)
        // Inicializar las listas
        imageViewList = listOf(
            findViewById(R.id.photoPerfil1),
            findViewById(R.id.photoPerfil2),
            findViewById(R.id.photoPerfil3),
            findViewById(R.id.photoPerfil4),
            findViewById(R.id.photoPerfil5),
            findViewById(R.id.photoPerfil6),
            findViewById(R.id.photoPerfil7)
        )


        val docGroupRef = db.collection("users").document(auth.currentUser?.uid!!)

        docGroupRef.get().addOnSuccessListener { document ->
            grupoID = document.getString("GrupoID")

            Log.d(TAG, "GrupoID: $grupoID")

            val docRefGroup = db.collection("grupos").document(grupoID!!)

            switchConsult.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {

                    Log.d(TAG, "Switch ON")

                    // Obtén los UIDs de los miembros del grupo
                    docRefGroup.get().addOnSuccessListener { document2 ->
                        for (i in 0 until 7) {
                            val email = document2.getString("email${i + 1}")
                            if (!email.isNullOrEmpty()) {
                                Log.d(TAG, "UID del miembro $i: $email")
                                uidList[i] = email

                                // Verifica si el UID no es nulo ni vacío antes de agregar el listener
                                uidList[i]?.let { uid ->
                                    val docRefImage = db.collection("users").document(uid)
                                    docRefImage.get().addOnSuccessListener { document3 ->

                                        if (uid.isNotEmpty()) {
                                            addLocationListener(i, uid)

                                            val photoUrl = document3.getString("photoUrl")
                                            if (!photoUrl.isNullOrEmpty()) {
                                                urllist[i] = photoUrl
                                                cargarFoto(urllist[i]!!, imageViewList[i])
                                            }
                                        } else {
                                            Log.w(TAG, "UID vacío en la posición $i")
                                        }
                                    }
                                }
                            } else {
                                Log.w(TAG, "Email nulo o vacío en la posición $i, no se agregará el usuario.")
                            }
                        }
                    }
                } else {
                    // Remueve los listeners cuando el switch está apagado
                    removeListeners()
                }
            }
        }

        // Bucle para configurar los ImageView
        for (i in imageViewList.indices) {
            imageViewList[i].setOnClickListener {
                val uid = uidList[i] // Obtén el UID correspondiente para este ImageView
                if (uid != null && uid.isNotEmpty()) {
                    // Referencia a la base de datos en el nodo de 'users' y el UID correspondiente
                    val userRef = database.child("users").child(uid)

                    // Consultar la base de datos para obtener la información relevante
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                // Obtener los valores de 'date' y 'timestamp' desde el snapshot
                                val miembro = "Miembro #${i + 1}"
                                val fecha = snapshot.child("date").getValue(String::class.java) ?: "00/00/0000"
                                val hora = snapshot.child("timestamp").getValue(Double::class.java) ?: 0.0
                                val nombre = snapshot.child("Nombre").getValue(String::class.java) ?: "Sin nombre"

                                val hourFormated = convertirTimestamp(hora.toLong())

                                Log.d(TAG, "Miembro: $miembro, Fecha: $fecha y hora: $hourFormated")

                                // Mostrar la información en un PopupWindow
                                showMemberPopup(it, miembro, fecha, hourFormated,nombre)
                                // Mover el mapa al marcador correspondiente
                                moveToMemberLocation(i)
                            } else {
                                Log.w(TAG, "No se encontró información para el UID: $uid")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Manejo de error
                            Log.e(TAG, "Error al consultar la base de datos: ${error.message}")
                        }
                    })
                } else {
                    Log.w(TAG, "UID vacío en la posición $i")
                }
            }
        }
    }
    private fun moveToMemberLocation(index: Int) {
        if (index < currentMarkers.size) {
            val marker = currentMarkers[index]
            if (marker != null) {
                val coordinates = marker.position // Obtener la posición del marcador
                Log.d(TAG, "Moviendo la cámara al marcador del miembro $index: $coordinates")
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(coordinates, 18f), // Zoom nivel 18
                    3000, // Duración de la animación (2 segundos)
                    null
                )
            } else {
                Log.w(TAG, "El marcador para el miembro $index es nulo")
            }
        } else {
            Log.e(TAG, "Índice fuera de límites: $index")
        }
    }

    private fun addLocationListener(index: Int, uid: String) {
        if (locationListeners[index] == null) {
            Log.d(TAG, "Agregando listener para el miembro $index")
            val postReference = database.child("users").child(uid)
            locationListeners[index] = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val latitud = dataSnapshot.child("latitud").getValue(String::class.java)?.toDoubleOrNull()
                    val longitud = dataSnapshot.child("longitud").getValue(String::class.java)?.toDoubleOrNull()

                    Log.d(TAG, "Latitud y longitud del miembro $index: $latitud $longitud")

                    // Asegúrate de inicializar currentMarkers con un tamaño fijo o dinámico dependiendo del número de miembros
                    if (currentMarkers.size < 7) {
                        currentMarkers = MutableList(7) { null } // Inicializa la lista con 7 elementos nulos
                    }

                    if (latitud != null && longitud != null) {
                        val coordinates = LatLng(latitud, longitud)

                        // Remueve el marcador anterior si existe
                        currentMarkers.getOrNull(index)?.remove()

                        // Cargar la foto y crear el marcador cuando la imagen esté lista
                        val photoUrl = urllist[index]
                        if (!photoUrl.isNullOrEmpty()) {
                            cargarFoto2(photoUrl, imageViewList[index]) { bitmap ->
                                // Redimensionar el bitmap para el marcador si es necesario
                                val resizedBitmap = resizeBitmap(bitmap, 120, 120)

                                Log.d(TAG, "Agregando marker con imagen para el miembro $index")
                                currentMarkers[index] = map.addMarker(
                                    MarkerOptions()
                                        .position(coordinates)
                                        .title("Miembro ${index + 1}")
                                        .icon(BitmapDescriptorFactory.fromBitmap(createCustomMarker(resizedBitmap)))
                                        .flat(true)
                                )
                            }
                        } else {
                            // Si no hay foto, agregar un marcador con un icono predeterminado
                            currentMarkers[index] = map.addMarker(
                                MarkerOptions()
                                    .position(coordinates)
                                    .title("Miembro ${index + 1}")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                    .flat(true)
                            )
                        }
                    } else {
                        Log.w(TAG, "Latitud o longitud nulas para el miembro $index")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                }
            }
            postReference.addValueEventListener(locationListeners[index]!!)
        }
    }
    private fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, false)
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
            Log.e(TAG, "Error al cargar la imagen: ${it.message}")
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
    private fun cargarFoto(photoUrl: String, imageView: ImageView) {
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(photoUrl)
        val localFile = File.createTempFile("tempImage", "jpg")

        storageRef.getFile(localFile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
            imageView.setImageBitmap(bitmap) // Actualiza el ImageView correspondiente
        }.addOnFailureListener {
            Log.e(TAG, "Error al cargar la imagen: ${it.message}")
        }
    }
    private fun showMemberPopup(anchorView: View, miembro: String, fecha: String, hora: String, nombre: String) {
        // Inflar el diseño del popup
        val popupView = LayoutInflater.from(this).inflate(R.layout.popup_info, null)

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = popupView.measuredWidth
        val popupHeight = popupView.measuredHeight

        val popupWindow = PopupWindow(
            popupView,
            popupWidth.coerceAtLeast(300), // Asegura un ancho mínimo
            popupHeight,
            true
        )

        // Configurar el contenido del popup
        val miembroText = popupView.findViewById<TextView>(R.id.miembro)
        val fechaText = popupView.findViewById<TextView>(R.id.fecha)
        val horaText = popupView.findViewById<TextView>(R.id.hora)
        val nombreText = popupView.findViewById<TextView>(R.id.name)

        // Establecer el texto en los TextView
        miembroText.text = miembro
        fechaText.text = fecha
        horaText.text = hora
        nombreText.text = nombre

        // Mostrar el PopupWindow cerca del ImageView
        popupWindow.showAsDropDown(anchorView, 0, -anchorView.height, Gravity.START)
    }
    private fun convertirTimestamp(timestamp: Long): String {
        return try {
            // Crea una instancia de Date usando el timestamp en milisegundos
            val date = Date(timestamp)

            // Define el formato de salida que será hh:mm:ss
            val format = SimpleDateFormat("hh:mm:ss a", Locale("es", "MX"))

            // Retorna la fecha formateada
            format.format(date)
        } catch (e: Exception) {
            "Hora no disponible" // En caso de error
        }
    }
    // Función para eliminar todos los listeners
    private fun removeListeners() {
        for (i in 0 until 7) {
            locationListeners[i]?.let { listener ->
                uidList[i]?.let { uid ->
                    Log.d(TAG, "Removiendo listener para el miembro $i")
                    database.child("users").child(uid).removeEventListener(listener)
                    locationListeners[i] = null
                    currentMarkers[i]?.remove() // Remueve el marker si es necesario
                    currentMarkers[i] = null // Limpia el valor después de remover el marker
                }
            }
        }
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
        //enableLocation()
        //map.setOnMyLocationButtonClickListener(this)
        //map.setOnMyLocationClickListener(this)
        map.uiSettings.isZoomControlsEnabled = true
        setupMap()
        val mexicoCity = LatLng(19.432608, -99.133209)
        map.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(mexicoCity, 5f))
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