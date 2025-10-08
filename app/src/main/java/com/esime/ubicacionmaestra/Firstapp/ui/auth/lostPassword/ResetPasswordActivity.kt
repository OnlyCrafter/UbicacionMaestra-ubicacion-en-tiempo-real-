    package com.esime.ubicacionmaestra.Firstapp.ui.auth.lostPassword

    import android.content.Intent
    import android.os.Bundle
    import android.view.inputmethod.EditorInfo
    import android.view.inputmethod.InputMethodManager
    import android.widget.Button
    import android.widget.EditText
    import android.widget.Toast
    import androidx.appcompat.app.AppCompatActivity
    import com.esime.ubicacionmaestra.Firstapp.ui.auth.login.loginActivity
    import com.esime.ubicacionmaestra.R
    import com.google.android.material.internal.ViewUtils.hideKeyboard
    import com.google.firebase.auth.FirebaseAuth

    class ResetPasswordActivity : AppCompatActivity() {

        private lateinit var auth: FirebaseAuth

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_reset_password)

            // Inicializa Firebase Auth
            auth = FirebaseAuth.getInstance()

            // Referencia al botón y campo de texto de correo electrónico
            val emailEditText = findViewById<EditText>(R.id.emailEditText)
            val resetButton = findViewById<Button>(R.id.resetButton)


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

            // Acción al presionar el botón de restablecimiento
            resetButton.setOnClickListener {
                val email = emailEditText.text.toString()

                if (email.isNotEmpty()) {
                    // Inicia el proceso de restablecimiento de contraseña
                    auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Correo de restablecimiento enviado", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, loginActivity::class.java)
                                startActivity(intent)
                                finish() // Cierra la actividad
                            } else {
                                Toast.makeText(this, "Error al enviar correo", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Por favor ingresa un correo electrónico", Toast.LENGTH_SHORT).show()
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

    }
