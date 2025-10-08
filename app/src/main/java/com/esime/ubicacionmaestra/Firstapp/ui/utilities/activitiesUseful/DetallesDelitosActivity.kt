package com.esime.ubicacionmaestra.Firstapp.ui.utilities.activitiesUseful

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.esime.ubicacionmaestra.Firstapp.ui.saveLocation.SaveUbicacionReal
import com.esime.ubicacionmaestra.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import android.graphics.Path
import android.view.View
import android.widget.ProgressBar


class DetallesDelitosActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private var delitosCercanos: ArrayList<SaveUbicacionReal.Delito>? = null
    private lateinit var fondoCarga: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalles_delitos)
        supportActionBar?.hide()

        // Referencia al ProgressBar
        val progressBar = findViewById<ProgressBar>(R.id.progressBarDelitos)
        fondoCarga = findViewById(R.id.fondo)

        // Obtener los datos del Intent
        delitosCercanos = intent.getSerializableExtra("delitosCercanos") as? ArrayList<SaveUbicacionReal.Delito>

        // Inicializar el mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapdelitos) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            mMap = googleMap
            mMap.uiSettings.isZoomControlsEnabled = true

            // Cargar los marcadores de manera asíncrona
            lifecycleScope.launch {
                fondoCarga.visibility = View.VISIBLE
                loadMarkersAsync(delitosCercanos ?: emptyList(), progressBar)
            }
        }
    }

    private suspend fun loadMarkersAsync(
        delitos: List<SaveUbicacionReal.Delito>,
        progressBar: ProgressBar
    ) {
        withContext(Dispatchers.IO) {
            if (delitos.isNotEmpty()) {
                for (delito in delitos) {
                    val ubicacionDelito = LatLng(delito.latitud, delito.longitud)

                    // Obtener el ícono adecuado para este tipo de delito
                    val iconResId = getMarkerIconForDelito(delito.categoriaDelito)

                    // Redimensionar el ícono
                    val resizedIcon = resizeBitmap(iconResId, 100, 100)

                    // Crear marcador personalizado
                    val customMarkerBitmap = createCustomMarker(resizedIcon)

                    // Agregar el marcador al mapa en el hilo principal
                    withContext(Dispatchers.Main) {
                        mMap.addMarker(
                            MarkerOptions()
                                .position(ubicacionDelito)
                                .title(delito.delito)
                                .icon(BitmapDescriptorFactory.fromBitmap(customMarkerBitmap))
                        )
                    }
                }

                // Mover la cámara al primer marcador
                withContext(Dispatchers.Main) {
                    val firstDelito = delitos[0]
                    val firstLocation = LatLng(firstDelito.latitud, firstDelito.longitud)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 12f))
                }
            }

            // Ocultar el ProgressBar y mostrar el mapa
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                fondoCarga.visibility = View.GONE
                findViewById<View>(R.id.mapdelitos).visibility = View.VISIBLE
            }
        }
    }


    // Función para redimensionar un Bitmap
    private fun resizeBitmap(resourceId: Int, width: Int, height: Int): Bitmap {
        val bitmap = BitmapFactory.decodeResource(resources, resourceId)
        return Bitmap.createScaledBitmap(bitmap, width, height, false)
    }

    // Función para crear un marcador personalizado con la imagen redimensionada
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


    private fun getMarkerIconForDelito(categoriaDelito: String): Int {
        return when (categoriaDelito) {
            "DELITO DE BAJO IMPACTO" -> R.drawable.ic_bajo_impacto
            "HOMICIDIO DOLOSO" -> R.drawable.ic_homicidio_doloso
            "LESIONES DOLOSAS POR DISPARO" -> R.drawable.ic_lesiones_dolosas
            "ROBO A CASA HABITACION CON VIOLENCIA" -> R.drawable.ic_robo_casa
            "ROBO A CUENTAHABIENTE SALIENDO DEL CAJERO CON VIOLENCIA" -> R.drawable.ic_robo_cuentahabiente
            "ROBO A NEGOCIO CON VIOLENCIA" -> R.drawable.ic_robo_negocio
            "ROBO A PASAJERO A BORDO DE MICROBUS CON Y SIN VIOLENCIA" -> R.drawable.ic_robo_microbus
            "ROBO A PASAJERO A BORDO DE TAXI CON VIOLENCIA" -> R.drawable.ic_robo_taxi
            "ROBO A PASAJERO A BORDO DE METRO CON Y SIN VIOLENCIA" -> R.drawable.ic_robo_metro
            "ROBO A REPARTIDOR CON Y SIN VIOLENCIA" -> R.drawable.ic_robo_repartidor
            "ROBO A TRANSEUNTE EN VÍA PÚBLICA CON Y SIN VIOLENCIA" -> R.drawable.ic_robo_transeunte
            "ROBO DE VEHÍCULO CON Y SIN VIOLENCIA" -> R.drawable.ic_robo_vehiculo
            "VIOLACIÓN" -> R.drawable.ic_violacion
            else -> R.drawable.ic_default // Un ícono por defecto si no coincide ningún tipo
        }
    }

    override fun onMapReady(p0: GoogleMap) {
    }

}