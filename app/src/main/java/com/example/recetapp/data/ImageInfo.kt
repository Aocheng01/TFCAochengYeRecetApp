package com.example.recetapp.data

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class ImageInfo(
    @SerializedName("THUMBNAIL") val thumbnail: ImageDetail? = null,
    @SerializedName("SMALL") val small: ImageDetail? = null,
    @SerializedName("REGULAR") val regular: ImageDetail? = null,
    @SerializedName("LARGE") val large: ImageDetail? = null
) : Parcelable

@IgnoreExtraProperties
@Parcelize
data class ImageDetail(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null
) : Parcelable