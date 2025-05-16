// En tu archivo ImageInfo.kt (o donde tengas estas clases)
package com.example.recetapp.data // O tu paquete

import android.os.Parcelable
import com.google.gson.annotations.SerializedName // Si usas esto
import kotlinx.parcelize.Parcelize

@Parcelize
data class ImageInfo(
    @SerializedName("THUMBNAIL") val thumbnail: ImageDetail?,
    @SerializedName("SMALL") val small: ImageDetail?,
    @SerializedName("REGULAR") val regular: ImageDetail?,
    @SerializedName("LARGE") val large: ImageDetail? // Puede que no siempre venga
) : Parcelable

@Parcelize
data class ImageDetail(
    val url: String?,
    val width: Int?,
    val height: Int?
) : Parcelable