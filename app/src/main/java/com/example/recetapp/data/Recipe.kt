package com.example.recetapp.data

import com.google.gson.annotations.SerializedName
import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
// Representa la información detallada de una receta
data class Recipe(
    @SerializedName("yield") val servings: Float?, // <-- ASEGÚRATE QUE ESTE CAMPO EXISTE Y SE LLAMA 'servings'
    val uri: String?, // Identificador único de la receta
    val label: String?, // Nombre de la receta (ej. "Chicken Soup")
    val image: String?, // URL de la imagen principal
    val images: ImageInfo?, // Objeto con diferentes tamaños de imagen
    val source: String?, // Fuente de la receta (ej. "Food Network")
    val url: String?, // URL de la receta original
    // val shareAs: String?,
    //val yield: Float?, // Porciones
    val dietLabels: List<String>?, // Etiquetas de dieta (ej. ["Low-Carb"])
    val healthLabels: List<String>?, // Etiquetas de salud (ej. ["Peanut-Free", "Tree-Nut-Free"])
    // val cautions: List<String>?,
    val ingredientLines: List<String>?, // Lista de ingredientes como texto (ej. "1 cup flour")
    val ingredients: List<Ingredient>?, // Lista detallada de ingredientes
    val calories: Double?,
    val totalWeight: Double?,
     val totalTime: Double?, // Tiempo total de preparación/cocción
    // val cuisineType: List<String>?,
    // val mealType: List<String>?,
    // val dishType: List<String>?,
    val totalNutrients: NutrientsInfo?, // Información nutricional detallada
    // val totalDaily: NutrientsInfo? // Porcentaje diario de nutrientes
    // val digest: List<DigestInfo>? // Información de digestión por nutriente
    val difficulty: String? = null, // Para la dificultad (ej. "Fácil", "Medio")
    val description: String? = null, // Para la descripción de la receta
    val instructions: List<String>? = null, // Para la lista de pasos de instrucciones
// Para información nutricional detallada, necesitarás una estructura, por ejemplo:
    val totalNutrientsDetailed: Map<String, NutrientDetail>? = null, // Usando la clase NutrientDetail que discutimos
    var isFavorite: Boolean = false // Para el estado de favorito (no parcelar si se determina en runtime)
) : Parcelable