package com.example.recetapp.viewmodels // O tu paquete com.example.recetapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SearchViewModel : ViewModel() {

    // LiveData para la consulta de búsqueda que se pasará al SearchRecipesFragment
    private val _navigateToSearchQuery = MutableLiveData<String?>()
    val navigateToSearchQuery: LiveData<String?> = _navigateToSearchQuery

    /**
     * Establece la consulta de búsqueda para que SearchRecipesFragment la observe.
     */
    fun setSearchQuery(query: String) {
        _navigateToSearchQuery.value = query
    }

    /**
     * Limpia la consulta después de que ha sido consumida por el SearchRecipesFragment.
     * Esto evita que la búsqueda se active de nuevo si el fragmento se recrea (ej. por rotación)
     * sin una nueva intención de búsqueda.
     */
    fun onSearchQueryNavigated() {
        _navigateToSearchQuery.value = null
    }
}
