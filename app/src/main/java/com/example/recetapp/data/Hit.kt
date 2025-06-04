package com.example.recetapp.data

import com.google.gson.annotations.SerializedName
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
// Representa un resultado individual (contiene una receta)
data class Hit(
    val recipe: Recipe?
) : Parcelable