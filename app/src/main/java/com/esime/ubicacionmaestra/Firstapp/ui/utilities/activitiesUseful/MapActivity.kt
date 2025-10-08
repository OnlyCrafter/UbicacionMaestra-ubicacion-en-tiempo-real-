package com.esime.ubicacionmaestra.Firstapp.ui.utilities.activitiesUseful

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.esime.ubicacionmaestra.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

// Activity del mapa
class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {
    private lateinit var mMap: GoogleMap
    private var selectedLocation: LatLng? = null
    private var geofenceRadius: Float = 100f
    private var geofenceCircle: Circle? = null
    private lateinit var placesClient: PlacesClient
    companion object {
        private const val TAG = "MapActivity"
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        supportActionBar?.hide()
        // Inicializar Places API
        Places.initialize(applicationContext, getString(R.string.google_maps_api_key))
        placesClient = Places.createClient(this)

        // Obtener el mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val btnConfirmLocation: Button = findViewById(R.id.btnConfirmLocation)
        val radiusSeekBar: SeekBar = findViewById(R.id.radiusSeekBar)

        // Configurar AutocompleteSupportFragment para búsqueda de lugares
        val autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME))
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {

                val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

                // Mover la cámara a la ubicación seleccionada
                val latLng = place.latLng
                latLng?.let {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
                    mMap.addMarker(MarkerOptions().position(it).title(place.name))
                    selectedLocation = it
                    updateGeofenceCircle()
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Log.e("MapActivity", "Error al seleccionar el lugar: $status")
            }
        })

        btnConfirmLocation.setOnClickListener {
            // Enviar los datos seleccionados a la actividad principal
            selectedLocation?.let {
                val resultIntent = Intent()
                resultIntent.putExtra("latitude", it.latitude)
                resultIntent.putExtra("longitude", it.longitude)
                resultIntent.putExtra("radius", geofenceRadius)
                resultIntent.putExtra("geofenceIndex", intent.getIntExtra("geofenceIndex", -1))
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
        radiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                geofenceRadius = progress.toFloat()
                updateGeofenceCircle()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMyLocationButtonClickListener(this)
        mMap.setOnMyLocationClickListener(this)
        mMap.isMyLocationEnabled = true

        // Configuración inicial del mapa
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setOnMapClickListener { latLng ->
            // Limpiar marcadores previos
            mMap.clear()
            geofenceCircle = null
            // Agregar un nuevo marcador en la ubicación seleccionada
            mMap.addMarker(MarkerOptions().position(latLng).title("Ubicación seleccionada"))
            // Guardar la ubicación seleccionada
            selectedLocation = latLng
            updateGeofenceCircle()
        }
    }

    private fun updateGeofenceCircle() {
        selectedLocation?.let {
            // Eliminar el círculo previo si existe
            geofenceCircle?.remove()
            // Agregar un nuevo círculo con el radio seleccionado
            geofenceCircle = mMap.addCircle(
                CircleOptions()
                    .center(it)
                    .radius(geofenceRadius.toDouble())
                    .strokeColor(Color.BLUE)
                    .fillColor(0x220000FF)
                    .strokeWidth(2f)
            )
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
        Log.d(TAG,"onMyLocationClick: $p0")
    }
}