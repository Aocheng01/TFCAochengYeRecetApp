package com.example.recetapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetapp.adapters.RecipeAdapter
import com.example.recetapp.api.RetrofitClient
import com.example.recetapp.data.Hit
import com.example.recetapp.data.Recipe
import com.example.recetapp.data.RecipeResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchRecipesFragment : Fragment() {

    private val TAG = "SearchRecipesFragment"
    private lateinit var auth: FirebaseAuth

    // Vistas UI (IDs de fragment_search_recipes.xml)
    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var editTextSearchQuery: EditText
    private lateinit var buttonSearch: Button
    private lateinit var buttonLogout: Button
    private lateinit var buttonLoadMore: Button

    // Variables para paginación
    private var nextPageUrl: String? = null
    private var isLoading = false
    private var currentQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el layout para este fragment
        return inflater.inflate(R.layout.fragment_search_recipes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        // Inicializar Vistas usando 'view.findViewById'
        // Los IDs son los mismos que tenías en tu activity_main.xml original
        recyclerView = view.findViewById(R.id.recyclerViewRecipes)
        editTextSearchQuery = view.findViewById(R.id.editTextSearchQuery)
        buttonSearch = view.findViewById(R.id.buttonSearch)
        buttonLogout = view.findViewById(R.id.buttonLogout)
        buttonLoadMore = view.findViewById(R.id.buttonLoadMore)

        // Configuración del RecyclerView
        recipeAdapter = RecipeAdapter(mutableListOf()) { clickedRecipe ->
            openRecipeDetail(clickedRecipe)
        }
        // Usa requireContext() para obtener el Context en un Fragment
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = recipeAdapter

        // ----- AQUÍ MOVERÁS LOS LISTENERS Y FUNCIONES DE MainActivity -----
        // Por ejemplo:
        // buttonSearch.setOnClickListener { ... }
        // buttonLoadMore.setOnClickListener { ... }
        // buttonLogout.setOnClickListener { ... }
        // recyclerView.addOnScrollListener( ... )
        // Y las funciones: searchRecipesApi, updateLoadMoreButtonVisibility, hideKeyboard, openRecipeDetail
        // Asegúrate de adaptar el contexto (usar requireContext() o activity)
        // y la forma de obtener el inputMethodManager.
        // ----- FIN DE LA SECCIÓN A MOVER -----

        // Ejemplo de cómo adaptar el listener de logout:
        buttonLogout.setOnClickListener {
            Log.d(TAG, "Botón de logout presionado en Fragment.")
            auth.signOut()
            Toast.makeText(requireContext(), "Sesión cerrada.", Toast.LENGTH_SHORT).show()
            // Para navegar desde un Fragment, usualmente obtienes la Activity
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish() // Cierra la MainActivity contenedora
        }

        // Ejemplo de cómo adaptar el listener de búsqueda:
        buttonSearch.setOnClickListener {
            val query = editTextSearchQuery.text.toString().trim()
            if (query.isNotBlank()) {
                Log.d(TAG, "Botón presionado en Fragment, buscando: $query")
                currentQuery = query
                nextPageUrl = null
                recipeAdapter.submitNewList(emptyList())
                buttonLoadMore.visibility = View.GONE
                searchRecipesApi(query, true) // Esta función la moverás aquí
                hideKeyboard() // Esta función la moverás aquí
            } else {
                Toast.makeText(requireContext(), "Introduce un término de búsqueda.", Toast.LENGTH_SHORT).show()
            }
        }

        buttonLoadMore.setOnClickListener {
            if (!isLoading && nextPageUrl != null) {
                Log.d(TAG, "Botón Cargar Más presionado en Fragment. URL: $nextPageUrl")
                buttonLoadMore.visibility = View.GONE
                searchRecipesApi(nextPageUrl!!, false) // Esta función la moverás aquí
            }
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 || (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition() == recipeAdapter.itemCount - 1) {
                    updateLoadMoreButtonVisibility() // Esta función la moverás aquí
                }
            }
        })
    }

    // ----- COPIA Y PEGA TUS FUNCIONES DE MainActivity AQUÍ Y ADÁPTALAS -----
    // openRecipeDetail, hideKeyboard, updateLoadMoreButtonVisibility, searchRecipesApi
    // Recuerda cambiar 'this' por 'requireContext()' o 'activity' donde sea necesario.
    // Y 'findViewById' por 'view.findViewById' si accedes a vistas dentro de estas funciones.

    private fun openRecipeDetail(recipe: Recipe) {
        Log.d(TAG, "Abriendo detalle para: ${recipe.label}")
        val intent = Intent(activity, RecipeDetailActivity::class.java)
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE, recipe)
        startActivity(intent)
    }

    private fun hideKeyboard() {
        val inputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        // Usa 'view?.windowToken' si la vista raíz del fragment es la que tiene el foco,
        // o 'activity?.currentFocus?.windowToken'
        val currentFocus = activity?.currentFocus
        if (currentFocus != null) {
            inputMethodManager?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        } else {
            inputMethodManager?.hideSoftInputFromWindow(view?.windowToken, 0)
        }
    }

    private fun updateLoadMoreButtonVisibility() {
        if (!isAdded) return // Comprueba si el fragment está añadido a la activity

        if (isLoading) {
            buttonLoadMore.visibility = View.GONE
            return
        }

        if (nextPageUrl != null) {
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
            if (layoutManager != null) {
                val lastVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition()
                val totalItemCount = recipeAdapter.itemCount

                if (totalItemCount > 0 && lastVisibleItemPosition == totalItemCount - 1) {
                    buttonLoadMore.visibility = View.VISIBLE
                } else {
                    buttonLoadMore.visibility = View.GONE
                }
            }
        } else {
            buttonLoadMore.visibility = View.GONE
        }
    }

    private fun searchRecipesApi(queryOrUrl: String, isInitialSearch: Boolean) {
        if (!isAdded) return // Comprueba si el fragment está añadido a la activity
        if (isLoading) return
        isLoading = true
        if (isInitialSearch) {
            buttonLoadMore.visibility = View.GONE
        }
        // TODO: Mostrar ProgressBar (necesitarás añadir un ProgressBar al layout del fragment)

        val apiService = RetrofitClient.instance
        val call: Call<RecipeResponse>

        if (isInitialSearch) {
            call = apiService.searchRecipes(query = queryOrUrl)
        } else {
            call = apiService.getNextPageRecipes(nextPageUrl = queryOrUrl)
        }

        call.enqueue(object : Callback<RecipeResponse> {
            override fun onResponse(call: Call<RecipeResponse>, response: Response<RecipeResponse>) {
                if (!isAdded) return // Comprueba si el fragment sigue añadido
                isLoading = false
                // TODO: Ocultar ProgressBar

                if (response.isSuccessful) {
                    val recipeResponse = response.body()
                    val newHits = recipeResponse?.hits ?: emptyList()

                    if (isInitialSearch) {
                        recipeAdapter.submitNewList(newHits)
                    } else {
                        recipeAdapter.addRecipes(newHits)
                    }
                    Log.d(TAG, (if(isInitialSearch) "Búsqueda inicial" else "Paginación") + ": ${newHits.size} recetas.")

                    nextPageUrl = recipeResponse?.links?.next?.href
                    updateLoadMoreButtonVisibility()

                    if (nextPageUrl == null && !isInitialSearch && newHits.isEmpty() && recipeAdapter.itemCount > 0) {
                        Toast.makeText(requireContext(), "No hay más recetas para cargar.", Toast.LENGTH_SHORT).show()
                    }
                    if (isInitialSearch && newHits.isEmpty()) {
                        Toast.makeText(requireContext(), "No se encontraron recetas para '$currentQuery'.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Error API: ${response.code()} - ${response.message()}")
                    nextPageUrl = null
                    updateLoadMoreButtonVisibility()
                    Toast.makeText(requireContext(), "Error de API: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RecipeResponse>, t: Throwable) {
                if (!isAdded) return // Comprueba si el fragment sigue añadido
                isLoading = false
                // TODO: Ocultar ProgressBar
                Log.e(TAG, "Fallo en la llamada a la API: ${t.message}", t)
                nextPageUrl = null
                updateLoadMoreButtonVisibility()
                Toast.makeText(requireContext(), "Error de red.", Toast.LENGTH_SHORT).show()
            }
        })
    }
    // ---------------------------------------------------------------------
}