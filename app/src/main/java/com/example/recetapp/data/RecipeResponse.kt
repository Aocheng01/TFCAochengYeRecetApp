package com.example.recetapp.data

// Representa la respuesta completa de la API de búsqueda
data class RecipeResponse(
    // @SerializedName("_links") // Descomenta si necesitas los enlaces de paginación
    // val links: Links?,
    val hits: List<Hit>? // La lista de recetas encontradas
)