package com.example.recetapp.data // O en tu paquete de 'data'

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class ShoppingListItem(
    val id: String,
    var name: String,
    var isPurchased: Boolean = false
)