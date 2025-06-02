package com.example.recetapp

import android.content.Intent // Asegúrate de que esté para Uri
import android.net.Uri // Para abrir URLs
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar // Importa Toolbar
import coil.load
import com.example.recetapp.data.NutrientDetail // Importa NutrientDetail
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
        private const val TAG = "RecipeDetailActivity"

        // --- MAPAS PARA TRADUCCIÓN MANUAL ---
        private val dietHealthLabelTranslations = mapOf(
            "balanced" to "Equilibrado", "high-fiber" to "Alto en Fibra", "high-protein" to "Alto en Proteínas",
            "low-carb" to "Bajo en Carbohidratos", "low-fat" to "Bajo en Grasas", "low-sodium" to "Bajo en Sodio",
            "alcohol-cocktail" to "Cóctel con Alcohol", "alcohol-free" to "Sin Alcohol", "celery-free" to "Sin Apio",
            "crustacean-free" to "Sin Crustáceos", "dairy-free" to "Sin Lácteos", "egg-free" to "Sin Huevo",
            "fish-free" to "Sin Pescado", "fodmap-free" to "Sin FODMAP", "gluten-free" to "Sin Gluten",
            "immuno-supportive" to "Refuerzo Inmunológico", "keto-friendly" to "Apto Dieta Keto",
            "kidney-friendly" to "Apto para Riñón", "kosher" to "Kosher", "low potassium" to "Bajo en Potasio",
            "low sugar" to "Bajo en Azúcar", "lupine-free" to "Sin Altramuces", "mediterranean" to "Mediterráneo",
            "mollusk-free" to "Sin Moluscos", "mustard-free" to "Sin Mostaza", "no oil added" to "Sin Aceite Añadido",
            "paleo" to "Paleo", "peanut-free" to "Sin Cacahuetes", "pescatarian" to "Pescetariano",
            "pork-free" to "Sin Cerdo", "red-meat-free" to "Sin Carne Roja", "sesame-free" to "Sin Sésamo",
            "shellfish-free" to "Sin Mariscos", "soy-free" to "Sin Soja", "sugar-conscious" to "Bajo en Azúcares",
            "sulfite-free" to "Sin Sulfitos", "tree-nut-free" to "Sin Frutos Secos de Árbol",
            "vegan" to "Vegano", "vegetarian" to "Vegetariano", "wheat-free" to "Sin Trigo"
            // COMPLETA ESTA LISTA CON LOS VALORES EXACTOS DE LA API
        )

        private val nutrientNameTranslations = mapOf(
            "energy" to "Energía", "fat" to "Grasas Totales", "total lipid (fat)" to "Grasas Totales",
            "saturated" to "Grasas Saturadas", "fatty acids, total saturated" to "Grasas Saturadas",
            "trans" to "Grasas Trans", "fatty acids, total trans" to "Grasas Trans",
            "cholesterol" to "Colesterol", "sodium" to "Sodio",
            "carbohydrate, by difference" to "Carbohidratos", "carbs" to "Carbohidratos",
            "carbohydrate (net)" to "Carbohidratos (Netos)", "fiber" to "Fibra",
            "fiber, total dietary" to "Fibra Dietética Total", "sugars, total including nlea" to "Azúcares Totales",
            "sugar" to "Azúcares", "sugars, added" to "Azúcares Añadidos", "protein" to "Proteínas",
            "vitamin a" to "Vitamina A", "vitamin c" to "Vitamina C", "vitamin d" to "Vitamina D",
            "vitamin e" to "Vitamina E", "vitamin k" to "Vitamina K", "thiamin" to "Tiamina (B1)",
            "riboflavin" to "Riboflavina (B2)", "niacin" to "Niacina (B3)", "vitamin b6" to "Vitamina B6",
            "folate" to "Folato (B9)", "vitamin b12" to "Vitamina B12", "calcium" to "Calcio",
            "iron" to "Hierro", "magnesium" to "Magnesio", "phosphorus" to "Fósforo",
            "potassium" to "Potasio", "zinc" to "Zinc", "water" to "Agua"
            // COMPLETA ESTA LISTA CON LOS NOMBRES DE NUTRIENTES DE LA API (EN MINÚSCULAS COMO CLAVE)
        )

        private val unitTranslations = mapOf(
            "g" to "g", "mg" to "mg", "µg" to "µg", "kcal" to "kcal", "%" to "%", "iu" to "UI"
        )
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fabFavorite: FloatingActionButton
    private var currentRecipe: Recipe? = null

    // Referencias a las vistas (para evitar múltiples findViewById)
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var imageViewRecipeDetail: ImageView
    private lateinit var textViewRecipeLabelBody: TextView
    private lateinit var textViewCaloriesDetailStat: TextView // Renombrado para claridad con el de nutrición
    private lateinit var textViewTotalTimeDetailStat: TextView // Renombrado para claridad
    private lateinit var textViewDifficulty: TextView
    private lateinit var imageViewDifficultyIcon: ImageView
    private lateinit var textViewRecipeDescription: TextView
    private lateinit var textViewLabelsTitle: TextView
    private lateinit var chipGroupLabels: ChipGroup
    private lateinit var textViewServingsInfoForIngredients: TextView
    private lateinit var textViewIngredientsDetail: TextView
    private lateinit var textViewInstructionsTitle: TextView
    private lateinit var textViewInstructions: TextView
    private lateinit var textViewNutritionTitle: TextView
    private lateinit var textViewNutritionInfoDetailed: TextView
    private lateinit var textViewSourceUrlTitle: TextView
    private lateinit var textViewRecipeSourceDetail: TextView
    private lateinit var textViewRecipeUrlDetail: TextView
    private lateinit var fabAddToList: FloatingActionButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        auth = Firebase.auth
        db = Firebase.firestore

        // Inicializar Vistas
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        collapsingToolbar = findViewById(R.id.collapsingToolbar)
        imageViewRecipeDetail = findViewById(R.id.imageViewRecipeDetail)
        textViewRecipeLabelBody = findViewById(R.id.textViewRecipeLabelDetail_Body)
        textViewCaloriesDetailStat = findViewById(R.id.textViewCaloriesDetail) // ID del XML para stats
        textViewTotalTimeDetailStat = findViewById(R.id.textViewTotalTimeDetail) // ID del XML para stats
        textViewDifficulty = findViewById(R.id.textViewDifficulty)
        imageViewDifficultyIcon = findViewById(R.id.imageViewDifficultyIcon)
        textViewRecipeDescription = findViewById(R.id.textViewRecipeDescription)
        textViewLabelsTitle = findViewById(R.id.textViewLabelsTitle)
        chipGroupLabels = findViewById(R.id.chipGroupLabels)
        textViewServingsInfoForIngredients = findViewById(R.id.textViewServingsInfoForIngredients)
        textViewIngredientsDetail = findViewById(R.id.textViewIngredientsDetail)
        textViewInstructionsTitle = findViewById(R.id.textViewInstructionsTitle)
        textViewInstructions = findViewById(R.id.textViewInstructions)
        textViewNutritionTitle = findViewById(R.id.textViewNutritionTitle)
        textViewNutritionInfoDetailed = findViewById(R.id.textViewNutritionInfoDetailed)
        textViewSourceUrlTitle = findViewById(R.id.textViewSourceUrlTitle)
        textViewRecipeSourceDetail = findViewById(R.id.textViewRecipeSourceDetail)
        textViewRecipeUrlDetail = findViewById(R.id.textViewRecipeUrlDetail)
        fabFavorite = findViewById(R.id.fabFavorite)
        fabAddToList = findViewById(R.id.fabAddToList)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        currentRecipe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RECIPE, Recipe::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RECIPE)
        }

        if (currentRecipe != null) {
            val recipe = currentRecipe!!
            populateUI(recipe) // Llamada a función para poblar la UI
        } else {
            handleRecipeError()
        }
    }

    private fun populateUI(recipe: Recipe) {
        collapsingToolbar.title = recipe.label ?: "Detalle de Receta"
        textViewRecipeLabelBody.text = recipe.label ?: "N/A"

        imageViewRecipeDetail.load(recipe.image) {
            crossfade(true)
            placeholder(R.drawable.ic_placeholder_image)
            error(R.drawable.ic_error_image)
        }

        // --- Poblar Estadísticas (calculando calorías por porción si es posible) ---
        val servingsForStats = recipe.servings?.takeIf { it > 0f }
        val caloriesPerServingStat = if (servingsForStats != null && recipe.calories != null) {
            recipe.calories / servingsForStats
        } else {
            recipe.calories // Mostrar total si no hay porciones
        }
        val caloriesStatLabel = if (servingsForStats != null) "kcal/porción" else "kcal total"
        textViewCaloriesDetailStat.text = caloriesPerServingStat?.let { String.format(Locale.getDefault(), "%.0f $caloriesStatLabel", it) } ?: "N/A"
        textViewTotalTimeDetailStat.text = recipe.totalTime?.takeIf { it > 0 }?.let { "${it.toInt()} min" } ?: "N/A"

        if (!recipe.difficulty.isNullOrBlank()) {
            textViewDifficulty.text = recipe.difficulty
            textViewDifficulty.visibility = View.VISIBLE
            imageViewDifficultyIcon.visibility = View.VISIBLE
        } else {
            textViewDifficulty.visibility = View.GONE
            imageViewDifficultyIcon.visibility = View.GONE
        }

        // --- Poblar Descripción ---
        textViewRecipeDescription.text = recipe.description
        textViewRecipeDescription.visibility = if (recipe.description.isNullOrBlank()) View.GONE else View.VISIBLE

        // --- Poblar Etiquetas (Chips) - CON TRADUCCIÓN ---
        chipGroupLabels.removeAllViews()
        val allLabels = mutableListOf<String>()
        recipe.dietLabels?.let { allLabels.addAll(it) }
        recipe.healthLabels?.let { allLabels.addAll(it) }

        if (allLabels.isNotEmpty()) {
            textViewLabelsTitle.visibility = View.VISIBLE
            chipGroupLabels.visibility = View.VISIBLE
            allLabels.distinct().forEach { labelString ->
                val chip = Chip(this)
                val translatedLabel = dietHealthLabelTranslations[labelString.lowercase(Locale.ROOT)] ?: labelString
                chip.text = translatedLabel
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
        if (!recipe.instructions.isNullOrEmpty()) {
            textViewInstructionsTitle.visibility = View.VISIBLE
            textViewInstructions.text = recipe.instructions.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n")
            textViewInstructions.visibility = View.VISIBLE
        } else {
            textViewInstructionsTitle.visibility = View.GONE
            textViewInstructions.visibility = View.GONE
        }

        // --- Poblar Información Nutricional Detallada - CON TRADUCCIÓN Y POR PORCIÓN ---
        val nutritionBuilder = StringBuilder()
        val servingsForNutrition = recipe.servings?.takeIf { it > 0f }

        val nutritionSectionTitleText: String
        if (servingsForNutrition != null) {
            nutritionSectionTitleText = "Información Nutricional (por porción)"
        } else {
            nutritionSectionTitleText = "Información Nutricional (Total Receta)"
        }
        textViewNutritionTitle.text = nutritionSectionTitleText

        recipe.totalNutrients?.forEach { (_, nutrientDetail) -> // Clave del mapa no usada aquí, solo el objeto NutrientDetail
            if (nutrientDetail.label != null && nutrientDetail.quantity != null && nutrientDetail.unit != null) {
                val nutrientLabelInLower = nutrientDetail.label.lowercase(Locale.ROOT)
                val translatedNutrientName = nutrientNameTranslations[nutrientLabelInLower] ?: nutrientDetail.label
                val translatedUnit = unitTranslations[nutrientDetail.unit.lowercase(Locale.ROOT)] ?: nutrientDetail.unit

                var quantityToShow = nutrientDetail.quantity
                if (servingsForNutrition != null) {
                    quantityToShow /= servingsForNutrition
                }

                if (nutrientNameTranslations.containsKey(nutrientLabelInLower) ||
                    listOf("energy", "protein", "fat", "total lipid (fat)", "carbohydrate, by difference", "carbs", "fiber", "fiber, total dietary").contains(nutrientLabelInLower)
                ) {
                    nutritionBuilder.append("${translatedNutrientName}: ${String.format(Locale.getDefault(), "%.1f", quantityToShow)} ${translatedUnit}\n")
                }
            }
        }

        if (nutritionBuilder.isNotBlank()) {
            textViewNutritionTitle.visibility = View.VISIBLE
            textViewNutritionInfoDetailed.text = nutritionBuilder.toString().trim()
            textViewNutritionInfoDetailed.visibility = View.VISIBLE
        } else {
            val caloriesFallback: Double?
            val caloriesFallbackLabel: String

            if (servingsForNutrition != null && recipe.calories != null) {
                caloriesFallback = recipe.calories / servingsForNutrition
                caloriesFallbackLabel = "Calorías (por porción):"
                textViewNutritionTitle.text = "Información Nutricional (por porción)"
            } else {
                caloriesFallback = recipe.calories
                caloriesFallbackLabel = "Calorías Totales:"
                textViewNutritionTitle.text = "Información Nutricional (Total Receta)"
            }

            caloriesFallback?.let {
                textViewNutritionTitle.visibility = View.VISIBLE
                textViewNutritionInfoDetailed.text = "$caloriesFallbackLabel ${String.format(Locale.getDefault(), "%.0f kcal", it)}"
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
        textViewSourceUrlTitle.visibility = if(sourceOrUrlExists) View.VISIBLE else View.GONE


        // --- Configurar FABs ---
        updateFavoriteIcon(recipe.isFavorite)
        fabFavorite.setOnClickListener {
            recipe.isFavorite = !recipe.isFavorite
            updateFavoriteIcon(recipe.isFavorite)
            val action = if (recipe.isFavorite) "añadida a" else "eliminada de"
            Toast.makeText(this, "Receta $action favoritos (Implementar Firestore)", Toast.LENGTH_SHORT).show()
            // Aquí iría la lógica de Firestore para favoritos
        }
        fabAddToList.setOnClickListener {
            addIngredientsToShoppingList(recipe)
        }
    }

    private fun handleRecipeError() {
        collapsingToolbar.title = "Error"
        textViewRecipeLabelBody.text = "Error al cargar detalles" // Asegúrate que textViewRecipeLabelBody esté inicializado antes
        Toast.makeText(this, "No se pudieron cargar los detalles.", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun updateFavoriteIcon(isFav: Boolean) {
        if (isFav) {
            fabFavorite.setImageResource(R.drawable.ic_favorite)
        } else {
            fabFavorite.setImageResource(R.drawable.ic_favorite_border)
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

        Log.d(TAG, "addIngredientsToShoppingList - Recipe URI: '${recipe.uri}', Recipe Label: '${recipe.label}'")
        val shoppingListCollection = db.collection("users").document(userId).collection("shoppingListItems")
        val batch = db.batch()
        var itemsToAttemptCount = 0

        recipe.ingredientLines.forEach { ingredientName ->
            if (ingredientName.isNotBlank()) {
                itemsToAttemptCount++
                Log.d(TAG, "Preparando para añadir: '$ingredientName'. Usando RecipeId: '${recipe.uri}', RecipeName: '${recipe.label}'")
                val shoppingItemData = hashMapOf(
                    "name" to ingredientName.trim(),
                    "isPurchased" to false,
                    "recipeId" to recipe.uri,
                    "recipeName" to recipe.label,
                    "addedAt" to FieldValue.serverTimestamp()
                )
                val newDocRef = shoppingListCollection.document()
                batch.set(newDocRef, shoppingItemData)
            }
        }

        if (itemsToAttemptCount == 0) {
            Toast.makeText(this, "No hay ingredientes válidos para añadir.", Toast.LENGTH_SHORT).show()
            return
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "$itemsToAttemptCount ingredientes de '${recipe.label}' procesados para añadir a la lista.")
                Toast.makeText(this, "$itemsToAttemptCount ingrediente(s) añadido(s) a la lista.", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al añadir ingredientes de '${recipe.label}' a la lista con batch.", e)
                Toast.makeText(this, "Error al añadir ingredientes: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}