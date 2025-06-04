package com.example.recetapp.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button // Import Button
import android.widget.TextView
import android.widget.Toast // Import Toast
import androidx.appcompat.app.AlertDialog // Import AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetapp.R
import com.example.recetapp.RecipeDetailActivity
import com.example.recetapp.adapters.RecipeAdapter
import com.example.recetapp.data.Hit
import com.example.recetapp.data.Recipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

class FavoritesFragment : Fragment() {

    private val TAG = "FavoritesFragment"

    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var textViewEmpty: TextView
    private lateinit var buttonDeleteAllFavorites: Button // NUEVA PROPIEDAD PARA EL BOTÓN

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var favoritesListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)

        auth = Firebase.auth
        db = Firebase.firestore

        recyclerView = view.findViewById(R.id.recyclerViewFavoriteRecipes)
        textViewEmpty = view.findViewById(R.id.textViewFavoritesEmpty)
        buttonDeleteAllFavorites = view.findViewById(R.id.buttonDeleteAllFavorites) // INICIALIZAR BOTÓN

        setupRecyclerView()
        setupListeners() // NUEVO MÉTODO PARA CONFIGURAR LISTENERS

        return view
    }

    // NUEVO MÉTODO PARA CONFIGURAR LISTENERS
    private fun setupListeners() {
        buttonDeleteAllFavorites.setOnClickListener {
            showDeleteAllConfirmationDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            attachFavoritesListener()
        } else {
            Log.w(TAG, "Usuario no logueado, no se puede mostrar favoritos.")
            if(::recipeAdapter.isInitialized) {
                recipeAdapter.submitNewList(emptyList())
            }
            updateEmptyViewVisibility()
        }
    }

    override fun onStop() {
        super.onStop()
        favoritesListener?.remove()
        Log.d(TAG, "Favorites listener removed.")
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(mutableListOf()) { recipe ->
            openRecipeDetail(recipe)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = recipeAdapter
    }

    private fun attachFavoritesListener() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "Cannot attach listener, user is not logged in.")
            if(::recipeAdapter.isInitialized) recipeAdapter.submitNewList(emptyList())
            updateEmptyViewVisibility()
            return
        }

        favoritesListener?.remove()

        favoritesListener = db.collection("users").document(userId)
            .collection("favoriteRecipes")
            .orderBy("label", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Error listening for favorite recipes.", e)
                    return@addSnapshotListener
                }

                val favoriteRecipes = mutableListOf<Recipe>()
                snapshots?.forEach { document ->
                    try {
                        val recipe = document.toObject<Recipe>()
                        recipe.isFavorite = true // Asegurar que se marca como favorita
                        favoriteRecipes.add(recipe)
                    } catch (ex: Exception) {
                        Log.e(TAG, "Error al convertir receta favorita: ${document.id}", ex)
                    }
                }

                val hits = favoriteRecipes.map { Hit(recipe = it) }
                if(::recipeAdapter.isInitialized) {
                    recipeAdapter.submitNewList(hits)
                }
                updateEmptyViewVisibility()
            }
    }

    private fun updateEmptyViewVisibility() {
        if (!isAdded || !::recipeAdapter.isInitialized) return // Evitar crashes si el fragmento no está añadido

        val isEmpty = recipeAdapter.itemCount == 0
        textViewEmpty.isVisible = isEmpty
        recyclerView.isVisible = !isEmpty
        if(::buttonDeleteAllFavorites.isInitialized) { // Asegurar que el botón está inicializado
            buttonDeleteAllFavorites.isVisible = !isEmpty
        }
    }

    private fun openRecipeDetail(recipe: Recipe) {
        if (!isAdded) return
        val intent = Intent(activity, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeDetailActivity.EXTRA_RECIPE, recipe)
        }
        startActivity(intent)
    }

    // ----- MÉTODOS PARA BORRAR TODOS LOS FAVORITOS -----
    private fun showDeleteAllConfirmationDialog() {
        if (!isAdded) return

        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que quieres eliminar TODAS tus recetas favoritas? Esta acción no se puede deshacer.")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Sí, Borrar Todo") { _, _ ->
                deleteAllFavoriteRecipes()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteAllFavoriteRecipes() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "Error: Usuario no identificado.", Toast.LENGTH_SHORT).show()
            return
        }

        val favoritesCollection = db.collection("users").document(userId).collection("favoriteRecipes")

        favoritesCollection.get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(context, "No hay favoritos para eliminar.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                querySnapshot.documents.forEach { document ->
                    batch.delete(document.reference)
                }

                batch.commit()
                    .addOnSuccessListener {
                        Log.d(TAG, "Todas las recetas favoritas han sido eliminadas.")
                        Toast.makeText(context, "Todos los favoritos eliminados.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error al eliminar todos los favoritos.", e)
                        Toast.makeText(context, "Error al eliminar favoritos: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al obtener favoritos para eliminar.", e)
                Toast.makeText(context, "Error al acceder a los favoritos: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}