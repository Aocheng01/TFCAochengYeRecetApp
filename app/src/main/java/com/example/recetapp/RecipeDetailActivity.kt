package com.example.recetapp // O el paquete donde tengas tus actividades

import android.os.Build // Para comprobar la versión del SDK
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod // Para hacer links clickeables
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import coil.load // Para cargar imágenes con Coil
import com.example.recetapp.data.Recipe // Importa tu clase Recipe Parcelable
import java.util.Locale // Para formatear números como las calorías

class RecipeDetailActivity : AppCompatActivity() {

    // Constante para usar como clave al pasar datos en el Intent
    companion object {
        const val EXTRA_RECIPE = "extra_recipe_data"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail) // Asegúrate que este layout existe

        // Referencias a las vistas del layout de detalle
        val imageView: ImageView = findViewById(R.id.imageViewRecipeDetail)
        val labelTextView: TextView = findViewById(R.id.textViewRecipeLabelDetail)
        val sourceTextView: TextView = findViewById(R.id.textViewRecipeSourceDetail)
        val urlTextView: TextView = findViewById(R.id.textViewRecipeUrlDetail)
        val ingredientsTextView: TextView = findViewById(R.id.textViewIngredientsDetail)
        val caloriesTextView: TextView = findViewById(R.id.textViewCaloriesDetail)
        val yieldTextView: TextView = findViewById(R.id.textViewYieldDetail)
        // Añade más findViewById si tienes más vistas en tu layout (ej. totalTimeTextView)

        // Obtener el objeto Recipe del Intent
        val recipe: Recipe? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RECIPE, Recipe::class.java)
        } else {
            @Suppress("DEPRECATION") // Suprime la advertencia para getParcelableExtra antiguo
            intent.getParcelableExtra(EXTRA_RECIPE)
        }

        if (recipe != null) {
            // Establecer el título de la ActionBar (opcional)
            supportActionBar?.title = recipe.label ?: "Detalle de Receta"
            // Habilitar el botón "Atrás" en la ActionBar
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)


            // Poblar las vistas con los datos de la receta
            imageView.load(recipe.image) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder_image) // Asegúrate de tener este drawable
                error(R.drawable.ic_error_image)         // Asegúrate de tener este drawable
            }

            labelTextView.text = recipe.label ?: "Nombre no disponible"
            sourceTextView.text = "Fuente: ${recipe.source ?: "N/A"}"

            // Hacer el URL clickeable
            if (recipe.url != null) {
                urlTextView.text = recipe.url
                urlTextView.movementMethod = LinkMovementMethod.getInstance()
            } else {
                urlTextView.text = "URL no disponible"
            }

            // Formatear y mostrar la lista de ingredientes
            if (!recipe.ingredientLines.isNullOrEmpty()) {
                ingredientsTextView.text = recipe.ingredientLines.joinToString(separator = "\n- ", prefix = "- ")
            } else {
                ingredientsTextView.text = "No hay ingredientes listados."
            }

            // Mostrar calorías y porciones (formateando los números)
            caloriesTextView.text = String.format(Locale.getDefault(), "Calorías: %.0f kcal", recipe.calories ?: 0.0)
            yieldTextView.text = String.format(Locale.getDefault(), "Porciones: %.0f", recipe.yield ?: 0.0)

            // Ejemplo para mostrar totalTime si lo tienes en tu layout y data class
            // val totalTimeTextView: TextView = findViewById(R.id.textViewTotalTimeDetail) // Si tuvieras este ID
            // totalTimeTextView.text = String.format(Locale.getDefault(), "Tiempo total: %.0f min", recipe.totalTime ?: 0.0)

        } else {
            // Manejar el caso donde la receta es nula (no debería ocurrir si se envía correctamente)
            labelTextView.text = "Error al cargar detalles"
            Toast.makeText(this, "No se pudieron cargar los detalles de la receta.", Toast.LENGTH_LONG).show()
            // Opcionalmente, podrías cerrar la actividad si no hay datos: finish()
        }
    }

    // Maneja el clic en el botón "Atrás" de la ActionBar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // Comportamiento estándar de "Atrás"
        return true
    }
}