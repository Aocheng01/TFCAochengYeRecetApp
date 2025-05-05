package com.example.recetapp

import android.content.Context // Importar Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager // Importar InputMethodManager
import android.widget.Button // Importar Button
import android.widget.EditText // Importar EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetapp.adapters.RecipeAdapter
import com.example.recetapp.api.RetrofitClient
import com.example.recetapp.data.Hit
import com.example.recetapp.data.RecipeResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    // Referencias a las vistas
    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var editTextSearchQuery: EditText // Referencia al EditText
    private lateinit var buttonSearch: Button         // Referencia al Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Inicializar Vistas ---
        recyclerView = findViewById(R.id.recyclerViewRecipes)
        editTextSearchQuery = findViewById(R.id.editTextSearchQuery) // Busca el EditText
        buttonSearch = findViewById(R.id.buttonSearch)             // Busca el Button

        // --- Configuración del RecyclerView (igual que antes) ---
        recipeAdapter = RecipeAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = recipeAdapter

        // --- Configuración del Listener del Botón ---
        buttonSearch.setOnClickListener {
            val query = editTextSearchQuery.text.toString().trim() // Obtiene el texto y quita espacios extra
            if (query.isNotBlank()) { // Verifica que no esté vacío
                Log.d(TAG, "Botón presionado, buscando: $query")
                searchRecipesApi(query) // Llama a la API con la búsqueda del usuario
                hideKeyboard() // Oculta el teclado después de buscar
            } else {
                Log.d(TAG, "Intento de búsqueda con campo vacío.")
                // Opcional: Mostrar mensaje al usuario que debe escribir algo
            }
        }

        // Ya NO llamamos a la API automáticamente al crear la actividad
        // searchRecipesApi("pasta") // Comenta o elimina esta línea
    }

    // Función para ocultar el teclado (útil)
    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusedView = this.currentFocus
        if (currentFocusedView != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocusedView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }


    // La función searchRecipesApi se mantiene igual que antes
    private fun searchRecipesApi(query: String) {
        Log.d(TAG, "Iniciando búsqueda de recetas para: $query")
        val apiService = RetrofitClient.instance
        val call: Call<RecipeResponse> = apiService.searchRecipes(query = query)

        // TODO: (Siguiente paso) Mostrar ProgressBar aquí

        call.enqueue(object : Callback<RecipeResponse> {
            override fun onResponse(call: Call<RecipeResponse>, response: Response<RecipeResponse>) {
                // TODO: (Siguiente paso) Ocultar ProgressBar aquí

                if (response.isSuccessful) {
                    val recipeResponse = response.body()
                    val hits = recipeResponse?.hits ?: emptyList()
                    if (hits.isNotEmpty()) {
                        Log.d(TAG, "Recetas encontradas: ${hits.size}")
                        recipeAdapter.updateRecipes(hits)
                    } else {
                        Log.d(TAG, "No se encontraron recetas para '$query'. Código: ${response.code()}")
                        recipeAdapter.updateRecipes(emptyList())
                        // TODO: (Siguiente paso) Mostrar mensaje "No hay resultados"
                    }
                } else {
                    Log.e(TAG, "Error en la respuesta de la API: Código: ${response.code()}, Mensaje: ${response.message()}")
                    recipeAdapter.updateRecipes(emptyList())
                    // TODO: (Siguiente paso) Mostrar mensaje de error API
                }
            }

            override fun onFailure(call: Call<RecipeResponse>, t: Throwable) {
                // TODO: (Siguiente paso) Ocultar ProgressBar aquí
                Log.e(TAG, "Fallo en la llamada a la API: ${t.message}", t)
                recipeAdapter.updateRecipes(emptyList())
                // TODO: (Siguiente paso) Mostrar mensaje de error de red
            }
        })
    }
}