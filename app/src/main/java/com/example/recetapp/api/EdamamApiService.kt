package com.example.recetapp.api

import com.example.recetapp.BuildConfig // <-- Importa BuildConfig
import com.example.recetapp.data.RecipeResponse // Asegúrate que la ruta sea correcta
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface EdamamApiService {

    // Constantes para parámetros comunes (opcional pero útil)
    companion object {
        const val API_TYPE = "public"
        // ¡¡NO pongas tus claves aquí en producción!! Úsalas de forma segura.
        val APP_ID: String = BuildConfig.EDAMAM_APP_ID // Se obtiene de BuildConfig
        val APP_KEY: String = BuildConfig.EDAMAM_APP_KEY // Se obtiene de
    }

    @GET("api/recipes/v2")
    fun searchRecipes(
        @Query("type") type: String = API_TYPE, // Siempre 'public' para búsqueda estándar
        @Query("q") query: String,              // El término de búsqueda del usuario
        @Query("app_id") appId: String = APP_ID,     // Añade "= APP_ID"
        @Query("app_key") appKey: String = APP_KEY,   // Añade "= APP_KEY"

        // --- Parámetros Opcionales (ejemplos, añade/quita según necesites) ---
        // @Query("diet") diet: String? = null,         // ej. "balanced", "high-protein", "low-carb"
        // @Query("health") health: String? = null,     // ej. "vegetarian", "peanut-free" (pueden ser múltiples separados por &health=...)
        // @Query("cuisineType") cuisineType: String? = null, // ej. "Mexican", "Italian"
        // @Query("mealType") mealType: String? = null,    // ej. "Lunch", "Dinner", "Snack"
        // @Query("calories") calories: String? = null,   // Rango ej. "100-300" o un número "300" (significa <=300)
        // @Query("time") time: String? = null,           // Rango ej. "1-60" (minutos) o "60+"
        @Query("imageSize") imageSize: String = "REGULAR" // Puedes pedir "LARGE", "SMALL", "THUMBNAIL"
        // @Query("random") random: Boolean? = null     // Para obtener recetas aleatorias

    ): Call<RecipeResponse> // Devuelve un objeto Call que Retrofit puede ejecutar

    // --- Alternativa con Coroutines ---
    /*
    @GET("api/recipes/v2")
    suspend fun searchRecipesSuspend(
        @Query("type") type: String = API_TYPE,
        @Query("q") query: String,
        @Query("app_id") appId: String,
        @Query("app_key") appKey: String,
        @Query("imageSize") imageSize: String = "REGULAR"
        // ... otros parámetros
    ): Response<RecipeResponse> // O directamente RecipeResponse si manejas errores/nulos de otra forma
    */


    // Podrías añadir aquí otros endpoints si los necesitas, por ejemplo, para obtener una receta por su URI:
    /*
    @GET("api/recipes/v2/{id}") // id sería el hash que viene en la URI de la receta
    fun getRecipeByUri(
        @Path("id") recipeId: String,
        @Query("type") type: String = API_TYPE,
        @Query("app_id") appId: String,
        @Query("app_key") appKey: String
    ): Call<RecipeResponse> // La respuesta puede ser ligeramente diferente, ajusta RecipeResponse si es necesario
    */
}