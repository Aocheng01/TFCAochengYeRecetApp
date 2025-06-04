package com.example.recetapp.data

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class Ingredient(
    val text: String? = null,
    val quantity: Double? = null,
    val measure: String? = null,
    val food: String? = null,
    val weight: Double? = null,
    val foodCategory: String? = null,
    val foodId: String? = null,
    val image: String? = null
) : Parcelable