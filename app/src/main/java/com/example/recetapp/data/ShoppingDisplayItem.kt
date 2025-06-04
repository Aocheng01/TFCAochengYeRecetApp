package com.example.recetapp.data

sealed class ShoppingDisplayItem {
    // Añadimos isExpanded, por defecto true (expandido)
    data class RecipeHeader(
        val recipeId: String,
        val recipeName: String,
        var isExpanded: Boolean = true
    ) : ShoppingDisplayItem()

    data class RecipeIngredient(
        val shoppingListItem: ShoppingListItem,
        val originalDocumentId: String
    ) : ShoppingDisplayItem()

    data class StandaloneItem(
        val shoppingListItem: ShoppingListItem,
        val originalDocumentId: String
    ) : ShoppingDisplayItem()
}