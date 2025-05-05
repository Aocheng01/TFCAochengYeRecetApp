package com.example.recetapp.data

import com.google.gson.annotations.SerializedName

data class ImageInfo(
    @SerializedName("THUMBNAIL") val thumbnail: ImageDetail?,
    @SerializedName("SMALL") val small: ImageDetail?,
    @SerializedName("REGULAR") val regular: ImageDetail?,
    @SerializedName("LARGE") val large: ImageDetail? // Puede que no siempre venga
)

data class ImageDetail(
    val url: String?,
    val width: Int?,
    val height: Int?
)