package com.example.recetapp.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recetapp.R
import com.example.recetapp.data.ShoppingListItem

// Data class para el ítem de la lista de compra (si no la creaste en un archivo separado)
// data class ShoppingListItem(
//     val id: String, // ID del documento en Firestore
//     var name: String,
//     var isPurchased: Boolean = false
// )

class ShoppingListAdapter(
    private val shoppingListItems: MutableList<ShoppingListItem>,
    private val onItemCheckedChanged: (item: ShoppingListItem, isChecked: Boolean, position: Int) -> Unit,
    private val onDeleteClick: (item: ShoppingListItem, position: Int) -> Unit
) : RecyclerView.Adapter<ShoppingListAdapter.ShoppingItemViewHolder>() {

    inner class ShoppingItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemNameTextView: TextView = itemView.findViewById(R.id.textViewShoppingItemName)
        val purchasedCheckBox: CheckBox = itemView.findViewById(R.id.checkBoxShoppingItemPurchased)
        val deleteButton: ImageButton = itemView.findViewById(R.id.imageButtonDeleteShoppingItem)

        fun bind(item: ShoppingListItem, position: Int) {
            itemNameTextView.text = item.name
            purchasedCheckBox.isChecked = item.isPurchased

            // Aplicar/quitar tachado según el estado
            if (item.isPurchased) {
                itemNameTextView.paintFlags = itemNameTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                itemNameTextView.paintFlags = itemNameTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            purchasedCheckBox.setOnCheckedChangeListener { _, isChecked ->
                // Evitar llamadas múltiples si el estado ya es el correcto (importante si se reciclan vistas)
                if (adapterPosition != RecyclerView.NO_POSITION && shoppingListItems[adapterPosition].isPurchased != isChecked) {
                    onItemCheckedChanged(shoppingListItems[adapterPosition], isChecked, adapterPosition)
                }
            }

            deleteButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDeleteClick(shoppingListItems[adapterPosition], adapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingItemViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list, parent, false) // Usa tu item_shopping_list.xml
        return ShoppingItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ShoppingItemViewHolder, position: Int) {
        holder.bind(shoppingListItems[position], position)
    }

    override fun getItemCount(): Int = shoppingListItems.size

    fun submitList(newItems: List<ShoppingListItem>) {
        shoppingListItems.clear()
        shoppingListItems.addAll(newItems)
        notifyDataSetChanged() // Podría optimizarse con DiffUtil para mejor rendimiento
    }

    // Podrías necesitar métodos para actualizar un solo ítem si cambias 'isPurchased' localmente
    // antes de que Firestore actualice, o si Firestore no devuelve el item actualizado inmediatamente.
    // Por ahora, con SnapshotListener, submitList debería ser suficiente.

    fun updateItemPurchasedState(position: Int, isPurchased: Boolean) {
        if (position >= 0 && position < shoppingListItems.size) {
            shoppingListItems[position].isPurchased = isPurchased
            notifyItemChanged(position)
        }
    }


    fun isEmpty(): Boolean {
        return shoppingListItems.isEmpty()
    }
}
