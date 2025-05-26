package com.example.recetapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton // Si tienes el ImageButton de perfil
import android.widget.Toast
import androidx.activity.viewModels // Importa para by viewModels()
import androidx.appcompat.widget.Toolbar // Si usas Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.example.recetapp.adapters.ViewPagerAdapter
import com.example.recetapp.fragments.PantryFragmentListener // Importa la interfaz
import com.example.recetapp.viewmodels.SearchViewModel // Importa tu SearchViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

class MainActivity : AppCompatActivity(), PantryFragmentListener { // Implementa la interfaz

    private val TAG = "MainActivity"
    private lateinit var auth: FirebaseAuth

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var toolbar: Toolbar // Asumiendo que tienes una Toolbar
    private lateinit var imageButtonUserProfile: ImageButton // Asumiendo que tienes este botón

    // Obtiene una instancia del SharedViewModel
    private val searchViewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Tu layout con Toolbar, TabLayout, ViewPager2

        auth = Firebase.auth

        toolbar = findViewById(R.id.toolbar) // Asegúrate de que el ID sea correcto
        setSupportActionBar(toolbar)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        imageButtonUserProfile = findViewById(R.id.imageButtonUserProfile) // Asegúrate de que el ID sea correcto

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Despensa"
                1 -> "Buscar"
                2 -> "Lista Compra"
                else -> null
            }
        }.attach()

        if (auth.currentUser == null && !isFinishing) {
            Log.w(TAG, "Usuario no logueado en MainActivity, volviendo a Login.")
            navigateToLogin()
        } else {
            viewPager.setCurrentItem(1, false) // Pestaña de búsqueda como inicial
        }

        imageButtonUserProfile.setOnClickListener {
            // Lógica para el botón de perfil (ej. mostrar diálogo de logout)
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
        val options = arrayOf("Ver perfil de $displayName", "Cerrar Sesión")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Opciones de Usuario")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        Toast.makeText(this, "Funcionalidad de perfil próximamente.", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        auth.signOut()
                        // (Opcional) Google Sign Out si lo implementaste
                        // val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                        // GoogleSignIn.getClient(this, gso).signOut()
                        Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
                        navigateToLogin()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ---- Implementación de PantryFragmentListener ----
    override fun onSearchRequestedFromPantry(query: String) {
        Log.d(TAG, "MainActivity: Búsqueda solicitada desde despensa con query: $query")
        // 1. Actualiza el ViewModel con la nueva consulta
        searchViewModel.setSearchQuery(query)
        // 2. Cambia a la pestaña de búsqueda (posición 1)
        viewPager.setCurrentItem(1, true) // true para una animación suave
    }
}
