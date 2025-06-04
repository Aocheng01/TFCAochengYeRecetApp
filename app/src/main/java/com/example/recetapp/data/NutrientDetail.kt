package com.example.recetapp.data

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class NutrientDetail(
    val label: String? = null,
    val quantity: Double? = null,
    val unit: String? = null
) : Parcelable