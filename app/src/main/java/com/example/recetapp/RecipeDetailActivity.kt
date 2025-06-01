package com.example.recetapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

class RecipeDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RECIPE = "extra_recipe_data"
        private const val TAG = "RecipeDetailActivity" // Para logs
    }

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Variables para los FABs (para poder cambiar el icono de favorito)
    private lateinit var fabFavorite: FloatingActionButton
    private var currentRecipe: Recipe? = null // Para acceder a la receta en los listeners

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        // Inicializar Firebase Auth y Firestore
        auth = Firebase.auth
        db = Firebase.firestore

        // Configurar Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val collapsingToolbar: CollapsingToolbarLayout = findViewById(R.id.collapsingToolbar)

        // Referencias a las vistas (asumiendo que los IDs son los del XML que adaptamos)
        val imageViewRecipeDetail: ImageView = findViewById(R.id.imageViewRecipeDetail)
        val textViewRecipeLabelBody: TextView = findViewById(R.id.textViewRecipeLabelDetail_Body)

        val textViewCaloriesDetail: TextView = findViewById(R.id.textViewCaloriesDetail)
        val textViewTotalTimeDetail: TextView = findViewById(R.id.textViewTotalTimeDetail)
        val textViewDifficulty: TextView = findViewById(R.id.textViewDifficulty)
        val imageViewDifficultyIcon: ImageView = findViewById(R.id.imageViewDifficultyIcon) // Asegúrate que este ID existe en el XML

        val textViewRecipeDescription: TextView = findViewById(R.id.textViewRecipeDescription)
        val textViewLabelsTitle: TextView = findViewById(R.id.textViewLabelsTitle)
        val chipGroupLabels: ChipGroup = findViewById(R.id.chipGroupLabels)

        val textViewServingsInfoForIngredients: TextView = findViewById(R.id.textViewServingsInfoForIngredients)
        val textViewIngredientsDetail: TextView = findViewById(R.id.textViewIngredientsDetail)

        val textViewInstructionsTitle: TextView = findViewById(R.id.textViewInstructionsTitle)
        val textViewInstructions: TextView = findViewById(R.id.textViewInstructions)

        val textViewNutritionTitle: TextView = findViewById(R.id.textViewNutritionTitle)
        val textViewNutritionInfoDetailed: TextView = findViewById(R.id.textViewNutritionInfoDetailed)

        val textViewSourceUrlTitle: TextView = findViewById(R.id.textViewSourceUrlTitle)
        val textViewRecipeSourceDetail: TextView = findViewById(R.id.textViewRecipeSourceDetail)
        val textViewRecipeUrlDetail: TextView = findViewById(R.id.textViewRecipeUrlDetail)

        fabFavorite = findViewById(R.id.fabFavorite)
        val fabAddToList: FloatingActionButton = findViewById(R.id.fabAddToList)

        currentRecipe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RECIPE, Recipe::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RECIPE)
        }

        if (currentRecipe != null) {
            val recipe = currentRecipe!!

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

            if (!recipe.difficulty.isNullOrBlank()) { // Asumiendo que añadiste 'difficulty' a Recipe.kt
                textViewDifficulty.text = recipe.difficulty
                textViewDifficulty.visibility = View.VISIBLE
                imageViewDifficultyIcon.visibility = View.VISIBLE
            } else {
                textViewDifficulty.visibility = View.GONE
                imageViewDifficultyIcon.visibility = View.GONE
            }

            // --- Poblar Descripción ---
            if (!recipe.description.isNullOrBlank()) { // Asumiendo que añadiste 'description' a Recipe.kt
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
                    // No es necesario chip.setStyle(...) aquí si tu tema de Material 3 ya define estilos para Chip
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
                textViewIngredientsDetail.visibility = View.VISIBLE
            }

            // --- Poblar Instrucciones ---
            if (!recipe.instructions.isNullOrEmpty()) { // Asumiendo que añadiste 'instructions' a Recipe.kt
                textViewInstructionsTitle.visibility = View.VISIBLE
                textViewInstructions.text = recipe.instructions.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n")
                textViewInstructions.visibility = View.VISIBLE
            } else {
                textViewInstructionsTitle.visibility = View.GONE
                textViewInstructions.visibility = View.GONE
            }

            // --- Poblar Información Nutricional Detallada ---
            val nutritionBuilder = StringBuilder()
            // Asumiendo que 'totalNutrients' en tu Recipe.kt es Map<String, NutrientDetail>?
            // y que NutrientDetail tiene label, quantity, unit
            recipe.totalNutrients?.forEach { (key, nutrient) ->
                if (nutrient.label != null && nutrient.quantity != null && nutrient.unit != null) {
                    // Mostrar solo algunos nutrientes clave como ejemplo
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
                textViewRecipeUrlDetail.text = recipe.url
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
                recipe.isFavorite = !recipe.isFavorite
                updateFavoriteIcon(recipe.isFavorite)
                // AQUÍ VA LA LÓGICA PARA GUARDAR/ELIMINAR DE FAVORITOS EN FIRESTORE
                val action = if (recipe.isFavorite) "añadida a" else "eliminada de"
                Toast.makeText(this, "Receta $action favoritos (Implementar Firestore)", Toast.LENGTH_SHORT).show()
            }

            fabAddToList.setOnClickListener {
                addIngredientsToShoppingList(recipe)
            }

        } else {
            collapsingToolbar.title = "Error"
            textViewRecipeLabelBody.text = "Error al cargar detalles"
            Toast.makeText(this, "No se pudieron cargar los detalles de la receta.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun updateFavoriteIcon(isFav: Boolean) {
        if (isFav) {
            fabFavorite.setImageResource(R.drawable.ic_favorite) // Icono de corazón relleno
        } else {
            fabFavorite.setImageResource(R.drawable.ic_favorite_border) // Icono de corazón vacío
        }
    }

    private fun addIngredientsToShoppingList(recipe: Recipe) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Debes iniciar sesión para añadir a la lista.", Toast.LENGTH_SHORT).show()
            return
        }

        if (recipe.ingredientLines.isNullOrEmpty()) {
            Toast.makeText(this, "No hay ingredientes para añadir.", Toast.LENGTH_SHORT).show()
            return
        }

        val shoppingListCollection = db.collection("users").document(userId)
            .collection("shoppingListItems")

        var itemsAddedSuccessfully = 0
        var itemsFailedToAdd = 0
        val totalItemsToAttempt = recipe.ingredientLines.size

        // Deshabilitar botón mientras se procesa (opcional)
        // fabAddToList.isEnabled = false

        recipe.ingredientLines.forEach { ingredientName ->
            if (ingredientName.isNotBlank()) {
                val shoppingItemData = hashMapOf(
                    "name" to ingredientName, // El string completo del ingrediente
                    "isPurchased" to false,
                    "addedAt" to FieldValue.serverTimestamp()
                )

                // Usamos .add() para que Firestore genere un ID único para cada ítem
                shoppingListCollection.add(shoppingItemData)
                    .addOnSuccessListener {
                        itemsAddedSuccessfully++
                        if (itemsAddedSuccessfully + itemsFailedToAdd == totalItemsToAttempt) {
                            showShoppingListFeedback(itemsAddedSuccessfully, itemsFailedToAdd)
                        }
                    }
                    .addOnFailureListener { e ->
                        itemsFailedToAdd++
                        Log.w(TAG, "Error al añadir '$ingredientName' a la lista de compra", e)
                        if (itemsAddedSuccessfully + itemsFailedToAdd == totalItemsToAttempt) {
                            showShoppingListFeedback(itemsAddedSuccessfully, itemsFailedToAdd)
                        }
                    }
            } else {
                // Si un ingrediente está en blanco, se cuenta como fallo para el feedback final
                itemsFailedToAdd++
                if (itemsAddedSuccessfully + itemsFailedToAdd == totalItemsToAttempt) {
                    showShoppingListFeedback(itemsAddedSuccessfully, itemsFailedToAdd)
                }
            }
        }
    }

    private fun showShoppingListFeedback(successful: Int, failed: Int) {
        // Habilitar botón de nuevo (opcional)
        // fabAddToList.isEnabled = true

        if (successful > 0 && failed == 0) {
            Toast.makeText(this, "$successful ingrediente(s) añadido(s) a la lista.", Toast.LENGTH_LONG).show()
        } else if (successful > 0 && failed > 0) {
            Toast.makeText(this, "$successful ingrediente(s) añadido(s), $failed no se pudieron añadir.", Toast.LENGTH_LONG).show()
        } else if (successful == 0 && failed > 0) {
            Toast.makeText(this, "No se pudieron añadir los ingredientes a la lista.", Toast.LENGTH_LONG).show()
        }
        // No mostrar nada si no había ingredientes para intentar
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}