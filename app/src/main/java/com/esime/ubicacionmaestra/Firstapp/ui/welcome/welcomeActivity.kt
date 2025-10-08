package com.esime.ubicacionmaestra.Firstapp.ui.welcome

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.esime.ubicacionmaestra.Firstapp.ui.auth.login.loginActivity
import com.esime.ubicacionmaestra.Firstapp.ui.auth.lostPassword.ResetPasswordActivity
import com.esime.ubicacionmaestra.Firstapp.ui.auth.register.registerActivity
import com.esime.ubicacionmaestra.Firstapp.ui.home.HomeActivity
import com.esime.ubicacionmaestra.R
import com.google.firebase.auth.FirebaseAuth

class welcomeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth   //variable de autenticacion
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        supportActionBar?.hide()
        auth = FirebaseAuth.getInstance()
        ///////// implementacion de la autenticacion
        auth = FirebaseAuth.getInstance()   //creamos una autenticacion
        val currentUser = auth.currentUser
        if (currentUser != null) {
            //si el usuario esta autenticasdo, redirige a home
            val intent = Intent(this, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK) //flag para definir la actividad actual y limpiar la anterior
            startActivity(intent)   // inicia la activity
            finish()  //finaliza la pila de actividad anterior
        }
        else{
            //no hace nada y pasa a las demas actividades
        }

        val loginButton = findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.buttonLogin)
        val createAccountButton = findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.createAccountButton)
        val lostPassButton = findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.lostPassButton)

        loginButton.setOnClickListener {
            val intent = Intent(this, loginActivity::class.java)
            startActivity(intent)
            finish()
        }
        createAccountButton.setOnClickListener {
            val intent = Intent(this, registerActivity::class.java)
            startActivity(intent)
            finish()
        }
        lostPassButton.setOnClickListener {
            val intent = Intent(this, ResetPasswordActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
}