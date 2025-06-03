// --- File: com/example/recetapp/adapters/ShoppingListAdapter.kt ---
package com.example.recetapp.adapters

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.Space
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.recetapp.R
import com.example.recetapp.data.ShoppingDisplayItem
import com.example.recetapp.data.ShoppingListItem

class ShoppingListAdapter(
    private val onItemCheckedChanged: (shoppingListItem: ShoppingListItem, documentId: String, isChecked: Boolean) -> Unit,
    private val onDeleteClick: (shoppingListItem: ShoppingListItem, documentId: String) -> Unit,
    private val onRecipeHeaderClick: (recipeId: String, recipeName: String) -> Unit, // Puede usarse para toggle o para otra acción
    // Nuevos Callbacks
    private val onToggleRecipeExpandClick: (recipeId: String) -> Unit,
    private val onDeleteRecipeClick: (recipeId: String, recipeName: String) -> Unit
) : ListAdapter<ShoppingDisplayItem, RecyclerView.ViewHolder>(ShoppingDisplayItemDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_RECIPE_HEADER = 0
        private const val VIEW_TYPE_SHOPPING_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ShoppingDisplayItem.RecipeHeader -> VIEW_TYPE_RECIPE_HEADER
            is ShoppingDisplayItem.RecipeIngredient, is ShoppingDisplayItem.StandaloneItem -> VIEW_TYPE_SHOPPING_ITEM
            // else -> throw IllegalStateException("Unknown view type at position $position") // Mejor manejo para casos desconocidos
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_RECIPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_shopping_list_recipe_header, parent, false)
                RecipeHeaderViewHolder(view)
            }
            VIEW_TYPE_SHOPPING_ITEM -> {
                val view = inflater.inflate(R.layout.item_shopping_list, parent, false)
                ShoppingItemViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val displayItem = getItem(position)
        when (holder) {
            is RecipeHeaderViewHolder -> holder.bind(displayItem as ShoppingDisplayItem.RecipeHeader)
            is ShoppingItemViewHolder -> {
                when (displayItem) {
                    is ShoppingDisplayItem.RecipeIngredient -> holder.bind(displayItem.shoppingListItem, displayItem.originalDocumentId, true)
                    is ShoppingDisplayItem.StandaloneItem -> holder.bind(displayItem.shoppingListItem, displayItem.originalDocumentId, false)
                    else -> {}
                }
            }
        }
    }

    inner class RecipeHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recipeNameTextView: TextView = itemView.findViewById(R.id.textViewRecipeNameHeader)
        // Nuevos botones
        private val toggleExpandButton: ImageButton = itemView.findViewById(R.id.buttonToggleExpandRecipe)
        private val deleteRecipeButton: ImageButton = itemView.findViewById(R.id.buttonDeleteRecipe)

        fun bind(headerItem: ShoppingDisplayItem.RecipeHeader) {
            recipeNameTextView.text = headerItem.recipeName // Ya no es necesario añadir ":" aquí si el layout lo gestiona bien

            // Configurar icono de expansión
            if (headerItem.isExpanded) {
                toggleExpandButton.setImageResource(R.drawable.ic_keyboard_arrow_up)
            } else {
                toggleExpandButton.setImageResource(R.drawable.ic_keyboard_arrow_down)
            }

            // Click en el layout del header para expandir/colapsar (opcional, además del botón)
            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onRecipeHeaderClick(headerItem.recipeId, headerItem.recipeName) // O llama a onToggleRecipeExpandClick directamente
                }
            }

            toggleExpandButton.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onToggleRecipeExpandClick(headerItem.recipeId)
                }
            }

            deleteRecipeButton.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onDeleteRecipeClick(headerItem.recipeId, headerItem.recipeName)
                }
            }
        }
    }

    inner class ShoppingItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemNameTextView: TextView = itemView.findViewById(R.id.textViewShoppingItemName)
        private val purchasedCheckBox: CheckBox = itemView.findViewById(R.id.checkBoxShoppingItemPurchased)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.imageButtonDeleteShoppingItem)
        private val indentationSpace: Space = itemView.findViewById(R.id.indentationSpace)

        fun bind(item: ShoppingListItem, documentId: String, isRecipeIngredient: Boolean) {
            itemNameTextView.text = item.name
            purchasedCheckBox.setOnCheckedChangeListener(null)
            purchasedCheckBox.isChecked = item.isPurchased
            applyTextPaintFlags(itemNameTextView, item.isPurchased)

            val indentPx = if (isRecipeIngredient) dpToPx(24, itemView.context) else 0
            indentationSpace.layoutParams.width = indentPx
            indentationSpace.requestLayout()

            purchasedCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    val currentDisplayItem = getItem(bindingAdapterPosition)
                    val actualListItem = when(currentDisplayItem) {
                        is ShoppingDisplayItem.RecipeIngredient -> currentDisplayItem.shoppingListItem
                        is ShoppingDisplayItem.StandaloneItem -> currentDisplayItem.shoppingListItem
                        else -> null
                    }
                    actualListItem?.let {
                        onItemCheckedChanged(it, documentId, isChecked)
                    }
                }
            }
            deleteButton.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    val currentDisplayItem = getItem(bindingAdapterPosition)
                    val (listItem, docId) = when (currentDisplayItem) {
                        is ShoppingDisplayItem.RecipeIngredient -> currentDisplayItem.shoppingListItem to currentDisplayItem.originalDocumentId
                        is ShoppingDisplayItem.StandaloneItem -> currentDisplayItem.shoppingListItem to currentDisplayItem.originalDocumentId
                        else -> null to null
                    }
                    if (listItem != null && docId != null) {
                        onDeleteClick(listItem, docId)
                    }
                }
            }
        }

        private fun dpToPx(dp: Int, context: Context): Int {
            return (dp * context.resources.displayMetrics.density).toInt()
        }
    }

    private fun applyTextPaintFlags(textView: TextView, isPurchased: Boolean) {
        if (isPurchased) {
            textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }
}

