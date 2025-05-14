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
import com.example.recetapp.adapters.RecipeAdapter
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

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var auth: FirebaseAuth

    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var editTextSearchQuery: EditText
    private lateinit var buttonSearch: Button
    private lateinit var buttonLogout: Button
    private lateinit var buttonLoadMore: Button

    private var nextPageUrl: String? = null
    private var isLoading = false
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth

        recyclerView = findViewById(R.id.recyclerViewRecipes)
        editTextSearchQuery = findViewById(R.id.editTextSearchQuery)
        buttonSearch = findViewById(R.id.buttonSearch)
        buttonLogout = findViewById(R.id.buttonLogout)
        buttonLoadMore = findViewById(R.id.buttonLoadMore)

        recipeAdapter = RecipeAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = recipeAdapter

        buttonSearch.setOnClickListener {
            val query = editTextSearchQuery.text.toString().trim()
            if (query.isNotBlank()) {
                Log.d(TAG, "Botón presionado, buscando: $query")
                currentQuery = query
                nextPageUrl = null
                recipeAdapter.submitNewList(emptyList())
                buttonLoadMore.visibility = View.GONE // Oculta al iniciar nueva búsqueda
                searchRecipesApi(query, true)
                hideKeyboard()
            } else {
                Log.d(TAG, "Intento de búsqueda con campo vacío.")
            }
        }

        buttonLoadMore.setOnClickListener {
            if (!isLoading && nextPageUrl != null) {
                Log.d(TAG, "Botón Cargar Más presionado. URL: $nextPageUrl")
                buttonLoadMore.visibility = View.GONE // Oculta para evitar doble clic
                searchRecipesApi(nextPageUrl!!, false)
            }
        }

        buttonLogout.setOnClickListener {
            Log.d(TAG, "Botón de logout presionado.")
            auth.signOut()
            Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 || (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition() == recipeAdapter.itemCount - 1) {
                    updateLoadMoreButtonVisibility()
                }
            }
        })

        if (auth.currentUser == null && !isFinishing) {
            Log.w(TAG, "¡Alerta! Usuario no logueado en MainActivity, volviendo a Login.")
            navigateToLogin()
        }
    } // Fin de onCreate

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

    // --- updateLoadMoreButtonVisibility AHORA ES UN MÉTODO DE LA CLASE MainActivity ---
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
                    Log.d(TAG, "Botón Cargar Más: VISIBLE (al final y hay más páginas)")
                } else {
                    buttonLoadMore.visibility = View.GONE
                    // Log.d(TAG, "Botón Cargar Más: OCULTO (no al final o lista vacía aún)")
                }
            }
        } else {
            buttonLoadMore.visibility = View.GONE
            Log.d(TAG, "Botón Cargar Más: OCULTO (no hay más páginas url)")
        }
    }

    private fun searchRecipesApi(queryOrUrl: String, isInitialSearch: Boolean) {
        if (isLoading) return
        isLoading = true
        if (isInitialSearch) {
            buttonLoadMore.visibility = View.GONE
        }
        // TODO: Mostrar ProgressBar aquí

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
                // TODO: Ocultar ProgressBar aquí

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
                    updateLoadMoreButtonVisibility() // Llamar aquí para actualizar visibilidad

                    if (nextPageUrl == null && !isInitialSearch && newHits.isEmpty() && recipeAdapter.itemCount > 0) {
                        Toast.makeText(this@MainActivity, "No hay más recetas para cargar.", Toast.LENGTH_SHORT).show()
                    }
                    if (isInitialSearch && newHits.isEmpty()) {
                        Toast.makeText(this@MainActivity, "No se encontraron recetas para '$currentQuery'.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Error API: ${response.code()} - ${response.message()}")
                    nextPageUrl = null
                    updateLoadMoreButtonVisibility()
                    Toast.makeText(this@MainActivity, "Error de API: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RecipeResponse>, t: Throwable) {
                isLoading = false
                // TODO: Ocultar ProgressBar aquí
                Log.e(TAG, "Fallo en la llamada a la API: ${t.message}", t)
                nextPageUrl = null
                updateLoadMoreButtonVisibility()
                Toast.makeText(this@MainActivity, "Error de red.", Toast.LENGTH_SHORT).show()
            }
        }) // Fin de call.enqueue
    } // Fin de searchRecipesApi

    // --- ELIMINÉ LAS FUNCIONES handleSignWithGoogleCredential, firebaseAuthWithGoogle Y EL onStart DUPLICADO DE AQUÍ ---
    // --- ESAS FUNCIONES PERTENECEN A LoginActivity.kt ---

} // Fin de MainActivity