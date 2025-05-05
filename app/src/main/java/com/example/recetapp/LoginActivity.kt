package com.example.recetapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
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



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 1- Inicar firebase auth
        auth = Firebase.auth

        // 2- Inicializar vistas
        emailEditText = findViewById(R.id.editTextLoginEmail)
        passwordEditText = findViewById(R.id.editTextLoginPassword)
        loginButton = findViewById(R.id.buttonLoginEmail)
        registerButton = findViewById(R.id.buttonRegisterEmail)
        // googleSignInButton = findViewById(R.id.buttonGoogleSignIn)

        // 3- Configurar listener para el boton de registro
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

        // 4- Configurar listener para el boton de login
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

    }
    // -5 Comprobar si el usuario ya esta conectado al iniciar la activity
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

    fun navigateToMain(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}