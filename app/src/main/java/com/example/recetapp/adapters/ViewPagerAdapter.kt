package com.example.recetapp.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.recetapp.fragments.FavoritesFragment
import com.example.recetapp.fragments.PantryFragment
import com.example.recetapp.fragments.SearchRecipesFragment
import com.example.recetapp.fragments.ShoppingListFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4 // Se mantiene en 4 pestañas

    override fun createFragment(position: Int): Fragment {
        // ----- MODIFICADO: Nuevo orden de los fragmentos -----
        return when (position) {
            0 -> PantryFragment()          // Izquierda: Despensa
            1 -> SearchRecipesFragment()    // Centro (Principal): Búsqueda
            2 -> ShoppingListFragment()    // Derecha 1: Lista de Compra
            3 -> FavoritesFragment()       // Derecha 2: Favoritos
            else -> throw IllegalStateException("Posición de fragment inválida: $position")
        }
    }
}