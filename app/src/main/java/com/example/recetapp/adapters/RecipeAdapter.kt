package com.example.recetapp.adapters // O tu paquete

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView // Importa ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load // <--- IMPORTANTE: Importa la función 'load' de Coil
import com.example.recetapp.R
import com.example.recetapp.data.Hit

class RecipeAdapter(private var recipes: MutableList<Hit>) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() { // Cambiado a MutableList

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.textViewRecipeName)
        val ingredientsTextView: TextView = itemView.findViewById(R.id.textViewIngredientCount)
        // Añade la referencia al ImageView
        val recipeImageView: ImageView = itemView.findViewById(R.id.imageViewRecipe) // <--- AÑADE ESTO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder { // Asegúrate que esto está bien
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val currentHit = recipes[position]
        val currentRecipe = currentHit.recipe

        // Configura los textos (igual que antes)
        holder.nameTextView.text = currentRecipe?.label ?: "Sin nombre"
        val ingredientCount = currentRecipe?.ingredientLines?.size ?: 0
        holder.ingredientsTextView.text = "$ingredientCount ingredientes"

        // --- Carga la imagen usando Coil ---
        val imageUrl = currentRecipe?.image // Obtiene la URL de la imagen del objeto Recipe
        holder.recipeImageView.load(imageUrl) { // Llama a la función 'load' de Coil en el ImageView
            crossfade(true) // Efecto de transición suave (opcional)
            placeholder(R.drawable.ic_placeholder_image) // Imagen mientras carga (opcional)
            error(R.drawable.ic_error_image) // Imagen si falla la carga (opcional)
            // Coil maneja automáticamente la descarga en segundo plano y el caché
        }
        // --- Fin carga de imagen ---
    }

    override fun getItemCount(): Int {
        return recipes.size
    }

    /**
     * Reemplaza la lista actual. Usar para una nueva búsqueda.
     */
    fun submitNewList(newRecipes: List<Hit>) {
        this.recipes.clear()
        this.recipes.addAll(newRecipes)
        notifyDataSetChanged() // Para una lista completamente nueva, esto está bien por ahora
    }

    /**
     * Añade nuevas recetas al final de la lista existente. Usar para paginación.
     */
    fun addRecipes(newRecipes: List<Hit>) {
        val startPosition = this.recipes.size
        this.recipes.addAll(newRecipes)
        notifyItemRangeInserted(startPosition, newRecipes.size)
    }
}