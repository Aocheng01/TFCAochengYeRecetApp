package com.example.recetapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"

    // Referencias a Firebase Authentication
    private lateinit var auth: FirebaseAuth

    // Referencias a las vistas
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button

    // El botón de Google lo configuraremos en el siguiente paso
    // private lateinit var googleSignInButton: com.google.android.gms.common.SignInButton

    //  Componentes para Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var googleSignInButton: SignInButton
    // -------------------------------------------------------------


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 1- Inicar firebase auth
        auth = Firebase.auth

        // 1.2- Inicializar vistas
        emailEditText = findViewById(R.id.editTextLoginEmail)
        passwordEditText = findViewById(R.id.editTextLoginPassword)
        loginButton = findViewById(R.id.buttonLoginEmail)
        registerButton = findViewById(R.id.buttonRegisterEmail)
        // googleSignInButton = findViewById(R.id.buttonGoogleSignIn)

        // 1.3- Configurar listener para el boton de registro
        registerButton.setOnClickListener{
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            //validacion
            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Email y contraseña no pueden estar vacíos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Sale del listener si hay error
            }
            if(password.length<6){
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Sale del listener si hay error
            }

            Log.d(TAG, "Intentando registrar usuario con email: $email")
            //Llamma a Firebase para crear el usuario
            auth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Registro exitoso para $email")
                        Toast.makeText(this, "Registro exitoso para $email", Toast.LENGTH_SHORT)
                            .show()
                        navigateToMain() //Navega a la actividad principal
                    } else {
                        // Si el registro falla, muestra un mensaje al usuario
                        Log.w(TAG, "Error en el registro", task.exception)
                        Toast.makeText(
                            this,
                            "Error en el registro: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        // 1.4- Configurar listener para el boton de login
        loginButton.setOnClickListener{
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            //validacion
            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Email y contraseña no pueden estar vacíos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Sale del listener si hay error
            }

            Log.d(TAG, "Intentando iniciar sesión con email: $email")
            auth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Inicio de sesión exitoso para $email")
                        Toast.makeText(
                            this,
                            "Inicio de sesión exitoso para $email",
                            Toast.LENGTH_SHORT
                        ).show()
                        navigateToMain()
                    } else {
                        //si el inicio de sesion falla
                        Log.w(TAG, "Error en el inicio de sesión", task.exception)
                        Toast.makeText(
                            this,
                            "Error en el inicio de sesión: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        // --- Configuración de Google Sign-In---
        // 2. Configurar el ActivityResultLauncher
        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleGoogleSignInResultTask(task)
            } else {
                Log.w(TAG, "Google Sign In (clásico) cancelado o fallido. ResultCode: ${result.resultCode}")
                Toast.makeText(this, "Inicio de sesión con Google cancelado.", Toast.LENGTH_SHORT).show()
            }
        }

        // 2.1 Configurar GoogleSignInOptions para solicitar ID Token y email.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // 2.3 Construir un GoogleSignInClient con las opciones especificadas.
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 2.4 Configurar el listener para el botón de Google Sign-In
        googleSignInButton = findViewById(R.id.buttonGoogleSignIn)
        googleSignInButton.setOnClickListener {
            Log.d(TAG, "Botón Google Sign-In (clásico) presionado")
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    } // Fin de onCreate


    // -1.5 Comprobar si el usuario ya esta conectado al iniciar la activity
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Usuario ya conectado (${currentUser.email}), navegando a Main.")
            navigateToMain()
        }else  {
            Log.d(TAG, "No hay usuario conectado.")
        }
    }

    // 2.5--- Manejador para el resultado de Google Sign-In (versión "clásica") ---
    private fun handleGoogleSignInResultTask(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Inicio de sesión con Google exitoso, ahora autentica con Firebase
            Log.d(TAG, "Google Sign-In (clásico) exitoso, cuenta: ${account?.email}")
            firebaseAuthWithGoogle(account?.idToken) // Pasa el ID token
        } catch (e: ApiException) {
            // Google Sign In falló
            Log.w(TAG, "Google Sign-In (clásico) fallido, código de APIException: ${e.statusCode}", e)
            Toast.makeText(this, "Fallo al iniciar sesión con Google: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    // 2.6 Función para autenticar con Firebase usando el ID Token de Google
    private fun firebaseAuthWithGoogle(idToken: String?) {
        if (idToken == null) {
            Toast.makeText(this, "No se pudo obtener el token de Google.", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Google ID Token es nulo")
            return
        }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase signInWithGoogle:success")
                    val user = auth.currentUser
                    Toast.makeText(this, "Bienvenido ${user?.displayName ?: user?.email}", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Log.w(TAG, "Firebase signInWithGoogle:failure", task.exception)
                    Toast.makeText(this, "Fallo de autenticación con Firebase/Google: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    fun navigateToMain(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}