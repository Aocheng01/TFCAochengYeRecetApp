package com.example.recetapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.recetapp.R
import com.example.recetapp.data.Hit
import com.example.recetapp.data.Recipe // Importa Recipe

class RecipeAdapter(
    private var recipes: MutableList<Hit>,
    private val onItemClicked: (Recipe) -> Unit // Lambda para el clic, recibe un objeto Recipe
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.textViewRecipeName)
        val ingredientsTextView: TextView = itemView.findViewById(R.id.textViewIngredientCount)
        val recipeImageView: ImageView = itemView.findViewById(R.id.imageViewRecipe)

        // Configura el listener de clic para todo el ítem
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) { // Verifica que la posición sea válida
                    val clickedHit = recipes[position]
                    clickedHit.recipe?.let { recipeObject -> // Asegúrate que recipe no sea nulo
                        onItemClicked(recipeObject) // Llama a la lambda con el objeto Recipe
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val currentHit = recipes[position]
        val currentRecipe = currentHit.recipe

        holder.nameTextView.text = currentRecipe?.label ?: "Sin nombre"
        val ingredientCount = currentRecipe?.ingredientLines?.size ?: 0
        holder.ingredientsTextView.text = "$ingredientCount ingredientes"
        holder.recipeImageView.load(currentRecipe?.image) {
            crossfade(true)
            placeholder(R.drawable.ic_placeholder_image)
            error(R.drawable.ic_error_image)
        }
    }

    override fun getItemCount(): Int = recipes.size

    fun submitNewList(newRecipes: List<Hit>) {
        recipes.clear()
        recipes.addAll(newRecipes)
        notifyDataSetChanged()
    }

    fun addRecipes(newRecipes: List<Hit>) {
        val startPosition = recipes.size
        recipes.addAll(newRecipes)
        notifyItemRangeInserted(startPosition, newRecipes.size)
    }
}