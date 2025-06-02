// --- File: com/example/recetapp/data/ShoppingDisplayItem.kt ---
// --- Modelo sellado para los diferentes tipos de ítems en el RecyclerView de la Lista de Compra ---
package com.example.recetapp.data

sealed class ShoppingDisplayItem {
    data class RecipeHeader(val recipeId: String, val recipeName: String) : ShoppingDisplayItem()
    data class RecipeIngredient(val shoppingListItem: ShoppingListItem, val originalDocumentId: String) : ShoppingDisplayItem()
    data class StandaloneItem(val shoppingListItem: ShoppingListItem, val originalDocumentId: String) : ShoppingDisplayItem()
}