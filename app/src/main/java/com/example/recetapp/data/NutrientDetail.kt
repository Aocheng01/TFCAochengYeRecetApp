package com.example.recetapp.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

// En com/example/recetapp/data/RecipeDataModels.kt
@Parcelize
data class NutrientDetail(
    val label: String?,
    val quantity: Double?,
    val unit: String?
) : Parcelable