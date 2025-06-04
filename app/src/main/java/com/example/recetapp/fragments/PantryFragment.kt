package com.example.recetapp.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible // Para isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetapp.R
import com.example.recetapp.adapters.PantryAdapter
import com.example.recetapp.adapters.PantryItem
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

interface PantryFragmentListener {
    fun onSearchRequestedFromPantry(query: String)
}

class PantryFragment : Fragment() {

    private val TAG = "PantryFragment" // Se mantiene el TAG aunque no se usen los logs para fines de referencia, puede ser útil para depuración temporal.

    private lateinit var editTextPantryIngredient: TextInputEditText
    private lateinit var buttonAddPantryIngredient: Button
    private lateinit var recyclerViewPantryItems: RecyclerView
    private lateinit var textViewPantryEmpty: TextView
    private lateinit var pantryAdapter: PantryAdapter
    private lateinit var buttonSuggestRecipes: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val pantryItemListLocal = mutableListOf<PantryItem>()
    private var pantryListenerRegistration: ListenerRegistration? = null

    private var listener: PantryFragmentListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PantryFragmentListener) {
            listener = context
        } else {
            throw RuntimeException("$context debe implementar PantryFragmentListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pantry, container, false)

        auth = Firebase.auth
        db = Firebase.firestore

        editTextPantryIngredient = view.findViewById(R.id.editTextPantryIngredient)
        buttonAddPantryIngredient = view.findViewById(R.id.buttonAddPantryIngredient)
        recyclerViewPantryItems = view.findViewById(R.id.recyclerViewPantryItems)
        textViewPantryEmpty = view.findViewById(R.id.textViewPantryEmpty)
        buttonSuggestRecipes = view.findViewById(R.id.buttonSuggestRecipesFromPantry)

        setupRecyclerView()
        setupListeners()
        return view
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            attachPantryListener()
        } else {
            pantryItemListLocal.clear()
            if (::pantryAdapter.isInitialized) {
                pantryAdapter.submitList(pantryItemListLocal.toList())
            }
            updateEmptyViewVisibility()
        }
    }

    override fun onStop() {
        super.onStop()
        pantryListenerRegistration?.remove()
    }

    private fun setupRecyclerView() {
        pantryAdapter = PantryAdapter(
            pantryItemListLocal,
            onSearchClick = { ingredientName ->
                listener?.onSearchRequestedFromPantry(ingredientName)
            },
            onAddToShoppingListClick = { ingredientName ->
                addItemToShoppingListFromPantry(ingredientName)
            },
            onDeleteClick = { pantryItem, position ->
                showDeleteConfirmationDialog(pantryItem)
            }
        )
        recyclerViewPantryItems.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewPantryItems.adapter = pantryAdapter
    }

    private fun setupListeners() {
        buttonAddPantryIngredient.setOnClickListener {
            addIngredientToPantry()
        }
        editTextPantryIngredient.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addIngredientToPantry()
                true
            } else {
                false
            }
        }
        buttonSuggestRecipes.setOnClickListener {
            if (pantryItemListLocal.isNotEmpty()) {
                val ingredientsQuery = pantryItemListLocal.joinToString(separator = ",") { it.name }
                listener?.onSearchRequestedFromPantry(ingredientsQuery)
            } else {
                Toast.makeText(requireContext(), "Tu despensa está vacía.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmationDialog(pantryItem: PantryItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que quieres eliminar '${pantryItem.name}' de tu despensa?")
            .setPositiveButton("Eliminar") { _, _ ->
                deletePantryItemFromFirestore(pantryItem.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addIngredientToPantry() {
        val ingredientName = editTextPantryIngredient.text.toString().trim()
        val userId = auth.currentUser?.uid

        if (ingredientName.isBlank()) {
            Toast.makeText(requireContext(), "El nombre del ingrediente no puede estar vacío.", Toast.LENGTH_SHORT).show()
            return
        }
        if (userId == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión para añadir ingredientes.", Toast.LENGTH_SHORT).show()
            return
        }

        val normalizedIngredientName = ingredientName.lowercase(Locale.getDefault())
        if (pantryItemListLocal.any { it.id.equals(normalizedIngredientName, ignoreCase = true) }) {
            Toast.makeText(requireContext(), "$ingredientName ya está en la despensa.", Toast.LENGTH_SHORT).show()
            hideKeyboard()
            return
        }

        val pantryItemData = hashMapOf(
            "name" to ingredientName,
            "addedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users").document(userId)
            .collection("pantryItems").document(normalizedIngredientName)
            .set(pantryItemData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "$ingredientName añadido a la despensa", Toast.LENGTH_SHORT).show()
                editTextPantryIngredient.text?.clear()
                // El SnapshotListener se encargará de actualizar la UI
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al añadir ingrediente: ${e.message}", Toast.LENGTH_LONG).show()
            }
        hideKeyboard()
    }

    private fun addItemToShoppingListFromPantry(itemName: String) {
        val userId = auth.currentUser?.uid
        if (itemName.isBlank() || userId == null) { /* ... (validaciones) ... */ return }
        val normalizedItemNameId = itemName.lowercase(Locale.getDefault()).replace(" ", "_")
        val shoppingItemData = hashMapOf(
            "name" to itemName,
            "isPurchased" to false,
            "addedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(userId)
            .collection("shoppingListItems").document(normalizedItemNameId)
            .set(shoppingItemData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "'$itemName' añadido a tu lista de la compra.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al añadir a la lista de compra: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun attachPantryListener() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            pantryListenerRegistration?.remove()
            pantryItemListLocal.clear()
            if (::pantryAdapter.isInitialized) pantryAdapter.submitList(pantryItemListLocal.toList())
            updateEmptyViewVisibility()
            return
        }

        pantryListenerRegistration?.remove()

        pantryListenerRegistration = db.collection("users").document(userId)
            .collection("pantryItems")
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(requireContext(), "Error al cargar la despensa: ${e.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                pantryItemListLocal.clear()

                if (snapshots != null && !snapshots.isEmpty) {
                    for (document in snapshots.documents) {
                        val name = document.getString("name")
                        if (name != null) {
                            pantryItemListLocal.add(PantryItem(id = document.id, name = name))
                        }
                    }
                }

                if (::pantryAdapter.isInitialized) {
                    pantryAdapter.submitList(pantryItemListLocal.toList())
                }
                updateEmptyViewVisibility()
            }
    }

    private fun deletePantryItemFromFirestore(documentId: String) {
        val userId = auth.currentUser?.uid
        if (userId == null || documentId.isBlank()) { /* ... */ return }
        db.collection("users").document(userId)
            .collection("pantryItems").document(documentId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Ingrediente eliminado de la despensa.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { ex ->
                Toast.makeText(requireContext(), "Error al eliminar: ${ex.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateEmptyViewVisibility() {
        if (::pantryAdapter.isInitialized) {
            val isEmpty = pantryAdapter.isEmpty()
            textViewPantryEmpty.isVisible = isEmpty
            recyclerViewPantryItems.isVisible = !isEmpty
            buttonSuggestRecipes.isVisible = !isEmpty
        } else {
            textViewPantryEmpty.isVisible = true
            recyclerViewPantryItems.isVisible = false
            buttonSuggestRecipes.isVisible = false
        }
    }

    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}