// DiffUtil.ItemCallback no necesita cambios si RecipeHeader es una data class,
// ya que la comparación por defecto de 'areContentsTheSame' con 'oldItem == newItem'
// comparará todos los campos, incluyendo 'isExpanded'.
class ShoppingDisplayItemDiffCallback : DiffUtil.ItemCallback<ShoppingDisplayItem>() {
    override fun areItemsTheSame(oldItem: ShoppingDisplayItem, newItem: ShoppingDisplayItem): Boolean {
        return when {
            oldItem is ShoppingDisplayItem.RecipeHeader && newItem is ShoppingDisplayItem.RecipeHeader ->
                oldItem.recipeId == newItem.recipeId
            oldItem is ShoppingDisplayItem.RecipeIngredient && newItem is ShoppingDisplayItem.RecipeIngredient ->
                oldItem.originalDocumentId == newItem.originalDocumentId // Compara por ID único del item
            oldItem is ShoppingDisplayItem.StandaloneItem && newItem is ShoppingDisplayItem.StandaloneItem ->
                oldItem.originalDocumentId == newItem.originalDocumentId // Compara por ID único del item
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: ShoppingDisplayItem, newItem: ShoppingDisplayItem): Boolean {
        // Para data classes, la comparación '==' compara todos los campos.
        // Para RecipeIngredient y StandaloneItem, podríamos querer comparar el shoppingListItem interno.
        return when {
            oldItem is ShoppingDisplayItem.RecipeHeader && newItem is ShoppingDisplayItem.RecipeHeader ->
                oldItem == newItem // Compara recipeId, recipeName, y isExpanded
            oldItem is ShoppingDisplayItem.RecipeIngredient && newItem is ShoppingDisplayItem.RecipeIngredient ->
                oldItem.shoppingListItem == newItem.shoppingListItem && oldItem.originalDocumentId == newItem.originalDocumentId
            oldItem is ShoppingDisplayItem.StandaloneItem && newItem is ShoppingDisplayItem.StandaloneItem ->
                oldItem.shoppingListItem == newItem.shoppingListItem && oldItem.originalDocumentId == newItem.originalDocumentId
            else -> false
        }
    }
}