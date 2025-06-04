package com.example.recetapp.data

import com.google.gson.annotations.SerializedName

data class RecipeResponse(
    val from: Int?,
    val to: Int?,
    val count: Int?,
    @SerializedName("_links") val links: com.example.recetapp.data.Links?,
    val hits: List<Hit>?
)