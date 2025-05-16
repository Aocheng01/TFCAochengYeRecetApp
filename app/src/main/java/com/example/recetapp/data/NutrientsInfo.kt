// En tu archivo NutrientsInfo.kt (o donde tengas estas clases)
package com.example.recetapp.data // O tu paquete

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class NutrientsInfo(
    @SerializedName("ENERC_KCAL") val energyKcal: Nutrient?,
    @SerializedName("FAT") val fat: Nutrient?,
    @SerializedName("CHOCDF") val carbs: Nutrient?,
    @SerializedName("PROCNT") val protein: Nutrient?,
    @SerializedName("FIBTG") val fiber: Nutrient?
    // ... más nutrientes ...
) : Parcelable

@Parcelize
data class Nutrient(
    val label: String?,
    val quantity: Double?,
    val unit: String?
) : Parcelable