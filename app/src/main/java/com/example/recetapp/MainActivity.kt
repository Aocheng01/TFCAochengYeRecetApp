package com.example.recetapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.example.recetapp.adapters.ViewPagerAdapter
import com.example.recetapp.fragments.PantryFragmentListener
import com.example.recetapp.utils.ZoomOutPageTransformer // Asegúrate que la ruta sea correcta
import com.example.recetapp.viewmodels.SearchViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn // NUEVA IMPORTACIÓN
import com.google.android.gms.auth.api.signin.GoogleSignInClient // NUEVA IMPORTACIÓN
import com.google.android.gms.auth.api.signin.GoogleSignInOptions // NUEVA IMPORTACIÓN
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity(), PantryFragmentListener {

    private val TAG = "MainActivity"
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient // NUEVA PROPIEDAD

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var toolbar: Toolbar
    private lateinit var imageButtonUserProfile: ImageButton

    private val searchViewModel: SearchViewModel by viewModels()

    private lateinit var themePrefs: SharedPreferences
    private val PREFS_NAME = "theme_prefs"
    private val PREF_KEY_THEME = "selected_theme"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        themePrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        applySavedTheme()

        setContentView(R.layout.activity_main)

        auth = Firebase.auth

        // ----- Configurar GoogleSignInClient para el logout -----
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Asegúrate que este string existe
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        // -------------------------------------------------------------

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        imageButtonUserProfile = findViewById(R.id.imageButtonUserProfile)

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.setPageTransformer(ZoomOutPageTransformer())

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Despensa"
                1 -> "Buscar"
                2 -> "Lista Compra"
                3 -> "Favoritos"
                else -> null
            }
        }.attach()

        if (auth.currentUser == null && !isFinishing) {
            Log.w(TAG, "Usuario no logueado en MainActivity, volviendo a Login.")
            navigateToLogin()
        } else {
            viewPager.setCurrentItem(1, false)
        }

        imageButtonUserProfile.setOnClickListener {
            showUserProfileOptions()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showUserProfileOptions() {
        val user = auth.currentUser
        val displayName = user?.displayName ?: user?.email ?: "Usuario"

        val options = arrayOf(
            "Usuario: $displayName",
            "Cambiar Contraseña",
            "Cambiar Modo (Oscuro/Claro)",
            "Cerrar Sesión"
        )

        AlertDialog.Builder(this)
            .setTitle("Opciones de Usuario")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                    }
                    1 -> {
                        handleChangePassword()
                    }
                    2 -> {
                        toggleTheme()
                    }
                    3 -> { // Cerrar Sesiónd

                        auth.signOut() // Firebase sign out
                        googleSignInClient.signOut().addOnCompleteListener { // Google Sign Out -----
                            Log.d(TAG, "Google Sign-Out completado.")
                        }
                        // ------------------------------------------
                        Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
                        navigateToLogin()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun handleChangePassword() {
        val user = auth.currentUser
        if (user?.email != null) {
            auth.sendPasswordResetEmail(user.email!!)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Se ha enviado un correo a ${user.email} para restablecer tu contraseña.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Error al enviar correo de restablecimiento: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Error sendPasswordResetEmail", task.exception)
                    }
                }
        } else {
            Toast.makeText(this, "No se pudo obtener el email del usuario.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applySavedTheme() {
        val savedTheme = themePrefs.getInt(PREF_KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        if (AppCompatDelegate.getDefaultNightMode() != savedTheme) {
            AppCompatDelegate.setDefaultNightMode(savedTheme)
        }
    }

    private fun toggleTheme() {
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        val newNightMode = if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES ||
            (currentNightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM &&
                    (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES)) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }

        AppCompatDelegate.setDefaultNightMode(newNightMode)
        themePrefs.edit().putInt(PREF_KEY_THEME, newNightMode).apply()
    }

    override fun onSearchRequestedFromPantry(query: String) {
        Log.d(TAG, "MainActivity: Búsqueda solicitada desde despensa con query: $query")
        searchViewModel.setSearchQuery(query)
        viewPager.setCurrentItem(1, true) // Navega a la pestaña "Buscar" (índice 1)
    }
}