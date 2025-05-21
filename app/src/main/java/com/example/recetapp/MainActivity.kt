package com.example.recetapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast // Añadido para el Toast de ejemplo
import androidx.activity.viewModels // Importante para 'by viewModels()'
import androidx.viewpager2.widget.ViewPager2
import com.example.recetapp.adapters.ViewPagerAdapter
import com.example.recetapp.fragments.PantryFragmentListener // ---- CORREGIDO: Importar la interfaz directamente ----
import com.example.recetapp.viewmodels.SearchViewModel // Importa tu SearchViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

// ---- CORREGIDO: MainActivity implementa la interfaz directamente ----
class MainActivity : AppCompatActivity(), PantryFragmentListener {

    private val TAG = "MainActivity"
    private lateinit var auth: FirebaseAuth

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    // ---- Obtener instancia del SharedViewModel ----
    private val searchViewModel: SearchViewModel by viewModels()
    // ----------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

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
            // Solo establece el item actual si el usuario está logueado,
            // para evitar problemas si navigateToLogin() se llama antes.
            viewPager.setCurrentItem(1, false)
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // ---- Implementar el método de la interfaz ----
    override fun onSearchRequestedFromPantry(query: String) {
        Log.d(TAG, "MainActivity: Búsqueda solicitada desde despensa con query: $query")

        // 1. Actualiza el ViewModel con la nueva consulta
        searchViewModel.setSearchQuery(query)

        // 2. Cambia a la pestaña de búsqueda (posición 1)
        viewPager.setCurrentItem(1, true) // true para una animación suave

        // (Opcional) Muestra un Toast indicando la acción
        // Toast.makeText(this, "Buscando recetas para: $query", Toast.LENGTH_SHORT).show()
    }
    // ----------------------------------------------------

    // La lógica de búsqueda, paginación, y logout ahora residen en SearchRecipesFragment.
}
