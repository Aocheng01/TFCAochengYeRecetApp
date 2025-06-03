package com.example.recetapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType // Importar InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout // Para el layout del AlertDialog
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog // Importar AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var auth: FirebaseAuth

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var googleSignInButton: SignInButton
    private lateinit var textViewForgotPassword: TextView // NUEVA PROPIEDAD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        emailEditText = findViewById(R.id.editTextLoginEmail)
        passwordEditText = findViewById(R.id.editTextLoginPassword)
        loginButton = findViewById(R.id.buttonLoginEmail)
        registerButton = findViewById(R.id.buttonRegisterEmail)
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword) // INICIALIZAR TEXTVIEW

        registerButton.setOnClickListener{
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Email y contraseña no pueden estar vacíos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(password.length<6){
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(TAG, "Intentando registrar usuario con email: $email")
            auth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Registro exitoso para $email")
                        Toast.makeText(this, "Registro exitoso para $email", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    } else {
                        Log.w(TAG, "Error en el registro", task.exception)
                        Toast.makeText(this, "Error en el registro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        loginButton.setOnClickListener{
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Email y contraseña no pueden estar vacíos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(TAG, "Intentando iniciar sesión con email: $email")
            auth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Inicio de sesión exitoso para $email")
                        Toast.makeText(this, "Inicio de sesión exitoso para $email", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    } else {
                        Log.w(TAG, "Error en el inicio de sesión", task.exception)
                        Toast.makeText(this, "Error en el inicio de sesión: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // ----- NUEVO LISTENER PARA OLVIDÉ CONTRASEÑA -----
        textViewForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
        // -----------------------------------------------

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleGoogleSignInResultTask(task)
            } else {
                Log.w(TAG, "Google Sign In (clásico) cancelado o fallido. ResultCode: ${result.resultCode}")
                Toast.makeText(this, "Inicio de sesión con Google cancelado.", Toast.LENGTH_SHORT).show()
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInButton = findViewById(R.id.buttonGoogleSignIn)
        googleSignInButton.setOnClickListener {
            Log.d(TAG, "Botón Google Sign-In (clásico) presionado")
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

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

    private fun handleGoogleSignInResultTask(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d(TAG, "Google Sign-In (clásico) exitoso, cuenta: ${account?.email}")
            firebaseAuthWithGoogle(account?.idToken)
        } catch (e: ApiException) {
            Log.w(TAG, "Google Sign-In (clásico) fallido, código de APIException: ${e.statusCode}", e)
            Toast.makeText(this, "Fallo al iniciar sesión con Google: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

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

    // ----- NUEVO MÉTODO PARA MOSTRAR DIÁLOGO DE RECUPERACIÓN -----
    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Recuperar Contraseña")
        builder.setMessage("Introduce tu email para enviarte las instrucciones de recuperación:")

        val inputEmail = EditText(this)
        inputEmail.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        // Añadir un poco de padding al EditText
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        inputEmail.layoutParams = lp
        // Para dar margen interno al EditText dentro del diálogo, creamos un contenedor
        val container = LinearLayout(this)
        val containerParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val marginInDp = 16 // Margen en dp
        val marginInPx = (marginInDp * resources.displayMetrics.density).toInt()
        containerParams.setMargins(marginInPx, marginInPx/2, marginInPx, marginInPx/2) // Izquierda, Arriba, Derecha, Abajo
        container.layoutParams = containerParams
        container.addView(inputEmail)

        builder.setView(container) // Añadir el contenedor con el EditText

        builder.setPositiveButton("Enviar") { dialog, _ ->
            val email = inputEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(this, "Por favor, introduce tu email.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }
    // ----------------------------------------------------------

    // ----- NUEVO MÉTODO PARA ENVIAR CORREO DE RECUPERACIÓN -----
    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Email de restablecimiento enviado a $email")
                    Toast.makeText(this, "Se ha enviado un correo para restablecer tu contraseña a $email", Toast.LENGTH_LONG).show()
                } else {
                    Log.w(TAG, "Error al enviar email de restablecimiento", task.exception)
                    Toast.makeText(this, "Error al enviar correo: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
    // ---------------------------------------------------------
}