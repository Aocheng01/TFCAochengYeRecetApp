package com.example.recetapp.api

import com.example.recetapp.BuildConfig // Para leer las claves
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // URL Base de la API de Edamam
    private const val BASE_URL = "https://api.edamam.com/"
    // Definir el User ID (PARA PRUEBAS - usa uno fijo)

    private const val EDAMAM_USER_ID = "mi-usuario-prueba-001"

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

    // --- Cliente OkHttp personalizado para añadir el interceptor de cabecera ---
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(headerInterceptor)
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