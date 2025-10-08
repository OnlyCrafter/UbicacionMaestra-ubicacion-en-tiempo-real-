package com.esime.ubicacionmaestra.Firstapp.ui.home

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.esime.ubicacionmaestra.Firstapp.ui.profile.PerfilActivity
import com.esime.ubicacionmaestra.Firstapp.ui.utilities.broadcasts.BatteryDarkModeReceiver
import com.esime.ubicacionmaestra.Firstapp.ui.utilities.broadcasts.BatteryMapReceiver
import com.esime.ubicacionmaestra.Firstapp.ui.welcome.welcomeActivity
import com.esime.ubicacionmaestra.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

enum class ProviderType() {
    CORREO_ELECTRONICO,
}
class HomeActivity : AppCompatActivity() {

    val db = FirebaseFirestore.getInstance()
    private lateinit var batteryDarkModeReceiver: BatteryDarkModeReceiver
    private lateinit var auth: FirebaseAuth

    private var grupoID: String? = null

    // Definimos la variable TAG para el Logcat
    companion object {
        const val TAG = "HomeActivity" // Definimos la variable TAG aqui
    }

    // Funcion que se inicia al entrar a la activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Ocultar la barra de título
        supportActionBar?.hide()

        // Registrar el BroadcastReceiver para el nivel de batería (cambiar modo oscuro)
        batteryDarkModeReceiver = BatteryDarkModeReceiver()
        val batteryStatusIntentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryDarkModeReceiver, batteryStatusIntentFilter)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?:""

        val docGruopRef = db.collection("users").document(uid)

        docGruopRef.get().addOnSuccessListener { document ->
            grupoID = document.getString("GrupoID")
        }

        // Recuperamos el email persistente
//        val sharedPreferences = getSharedPreferences("my_prefs", MODE_PRIVATE)
//        val emailP = sharedPreferences.getString("emailPersistente", "Guest")?:"Guest" // "Guest" es el valor por defecto

        // Setup
        val bundle = intent.extras                              // recuperar parametros
//        val email = bundle?.getString("Email")  ?: emailP            //parametro del home layut "como nombramos al edit text"
        val provider = bundle?.getString("provider") ?:""


        Log.d(TAG, "UID: $uid")

        setup(provider,uid!!)  //en caso de no existir se manda algo vacio

    }

    private fun setup(provider: String, uid: String) {
        title = "inicio"

        // Declaracion de los botones de la interfaz etc
        val emailTextView = findViewById<TextView>(R.id.emailTextView)
        val providerTextView= findViewById<TextView>(R.id.providerTextView)
        val logoutBottom = findViewById<Button>(R.id.logoutButtom)
        val goButton = findViewById<Button>(R.id.goButton)

        // Guardamos el email persistente
//        val sharedPreferences = getSharedPreferences("my_prefs", MODE_PRIVATE)
//        val editor = sharedPreferences.edit()
//        editor.putString("emailPersistente", email)
//        editor.apply()


        //on back pressed "ir para atras"
        onBackPressedDispatcher.addCallback{
        }

        // Boton de perfil declaracion
        val perfilButton = findViewById<Button>(R.id.perfilButton)

        // para que guarde el email de manera persistente
        //emailTextView.text = email
        providerTextView.text = provider

        logoutBottom.setOnClickListener(){ // Boton de logout si es presionado hace lo que tiene dentro
            auth.signOut()
            val intent = Intent(this, welcomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) //flag para definir la actividad actual y limpiar la anterior
            startActivity(intent)   // inicia la actividad
            finish() //mata la actividad anterior
        }

        perfilButton.setOnClickListener{    // Boton de perfil si es presionado hace lo que tiene dentro
            val intent = Intent(this, PerfilActivity::class.java).apply {
                //putExtra("Email1", email)
                putExtra("UID", uid)
            }
            startActivity(intent)
            finish()
        }

        // lanza la aplicacion
        goButton.setOnClickListener{    // Boton de perfil si es presionado hace lo que tiene dentro
            if (grupoID != "-"){
                val intent = Intent(this, MenuPrincipalActivity::class.java).apply {
                    //putExtra("Email1", email)   // aqui se manda el email a la siguiente activity
                    putExtra("UID", uid)
                }
                startActivity(intent)   // inicia la actividad
            }else{
                Toast.makeText(this, "Cree un grupo o unase a uno para hacer uso de la aplicacion", Toast.LENGTH_LONG).show()

                val intent = Intent(this, PerfilActivity::class.java).apply {
                    //putExtra("Email1", email)
                    putExtra("UID", uid)
                }
                startActivity(intent)
                finish()
            }
        }
    }
}