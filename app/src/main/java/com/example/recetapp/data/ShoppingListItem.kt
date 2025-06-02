// --- File: com/example/recetapp/data/ShoppingListItem.kt ---
// --- Código para el modelo de ítem de la lista de compra ---
package com.example.recetapp.data

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties // Útil para que Firestore no falle si hay campos extra en el doc
data class ShoppingListItem(
    var name: String = "",
    var isPurchased: Boolean = false,
    var recipeId: String? = null,   // ID/URI de la receta a la que pertenece (si aplica)
    var recipeName: String? = null, // Nombre de la receta a la que pertenece (si aplica)

    @ServerTimestamp // Firestore pondrá el timestamp del servidor aquí
    var addedAt: Date? = null,

    // El ID del documento de Firestore no se incluye aquí porque se obtiene
    // por separado o no es parte de los datos que *escribes* al crear un nuevo ítem con .add()
    // Si lo necesitas en el objeto después de leerlo, puedes mapearlo.
    @get:com.google.firebase.firestore.Exclude @set:com.google.firebase.firestore.Exclude
    var documentId: String? = null // Para mantener el ID del documento después de leerlo
) {
    // Constructor sin argumentos requerido por Firestore para deserialización con toObject()
    constructor() : this("", false, null, null, null)
}