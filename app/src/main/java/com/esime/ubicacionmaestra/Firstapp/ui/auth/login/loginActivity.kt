package com.esime.ubicacionmaestra.Firstapp.ui.auth.login

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
import androidx.activity.enableEdgeToEdge
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

class loginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth   //variable de autenticacion
        companion object {
        const val TAG = "registerActivity" // Definimos la variable TAG aqui
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        supportActionBar?.hide()
        setup()
    }
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, welcomeActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun setup() {
        // Declaracion de lo botones de la interfaz
        val loginButton = findViewById<Button>(R.id.loginButton)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passEditText = findViewById<EditText>(R.id.passEditText)
        val showHidePassButton = findViewById<ImageButton>(R.id.showHidePassButton)

        var isPasswordVisible = false

        emailEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                emailEditText.clearFocus()
                true
            } else {
                false
            }
        }
        passEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                emailEditText.clearFocus()
                true
            } else {
                false
            }
        }

        auth = FirebaseAuth.getInstance()   //creamos una autenticacion
        passEditText.transformationMethod = PasswordTransformationMethod.getInstance()
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

        // ACCIONES AL PULSAR LE BOTON DE INGRESAR
        loginButton.setOnClickListener(){
            if(emailEditText.text.isNotEmpty() && passEditText.text.isNotEmpty()){
                FirebaseAuth.getInstance().signInWithEmailAndPassword(emailEditText.text.toString(),passEditText.text.toString()).addOnCompleteListener(){
                    if (it.isSuccessful){
                        showHome(it.result?.user?.email?:"", ProviderType.CORREO_ELECTRONICO)   //en caso de no existir email manda un vacio, si no da error
                    }
                    else{
                        showAlert()
                    }
                }
            }
        }
    }
    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    // FUNCIONES AUXIALIARES EN CASO DE CUALQUIER ACCION ANTERIOR
    private fun showAlert(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("¡Error O_O!")
        builder.setMessage("Se ha producido un erro de autenticacion al usuario X_X")
        builder.setPositiveButton("aceptar",null)
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

}