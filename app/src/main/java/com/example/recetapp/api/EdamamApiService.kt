package com.example.recetapp.api

import com.example.recetapp.BuildConfig
import com.example.recetapp.data.RecipeResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface EdamamApiService {
    // Constantes para parámetros comunes
    companion object {
        const val API_TYPE = "public"
        val APP_ID: String = BuildConfig.EDAMAM_APP_ID // Se obtiene de BuildConfig
        val APP_KEY: String = BuildConfig.EDAMAM_APP_KEY
    }

    @GET("api/recipes/v2")
    fun searchRecipes(
        @Query("type") type: String = API_TYPE, // Siempre 'public' para búsqueda estándar
        @Query("q") query: String,              // El término de búsqueda del usuario
        @Query("app_id") appId: String = APP_ID,
        @Query("app_key") appKey: String = APP_KEY,
        @Query("imageSize") imageSize: String = "REGULAR"

    ): Call<RecipeResponse> // Devuelve un objeto Call que Retrofit puede ejecutar

    @GET
    fun getNextPageRecipes(@Url nextPageUrl: String): Call<RecipeResponse>
    // app_id y app_key ya vienen en la nextPageUrl que proporciona Edamam.
}