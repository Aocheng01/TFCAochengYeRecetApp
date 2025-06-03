package com.example.recetapp

import android.content.Context // Importar Context
import android.content.Intent
import android.content.SharedPreferences // Importar SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate // Importar AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.example.recetapp.adapters.ViewPagerAdapter
import com.example.recetapp.fragments.PantryFragmentListener
import com.example.recetapp.viewmodels.SearchViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity(), PantryFragmentListener {

    private val TAG = "MainActivity"
    private lateinit var auth: FirebaseAuth

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var toolbar: Toolbar
    private lateinit var imageButtonUserProfile: ImageButton

    private val searchViewModel: SearchViewModel by viewModels()

    // --- NUEVO: Para SharedPreferences de Tema ---
    private lateinit var themePrefs: SharedPreferences
    private val PREFS_NAME = "theme_prefs"
    private val PREF_KEY_THEME = "selected_theme"
    // -------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- NUEVO: Cargar y aplicar tema guardado ---
        themePrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        applySavedTheme()
        // ------------------------------------------

        setContentView(R.layout.activity_main) // Tu layout con Toolbar, TabLayout, ViewPager2

        auth = Firebase.auth

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        imageButtonUserProfile = findViewById(R.id.imageButtonUserProfile)

        val adapter = ViewPagerAdapter(this) //
        viewPager.adapter = adapter

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
            viewPager.setCurrentItem(1, false) // Pestaña de búsqueda (índice 1) como inicial
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

    // ----- MODIFICADO: showUserProfileOptions con nuevas opciones -----
    private fun showUserProfileOptions() {
        val user = auth.currentUser
        val displayName = user?.displayName ?: user?.email ?: "Usuario"

        // Nuevas opciones añadidas
        val options = arrayOf(
            "Perfil de $displayName",
            "Cambiar Contraseña",
            "Cambiar Modo (Oscuro/Claro)",
            "Cerrar Sesión"
        )

        AlertDialog.Builder(this)
            .setTitle("Opciones de Usuario")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Ver Perfil
                    }
                    1 -> { // Cambiar Contraseña
                        handleChangePassword()
                    }
                    2 -> { // Cambiar Modo
                        toggleTheme()
                    }
                    3 -> { // Cerrar Sesión
                        auth.signOut()
                        Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
                        navigateToLogin()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    // -------------------------------------------------------------------

    // ----- NUEVO: Lógica para cambiar contraseña -----
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
    // ----------------------------------------------

    // ----- NUEVO: Lógica para cambiar y guardar el tema -----
    private fun applySavedTheme() {
        val savedTheme = themePrefs.getInt(PREF_KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedTheme)
    }

    private fun toggleTheme() {
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        var newNightMode: Int

        // Alternar entre modo claro y oscuro. Se podría añadir opción de sistema.
        if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            newNightMode = AppCompatDelegate.MODE_NIGHT_NO
            Toast.makeText(this, "Cambiando a Modo Claro", Toast.LENGTH_SHORT).show()
        } else {
            newNightMode = AppCompatDelegate.MODE_NIGHT_YES
            Toast.makeText(this, "Cambiando a Modo Oscuro", Toast.LENGTH_SHORT).show()
        }

        AppCompatDelegate.setDefaultNightMode(newNightMode)
        themePrefs.edit().putInt(PREF_KEY_THEME, newNightMode).apply()

        // Opcional: recrear la actividad para que los cambios de tema se apliquen inmediatamente
        // recreate()
        // Si no usas recreate(), algunos cambios pueden requerir reiniciar la app o navegar
        // entre actividades para verse completamente. AppCompatDelegate intenta manejarlo,
        // pero recreate() es más directo.
    }
    // -------------------------------------------------------


    override fun onSearchRequestedFromPantry(query: String) {
        Log.d(TAG, "MainActivity: Búsqueda solicitada desde despensa con query: $query")
        searchViewModel.setSearchQuery(query)
        viewPager.setCurrentItem(1, true) // Navega a la pestaña "Buscar" (índice 1)
    }
}