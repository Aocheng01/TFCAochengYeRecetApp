package com.example.recetapp.data

import com.google.gson.annotations.SerializedName
import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class Recipe(
    @SerializedName("yield") val servings: Float? = null,
    val uri: String? = null,
    val label: String? = null,
    val image: String? = null,
    val images: ImageInfo? = null,
    val source: String? = null,
    val url: String? = null,
    val dietLabels: List<String>? = null,
    val healthLabels: List<String>? = null,
    val ingredientLines: List<String>? = null,
    val ingredients: List<Ingredient>? = null,
    val calories: Double? = null,
    val totalWeight: Double? = null,
    val totalTime: Double? = null,
    val totalNutrients: Map<String, NutrientDetail>? = null,
    val difficulty: String? = null,
    val description: String? = null,
    val instructions: List<String>? = null,
    val totalNutrientsDetailed: Map<String, NutrientDetail>? = null,
    var isFavorite: Boolean = false
) : Parcelable