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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetapp.R
import com.example.recetapp.RecipeDetailActivity
import com.example.recetapp.adapters.RecipeAdapter
import com.example.recetapp.api.RetrofitClient
import com.example.recetapp.data.Hit
import com.example.recetapp.data.Recipe
import com.example.recetapp.viewmodels.SearchViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
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
    private lateinit var buttonLoadMore: Button
    private lateinit var textViewAlternativeSearchInfo: TextView
    private lateinit var imageViewDecorativeSearch: ImageView
    private lateinit var textViewWelcome: TextView // NUEVA PROPIEDAD PARA EL TEXTO DE BIENVENIDA

    private var isLoading = false
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
        buttonLoadMore = view.findViewById(R.id.buttonLoadMore)
        textViewAlternativeSearchInfo = view.findViewById(R.id.textViewAlternativeSearchInfo)
        imageViewDecorativeSearch = view.findViewById(R.id.imageViewDecorativeSearch)
        textViewWelcome = view.findViewById(R.id.textViewWelcome) // INICIALIZAR TEXTO DE BIENVENIDA

        recipeAdapter = RecipeAdapter(mutableListOf()) { openRecipeDetail(it) }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = recipeAdapter

        updateUIForNoSearchState() // Establecer estado inicial

        buttonSearch.setOnClickListener {
            performSearchFromInput()
        }
        editTextSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearchFromInput()
                true
            } else { false }
        }

        searchViewModel.navigateToSearchQuery.observe(viewLifecycleOwner) { query ->
            if (query != null && query.isNotBlank()) {
                Log.d(TAG, "SearchRecipesFragment: Recibida query del ViewModel: $query")
                editTextSearchQuery.setText(query)
                // Asegurarse de que la UI se actualiza para el estado de búsqueda
                imageViewDecorativeSearch.visibility = View.GONE
                textViewWelcome.visibility = View.GONE
                recyclerView.visibility = View.GONE // Ocultar hasta que haya resultados
                textViewAlternativeSearchInfo.visibility = View.VISIBLE
                performSearch(query)
                searchViewModel.onSearchQueryNavigated()
            } else if (query == null) { // Si la query es null (ej. después de navegar y volver)
                // Solo restaurar estado inicial si no hay texto en el EditText
                if (editTextSearchQuery.text.isBlank() && recipeAdapter.itemCount == 0) {
                    updateUIForNoSearchState()
                }
            }
        }
    }

    private fun updateUIForNoSearchState() {
        imageViewDecorativeSearch.visibility = View.VISIBLE
        textViewWelcome.visibility = View.VISIBLE
        textViewAlternativeSearchInfo.visibility = View.GONE
        recyclerView.visibility = View.GONE
        buttonLoadMore.visibility = View.GONE
        if (::recipeAdapter.isInitialized) {
            recipeAdapter.submitNewList(emptyList())
        }
    }

    private fun performSearchFromInput() {
        val query = editTextSearchQuery.text.toString().trim()
        if (query.isBlank()) {
            Toast.makeText(requireContext(), "Introduce al menos un ingrediente.", Toast.LENGTH_SHORT).show()
            // Decidir si volver al estado inicial o mantener la última búsqueda/mensaje
            // Por ahora, no cambiamos el estado aquí si la query es vacía tras un click,
            // solo mostramos el Toast. El estado inicial se maneja en onViewCreated y
            // potencialmente si el ViewModel se resetea.
            return
        }
        performSearch(query)
    }

    private fun performSearch(query: String) {
        Log.d(TAG, "Iniciando búsqueda OR para: '$query'")
        hideKeyboard()

        // Actualizar UI para estado de búsqueda
        imageViewDecorativeSearch.visibility = View.GONE
        textViewWelcome.visibility = View.GONE
        recyclerView.visibility = View.GONE // Ocultar mientras se carga
        textViewAlternativeSearchInfo.visibility = View.VISIBLE
        textViewAlternativeSearchInfo.text = "Buscando recetas para \"$query\"..." // Mensaje de búsqueda
        buttonLoadMore.visibility = View.GONE // Ocultar el botón de cargar más
        if (::recipeAdapter.isInitialized) {
            recipeAdapter.submitNewList(emptyList()) // Limpiar resultados anteriores
        }

        val ingredients = query.split(" ", ",")
            .map { it.trim().lowercase(java.util.Locale.getDefault()) }
            .filter { it.isNotBlank() }
            .distinct()

        if (ingredients.isEmpty()) {
            Toast.makeText(requireContext(), "No se ingresaron ingredientes válidos.", Toast.LENGTH_SHORT).show()
            textViewAlternativeSearchInfo.text = "Por favor, introduce ingredientes válidos."
            // Aquí no volvemos a updateUIForNoSearchState() porque se intentó una búsqueda.
            return
        }
        startIndividualIngredientSearches(ingredients, query)
    }

    private fun startIndividualIngredientSearches(ingredients: List<String>, originalQuery: String) {
        if (!isAdded) return
        isLoading = true
        // El mensaje "Buscando..." ya se estableció en performSearch

        val accumulatedHits = mutableListOf<Hit>()
        viewLifecycleOwner.lifecycleScope.launch {
            // ... (lógica de deferredResults.awaitAll() sin cambios)
            val responses = ingredients.map { ingredient ->
                async(Dispatchers.IO) {
                    try {
                        Log.d(TAG, "Búsqueda individual para: '$ingredient' en hilo: ${Thread.currentThread().name}")
                        RetrofitClient.instance.searchRecipes(query = ingredient).execute()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error en búsqueda individual para '$ingredient': ${e.message}", e)
                        null
                    }
                }
            }.awaitAll()


            if (!isAdded) {
                isLoading = false
                return@launch
            }
            isLoading = false

            responses.forEach { response ->
                if (response != null && response.isSuccessful) {
                    response.body()?.hits?.let { hits ->
                        accumulatedHits.addAll(hits)
                    }
                } else if (response != null) {
                    Log.e(TAG, "Error en respuesta de búsqueda individual: ${response.code()} - ${response.message()}")
                }
            }

            val distinctResults = accumulatedHits.distinctBy { it.recipe?.uri }
            val numberOfResults = distinctResults.size

            imageViewDecorativeSearch.visibility = View.GONE // Asegurar que la imagen decorativa está oculta
            textViewWelcome.visibility = View.GONE       // Asegurar que el texto de bienvenida está oculto

            if (numberOfResults > 0) {
                Log.d(TAG, "Mostrando $numberOfResults resultados únicos combinados.")
                recipeAdapter.submitNewList(distinctResults)
                val searchedIngredientsText = if (ingredients.size > 1) {
                    "para: ${ingredients.joinToString(", ")}"
                } else if (ingredients.isNotEmpty()){
                    "para: \"${ingredients.first()}\""
                } else {
                    ""
                }
                textViewAlternativeSearchInfo.text = "Se encontraron $numberOfResults recetas $searchedIngredientsText"
                recyclerView.visibility = View.VISIBLE
                textViewAlternativeSearchInfo.visibility = View.VISIBLE
            } else {
                Log.d(TAG, "No se encontraron resultados en ninguna búsqueda individual.")
                textViewAlternativeSearchInfo.text = "No se encontraron recetas para \"$originalQuery\"."
                recyclerView.visibility = View.GONE
                recipeAdapter.submitNewList(emptyList<Hit>())
                textViewAlternativeSearchInfo.visibility = View.VISIBLE
            }
            updateLoadMoreButtonVisibility()
        }
    }

    private fun openRecipeDetail(recipe: Recipe) {
        if (!isAdded) return
        Log.d(TAG, "Abriendo detalle para: ${recipe.label}")
        val intent = Intent(activity, RecipeDetailActivity::class.java)
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE, recipe)
        startActivity(intent)
        activity?.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
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
        if (::buttonLoadMore.isInitialized) {
            buttonLoadMore.isVisible = false
        }
    }
}