package com.example.recetapp.api // Asegúrate que este sea tu paquete

import com.example.recetapp.BuildConfig // Para leer las claves
import okhttp3.Interceptor // Importa Interceptor
import okhttp3.OkHttpClient
// Si quitaste el logging interceptor, no necesitas esta importación:
// import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // URL Base de la API de Edamam
    private const val BASE_URL = "https://api.edamam.com/"

    // --- Definir el User ID (PARA PRUEBAS - usa uno fijo) ---
    // En una app real, este ID debería ser único para cada usuario de tu app.
    private const val EDAMAM_USER_ID = "mi-usuario-prueba-001" // Puedes cambiar esto si quieres

    // --- Interceptor para añadir la cabecera Edamam-Account-User ---
    private val headerInterceptor = Interceptor { chain ->
        // Obtiene la petición original
        val originalRequest = chain.request()
        // Construye una nueva petición añadiendo la cabecera requerida
        val newRequest = originalRequest.newBuilder()
            .header("Edamam-Account-User", EDAMAM_USER_ID)
            .build()
        // Procede con la nueva petición
        chain.proceed(newRequest)
    }

    /* --- Descomenta esto si quieres volver a activar el logging ---
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    */

    // --- Cliente OkHttp personalizado para añadir el interceptor de cabecera ---
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(headerInterceptor) // <--- AÑADE EL INTERCEPTOR DE CABECERA
        // .addInterceptor(loggingInterceptor) // <--- Descomenta si reactivas el logging
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // --- Creación de la instancia de Retrofit ---
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // <--- USA EL CLIENTE OKHTTP PERSONALIZADO
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // --- Creación de la implementación del servicio API ---
    val instance: EdamamApiService by lazy {
        retrofit.create(EdamamApiService::class.java)
    }
}