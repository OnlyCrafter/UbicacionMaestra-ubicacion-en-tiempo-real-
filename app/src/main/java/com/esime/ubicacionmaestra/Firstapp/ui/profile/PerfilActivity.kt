package com.esime.ubicacionmaestra.Firstapp.ui.profile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.esime.ubicacionmaestra.Firstapp.ui.home.HomeActivity
import com.esime.ubicacionmaestra.R
import com.google.android.material.internal.ViewUtils.hideKeyboard
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.yalantis.ucrop.UCrop
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.random.Random


class PerfilActivity : AppCompatActivity() {

    // Declaracion de las variables para los elementos de la interfaz
    private lateinit var saveButton: Button
    private lateinit var nombresEditText: EditText
    private lateinit var apellidosEditText: EditText
    private lateinit var telefonoEditText: EditText
    private lateinit var imageView: ImageView
    private lateinit var bitmap: Bitmap // Declaración global del bitmap
    private var resultUri: Uri? = null
    private lateinit var database: DatabaseReference

    private val PICK_IMAGE_REQUEST = 1
    private lateinit var fileUri: Uri

    private var uid: String? = null

    //private var emailCon: String? = null
    private var GrupoIDPublic: String? = null

    // Declaracion del objeto para la base de datos
    private val db = FirebaseFirestore.getInstance()

    // Constantes para el tag de la actividad pra poder usar el LOGCAT
    companion object {
        const val TAG = "PerfilActivity"
    }

    // Función para generar una clave aleatoria con el formato xxxx-xxxx-xxxx
    private fun generateRandomKey(): String {
        val random = Random.Default
        return buildString {
            repeat(4) {
                append(random.nextChar())
            }
            append('-')
            repeat(4) {
                append(random.nextChar())
            }
            append('-')
            repeat(4) {
                append(random.nextChar())
            }
        }
    }

    // Función de extensión para generar un carácter aleatorio entre 'A' y 'Z' o entre '0' y '9'
    fun Random.nextChar(): Char {
        val chars = ('A'..'Z') + ('0'..'9')
        return chars.random(this)
    }

