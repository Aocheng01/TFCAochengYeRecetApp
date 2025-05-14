package com.example.recetapp.data

import com.google.gson.annotations.SerializedName

data class RecipeResponse(
    val from: Int?,
    val to: Int?,
    val count: Int?, // Total de recetas que coinciden (puede ser > que las que se pueden obtener)
    @SerializedName("_links") val links: com.example.recetapp.data.Links?, // Importante para paginación
    val hits: List<Hit>?
)