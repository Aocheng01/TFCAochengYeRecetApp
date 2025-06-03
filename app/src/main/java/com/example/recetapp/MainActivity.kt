package com.example.recetapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog // Asegúrate que esta importación existe
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.example.recetapp.adapters.ViewPagerAdapter
import com.example.recetapp.fragments.PantryFragmentListener
import com.example.recetapp.viewmodels.SearchViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

class MainActivity : AppCompatActivity(), PantryFragmentListener {

    private val TAG = "MainActivity"
    private lateinit var auth: FirebaseAuth

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var toolbar: Toolbar
    private lateinit var imageButtonUserProfile: ImageButton

    private val searchViewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        imageButtonUserProfile = findViewById(R.id.imageButtonUserProfile)

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        // ----- MODIFICADO: Actualizar títulos de las pestañas para el nuevo orden -----
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
            // ----- MODIFICADO: La pestaña inicial ahora es Buscar (posición 1) -----
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
        val options = arrayOf("Ver perfil de $displayName", "Cerrar Sesión")
        AlertDialog.Builder(this) // Usar androidx.appcompat.app.AlertDialog
            .setTitle("Opciones de Usuario")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        Toast.makeText(this, "Funcionalidad de perfil próximamente.", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        auth.signOut()
                        Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
                        navigateToLogin()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ----- MODIFICADO: La navegación desde la despensa ahora va a la pestaña 1 (Buscar) -----
    override fun onSearchRequestedFromPantry(query: String) {
        Log.d(TAG, "MainActivity: Búsqueda solicitada desde despensa con query: $query")
        searchViewModel.setSearchQuery(query)
        // Cambia a la pestaña de búsqueda (posición 1)
        viewPager.setCurrentItem(1, true)
    }
}