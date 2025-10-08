package com.esime.ubicacionmaestra.Firstapp.ui.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.esime.ubicacionmaestra.Firstapp.ui.home.MenuPrincipalActivity
import com.esime.ubicacionmaestra.Firstapp.ui.utilities.broadcasts.BatteryDarkModeReceiver
import com.esime.ubicacionmaestra.Firstapp.ui.utilities.services.EarthquakeMonitoringService
import com.esime.ubicacionmaestra.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class preferenceUserActivity : AppCompatActivity() {

    private lateinit var spinnerMapType: Spinner
    private lateinit var switchTraffic: SwitchMaterial
    private lateinit var switchDarkMode: SwitchMaterial
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var imageMap: ImageView
    private lateinit var batteryDarkModeReceiver: BatteryDarkModeReceiver
    private lateinit var sharedPreferences1: SharedPreferences
    lateinit var sismosSwitch: SwitchMaterial
    private lateinit var spinnerGeovallas: Spinner
    private lateinit var btnEliminarGeovalla: Button
    private lateinit var database: FirebaseDatabase
    private lateinit var geovallasRef: DatabaseReference
    private lateinit var geovallasList: MutableList<String>
    private lateinit var geovallasKeys: MutableList<String> // Almacenar las claves únicas de las geovallas
    private lateinit var auth: FirebaseAuth

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference_user)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        supportActionBar?.hide()
        sharedPreferences = getSharedPreferences("MapSettings", Context.MODE_PRIVATE)
        sharedPreferences1 = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

        spinnerMapType = findViewById(R.id.spinner_map_type)
        switchTraffic = findViewById(R.id.switch_traffic)
        switchDarkMode = findViewById(R.id.switch_dark_mode)
        imageMap = findViewById(R.id.imageMap)
        sismosSwitch = findViewById<SwitchMaterial>(R.id.sismosSwitch)

        // Verificar el estado guardado en SharedPreferences y actualizar el Switch
        val isEarthquakeMonitoringEnabled = sharedPreferences.getBoolean("earthquake_monitoring", false)
        sismosSwitch.isChecked = isEarthquakeMonitoringEnabled

        // Configurar el listener para el cambio de estado del Switch
        sismosSwitch.setOnCheckedChangeListener { _, isChecked ->
            val intent = Intent(this, EarthquakeMonitoringService::class.java)
            if (isChecked) {
                startService(intent)
            } else {
                stopService(intent)
            }
            // Guardar la preferencia en SharedPreferences
            saveEarthquakeMonitoringPreference(isChecked)
        }

        // Iniciar o detener el servicio según la preferencia guardada al iniciar la app
        val intent = Intent(this, EarthquakeMonitoringService::class.java)
        if (isEarthquakeMonitoringEnabled) {
            startService(intent)
        } else {
            stopService(intent)
        }

        // Configurar el spinner para seleccionar el tipo de mapa
        ArrayAdapter.createFromResource(
            this,
            R.array.map_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerMapType.adapter = adapter
        }
        // Verificar el estado guardado en SharedPreferences y actualizar el Switch
        val isDarkModeEnabled = sharedPreferences1.getBoolean("dark_mode", false)
        switchDarkMode.isChecked = isDarkModeEnabled

        // Configurar el listener para el cambio de estado del Switch
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Activar el modo oscuro
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                saveDarkModePreference(true)
                saveUserSetDarkMode(true) // El usuario cambió manualmente el modo
            } else {
                // Desactivar el modo oscuro
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                saveDarkModePreference(false)
                saveUserSetDarkMode(true) // El usuario cambió manualmente el modo
            }
        }
        // Registrar el BroadcastReceiver para el nivel de batería (modo oscuro)
        batteryDarkModeReceiver = BatteryDarkModeReceiver()
        val batteryStatusIntentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryDarkModeReceiver, batteryStatusIntentFilter)

        // Cargar las preferencias guardadas
        val mapType = sharedPreferences.getInt("map_type", GoogleMap.MAP_TYPE_NORMAL)
        spinnerMapType.setSelection(mapType - 1) // Ajusta el índice

        val trafficEnabled = sharedPreferences.getBoolean("traffic_enabled", false)
        switchTraffic.isChecked = trafficEnabled

        // Guardar las preferencias cuando cambien
        spinnerMapType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val mapType = position + 1 // El índice comienza en 0
                saveMapType(mapType)
                // Cambiar la imagen del ImageView según el tipo de mapa seleccionado
                when (mapType) {
                    GoogleMap.MAP_TYPE_NORMAL -> imageMap.setImageResource(R.drawable.normal)
                    GoogleMap.MAP_TYPE_SATELLITE -> imageMap.setImageResource(R.drawable.satellite)
                    GoogleMap.MAP_TYPE_TERRAIN -> imageMap.setImageResource(R.drawable.terrain)
                    GoogleMap.MAP_TYPE_HYBRID -> imageMap.setImageResource(R.drawable.hybrid)
                    else -> imageMap.setImageResource(R.drawable.normal) // Default en caso de que no coincida
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        switchTraffic.setOnCheckedChangeListener { _, isChecked ->
            saveTrafficEnabled(isChecked)
        }

        // Inicializa el Spinner y SharedPreferences
        val spinnerGuardadoTiempo = findViewById<Spinner>(R.id.spinner_guardado_tiempo)
        val timesArray = arrayOf("10", "20", "40", "120", "360")

        // Crea un adaptador para el spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timesArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGuardadoTiempo.adapter = adapter

        // Carga la preferencia guardada
        val savedTime = sharedPreferences.getInt("guardado_tiempo", 10)
        spinnerGuardadoTiempo.setSelection(timesArray.indexOf(savedTime.toString()))

        // Guarda las preferencias cuando cambien
        spinnerGuardadoTiempo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedTime = timesArray[position].toInt()
                saveGuardadoTiempo(selectedTime)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        // Inicializa la base de datos
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        spinnerGeovallas = findViewById(R.id.spinnerGeovallas)
        btnEliminarGeovalla = findViewById(R.id.btnEliminarGeovalla)
        val userId = auth.currentUser?.uid // Aquí usa dinámicamente el ID del usuario
        geovallasRef = database.getReference("users").child(userId!!).child("Geovallas")

        // Inicializa listas
        geovallasList = mutableListOf()
        geovallasKeys = mutableListOf()

        // Cargar geovallas al Spinner
        cargarGeovallas()
        // Configurar acción del botón
        btnEliminarGeovalla.setOnClickListener {
            val selectedIndex = spinnerGeovallas.selectedItemPosition
            if (selectedIndex >= 0) {
                val geovallaKey = geovallasKeys[selectedIndex]
                eliminarGeovalla(geovallaKey)
            } else {
                Toast.makeText(this, "No se seleccionó ninguna geovalla", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Método para guardar el tiempo seleccionado
    private fun saveGuardadoTiempo(guardadoTiempo: Int) {
        with(sharedPreferences.edit()) {
            putInt("guardado_tiempo", guardadoTiempo)
            apply()
        }
    }

    // Guardar la preferencia en SharedPreferences
    private fun saveEarthquakeMonitoringPreference(isEnabled: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("earthquake_monitoring", isEnabled)
        editor.apply()
    }

    private fun saveMapType(mapType: Int) {
        with(sharedPreferences.edit()) {
            putInt("map_type", mapType)
            apply()
        }
    }

    private fun saveTrafficEnabled(enabled: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean("traffic_enabled", enabled)
            apply()
        }
    }
    // Guardar la preferencia en SharedPreferences
    private fun saveDarkModePreference(isEnabled: Boolean) {
        val editor = sharedPreferences1.edit()
        editor.putBoolean("dark_mode", isEnabled)
        editor.apply()
    }

    // Guardar la preferencia de si el usuario configuró manualmente el modo oscuro
    private fun saveUserSetDarkMode(isUserSet: Boolean) {
        val editor = sharedPreferences1.edit()
        editor.putBoolean("user_set_dark_mode", isUserSet)
        editor.apply()
    }
    private fun cargarGeovallas() {
        geovallasRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                geovallasList.clear()
                geovallasKeys.clear()

                for (geovallaSnapshot in snapshot.children) {
                    val key = geovallaSnapshot.key // Clave de la geovalla
                    val name = geovallaSnapshot.child("name").getValue(String::class.java) // Nombre de la geovalla
                    if (key != null && name != null) {
                        geovallasKeys.add(key)
                        geovallasList.add(name)
                    }
                }

                // Configurar el Spinner
                val adapter = ArrayAdapter(
                    this@preferenceUserActivity,
                    android.R.layout.simple_spinner_item,
                    geovallasList
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerGeovallas.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@preferenceUserActivity, "Error al cargar geovallas", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun eliminarGeovalla(geovallaKey: String) {
        geovallasRef.child(geovallaKey).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Geovalla eliminada con éxito", Toast.LENGTH_SHORT).show()
                cargarGeovallas() // Recargar las geovallas en el Spinner
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al eliminar la geovalla: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}