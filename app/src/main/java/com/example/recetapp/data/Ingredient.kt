package com.example.recetapp.data

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties // Asegúrate de importar esto
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties // <-- AÑADIDO
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
    // Si 'stability' es un campo que quieres usar en Ingredient:
    // val stability: String? = null,
) : Parcelable