    // Funcion que inicia al entrar a la activity
    @SuppressLint("MissingInflatedId", "ServiceCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        supportActionBar?.hide()    // Oculta la barra de título
        database = Firebase.database.reference
        // Inicializar los elementos de la interfaz con los IDs correspondientes
        saveButton = findViewById(R.id.GuardarDatosButton)
        nombresEditText = findViewById(R.id.Nombres)
        apellidosEditText = findViewById(R.id.Apellidos)
        telefonoEditText = findViewById(R.id.NumTelefono)
        imageView = findViewById(R.id.imageViewFoto)

        // Inicializar los elementos de la interfaz con los IDs correspondientes
        val JoinGrupoButton = findViewById<Button>(R.id.JoinGrupoButton)
        val CreateGrupoButton = findViewById<Button>(R.id.CreateGrupoButton)
        val SalirGrupoButton = findViewById<Button>(R.id.SalirGrupoButton)
        val IDGrupo = findViewById<TextView>(R.id.IDGrupo)
        val PertenecerGrupo = findViewById<TextView>(R.id.textGrupo)
        val botonHablarConBot: Button = findViewById(R.id.boton_iniciar_conversacion)


        // Aparecen muchas veces las declaraciones de los botones y asi pero es para que se muestren en los campos y puedas modificarlos directamente

        val bundle = intent.extras                              // recuperar parametros
        //emailCon = bundle?.getString("Email1")              //parametro del home layut "como nombramos al edit text"
        uid = bundle?.getString("UID")

        IDGrupo.transformationMethod =
            PasswordTransformationMethod.getInstance()   // Para que el ID del grupo este oculto

        // Obtener el documento del usuario en la base de datos y poder usar los datos
        val docRef3 = db.collection("users").document(uid!!)
        docRef3.get()
            .addOnSuccessListener { document ->    // Si se encuentra el documento se ejecuta el codigo dentro
                val GrupoID = document.getString("GrupoID") // Gurada el ID del usuario actual
                val nuevaPhoto = document.getString("photoUrl")
                IDGrupo.text = GrupoID
                if (GrupoID != "-") {
                    if (nuevaPhoto != null) {
                        cargarFoto(nuevaPhoto)
                    }
                    GrupoIDPublic = GrupoID
                    PertenecerGrupo.text =
                        "Pertenece al grupo" // Si tiene un ID de grupo cambia el texto a que si Pertence a un grupo
                } else {
                    PertenecerGrupo.text =
                        "No perteneces a un grupo"    // Si no tiene un ID de grupo cambia el texto a que no pertenece a un grupo
                }
            }

        // Copiar el ID del grupo al portapapeles
        IDGrupo.setOnClickListener {
            // Mostrar el contenido del TextView
            IDGrupo.transformationMethod = null
            val ID = IDGrupo.text.toString()
            // Copiar el contenido al portapapeles
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("GrupoID", ID)
            clipboard.setPrimaryClip(clip)

            // Notificar al usuario que el contenido se ha copiado
            Toast.makeText(this, "Grupo ID copiado al portapapeles", Toast.LENGTH_SHORT).show()
        }

        // Botones para crear un grupo
        CreateGrupoButton.setOnClickListener {
            val docRef = db.collection("users")
                .document(uid!!)     // Se obtiene el documento del usuario actual
            docRef.get()
                .addOnSuccessListener { document -> // Si se encuentra el documento se ejecuta el codigo dentro
                    val GrupoID =
                        document.getString("GrupoID") // Guarda el ID del grupo del usuario actual
                    if (GrupoID == "-") {   // Si el ID del grupo del usuario actual es igual a "-" se la asigna un nuevo ID de grupo
                        val randomKey = generateRandomKey() // Genera un nuevo ID de grupo aleatorio
                        db.collection("users").document(uid!!)
                            .update(
                                "GrupoID",
                                randomKey
                            )   // Actualiza el ID del grupo del usuario actual en la base de datos
                        IDGrupo.text = randomKey
                        PertenecerGrupo.text =
                            "Pertenece al grupo"  // Cambia el texto del botón para indicar que el usuario pertenece al grupo

                        // Datos de la base de datos (Formato)
                        val groupData = hashMapOf(
                            "email1" to uid,
                            "email2" to null,
                            "email3" to null,
                            "email4" to null,
                            "email5" to null,
                            "email6" to null,
                            "email7" to null,
                        )
                        GrupoIDPublic = randomKey
                        db.collection("grupos").document(randomKey).set(groupData)
                    } else {
                        Toast.makeText(this, "Ya perteneces a un grupo", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        JoinGrupoButton.setOnClickListener {    // Boton para unirse a un grupo
            val docRef = db.collection("users")
                .document(uid!!)    // Se obtiene el documento del usuario actual
            docRef.get()
                .addOnSuccessListener { document -> // Si se encuentra el documento se ejecuta el código dentro
                    val GrupoID =
                        document.getString("GrupoID") // Guarda el ID del grupo del usuario actual
                    if (GrupoID == "-") {   // Si el ID del grupo del usuario actual es igual a "-" desplegará el menú emergente para ingresar el ID del grupo
                        mostrarMenuEmergente { ID ->
                            db.collection("users").document(uid!!).update(
                                "GrupoID",
                                ID
                            ) // Actualiza el ID del grupo que ingresó en el menú emergente en la base de datos
                            PertenecerGrupo.text =
                                "Pertenece al grupo" // Cambia el texto del botón para indicar que el usuario pertenece al grupo
                            IDGrupo.text = ID

                            val grupoRef = db.collection("grupos").document(ID)

                            grupoRef.get().addOnSuccessListener { grupoDocument ->
                                if (grupoDocument.exists()) {
                                    // Recorre los campos del grupo
                                    var campoDisponible: String? = null
                                    for (i in 1..7) {
                                        val emailField = "email$i"
                                        val emailValue = grupoDocument.getString(emailField)
                                        if (emailValue == null) {
                                            campoDisponible = emailField
                                            break
                                        }
                                    }
                                    if (campoDisponible != null) {
                                        // Si hay un campo disponible, agrega el nuevo email
                                        grupoRef.update(campoDisponible, uid)
                                        Toast.makeText(
                                            this,
                                            "Te has unido al grupo",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        // Si no hay campos disponibles, muestra un mensaje
                                        Toast.makeText(
                                            this,
                                            "El grupo está lleno",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Ya perteneces a un grupo, si quieres cambiar a otro grupo por favor salte del grupo actual",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }


        SalirGrupoButton.setOnClickListener {   // Botón para salir de un grupo
            val docRef = db.collection("users")
                .document(uid!!)    // Se obtiene el documento del usuario actual
            docRef.get()
                .addOnSuccessListener { document -> // Si se encuentra el documento se ejecuta el código dentro
                    val GrupoID =
                        document.getString("GrupoID") // Guarda el ID del grupo del usuario actual
                    if (GrupoID != "-") {   // Si el ID del grupo del usuario actual es diferente de "-" ejecuta el código dentro

                        val grupoRef = db.collection("grupos").document(GrupoID!!)
                        grupoRef.get().addOnSuccessListener { grupoDocument ->
                            if (grupoDocument.exists()) {
                                val emailsMap = grupoDocument.data
                                var emailToRemove: String? = null

                                // Busca el email del usuario en el grupo
                                for ((key, value) in emailsMap!!) {
                                    if (value == uid) {
                                        emailToRemove = key
                                        break
                                    }
                                }

                                if (emailToRemove != null) {
                                    // Actualiza el campo del email del grupo a null (remueve el email del usuario)
                                    grupoRef.update(emailToRemove, null).addOnSuccessListener {
                                        // Actualiza el GrupoID del usuario a "-"
                                        db.collection("users").document(uid!!)
                                            .update("GrupoID", "-")
                                            .addOnSuccessListener {
                                                // Mensaje de éxito
                                                Toast.makeText(
                                                    this,
                                                    "Has salido del grupo",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                PertenecerGrupo.text =
                                                    "No perteneces a un grupo"   // Cambia el texto del botón para indicar que el usuario no pertenece a un grupo
                                                IDGrupo.text =
                                                    "-"  // Cambia el ID del grupo del usuario actual a "-"
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(
                                                    this,
                                                    "Error al actualizar tu grupo",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    }.addOnFailureListener {
                                        Toast.makeText(
                                            this,
                                            "Error al salir del grupo",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    // Si no se encuentra el correo en el grupo
                                    Toast.makeText(
                                        this,
                                        "Tu correo no fue encontrado en el grupo",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this, "No perteneces a un grupo", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Boton para registrar el chat ID del bot

        botonHablarConBot.setOnClickListener {
            // Copiar el contenido al portapapeles
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Codigo de Enlace", uid)
            clipboard.setPrimaryClip(clip)
            // Intent para abrir la conversación con el bot
            val botname = "ubimaster_bot"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("tg://resolve?domain=$botname")
            Log.d(TAG, "Intent creado a: $intent")

            //verificar si telegram esta instalado
            val pm = packageManager

            // Verifica si esta la app de telegram instalada
            try {
                pm.getPackageInfo("org.telegram.messenger", PackageManager.GET_ACTIVITIES)
                startActivity(intent) // Abre la app de Telegram si está instalada
            } catch (e: PackageManager.NameNotFoundException) {
                // Si Telegram no está instalado, abrir en el navegador
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$botname"))
                startActivity(webIntent)
            }
        }


        // Obtener los datos del usuario de la base de datos y mostrarlos en los campos
        val docRef = db.collection("users").document(uid!!)
        docRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val nombres = document.getString("Nombres")
                val apellidos = document.getString("Apellidos")
                val telefono = document.getString("Telefono")
                if (nombres != null) {
                    nombresEditText.setText(nombres)
                }
                if (apellidos != null) {
                    apellidosEditText.setText(apellidos)
                }
                if (telefono != null) {
                    telefonoEditText.setText(telefono)
                }
            } else {
                Log.d("PerfilActivity", "No se encontró el documento")
            }
        }

        saveButton.setOnClickListener {// Boton para guardar los datos del usuario
            if (resultUri != null) {
                subirFoto(resultUri!!)
            }
            uploadProfileData(uid!!) // Funcion para guardar los datos del usuario en la base de datos
            super.onBackPressed()
        }


        val btnSeleccionarFoto: Button = findViewById(R.id.btnSeleccionarFoto)
        btnSeleccionarFoto.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, "Selecciona una imagen"),
                PICK_IMAGE_REQUEST
            )
        }
        nombresEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                nombresEditText.clearFocus()
                nombresEditText.hideKeyboard()  // Oculta el teclado desde la vista actual
                true
            } else {
                false
            }
        }

        apellidosEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                apellidosEditText.clearFocus()
                apellidosEditText.hideKeyboard()
                true
            } else {
                false
            }
        }

        telefonoEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                telefonoEditText.clearFocus()
                telefonoEditText.hideKeyboard()
                true
            } else {
                false
            }
        }
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish() // Cierra la `PerfilActivity`
    }

    private fun cargarFoto(photoUrl: String) {
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(photoUrl)
        val localFile = File.createTempFile("tempImage", "jpg")

        storageRef.getFile(localFile).addOnSuccessListener {
            bitmap = BitmapFactory.decodeFile(localFile.absolutePath) // Actualiza el bitmap global
            imageView.setImageBitmap(bitmap)
        }.addOnFailureListener {
            Log.e(TAG, "Error al cargar la imagen: ${it.message}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            fileUri = data.data!!

            val options = UCrop.Options()
            options.setCircleDimmedLayer(true) // Esto crea el efecto de recorte circular
            options.setShowCropFrame(false)    // Oculta el marco del recorte
            options.setShowCropGrid(false)     // Oculta la cuadrícula de recorte

            // Establece el URI de destino para guardar la imagen recortada
            val destinationUri = Uri.fromFile(File(cacheDir, "tempImage.jpg"))

            // Iniciar uCrop con opciones para el recorte circular
            UCrop.of(fileUri, destinationUri)
                .withAspectRatio(1f, 1f) // Para un recorte cuadrado (1:1)
                .withMaxResultSize(150, 150) // Tamaño máximo del resultado
                .withOptions(options)
                .start(this)
        }

        // Manejar el resultado del recorte de uCrop
        if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
            resultUri = UCrop.getOutput(data!!)
            imageView.setImageURI(resultUri) // Mostrar la imagen recortada
        }
    }

    private fun subirFoto(fileUri: Uri) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        Log.d(TAG, "Subiendo foto para el usuario $userId")

        val storageRef = FirebaseStorage.getInstance().reference.child("fotos/${userId}.jpg")

        // Redimensiona el bitmap antes de subir
        val bitmap = redimensionarBitmap(fileUri)
        val baos = ByteArrayOutputStream()
        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            80,
            baos
        ) // Puedes ajustar el 80 para cambiar la calidad

        val data = baos.toByteArray()
        val uploadTask = storageRef.putBytes(data)
        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val photoUrl = uri.toString()
                savePhotoUrlToFirestore(photoUrl)
            }
        }.addOnFailureListener {
            Log.e(TAG, "Error al subir la foto: ${it.message}")
        }
    }

    private fun redimensionarBitmap(uri: Uri): Bitmap {
        val originalBitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))

        // Define el tamaño máximo que deseas
        val maxWidth = 150 // Ancho máximo
        val maxHeight = 150 // Alto máximo

        val width = originalBitmap.width
        val height = originalBitmap.height
        val scale = Math.min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)

        // Calcula el nuevo tamaño
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        // Crea el nuevo bitmap redimensionado
        return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
    }

    private fun savePhotoUrlToFirestore(photoUrl: String) {
        val firestore = FirebaseFirestore.getInstance()
        val data = hashMapOf("photoUrl" to photoUrl)

        firestore.collection("users").document(uid!!)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "URL de la foto guardada exitosamente")
                Toast.makeText(this, "Foto guardada exitosamente", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar la URL: $e")
                Toast.makeText(
                    this,
                    "Error al guardar Foto. Revise su conexión a internet y vuelva a intentarlo.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // Funcion para mostrar el menu emergente para ingresar el ID del grupo
    private fun mostrarMenuEmergente(onDataEntered: (String) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Ingrese el ID del grupo")

        // Crear un EditText para que el usuario ingrese el dato
        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("Guardar") { _, _ ->
            // Obtener el dato ingresado por el usuario y llamarlo a través del callback
            val datoIngresado = input.text.toString()

            if (isValidKeyFormat(datoIngresado)) {
                // Si el formato es válido, llamar al callback con el dato ingresado
                onDataEntered(datoIngresado)
            } else {
                // Si el formato no es válido, mostrar un mensaje de error
                Toast.makeText(
                    this,
                    "Formato de clave inválido. El ID del Grupo debe tener el formato xxxx-xxxx-xxxx",
                    Toast.LENGTH_LONG
                ).show()
                // Volver a mostrar el menú emergente
                mostrarMenuEmergente(onDataEntered)
            }

            Log.d(TAG, "Dato ingresado: $datoIngresado")
        }

        builder.setCancelable(true) // Para evitar que se cierre el diálogo al tocar fuera de él

        val dialog = builder.create()
        dialog.show()
    }

    // Funcion para validar el formato del ID del grupo
    private fun isValidKeyFormat(key: String): Boolean {
        // Expresión regular para el formato xxxx-xxxx-xxxx
        val regex = Regex("^[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$")
        return key.matches(regex)
    }

    // Funcion para guardar los datos del usuario en la base de datos
    private fun uploadProfileData(emailCon: String) {

        val email = emailCon
        val nombres = nombresEditText.text.toString()
        val apellidos = apellidosEditText.text.toString()
        val telefono = telefonoEditText.text.toString()
        val chat_id = null

        val userData = hashMapOf(   // Formato para la base de datos
            "Nombres" to nombres,
            "Apellidos" to apellidos,
            "Telefono" to telefono,
            "chat_id" to chat_id
        )
        // Conexion con la base de datos para guardar los datos del usuario
        db.collection("users").document(email)
            .update(userData as Map<String, Any>)
            .addOnSuccessListener {
                Log.d(TAG, "Datos guardados exitosamente")
                Toast.makeText(this, "Datos guardados exitosamente", Toast.LENGTH_SHORT).show()
                onBackPressed()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al guardar datos", e)
                Toast.makeText(
                    this,
                    "Error al guardar datos. Revise su conexión a internet y vuelva a intentarlo.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        // Conexión con Realtime Database
        val realtimeDb = FirebaseDatabase.getInstance().getReference("users")

        // Agregar o actualizar solo el campo "Nombre" dentro del ID del usuario
        realtimeDb.child(email).child("Nombre").setValue(nombres)
            .addOnSuccessListener {
                Log.d(TAG, "Nombre guardado exitosamente en Realtime Database")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al guardar el nombre en Realtime Database", e)
            }
    }
}

