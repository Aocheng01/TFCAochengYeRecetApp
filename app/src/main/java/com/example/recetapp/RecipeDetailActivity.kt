package com.example.recetapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import coil.load
import com.example.recetapp.data.NutrientDetail // Asegúrate de importar esto si lo usas
import com.example.recetapp.data.Recipe
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Locale

class RecipeDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RECIPE = "extra_recipe_data"
    }

    // Variables para los FABs (para poder cambiar el icono de favorito)
    private lateinit var fabFavorite: FloatingActionButton
    private var currentRecipe: Recipe? = null // Para acceder a la receta en los listeners

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        // Configurar Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val collapsingToolbar: CollapsingToolbarLayout = findViewById(R.id.collapsingToolbar)

        // Referencias a las vistas del layout
        val imageViewRecipeDetail: ImageView = findViewById(R.id.imageViewRecipeDetail)
        val textViewRecipeLabelBody: TextView = findViewById(R.id.textViewRecipeLabelDetail_Body) // ID del XML

        // Stats
        val textViewCaloriesDetail: TextView = findViewById(R.id.textViewCaloriesDetail)
        val textViewTotalTimeDetail: TextView = findViewById(R.id.textViewTotalTimeDetail)
        val textViewDifficulty: TextView = findViewById(R.id.textViewDifficulty) // Nuevo

        // Description
        val textViewRecipeDescription: TextView = findViewById(R.id.textViewRecipeDescription) // Nuevo

        // Labels
        val textViewLabelsTitle: TextView = findViewById(R.id.textViewLabelsTitle)
        val chipGroupLabels: ChipGroup = findViewById(R.id.chipGroupLabels)

        // Ingredients
        val textViewServingsInfoForIngredients: TextView = findViewById(R.id.textViewServingsInfoForIngredients)
        val textViewIngredientsDetail: TextView = findViewById(R.id.textViewIngredientsDetail)

        // Instructions
        val textViewInstructionsTitle: TextView = findViewById(R.id.textViewInstructionsTitle) // Nuevo
        val textViewInstructions: TextView = findViewById(R.id.textViewInstructions) // Nuevo

        // Nutritional Info
        val textViewNutritionTitle: TextView = findViewById(R.id.textViewNutritionTitle) // Nuevo
        val textViewNutritionInfoDetailed: TextView = findViewById(R.id.textViewNutritionInfoDetailed) // Nuevo

        // Source & URL
        val textViewSourceUrlTitle: TextView = findViewById(R.id.textViewSourceUrlTitle)
        val textViewRecipeSourceDetail: TextView = findViewById(R.id.textViewRecipeSourceDetail)
        val textViewRecipeUrlDetail: TextView = findViewById(R.id.textViewRecipeUrlDetail)

        // FABs
        fabFavorite = findViewById(R.id.fabFavorite) // Nuevo
        val fabAddToList: FloatingActionButton = findViewById(R.id.fabAddToList) // Nuevo

        // Obtener el objeto Recipe del Intent
        currentRecipe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RECIPE, Recipe::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RECIPE)
        }

        if (currentRecipe != null) {
            val recipe = currentRecipe!! // Non-null assertion para conveniencia aquí

            collapsingToolbar.title = recipe.label ?: "Detalle de Receta"
            textViewRecipeLabelBody.text = recipe.label ?: "N/A"

            imageViewRecipeDetail.load(recipe.image) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder_image)
                error(R.drawable.ic_error_image)
            }

            // --- Poblar Estadísticas ---
            textViewCaloriesDetail.text = recipe.calories?.let { String.format(Locale.getDefault(), "%.0f kcal", it) } ?: "N/A"
            textViewTotalTimeDetail.text = recipe.totalTime?.takeIf { it > 0 }?.let { "${it.toInt()} min" } ?: "N/A"
            if (!recipe.difficulty.isNullOrBlank()) {
                textViewDifficulty.text = recipe.difficulty
                textViewDifficulty.visibility = View.VISIBLE
                findViewById<ImageView>(R.id.imageViewDifficultyIcon).visibility = View.VISIBLE // Asumiendo que tienes un ImageView con este ID para el icono
            } else {
                textViewDifficulty.visibility = View.GONE
                findViewById<ImageView>(R.id.imageViewDifficultyIcon).visibility = View.GONE // Oculta también el icono
            }


            // --- Poblar Descripción ---
            if (!recipe.description.isNullOrBlank()) {
                textViewRecipeDescription.text = recipe.description
                textViewRecipeDescription.visibility = View.VISIBLE
            } else {
                textViewRecipeDescription.visibility = View.GONE
            }

            // --- Poblar Etiquetas (Chips) ---
            chipGroupLabels.removeAllViews()
            val allLabels = mutableListOf<String>()
            recipe.dietLabels?.let { allLabels.addAll(it) }
            recipe.healthLabels?.let { allLabels.addAll(it) }

            if (allLabels.isNotEmpty()) {
                textViewLabelsTitle.visibility = View.VISIBLE
                chipGroupLabels.visibility = View.VISIBLE
                allLabels.distinct().forEach { labelString ->
                    val chip = Chip(this)
                    chip.text = labelString
                    //chip.setStyle(com.google.android.material.R.style.Widget_Material3_Chip_Assist)
                    chipGroupLabels.addView(chip)
                }
            } else {
                textViewLabelsTitle.visibility = View.GONE
                chipGroupLabels.visibility = View.GONE
            }

            // --- Poblar Ingredientes ---
            textViewServingsInfoForIngredients.text = recipe.servings?.takeIf { it > 0 }?.let { "Para %.0f raciones".format(it) } ?: "Raciones no especificadas"
            if (!recipe.ingredientLines.isNullOrEmpty()) {
                textViewIngredientsDetail.text = recipe.ingredientLines.joinToString(separator = "\n") { "- $it" }
                textViewIngredientsDetail.visibility = View.VISIBLE
            } else {
                textViewIngredientsDetail.text = "Ingredientes no listados."
                textViewIngredientsDetail.visibility = View.VISIBLE // O GONE si prefieres ocultarlo
            }


            // --- Poblar Instrucciones ---
            if (!recipe.instructions.isNullOrEmpty()) {
                textViewInstructionsTitle.visibility = View.VISIBLE
                textViewInstructions.text = recipe.instructions.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n")
                textViewInstructions.visibility = View.VISIBLE
            } else {
                textViewInstructionsTitle.visibility = View.GONE
                textViewInstructions.visibility = View.GONE
            }

            // --- Poblar Información Nutricional Detallada ---
            // Esto es un ejemplo, necesitarás adaptarlo a cómo proceses recipe.totalNutrientsDetailed
            val nutritionBuilder = StringBuilder()
            recipe.totalNutrientsDetailed?.forEach { (key, nutrient) -> // Asumiendo que totalNutrientsDetailed es Map<String, NutrientDetail>
                // Filtra o formatea los nutrientes que quieras mostrar
                if (nutrient.label != null && nutrient.quantity != null && nutrient.unit != null) {
                    if (listOf("Energy", "Protein", "Fat", "Carbohydrate", "Fiber").any { nutrient.label.contains(it, ignoreCase = true)}) {
                        nutritionBuilder.append("${nutrient.label}: ${String.format(Locale.getDefault(), "%.1f", nutrient.quantity)} ${nutrient.unit}\n")
                    }
                }
            }
            if (nutritionBuilder.isNotBlank()) {
                textViewNutritionTitle.visibility = View.VISIBLE
                textViewNutritionInfoDetailed.text = nutritionBuilder.toString().trim()
                textViewNutritionInfoDetailed.visibility = View.VISIBLE
            } else {
                // Fallback a calorías simples si no hay detallada pero sí calorías
                recipe.calories?.let {
                    textViewNutritionTitle.visibility = View.VISIBLE
                    textViewNutritionInfoDetailed.text = "Calorías totales: ${String.format(Locale.getDefault(), "%.0f kcal", it)}"
                    textViewNutritionInfoDetailed.visibility = View.VISIBLE
                } ?: run {
                    textViewNutritionTitle.visibility = View.GONE
                    textViewNutritionInfoDetailed.visibility = View.GONE
                }
            }

            // --- Poblar Fuente y URL ---
            var sourceOrUrlExists = false
            if (!recipe.source.isNullOrBlank()) {
                textViewRecipeSourceDetail.text = "Fuente: ${recipe.source}"
                textViewRecipeSourceDetail.visibility = View.VISIBLE
                sourceOrUrlExists = true
            } else {
                textViewRecipeSourceDetail.visibility = View.GONE
            }

            if (!recipe.url.isNullOrBlank()) {
                textViewRecipeUrlDetail.text = recipe.url // El XML ya tiene autoLink, o puedes hacerlo programático
                textViewRecipeUrlDetail.movementMethod = LinkMovementMethod.getInstance()
                textViewRecipeUrlDetail.visibility = View.VISIBLE
                sourceOrUrlExists = true
            } else {
                textViewRecipeUrlDetail.visibility = View.GONE
            }

            if(sourceOrUrlExists) {
                textViewSourceUrlTitle.visibility = View.VISIBLE
            } else {
                textViewSourceUrlTitle.visibility = View.GONE
            }

            // --- Configurar FABs ---
            updateFavoriteIcon(recipe.isFavorite) // Asume que isFavorite se determina al cargar

            fabFavorite.setOnClickListener {
                recipe.isFavorite = !recipe.isFavorite // Cambia el estado local
                updateFavoriteIcon(recipe.isFavorite)
                // AQUÍ VA LA LÓGICA PARA GUARDAR/ELIMINAR DE FAVORITOS EN FIRESTORE
                val action = if (recipe.isFavorite) "añadida a" else "eliminada de"
                Toast.makeText(this, "Receta $action favoritos (Implementar Firestore)", Toast.LENGTH_SHORT).show()
            }

            fabAddToList.setOnClickListener {
                // AQUÍ VA LA LÓGICA PARA AÑADIR INGREDIENTES A LA LISTA DE LA COMPRA EN FIRESTORE
                Toast.makeText(this, "Añadir ingredientes a lista (Implementar Firestore)", Toast.LENGTH_SHORT).show()
                // Ejemplo de cómo podrías hacerlo:
                // recipe.ingredientLines?.forEach { ingredient ->
                //    viewModel.addShoppingListItem(ingredient) // Si tuvieras un ViewModel para esto
                // }
            }

        } else {
            collapsingToolbar.title = "Error"
            textViewRecipeLabelBody.text = "Error al cargar detalles"
            Toast.makeText(this, "No se pudieron cargar los detalles de la receta.", Toast.LENGTH_LONG).show()
            finish() // Cierra la actividad si no hay datos válidos
        }
    }

    private fun updateFavoriteIcon(isFav: Boolean) {
        if (isFav) {
            fabFavorite.setImageResource(R.drawable.ic_favfilled) // Icono de corazón relleno
        } else {
            fabFavorite.setImageResource(R.drawable.ic_favorite_borderfav) // Icono de corazón vacío
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

// Necesitarás añadir un ID al ImageView del icono de dificultad en tu XML para poder ocultarlo/mostrarlo:
// Ejemplo:
// <ImageView
//     android:id="@+id/imageViewDifficultyIcon" <--- AÑADE ESTE ID
//     android:layout_width="24dp"
//     ... />