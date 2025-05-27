package com.example.recetapp.fragments

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
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
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
import com.example.recetapp.viewmodels.SearchViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class SearchRecipesFragment : Fragment() {

    private val TAG = "SearchRecipesFragment"
    private lateinit var auth: FirebaseAuth

    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var editTextSearchQuery: EditText
    private lateinit var buttonSearch: Button
    private lateinit var buttonLogout: Button
    private lateinit var buttonLoadMore: Button
    private lateinit var textViewAlternativeSearchInfo: TextView

    private var isLoading = false
    // nextPageUrl no se usa activamente en la lógica OR pura para paginación combinada
    // private var nextPageUrl: String? = null

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

        recyclerView = view.findViewById(R.id.recyclerViewRecipes)
        editTextSearchQuery = view.findViewById(R.id.editTextSearchQuery)
        buttonSearch = view.findViewById(R.id.buttonSearch)
        buttonLogout = view.findViewById(R.id.buttonLogout)
        buttonLoadMore = view.findViewById(R.id.buttonLoadMore)
        textViewAlternativeSearchInfo = view.findViewById(R.id.textViewAlternativeSearchInfo)

        recipeAdapter = RecipeAdapter(mutableListOf()) { openRecipeDetail(it) }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = recipeAdapter
        buttonLoadMore.visibility = View.GONE // Ocultar, no hay paginación para OR

        buttonSearch.setOnClickListener {
            textViewAlternativeSearchInfo.visibility = View.GONE
            performSearchFromInput()
        }
        editTextSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                textViewAlternativeSearchInfo.visibility = View.GONE
                performSearchFromInput()
                true
            } else { false }
        }

        buttonLogout.setOnClickListener {
            Log.d(TAG, "Botón de logout presionado en SearchRecipesFragment.")
            auth.signOut()
            Toast.makeText(requireContext(), "Sesión cerrada.", Toast.LENGTH_SHORT).show()
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        // El OnScrollListener para cargar más no es relevante para la lista OR combinada
        // recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() { ... })

        searchViewModel.navigateToSearchQuery.observe(viewLifecycleOwner) { query ->
            if (query != null && query.isNotBlank()) {
                Log.d(TAG, "SearchRecipesFragment: Recibida query del ViewModel: $query")
                editTextSearchQuery.setText(query)
                textViewAlternativeSearchInfo.visibility = View.GONE
                performSearch(query)
                searchViewModel.onSearchQueryNavigated()
            }
        }
    }

    private fun performSearchFromInput() {
        val query = editTextSearchQuery.text.toString().trim()
        performSearch(query)
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            if (editTextSearchQuery.hasFocus()) {
                Toast.makeText(requireContext(), "Introduce al menos un ingrediente.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        Log.d(TAG, "Iniciando búsqueda OR para: '$query'")
        hideKeyboard()
        recipeAdapter.submitNewList(emptyList())
        textViewAlternativeSearchInfo.visibility = View.GONE
        buttonLoadMore.visibility = View.GONE

        val ingredients = query.split(" ", ",")
            .map { it.trim().lowercase(java.util.Locale.getDefault()) }
            .filter { it.isNotBlank() }
            .distinct()

        if (ingredients.isEmpty()) {
            Toast.makeText(requireContext(), "No se ingresaron ingredientes válidos.", Toast.LENGTH_SHORT).show()
            return
        }

        startIndividualIngredientSearches(ingredients)
    }

    private fun startIndividualIngredientSearches(ingredients: List<String>) {
        if (!isAdded) return
        isLoading = true
        textViewAlternativeSearchInfo.text = "Buscando recetas para: ${ingredients.joinToString(", ")}..."
        textViewAlternativeSearchInfo.visibility = View.VISIBLE
        // TODO: Mostrar ProgressBar general

        val accumulatedHits = mutableListOf<Hit>()

        viewLifecycleOwner.lifecycleScope.launch {
            val deferredResults = ingredients.map { ingredient ->
                async(Dispatchers.IO) {
                    try {
                        Log.d(TAG, "Búsqueda individual para: '$ingredient' en hilo: ${Thread.currentThread().name}")
                        RetrofitClient.instance.searchRecipes(query = ingredient).execute()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error en búsqueda individual para '$ingredient': ${e.message}", e)
                        null
                    }
                }
            }

            deferredResults.awaitAll().forEach { response ->
                if (response != null && response.isSuccessful) {
                    response.body()?.hits?.let { hits ->
                        Log.d(TAG, "Búsqueda individual encontró ${hits.size} recetas.")
                        accumulatedHits.addAll(hits)
                    }
                } else if (response != null) {
                    Log.e(TAG, "Error en respuesta de búsqueda individual: ${response.code()} - ${response.message()}")
                }
            }

            if (!isAdded) { // Comprobar de nuevo después de las operaciones asíncronas
                isLoading = false
                return@launch
            }

            isLoading = false
            // TODO: Ocultar ProgressBar general

            val distinctResults = accumulatedHits.distinctBy { it.recipe?.uri }
            val numberOfResults = distinctResults.size

            if (numberOfResults > 0) {
                Log.d(TAG, "Mostrando $numberOfResults resultados únicos combinados.")
                recipeAdapter.submitNewList(distinctResults)
                // ---- CAMBIO AQUÍ para mostrar el conteo ----
                val searchedIngredientsText = if (ingredients.size > 1) {
                    "para: ${ingredients.joinToString(", ")}"
                } else if (ingredients.isNotEmpty()){
                    "para: '${ingredients.first()}'"
                } else {
                    ""
                }
                textViewAlternativeSearchInfo.text = "Se encontraron $numberOfResults recetas $searchedIngredientsText"
                // -------------------------------------------
            } else {
                Log.d(TAG, "No se encontraron resultados en ninguna búsqueda individual.")
                textViewAlternativeSearchInfo.text = "No se encontraron recetas para los ingredientes proporcionados."
                recipeAdapter.submitNewList(emptyList<Hit>())
            }
            textViewAlternativeSearchInfo.visibility = View.VISIBLE
            // No hay paginación para esta lista combinada, así que nextPageUrl no se establece
            // y updateLoadMoreButtonVisibility() (si se llamara) ocultaría el botón.
            updateLoadMoreButtonVisibility() // Asegura que el botón de cargar más esté oculto
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
        // Con la lógica OR actual, no hay paginación para la lista combinada,
        // así que el botón "Cargar Más" siempre estará oculto.
        if (::buttonLoadMore.isInitialized) { // Añadir chequeo de inicialización
            buttonLoadMore.isVisible = false
        }
    }
}
