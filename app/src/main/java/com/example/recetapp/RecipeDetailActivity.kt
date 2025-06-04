package com.example.recetapp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri // Asegúrate de que esta importación esté presente
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString // Para subrayar el texto si quieres
import android.text.Spanned // Para subrayar el texto
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan // Para subrayar el texto
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import coil.load
import com.example.recetapp.data.NutrientDetail
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
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import java.util.Locale
import java.util.Date

// --- IMPORTACIONES DE ML KIT TRANSLATE ---
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class RecipeDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_RECIPE = "extra_recipe_data"
        private const val TAG = "RecipeDetailActivity"

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
        ).mapKeys { it.key.lowercase(Locale.ROOT) }


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
        ).mapKeys { it.key.lowercase(Locale.ROOT) }

        private val unitTranslations = mapOf(
            "g" to "g", "mg" to "mg", "µg" to "µg", "kcal" to "kcal", "%" to "%", "iu" to "UI"
        ).mapKeys { it.key.lowercase(Locale.ROOT) }
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fabFavorite: FloatingActionButton
    private var currentRecipe: Recipe? = null

    private var englishSpanishTranslator: Translator? = null
    private var isTranslationModelDownloaded = false

    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var imageViewRecipeDetail: ImageView
    private lateinit var textViewRecipeLabelBody: TextView
    private lateinit var textViewCaloriesDetailStat: TextView
    private lateinit var textViewTotalTimeDetailStat: TextView
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

        initializeViews()

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        initializeTranslator()

        currentRecipe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RECIPE, Recipe::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RECIPE)
        }

        if (currentRecipe != null) {
            checkIfFavorite(currentRecipe!!)
            populateUI(currentRecipe!!)
        } else {
            handleRecipeError()
        }
    }

    private fun initializeViews() {
        collapsingToolbar = findViewById(R.id.collapsingToolbar)
        imageViewRecipeDetail = findViewById(R.id.imageViewRecipeDetail)
        textViewRecipeLabelBody = findViewById(R.id.textViewRecipeLabelDetail_Body)
        textViewCaloriesDetailStat = findViewById(R.id.textViewCaloriesDetail)
        textViewTotalTimeDetailStat = findViewById(R.id.textViewTotalTimeDetail)
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
    }

    private fun initializeTranslator() {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.SPANISH)
            .build()
        englishSpanishTranslator = Translation.getClient(options)
        lifecycle.addObserver(englishSpanishTranslator!!)

        val conditions = DownloadConditions.Builder().build()

        Log.d(TAG, "Iniciando descarga del modelo de traducción ES (si es necesario)...")
        if (::textViewIngredientsDetail.isInitialized) {
            textViewIngredientsDetail.text = "Preparando traductor..."
        }

        englishSpanishTranslator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener {
                isTranslationModelDownloaded = true
                Log.d(TAG, "Modelo de traducción ES descargado o ya presente.")
                currentRecipe?.let { recipe ->
                    if (::textViewIngredientsDetail.isInitialized && !isDestroyed && !isFinishing) {
                        populateIngredientsUI(recipe)
                    }
                }
            }
            ?.addOnFailureListener { exception ->
                isTranslationModelDownloaded = false
                Log.e(TAG, "Error al descargar modelo de traducción ES.", exception)
                if (isFinishing || isDestroyed) return@addOnFailureListener

                Toast.makeText(this, "No se pudo descargar modelo para traducir.", Toast.LENGTH_LONG).show()
                currentRecipe?.let { recipe ->
                    if (::textViewIngredientsDetail.isInitialized && !isDestroyed && !isFinishing) {
                        populateIngredientsUI(recipe)
                    }
                }
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

        val servingsForStats = recipe.servings?.takeIf { it > 0f }
        val caloriesPerServingStat = if (servingsForStats != null && recipe.calories != null) {
            recipe.calories / servingsForStats
        } else { recipe.calories }
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

        textViewRecipeDescription.text = recipe.description
        textViewRecipeDescription.visibility = if (recipe.description.isNullOrBlank()) View.GONE else View.VISIBLE

        chipGroupLabels.removeAllViews()
        val allLabels = mutableListOf<String>()
        recipe.dietLabels?.let { allLabels.addAll(it) }
        recipe.healthLabels?.let { allLabels.addAll(it) }

        if (allLabels.isNotEmpty()) {
            textViewLabelsTitle.visibility = View.VISIBLE
            chipGroupLabels.visibility = View.VISIBLE
            allLabels.distinct().forEach { labelString ->
                val chip = Chip(ContextThemeWrapper(this, com.google.android.material.R.style.Widget_Material3_Chip_Assist))
                val translatedLabel = dietHealthLabelTranslations[labelString.lowercase(Locale.ROOT)] ?: labelString
                chip.text = translatedLabel
                chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#e9c46a"))
                chip.setTextColor(Color.BLACK)
                chipGroupLabels.addView(chip)
            }
        } else {
            textViewLabelsTitle.visibility = View.GONE
            chipGroupLabels.visibility = View.GONE
        }

        populateIngredientsUI(recipe)

        if (!recipe.instructions.isNullOrEmpty()) {
            textViewInstructionsTitle.visibility = View.VISIBLE
            textViewInstructions.text = recipe.instructions.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n")
            textViewInstructions.visibility = View.VISIBLE
        } else {
            textViewInstructionsTitle.visibility = View.GONE
            textViewInstructions.visibility = View.GONE
        }

        populateNutritionUI(recipe)

        var sourceOrUrlExists = false
        if (!recipe.source.isNullOrBlank()) {
            textViewRecipeSourceDetail.text = "Fuente: ${recipe.source}"
            textViewRecipeSourceDetail.visibility = View.VISIBLE
            sourceOrUrlExists = true
        } else { textViewRecipeSourceDetail.visibility = View.GONE }

        if (!recipe.url.isNullOrBlank()) {
            val urlText = recipe.url
            textViewRecipeUrlDetail.text = urlText // Mostrar la URL como texto
            textViewRecipeUrlDetail.visibility = View.VISIBLE
            sourceOrUrlExists = true

            // -----PARA HACER LA URL CLICKEABLE -----
            textViewRecipeUrlDetail.setOnClickListener {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlText))
                    startActivity(browserIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "No se pudo abrir la URL: $urlText", e)
                    Toast.makeText(this, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
                }
            }
            //subrayar el texto para que parezca más un enlace
            val spannableString = SpannableString(urlText)
            spannableString.setSpan(URLSpan(urlText), 0, urlText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            textViewRecipeUrlDetail.text = spannableString
            textViewRecipeUrlDetail.movementMethod = LinkMovementMethod.getInstance()


        } else {
            textViewRecipeUrlDetail.visibility = View.GONE
        }
        textViewSourceUrlTitle.visibility = if(sourceOrUrlExists) View.VISIBLE else View.GONE

        fabFavorite.setOnClickListener {
            currentRecipe?.let {
                if (it.isFavorite) {
                    removeRecipeFromFavorites(it)
                } else {
                    saveRecipeToFavorites(it)
                }
            }
        }

        fabAddToList.setOnClickListener {
            currentRecipe?.let {
                addIngredientsToShoppingList(it)
            }
        }
    }

    private fun populateIngredientsUI(recipe: Recipe) {
        if (!::textViewIngredientsDetail.isInitialized) {
            Log.e(TAG, "populateIngredientsUI: textViewIngredientsDetail no inicializada.")
            return
        }
        textViewServingsInfoForIngredients.text = recipe.servings?.takeIf { it > 0 }?.let { "Para %.0f raciones".format(it) } ?: "Raciones no especificadas"
        val ingredientLines = recipe.ingredientLines
        if (ingredientLines.isNullOrEmpty()) {
            textViewIngredientsDetail.text = "Ingredientes no listados."
            textViewIngredientsDetail.visibility = View.VISIBLE
            return
        }

        if (isTranslationModelDownloaded && englishSpanishTranslator != null) {
            if (textViewIngredientsDetail.text.toString().isBlank() || textViewIngredientsDetail.text.toString() == "Preparando traductor...") {
                textViewIngredientsDetail.text = "Traduciendo ingredientes..."
            }

            val translatedLinesArray = arrayOfNulls<String>(ingredientLines.size)
            var tasksCompleted = 0
            val totalTasksToExecute = ingredientLines.count { it.isNotBlank() }

            if (totalTasksToExecute == 0) {
                textViewIngredientsDetail.text = ingredientLines.joinToString(separator = "\n") { if (it.isBlank()) "" else "- $it" }.trim()
                Log.d(TAG, "No hay ingredientes válidos para traducir, mostrando originales/blancos.")
                textViewIngredientsDetail.visibility = View.VISIBLE
                return
            }

            ingredientLines.forEachIndexed { index, line ->
                translatedLinesArray[index] = if (line.isBlank()) "" else "- $line (Original)"
            }

            ingredientLines.forEachIndexed { index, line ->
                if (line.isNotBlank()) {
                    englishSpanishTranslator!!.translate(line)
                        .addOnSuccessListener { translatedText ->
                            translatedLinesArray[index] = "- $translatedText"
                        }
                        .addOnFailureListener { exception ->
                            Log.w(TAG, "Error al traducir línea: '$line'. Usando original.", exception)
                        }
                        .addOnCompleteListener {
                            tasksCompleted++
                            if (tasksCompleted == totalTasksToExecute) {
                                if (isDestroyed || isFinishing()) return@addOnCompleteListener
                                val finalText = translatedLinesArray.filterNotNull().joinToString("\n").trim()
                                textViewIngredientsDetail.text = finalText.ifEmpty { "No se pudieron traducir los ingredientes." }
                                Log.d(TAG, "Traducción de todos los ingredientes procesada.")
                            }
                        }
                } else {
                    tasksCompleted++ // Contar las líneas en blanco como tareas "completadas" para la lógica del contador
                    if (tasksCompleted == totalTasksToExecute) {
                        if (isDestroyed || isFinishing()) return@forEachIndexed
                        val finalText = translatedLinesArray.filterNotNull().joinToString("\n").trim()
                        textViewIngredientsDetail.text = finalText.ifEmpty { "No se pudieron traducir los ingredientes." }
                        Log.d(TAG, "Traducción de todos los ingredientes procesada (con líneas en blanco).")
                    }
                }
            }
        } else {
            Log.d(TAG, "Mostrando ingredientes originales (traductor no listo o descarga fallida).")
            textViewIngredientsDetail.text = ingredientLines.joinToString(separator = "\n") { "- $it" }
        }
        textViewIngredientsDetail.visibility = View.VISIBLE
    }

    private fun populateNutritionUI(recipe: Recipe){
        val nutritionBuilder = StringBuilder()
        val servingsForNutrition = recipe.servings?.takeIf { it > 0f }
        val nutritionSectionTitleText: String = if (servingsForNutrition != null) {
            "Información Nutricional (por porción)"
        } else {
            "Información Nutricional (Total Receta)"
        }
        textViewNutritionTitle.text = nutritionSectionTitleText

        recipe.totalNutrients?.forEach { (_, nutrientDetail) ->
            if (nutrientDetail.label != null && nutrientDetail.quantity != null && nutrientDetail.unit != null) {
                val nutrientLabelInLower = nutrientDetail.label.lowercase(Locale.ROOT)
                val translatedNutrientName = nutrientNameTranslations[nutrientLabelInLower] ?: nutrientDetail.label
                val translatedUnit = unitTranslations[nutrientDetail.unit.lowercase(Locale.ROOT)] ?: nutrientDetail.unit
                var quantityToShow = nutrientDetail.quantity
                if (servingsForNutrition != null) {
                    quantityToShow /= servingsForNutrition
                }
                if (nutrientNameTranslations.containsKey(nutrientLabelInLower) ||
                    listOf("energy", "protein", "fat", "total lipid (fat)", "carbohydrate, by difference", "carbs", "fiber", "fiber, total dietary", "sugars, total including nlea", "sugar").any { it == nutrientLabelInLower}
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
            } else {
                caloriesFallback = recipe.calories
                caloriesFallbackLabel = "Calorías Totales:"
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
    }

    private fun handleRecipeError() {
        if (!isFinishing && !isDestroyed) {
            if (::collapsingToolbar.isInitialized) { collapsingToolbar.title = "Error" }
            if (::textViewRecipeLabelBody.isInitialized) { textViewRecipeLabelBody.text = "Error al cargar detalles" }
            Toast.makeText(this, "No se pudieron cargar los detalles.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun updateFavoriteIcon(isFav: Boolean) {
        if(::fabFavorite.isInitialized) {
            if (isFav) {
                fabFavorite.setImageResource(R.drawable.ic_favorite)
                fabFavorite.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFC107")) // Amarillo
            } else {
                fabFavorite.setImageResource(R.drawable.ic_favorite_border)
                val typedValue = TypedValue()
                theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true)
                fabFavorite.backgroundTintList = ColorStateList.valueOf(typedValue.data)
                theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimaryContainer, typedValue, true)
                fabFavorite.imageTintList = ColorStateList.valueOf(typedValue.data)
            }
        }
    }

    private fun addIngredientsToShoppingList(recipe: Recipe) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Debes iniciar sesión para añadir a la lista.", Toast.LENGTH_SHORT).show()
            return
        }

        val originalIngredientLines = recipe.ingredientLines
        if (originalIngredientLines.isNullOrEmpty()) {
            Toast.makeText(this, "No hay ingredientes para añadir.", Toast.LENGTH_SHORT).show()
            return
        }

        val linesToProcess = originalIngredientLines.filter { it.isNotBlank() }
        if (linesToProcess.isEmpty()) {
            Toast.makeText(this, "No hay ingredientes válidos para añadir.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Preparando ingredientes para la lista...", Toast.LENGTH_SHORT).show()

        if (isTranslationModelDownloaded && englishSpanishTranslator != null) {
            val translatedItemsForFirestore = mutableListOf<String>()
            var translationTasksCompleted = 0
            val totalTranslationTasks = linesToProcess.size

            Log.d(TAG, "addIngredientsToShoppingList: Intentando traducir ${totalTranslationTasks} ingredientes.")

            linesToProcess.forEach { line ->
                englishSpanishTranslator!!.translate(line)
                    .addOnSuccessListener { translatedText ->
                        Log.d(TAG, "addIngredientsToShoppingList: Traducido '$line' a '$translatedText'")
                        translatedItemsForFirestore.add(translatedText.trim())
                    }
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "addIngredientsToShoppingList: Error al traducir '$line'. Usando original.", exception)
                        translatedItemsForFirestore.add(line.trim())
                    }
                    .addOnCompleteListener {
                        translationTasksCompleted++
                        if (translationTasksCompleted == totalTranslationTasks) {
                            if (!isDestroyed && !isFinishing()) {
                                commitIngredientsToFirestoreBatch(translatedItemsForFirestore, recipe, userId)
                            }
                        }
                    }
            }
        } else {
            Log.d(TAG, "addIngredientsToShoppingList: Traductor no disponible. Añadiendo ingredientes originales.")
            val itemsInOriginalLanguage = linesToProcess.map { it.trim() }
            commitIngredientsToFirestoreBatch(itemsInOriginalLanguage, recipe, userId)
        }
    }

    private fun commitIngredientsToFirestoreBatch(itemsToAdd: List<String>, recipe: Recipe, userId: String) {
        if (itemsToAdd.isEmpty()){
            Log.d(TAG, "commitIngredientsToFirestoreBatch: No hay items finales para añadir después del procesamiento.")
            if (!isDestroyed && !isFinishing()) Toast.makeText(this, "No se añadieron ingredientes (lista vacía después de procesar).", Toast.LENGTH_SHORT).show()
            return
        }

        val shoppingListCollection = db.collection("users").document(userId).collection("shoppingListItems")
        val batch = db.batch()
        var itemsAttemptedInBatch = 0

        Log.d(TAG, "commitIngredientsToFirestoreBatch: Añadiendo los siguientes items al batch: ${itemsToAdd.joinToString { "'$it'" }}")

        itemsToAdd.forEach { ingredientName ->
            if (ingredientName.isNotBlank()) {
                itemsAttemptedInBatch++
                val shoppingItemData = hashMapOf(
                    "name" to ingredientName,
                    "isPurchased" to false,
                    "recipeId" to recipe.uri,
                    "recipeName" to recipe.label,
                    "addedAt" to FieldValue.serverTimestamp()
                )
                val newDocRef = shoppingListCollection.document()
                batch.set(newDocRef, shoppingItemData)
            }
        }

        if (itemsAttemptedInBatch == 0) {
            if (!isDestroyed && !isFinishing()) Toast.makeText(this, "No hay ingredientes válidos para añadir al final.", Toast.LENGTH_SHORT).show()
            return
        }

        batch.commit()
            .addOnSuccessListener {
                if (!isDestroyed && !isFinishing()) {
                    Log.d(TAG, "$itemsAttemptedInBatch ingrediente(s) de '${recipe.label}' procesados y añadidos a la lista de la compra.")
                    Toast.makeText(this, "$itemsAttemptedInBatch ingrediente(s) añadido(s) a la lista de la compra.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                if (!isDestroyed && !isFinishing()) {
                    Log.w(TAG, "Error al añadir ingredientes de '${recipe.label}' a la lista con batch.", e)
                    Toast.makeText(this, "Error al añadir ingredientes a la lista: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun getRecipeId(recipe: Recipe): String {
        return recipe.uri?.substringAfterLast('#')
            ?: recipe.label!!.replace(Regex("[/\\\\#\\[\\]*?.:$]"), "_")
    }

    private fun checkIfFavorite(recipe: Recipe) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            currentRecipe?.isFavorite = false
            updateFavoriteIcon(false)
            return
        }
        val recipeId = getRecipeId(recipe)

        db.collection("users").document(userId)
            .collection("favoriteRecipes").document(recipeId)
            .get()
            .addOnSuccessListener { document ->
                if (!isDestroyed && !isFinishing()) {
                    val isFav = document.exists()
                    currentRecipe?.isFavorite = isFav
                    updateFavoriteIcon(isFav)
                }
            }
            .addOnFailureListener {e ->
                if (!isDestroyed && !isFinishing()) {
                    Log.w(TAG, "Error al comprobar favorito, asumiendo que no lo es.", e)
                    currentRecipe?.isFavorite = false
                    updateFavoriteIcon(false)
                }
            }
    }

    private fun saveRecipeToFavorites(recipe: Recipe) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            if (!isDestroyed && !isFinishing()) Toast.makeText(this, "Debes iniciar sesión para guardar favoritos", Toast.LENGTH_SHORT).show()
            return
        }
        val recipeId = getRecipeId(recipe)
        val recipeToSave = recipe.copy(isFavorite = true)

        db.collection("users").document(userId)
            .collection("favoriteRecipes").document(recipeId)
            .set(recipeToSave)
            .addOnSuccessListener {
                if (!isDestroyed && !isFinishing()) {
                    Log.d(TAG, "Receta añadida a favoritos en Firestore.")
                    currentRecipe?.isFavorite = true
                    updateFavoriteIcon(true)
                    Toast.makeText(this, "Guardada en favoritos", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                if (!isDestroyed && !isFinishing()) {
                    Log.w(TAG, "Error al guardar receta en favoritos", e)
                    Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun removeRecipeFromFavorites(recipe: Recipe) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            if (!isDestroyed && !isFinishing()) Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }
        val recipeId = getRecipeId(recipe)

        db.collection("users").document(userId)
            .collection("favoriteRecipes").document(recipeId)
            .delete()
            .addOnSuccessListener {
                if (!isDestroyed && !isFinishing()) {
                    Log.d(TAG, "Receta eliminada de favoritos en Firestore.")
                    currentRecipe?.isFavorite = false
                    updateFavoriteIcon(false)
                    Toast.makeText(this, "Eliminada de favoritos", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                if (!isDestroyed && !isFinishing()) {
                    Log.w(TAG, "Error al eliminar receta de favoritos", e)
                    Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // Esto ya llama a onBackPressed() si no hay nada más en la pila de back
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    override fun onDestroy() {
        englishSpanishTranslator?.close()
        super.onDestroy()
    }
}