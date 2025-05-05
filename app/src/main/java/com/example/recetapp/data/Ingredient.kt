package com.example.recetapp.data

import com.google.gson.annotations.SerializedName

data class Ingredient(
    val text: String?, // Descripción completa (ej. "1 large egg")
    val quantity: Double?,
    val measure: String?, // Unidad (ej. "cup", "tablespoon", null si es unidad como "large")
    val food: String?, // Nombre del alimento (ej. "egg")
    val weight: Double?, // Peso en gramos
    val foodCategory: String?, // Categoría (ej. "Eggs")
    val foodId: String?,
    val image: String? // URL de imagen del ingrediente (puede no estar)
)