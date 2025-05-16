package com.example.recetapp

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetapp.adapters.RecipeAdapter // Asegúrate que la ruta al adapter sea correcta
import com.example.recetapp.api.RetrofitClient
import com.example.recetapp.data.Hit
import com.example.recetapp.data.RecipeResponse
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.view.View
import android.widget.Toast
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.example.recetapp.data.Recipe // Importa tu clase Recipe (debe ser Parcelable)

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var auth: FirebaseAuth

    // Vistas UI
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Asegúrate que R.layout.activity_main es tu layout correcto

        // Inicializar Firebase Auth
        auth = Firebase.auth

        // Inicializar Vistas
        recyclerView = findViewById(R.id.recyclerViewRecipes)
        editTextSearchQuery = findViewById(R.id.editTextSearchQuery)
        buttonSearch = findViewById(R.id.buttonSearch)
        buttonLogout = findViewById(R.id.buttonLogout)
        buttonLoadMore = findViewById(R.id.buttonLoadMore)

        // Configuración del RecyclerView y su Adaptador (con listener de clic)
        recipeAdapter = RecipeAdapter(mutableListOf()) { clickedRecipe ->
            // Esta lambda se ejecutará cuando un ítem sea pulsado
            openRecipeDetail(clickedRecipe)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = recipeAdapter

        // Listener Botón de Búsqueda
        buttonSearch.setOnClickListener {
            val query = editTextSearchQuery.text.toString().trim()
            if (query.isNotBlank()) {
                Log.d(TAG, "Botón presionado, buscando: $query")
                currentQuery = query
                nextPageUrl = null // Resetea para nueva búsqueda
                recipeAdapter.submitNewList(emptyList()) // Limpia resultados anteriores
                buttonLoadMore.visibility = View.GONE // Oculta el botón al iniciar nueva búsqueda
                searchRecipesApi(query, true) // Indica que es una búsqueda inicial
                hideKeyboard()
            } else {
                Log.d(TAG, "Intento de búsqueda con campo vacío.")
                Toast.makeText(this, "Por favor, introduce un término de búsqueda.", Toast.LENGTH_SHORT).show()
            }
        }

        // Listener Botón Cargar Más
        buttonLoadMore.setOnClickListener {
            if (!isLoading && nextPageUrl != null) {
                Log.d(TAG, "Botón Cargar Más presionado. URL: $nextPageUrl")
                buttonLoadMore.visibility = View.GONE // Oculta para evitar doble clic mientras carga
                searchRecipesApi(nextPageUrl!!, false) // Indica que es paginación
            }
        }

        // Listener Botón Logout
        buttonLogout.setOnClickListener {
            Log.d(TAG, "Botón de logout presionado.")
            auth.signOut()
            // Si usabas GoogleSignInClient para el logout de Google también, esa lógica iría aquí
            // Ejemplo: GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
            Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }

        // OnScrollListener para el RecyclerView
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // Solo actualiza visibilidad si el scroll es hacia abajo o si ya está al final
                // para optimizar un poco y evitar llamadas innecesarias al subir.
                if (dy > 0 || (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition() == recipeAdapter.itemCount - 1) {
                    updateLoadMoreButtonVisibility()
                }
            }
        })

        // Verificar si el usuario está logueado al iniciar esta actividad
        if (auth.currentUser == null && !isFinishing) {
            Log.w(TAG, "Usuario no logueado en MainActivity, volviendo a Login.")
            navigateToLogin()
        }
    } // Fin de onCreate

    private fun openRecipeDetail(recipe: Recipe) {
        Log.d(TAG, "Abriendo detalle para: ${recipe.label}")
        val intent = Intent(this, RecipeDetailActivity::class.java)
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE, recipe) // Pasa el objeto Recipe Parcelable
        startActivity(intent)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusedView = this.currentFocus
        if (currentFocusedView != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocusedView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    private fun updateLoadMoreButtonVisibility() {
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
                    // Log.d(TAG, "Botón Cargar Más: VISIBLE (al final y hay más páginas)")
                } else {
                    buttonLoadMore.visibility = View.GONE
                    // Log.d(TAG, "Botón Cargar Más: OCULTO (no al final o lista vacía aún)")
                }
            }
        } else {
            buttonLoadMore.visibility = View.GONE
            // Log.d(TAG, "Botón Cargar Más: OCULTO (no hay más páginas url)")
        }
    }

    private fun searchRecipesApi(queryOrUrl: String, isInitialSearch: Boolean) {
        if (isLoading) return
        isLoading = true
        if (isInitialSearch) { // Para una nueva búsqueda, siempre ocultar el botón de cargar más inicialmente
            buttonLoadMore.visibility = View.GONE
        }
        // TODO: Mostrar ProgressBar aquí (ej. progressBar.visibility = View.VISIBLE)

        val apiService = RetrofitClient.instance
        val call: Call<RecipeResponse>

        if (isInitialSearch) {
            Log.d(TAG, "Iniciando búsqueda INICIAL para: $queryOrUrl")
            call = apiService.searchRecipes(query = queryOrUrl)
        } else {
            Log.d(TAG, "Cargando SIGUIENTE PÁGINA desde: $queryOrUrl")
            call = apiService.getNextPageRecipes(nextPageUrl = queryOrUrl)
        }

        call.enqueue(object : Callback<RecipeResponse> {
            override fun onResponse(call: Call<RecipeResponse>, response: Response<RecipeResponse>) {
                isLoading = false
                // TODO: Ocultar ProgressBar aquí (ej. progressBar.visibility = View.GONE)

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
                    updateLoadMoreButtonVisibility() // Actualiza la visibilidad basada en nextPageUrl y el scroll

                    // Toasts informativos
                    if (nextPageUrl == null && !isInitialSearch && newHits.isEmpty() && recipeAdapter.itemCount > 0) {
                        Toast.makeText(this@MainActivity, "No hay más recetas para cargar.", Toast.LENGTH_SHORT).show()
                    }
                    if (isInitialSearch && newHits.isEmpty()) {
                        Toast.makeText(this@MainActivity, "No se encontraron recetas para '$currentQuery'.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Error API: ${response.code()} - ${response.message()}")
                    nextPageUrl = null // No hay siguiente página si hay error
                    updateLoadMoreButtonVisibility() // Ocultará el botón
                    Toast.makeText(this@MainActivity, "Error de API: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RecipeResponse>, t: Throwable) {
                isLoading = false
                // TODO: Ocultar ProgressBar aquí
                Log.e(TAG, "Fallo en la llamada a la API: ${t.message}", t)
                nextPageUrl = null // No hay siguiente página si hay error
                updateLoadMoreButtonVisibility() // Ocultará el botón
                Toast.makeText(this@MainActivity, "Error de red.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}