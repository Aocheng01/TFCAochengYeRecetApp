package com.example.recetapp.data

import com.google.gson.annotations.SerializedName

// Representa un resultado individual (contiene una receta)
data class Hit(
    val recipe: Recipe?
    // @SerializedName("_links") // Descomenta si necesitas enlaces específicos del hit
    // val links: SelfLink?
)