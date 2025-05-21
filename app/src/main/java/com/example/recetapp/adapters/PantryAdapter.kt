package com.example.recetapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recetapp.R

// Modelo de datos simple para el item de la despensa (podría ser más complejo)
data class PantryItem(val id: String, val name: String) // id será el documentId de Firestore

class PantryAdapter(
    private val pantryItemsList: MutableList<PantryItem>,
    private val onSearchClick: (ingredientName: String) -> Unit,
    private val onAddToShoppingListClick: (ingredientName: String) -> Unit,
    private val onDeleteClick: (pantryItem: PantryItem, position: Int) -> Unit
) : RecyclerView.Adapter<PantryAdapter.PantryViewHolder>() {

    inner class PantryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemNameTextView: TextView = itemView.findViewById(R.id.textViewPantryItemName)
        val searchButton: ImageButton = itemView.findViewById(R.id.buttonSearchRecipesWithIngredient)
        val addToShoppingListButton: ImageButton = itemView.findViewById(R.id.buttonAddToShoppingList)
        val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDeletePantryItem)

        fun bind(item: PantryItem) {
            itemNameTextView.text = item.name

            searchButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onSearchClick(pantryItemsList[adapterPosition].name)
                }
            }
            addToShoppingListButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onAddToShoppingListClick(pantryItemsList[adapterPosition].name)
                }
            }
            deleteButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDeleteClick(pantryItemsList[adapterPosition], adapterPosition)
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
        holder.bind(pantryItemsList[position])
    }

    override fun getItemCount(): Int = pantryItemsList.size

    // Reemplaza la lista entera (usado al cargar desde Firestore)
    fun submitList(newItems: List<PantryItem>) {
        pantryItemsList.clear()
        pantryItemsList.addAll(newItems)
        notifyDataSetChanged() // Podría optimizarse con DiffUtil
    }

    // Añade un solo item (usado después de añadir a Firestore y obtener confirmación o ID)
    // Esta función podría no ser necesaria si siempre recargas la lista completa desde Firestore
    // o si Firestore te devuelve el item añadido para insertarlo localmente.
    // Por ahora, la comentaremos y nos basaremos en recargar la lista.
    /*
    fun addItem(item: PantryItem) {
        pantryItemsList.add(item)
        notifyItemInserted(pantryItemsList.size - 1)
    }
    */

    fun removeItem(position: Int) {
        if (position >= 0 && position < pantryItemsList.size) {
            pantryItemsList.removeAt(position)
            notifyItemRemoved(position)
            if (position < pantryItemsList.size) {
                notifyItemRangeChanged(position, pantryItemsList.size - position)
            }
        }
    }

    fun isEmpty(): Boolean {
        return pantryItemsList.isEmpty()
    }
}
