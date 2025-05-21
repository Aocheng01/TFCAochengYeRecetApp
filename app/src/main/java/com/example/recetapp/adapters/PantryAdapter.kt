package com.example.recetapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recetapp.R // Asegúrate que esta importación sea correcta

class PantryAdapter(
    private val pantryItems: MutableList<String>, // Lista de nombres de ingredientes
    private val onSearchClick: (ingredient: String) -> Unit,
    private val onAddToShoppingListClick: (ingredient: String) -> Unit,
    private val onDeleteClick: (ingredient: String, position: Int) -> Unit
) : RecyclerView.Adapter<PantryAdapter.PantryViewHolder>() {

    inner class PantryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemNameTextView: TextView = itemView.findViewById(R.id.textViewPantryItemName)
        val searchButton: ImageButton = itemView.findViewById(R.id.buttonSearchRecipesWithIngredient)
        val addToShoppingListButton: ImageButton = itemView.findViewById(R.id.buttonAddToShoppingList)
        val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDeletePantryItem)

        fun bind(ingredientName: String, position: Int) {
            itemNameTextView.text = ingredientName

            searchButton.setOnClickListener {
                // Solo llama si la posición es válida (evita problemas si el item se elimina rápidamente)
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onSearchClick(pantryItems[adapterPosition])
                }
            }
            addToShoppingListButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onAddToShoppingListClick(pantryItems[adapterPosition])
                }
            }
            deleteButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDeleteClick(pantryItems[adapterPosition], adapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PantryViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pantry_ingredient, parent, false)
        return PantryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PantryViewHolder, position: Int) {
        holder.bind(pantryItems[position], position)
    }

    override fun getItemCount(): Int = pantryItems.size

    fun addItem(item: String) {
        pantryItems.add(item)
        notifyItemInserted(pantryItems.size - 1)
    }

    fun removeItem(position: Int) {
        if (position >= 0 && position < pantryItems.size) {
            pantryItems.removeAt(position)
            notifyItemRemoved(position)
            // Es importante notificar el cambio de rango si las posiciones de otros items se ven afectadas
            if (position < pantryItems.size) { // Si no era el último item
                notifyItemRangeChanged(position, pantryItems.size - position)
            }
        }
    }

    fun getItems(): List<String> {
        return pantryItems.toList() // Devuelve una copia para evitar modificaciones externas
    }

    fun isEmpty(): Boolean {
        return pantryItems.isEmpty()
    }
}
