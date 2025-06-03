package com.example.recetapp.data

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties // Asegúrate de importar esto
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties // <-- AÑADIDO
@Parcelize
data class NutrientDetail(
    val label: String? = null,
    val quantity: Double? = null,
    val unit: String? = null
    // Si 'stability' es un campo que quieres usar en NutrientDetail:
    // val stability: String? = null,
) : Parcelable