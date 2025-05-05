package com.example.recetapp.data

import com.google.gson.annotations.SerializedName

// Contiene los totales de varios nutrientes. Añade los que necesites.
data class NutrientsInfo(
    @SerializedName("ENERC_KCAL") val energyKcal: Nutrient?,
    @SerializedName("FAT") val fat: Nutrient?,
    @SerializedName("CHOCDF") val carbs: Nutrient?, // Carbohidratos
    @SerializedName("PROCNT") val protein: Nutrient?,
    @SerializedName("FIBTG") val fiber: Nutrient?
    // ... añade más nutrientes según los códigos de Edamam (ej. "SUGAR", "NA" para Sodio, etc.)
)

data class Nutrient(
    val label: String?, // Nombre (ej. "Energy")
    val quantity: Double?,
    val unit: String? // Unidad (ej. "kcal", "g", "mg")
)