package com.esime.ubicacionmaestra.Firstapp.ui.auth.register

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.esime.ubicacionmaestra.Firstapp.ui.home.HomeActivity
import com.esime.ubicacionmaestra.Firstapp.ui.home.ProviderType
import com.esime.ubicacionmaestra.Firstapp.ui.welcome.welcomeActivity
import com.esime.ubicacionmaestra.R
import com.google.android.material.internal.ViewUtils.hideKeyboard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class registerActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth   //variable de autenticacion

    // Declaracion de la base de datos
    val db = FirebaseFirestore.getInstance()
    private lateinit var database: DatabaseReference

    companion object {
        const val TAG = "registerActivity" // Definimos la variable TAG aqui
    }
    // Datos de la base de datos (Formato)
    val user = hashMapOf(
        "Email" to null,
        "Telefono" to null,
        "Nombres" to null,
        "Apellidos" to null,
        "GrupoID" to "-",
        "photoUrl" to null
    )

    data class UserUbi(
        val latitud: String? = "-",
        val longitud: String? = "-",
    )

    // Clase para manejar los datos de la geovalla
    data class GeofenceD(
        val name: String = "ESIME",
        val latitud: String = "19.498850591050665",
        val longitud: String = "-99.13460084531137",
        val radius: String  = "100",
        val transitionTypes: String = "false"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportActionBar?.hide()    // oculta la barra de titulo
        setup() // llama a la funcion setup que inicia la magia
    }
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, welcomeActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun setup() {
        // Declaracion de lo botones de la interfaz
        val registrarButton = findViewById<Button>(R.id.registrarButton)
        val emailEditText = findViewById<androidx.appcompat.widget.AppCompatEditText>(R.id.emailEditText)
        val passEditText = findViewById<androidx.appcompat.widget.AppCompatEditText>(R.id.passEditText)
        val passEditText2 = findViewById<androidx.appcompat.widget.AppCompatEditText>(R.id.passEditText2)
        val showHidePassButton = findViewById<ImageButton>(R.id.showHidePassButton)
        val showHidePassConfirmButton = findViewById<ImageButton>(R.id.showHidePassConfirmButton)

        var isPasswordVisible = false
        var isPasswordVisible2 = false
        auth = FirebaseAuth.getInstance()
        passEditText.transformationMethod = PasswordTransformationMethod.getInstance()
        passEditText2.transformationMethod = PasswordTransformationMethod.getInstance()
        // Configurar la acción para mostrar/ocultar la contraseña
        showHidePassButton.setOnClickListener {
            if (isPasswordVisible) {
                // Si la contraseña está visible, la ocultamos
                passEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                showHidePassButton.setImageResource(R.drawable.ic_visibility_off)
            } else {
                // Si la contraseña está oculta, la mostramos
                passEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                showHidePassButton.setImageResource(R.drawable.ic_visibility)
            }
            // Mover el cursor al final del texto
            passEditText.setSelection(passEditText.text?.length ?: 0)
            isPasswordVisible = !isPasswordVisible
        }
        showHidePassConfirmButton.setOnClickListener {
            if (isPasswordVisible2) {
                // Si la contraseña está visible, la ocultamos
                passEditText2.transformationMethod = PasswordTransformationMethod.getInstance()
                showHidePassButton.setImageResource(R.drawable.ic_visibility_off)
            } else {
                // Si la contraseña está oculta, la mostramos
                passEditText2.transformationMethod = HideReturnsTransformationMethod.getInstance()
                showHidePassButton.setImageResource(R.drawable.ic_visibility)
            }
            // Mover el cursor al final del texto
            passEditText2.setSelection(passEditText2.text?.length ?: 0)
            isPasswordVisible2 = !isPasswordVisible2
        }
        emailEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                // Cierra el teclado
                emailEditText.clearFocus()
                hideKeyboard()
                true
            } else {
                false
            }
        }
        passEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                // Cierra el teclado
                emailEditText.clearFocus()
                hideKeyboard()
                true
            } else {
                false
            }
        }
        passEditText2.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                // Cierra el teclado
                emailEditText.clearFocus()
                hideKeyboard()
                true
            } else {
                false
            }
        }

        ///////// implementacion de la autenticacion
        auth = FirebaseAuth.getInstance()   //creamos una autenticacion

        // ACCIONES AL PULSAR LE BOTON DE REGISTRARSE
        registrarButton.setOnClickListener(){  // al hacer click en el boton de registrar hace lo que esta dentro
            val email = emailEditText.text.toString()
            if (emailEditText.text!!.isNotEmpty() && passEditText.text!!.isNotEmpty() && passEditText2.text!!.isNotEmpty()){
                if (passEditText.text.toString() == passEditText2.text.toString()){
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                        emailEditText.text.toString(),        //servicio de firebase autentication
                        passEditText.text.toString()
                    ).addOnCompleteListener(){    //notifica si el registro a sido satisfactorio
                        if (it.isSuccessful){ //si la operacion se completa correctamente ...
                            val UID = getUserId()

                            database = FirebaseDatabase.getInstance().reference

                            Log.d(TAG, "UID: $UID")

                            database.child("users").child(UID!!).setValue(UserUbi())

                            crearcolletion(email, UID)  // solo crea a base de datos si los edit text no estan vacios

                            showHome(
                                it.result?.user?.email ?: "",
                                ProviderType.CORREO_ELECTRONICO
                            )   //en caso de no existir email manda un vacio, si no da error
                        }
                        else{
                            showAlert()
                        }
                    }
                }else{
                    showAlertPass()
                }

            }
            // en caso de que los edit text esten vacios agrega esta alerta

            else {
                Toast.makeText(this, "Porfavor Ingrese Datos", Toast.LENGTH_SHORT)
                    .show()    // mensaje en caso de estar vacio
            }
        }
    }

    private fun showAlertPass() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("¡Error O_O!")
        builder.setMessage("Las contraseñas no coiciden")
        builder.setPositiveButton("aceptar",null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    // FUNCIONES AUXIALIARES EN CASO DE CUALQUIER ACCION ANTERIOR
    private fun showAlert(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("¡Error O_O!")
        builder.setMessage("Se ha producido un error de autenticacion al usuario. Comprueba tu conexión a internet.")
        builder.setPositiveButton("Entendido",null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showHome (email: String, provider: ProviderType){
        val homeIntent = Intent (this, HomeActivity::class.java).apply {
            putExtra("Email", email)
            putExtra("provider", provider.name)
        }
        startActivity(homeIntent)
        finish()
    }
    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
    fun crearcolletion(email: String, UID: String){
        db.collection("users").document(UID)
            .set(user)
            .addOnSuccessListener { Log.d(HomeActivity.TAG, "Documento creado exitosamente") }
            .addOnFailureListener { e -> Log.w(HomeActivity.TAG, "Error al crear el documento", e) }
        db.collection("users").document(UID).update("Email", email)
        database = FirebaseDatabase.getInstance().reference
        database.child("users").child(UID).setValue(UserUbi())
        database.child("users").child(UID).child("Geovallas").child("ESIME").setValue(GeofenceD())
    }
    fun getUserId(): String? {
        val currentUser = FirebaseAuth.getInstance().currentUser
        return currentUser?.uid // El UID generado es único para cada usuario
    }
}