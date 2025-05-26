package com.example.recetapp.fragments // O tu paquete

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.activityViewModels // Importa para by activityViewModels()
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetapp.LoginActivity
import com.example.recetapp.R
import com.example.recetapp.RecipeDetailActivity
import com.example.recetapp.adapters.RecipeAdapter
import com.example.recetapp.api.RetrofitClient
import com.example.recetapp.data.Hit
import com.example.recetapp.data.Recipe
import com.example.recetapp.data.RecipeResponse
import com.example.recetapp.viewmodels.SearchViewModel // Importa tu SearchViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
// Quita las importaciones de Retrofit si no las usas directamente aquí,
// ya que la llamada se hace en la función searchRecipesApi
// import retrofit2.Call
// import retrofit2.Callback
// import retrofit2.Response
// Asegúrate de que las siguientes sí estén si las usas en searchRecipesApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SearchRecipesFragment : Fragment() {

    private val TAG = "SearchRecipesFragment"
    private lateinit var auth: FirebaseAuth

    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var editTextSearchQuery: EditText
    private lateinit var buttonSearch: Button
    private lateinit var buttonLogout: Button // Asumiendo que este botón está en fragment_search_recipes.xml
    private lateinit var buttonLoadMore: Button

    private var nextPageUrl: String? = null
    private var isLoading = false
    private var currentQuery: String = ""

    // Obtiene la instancia del SharedViewModel (scoped to Activity)
    private val searchViewModel: SearchViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_recipes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        // IDs del layout fragment_search_recipes.xml
        recyclerView = view.findViewById(R.id.recyclerViewRecipes)
        editTextSearchQuery = view.findViewById(R.id.editTextSearchQuery)
        buttonSearch = view.findViewById(R.id.buttonSearch)
        buttonLogout = view.findViewById(R.id.buttonLogout) // Asegúrate que este ID existe en el fragment layout
        buttonLoadMore = view.findViewById(R.id.buttonLoadMore)

        recipeAdapter = RecipeAdapter(mutableListOf()) { clickedRecipe ->
            openRecipeDetail(clickedRecipe)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = recipeAdapter

        buttonSearch.setOnClickListener {
            performSearchFromInput()
        }

        editTextSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearchFromInput()
                true
            } else {
                false
            }
        }

        buttonLoadMore.setOnClickListener {
            if (!isLoading && nextPageUrl != null) {
                buttonLoadMore.visibility = View.GONE
                searchRecipesApi(nextPageUrl!!, false)
            }
        }

        buttonLogout.setOnClickListener {
            Log.d(TAG, "Botón de logout presionado en SearchRecipesFragment.")
            auth.signOut()
            // (Opcional) Google Sign Out
            // val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            // GoogleSignIn.getClient(requireActivity(), gso).signOut()

            Toast.makeText(requireContext(), "Sesión cerrada.", Toast.LENGTH_SHORT).show()
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish() // Cierra MainActivity
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 || (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition() == recipeAdapter.itemCount - 1) {
                    updateLoadMoreButtonVisibility()
                }
            }
        })

        // --- Observar el LiveData del ViewModel ---
        searchViewModel.navigateToSearchQuery.observe(viewLifecycleOwner) { query ->
            // Solo reacciona si la query es nueva y no nula/vacía
            if (query != null && query.isNotBlank() && query != currentQuery) {
                Log.d(TAG, "SearchRecipesFragment: Recibida query del ViewModel: $query")
                editTextSearchQuery.setText(query)
                performSearch(query) // Realiza la búsqueda
                searchViewModel.onSearchQueryNavigated() // Limpia la query para evitar re-búsquedas
            } else if (query == null) {
                // Si la query es null (después de onSearchQueryNavigated), no hacemos nada
                // o podríamos limpiar el campo de búsqueda si quisiéramos.
            }
        }
    }

    private fun performSearchFromInput() {
        val query = editTextSearchQuery.text.toString().trim()
        performSearch(query)
    }

    private fun performSearch(query: String) {
        if (query.isNotBlank()) {
            Log.d(TAG, "Iniciando búsqueda desde Fragment: $query")
            currentQuery = query // Actualiza la query actual
            nextPageUrl = null
            recipeAdapter.submitNewList(emptyList())
            buttonLoadMore.visibility = View.GONE
            searchRecipesApi(query, true)
            hideKeyboard()
        } else {
            // No mostrar Toast si la query viene del ViewModel y es vacía después de ser consumida
            if (editTextSearchQuery.hasFocus()) { // Solo muestra si el usuario intentó buscar con campo vacío
                Toast.makeText(requireContext(), "Introduce un término de búsqueda.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openRecipeDetail(recipe: Recipe) {
        Log.d(TAG, "Abriendo detalle para: ${recipe.label}")
        val intent = Intent(activity, RecipeDetailActivity::class.java)
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE, recipe)
        startActivity(intent)
    }

    private fun hideKeyboard() {
        val inputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val currentFocus = activity?.currentFocus
        if (currentFocus != null) {
            inputMethodManager?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        } else {
            inputMethodManager?.hideSoftInputFromWindow(view?.windowToken, 0)
        }
    }

    private fun updateLoadMoreButtonVisibility() {
        if (!isAdded) return
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
        if (!isAdded) return
        if (isLoading) return
        isLoading = true
        if (isInitialSearch) {
            buttonLoadMore.visibility = View.GONE
        }
        // TODO: Mostrar ProgressBar

        val apiService = RetrofitClient.instance
        val call: Call<RecipeResponse> = if (isInitialSearch) {
            apiService.searchRecipes(query = queryOrUrl)
        } else {
            apiService.getNextPageRecipes(nextPageUrl = queryOrUrl)
        }

        call.enqueue(object : Callback<RecipeResponse> {
            override fun onResponse(call: Call<RecipeResponse>, response: Response<RecipeResponse>) {
                if (!isAdded) return
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
                if (!isAdded) return
                isLoading = false
                // TODO: Ocultar ProgressBar
                Log.e(TAG, "Fallo en la llamada a la API: ${t.message}", t)
                nextPageUrl = null
                updateLoadMoreButtonVisibility()
                Toast.makeText(requireContext(), "Error de red.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
