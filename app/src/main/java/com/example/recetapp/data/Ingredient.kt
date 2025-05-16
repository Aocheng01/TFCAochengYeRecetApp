// En tu archivo Ingredient.kt
package com.example.recetapp.data // O tu paquete

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
// ... otras importaciones si las tiene ...

@Parcelize
data class Ingredient(
    // ... los campos de tu clase Ingredient ...
    val text: String?,
    val quantity: Double?,
    val measure: String?,
    val food: String?,
    val weight: Double?,
    val foodCategory: String?,
    val foodId: String?,
    val image: String?
) : Parcelable