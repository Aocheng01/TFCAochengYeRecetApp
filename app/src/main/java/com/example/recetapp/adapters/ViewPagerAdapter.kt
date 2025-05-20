package com.example.recetapp.adapters // O tu paquete

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.recetapp.PantryFragment
import com.example.recetapp.SearchRecipesFragment
import com.example.recetapp.fragments.ShoppingListFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3 // Tres pestañas

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PantryFragment()       // Izquierda: Despensa
            1 -> SearchRecipesFragment() // Centro: Búsqueda (principal)
            2 -> ShoppingListFragment() // Derecha: Lista de Compra
            else -> throw IllegalStateException("Posición de fragment inválida: $position")
        }
    }
}