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
import androidx.cardview.widget.CardView // Asegúrate que esté importado si usas CardView
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
import com.example.recetapp.data.RecipeResponse
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
    private lateinit var cardViewWelcome: CardView // ID del CardView que contiene la imagen y el texto de bienvenida

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
        cardViewWelcome = view.findViewById(R.id.cardViewWelcome)

        recipeAdapter = RecipeAdapter(mutableListOf()) { openRecipeDetail(it) }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = recipeAdapter

        updateUIForNoSearchState()

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
                performSearch(query)
                searchViewModel.onSearchQueryNavigated()
            } else if (query == null) {
                if (editTextSearchQuery.text.isBlank() && recipeAdapter.itemCount == 0 && !isLoading) {
                    updateUIForNoSearchState()
                }
            }
        }
    }

    private fun updateUIForNoSearchState() {
        if (!isAdded) return
        cardViewWelcome.visibility = View.VISIBLE
        textViewAlternativeSearchInfo.visibility = View.GONE
        recyclerView.visibility = View.GONE
        buttonLoadMore.visibility = View.GONE
        if (::recipeAdapter.isInitialized) {
            recipeAdapter.submitNewList(emptyList())
        }
    }

    private fun performSearchFromInput() {
        val userInput = editTextSearchQuery.text.toString().trim()
        if (userInput.isBlank()) {
            Toast.makeText(requireContext(), "Introduce al menos un término de búsqueda.", Toast.LENGTH_SHORT).show()
            updateUIForNoSearchState()
            return
        }
        performSearch(userInput)
    }

    private fun performSearch(userInput: String) {
        if (!isAdded) return
        Log.d(TAG, "Procesando input del usuario para búsqueda: '$userInput'")
        hideKeyboard()

        cardViewWelcome.visibility = View.GONE
        recyclerView.visibility = View.GONE
        textViewAlternativeSearchInfo.visibility = View.VISIBLE
        textViewAlternativeSearchInfo.text = "Buscando recetas para \"$userInput\"..."
        buttonLoadMore.visibility = View.GONE
        if (::recipeAdapter.isInitialized) {
            recipeAdapter.submitNewList(emptyList())
        }

        // ----- MODIFICADO: Parsear la entrada por comas para búsquedas individuales -----
        val searchTerms = userInput.split(',')
            .map { it.trim().lowercase(java.util.Locale.getDefault()) }
            .filter { it.isNotBlank() }
            .distinct()
        // -------------------------------------------------------------------------

        if (searchTerms.isEmpty()) {
            Toast.makeText(requireContext(), "No se ingresaron términos de búsqueda válidos.", Toast.LENGTH_SHORT).show()
            textViewAlternativeSearchInfo.text = "Por favor, introduce términos de búsqueda válidos."
            recyclerView.visibility = View.GONE // Asegurarse que el recycler está oculto
            return
        }

        Log.d(TAG, "Términos de búsqueda individuales: $searchTerms")
        startIndividualTermSearches(searchTerms, userInput) // Pasar userInput para el mensaje final
    }

    // ----- MODIFICADO Y RESTAURADO: Búsqueda individual por término y combinación de resultados (Lógica OR) -----
    private fun startIndividualTermSearches(searchTerms: List<String>, originalUserInput: String) {
        if (!isAdded) return
        isLoading = true
        // El mensaje "Buscando..." ya se ha puesto en performSearch

        val accumulatedHits = mutableListOf<Hit>()

        viewLifecycleOwner.lifecycleScope.launch {
            val deferredResults = searchTerms.map { term ->
                async(Dispatchers.IO) {
                    try {
                        Log.d(TAG, "Búsqueda individual para: '$term' en hilo: ${Thread.currentThread().name}")
                        RetrofitClient.instance.searchRecipes(query = term).execute()
                    } catch (e: IOException) {
                        Log.e(TAG, "IOException en búsqueda para '$term': ${e.message}", e)
                        null
                    } catch (e: HttpException) {
                        Log.e(TAG, "HttpException en búsqueda para '$term': ${e.code()} - ${e.message()}", e)
                        null
                    } catch (e: Exception) {
                        Log.e(TAG, "Error desconocido en búsqueda para '$term': ${e.message}", e)
                        null
                    }
                }
            }

            val responses = deferredResults.awaitAll()

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

            cardViewWelcome.visibility = View.GONE // Asegurar que sigue oculto

            if (numberOfResults > 0) {
                Log.d(TAG, "Mostrando $numberOfResults resultados únicos combinados para: \"$originalUserInput\"")
                recipeAdapter.submitNewList(distinctResults)
                textViewAlternativeSearchInfo.text = "Se encontraron $numberOfResults recetas para \"$originalUserInput\""
                recyclerView.visibility = View.VISIBLE
            } else {
                Log.d(TAG, "No se encontraron resultados para: \"$originalUserInput\"")
                textViewAlternativeSearchInfo.text = "No se encontraron recetas para \"$originalUserInput\"."
                recyclerView.visibility = View.GONE
                if (::recipeAdapter.isInitialized) recipeAdapter.submitNewList(emptyList())
            }
            textViewAlternativeSearchInfo.visibility = View.VISIBLE
            updateLoadMoreButtonVisibility()
        }
    }
    // ------------------------------------------------------------------------------------------------

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