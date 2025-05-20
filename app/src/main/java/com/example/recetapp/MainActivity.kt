package com.example.recetapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
// Quita las importaciones que ya no se usan aquí (RecyclerView, EditText, etc.)
// pero asegúrate de que estén en SearchRecipesFragment.kt
import androidx.viewpager2.widget.ViewPager2
import com.example.recetapp.adapters.ViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var auth: FirebaseAuth

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    // El botón de logout ahora estará dentro de SearchRecipesFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Usa el nuevo activity_main.xml

        auth = Firebase.auth

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Despensa"
                1 -> "Buscar" // Título para la pestaña central
                2 -> "Lista Compra"
                else -> null
            }
            // Aquí también podrías poner iconos a las pestañas: tab.icon = ...
        }.attach()

        // Establecer la pestaña central (Búsqueda de Recetas) como la inicial
        viewPager.setCurrentItem(1, false) // Posición 1 (0-indexado), sin animación

        // Verificar si el usuario está logueado
        if (auth.currentUser == null && !isFinishing) {
            Log.w(TAG, "Usuario no logueado en MainActivity, volviendo a Login.")
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // La lógica de búsqueda, paginación, etc., ahora está en SearchRecipesFragment.
    // La función de logout también se movió a SearchRecipesFragment.
}