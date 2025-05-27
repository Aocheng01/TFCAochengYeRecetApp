package com.example.recetapp.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
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

    private val TAG = "PantryFragment"

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
            Log.w(TAG, "Usuario no logueado, no se pueden cargar/escuchar items de despensa.")
            pantryItemListLocal.clear()
            if (::pantryAdapter.isInitialized) {
                pantryAdapter.submitList(pantryItemListLocal.toList()) // Usar toList()
            }
            updateEmptyViewVisibility()
        }
    }

    override fun onStop() {
        super.onStop()
        pantryListenerRegistration?.remove()
        Log.d(TAG, "Pantry listener desregistrado.")
    }


    private fun setupRecyclerView() {
        pantryAdapter = PantryAdapter(
            pantryItemListLocal, // El adapter trabaja con la referencia a la lista mutable
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
                val ingredientsQuery = pantryItemListLocal.joinToString(separator = " ") { it.name }
                Log.d(TAG, "Sugerir recetas con (todos los ingredientes): $ingredientsQuery")
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
                Log.d(TAG, "$ingredientName añadido/actualizado en Firestore con ID: $normalizedIngredientName")
                Toast.makeText(requireContext(), "$ingredientName añadido a la despensa", Toast.LENGTH_SHORT).show()
                editTextPantryIngredient.text?.clear()
                // El SnapshotListener se encargará de actualizar la UI
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al añadir $ingredientName a Firestore", e)
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
                Log.d(TAG, "'$itemName' añadido a la lista de compra en Firestore con ID: $normalizedItemNameId")
                Toast.makeText(requireContext(), "'$itemName' añadido a tu lista de la compra.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al añadir '$itemName' a la lista de compra en Firestore", e)
                Toast.makeText(requireContext(), "Error al añadir a la lista de compra: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun attachPantryListener() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "Intento de adjuntar listener sin usuario logueado.")
            pantryListenerRegistration?.remove()
            pantryItemListLocal.clear()
            if (::pantryAdapter.isInitialized) pantryAdapter.submitList(pantryItemListLocal.toList()) // Usar toList()
            updateEmptyViewVisibility()
            return
        }

        pantryListenerRegistration?.remove()

        Log.d(TAG, "Adjuntando SnapshotListener para items de despensa del usuario: $userId")
        pantryListenerRegistration = db.collection("users").document(userId)
            .collection("pantryItems")
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Error en SnapshotListener de despensa.", e)
                    Toast.makeText(requireContext(), "Error al cargar la despensa: ${e.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                Log.d(TAG, "SnapshotListener (Despensa): Callback recibido.")
                pantryItemListLocal.clear() // Limpiar antes de repoblar

                if (snapshots != null && !snapshots.isEmpty) {
                    Log.d(TAG, "SnapshotListener (Despensa): Recibidos ${snapshots.size()} documentos.")
                    for (document in snapshots.documents) {
                        val name = document.getString("name")
                        Log.d(TAG, "SnapshotListener (Despensa): Procesando Doc ID: ${document.id}, Name: $name, Data: ${document.data}")
                        if (name != null) {
                            pantryItemListLocal.add(PantryItem(id = document.id, name = name))
                            // ---- LOG ADICIONAL DENTRO DEL BUCLE ----
                            Log.d(TAG, "SnapshotListener (Despensa): Añadido '${name}' a pantryItemListLocal. Tamaño actual: ${pantryItemListLocal.size}")
                        } else {
                            Log.w(TAG, "SnapshotListener (Despensa): Documento ${document.id} tiene campo 'name' nulo o ausente. Data: ${document.data}")
                        }
                    }
                } else {
                    Log.d(TAG, "SnapshotListener (Despensa): Snapshots es nulo o vacío.")
                }

                if (::pantryAdapter.isInitialized) {
                    Log.d(TAG, "SnapshotListener (Despensa): Actualizando adaptador con ${pantryItemListLocal.size} items.")
                    pantryAdapter.submitList(pantryItemListLocal.toList()) // ---- USAR toList() ----
                } else {
                    Log.w(TAG, "SnapshotListener (Despensa): pantryAdapter no inicializado al intentar submitList.")
                }
                updateEmptyViewVisibility()
                Log.d(TAG, "SnapshotListener (Despensa): FIN de callback. Items locales finales: ${pantryItemListLocal.size}")
            }
    }

    private fun deletePantryItemFromFirestore(documentId: String) {
        val userId = auth.currentUser?.uid
        if (userId == null || documentId.isBlank()) { /* ... */ return }
        Log.d(TAG, "Intentando eliminar item de despensa con ID: $documentId")
        db.collection("users").document(userId)
            .collection("pantryItems").document(documentId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Documento $documentId eliminado de despensa en Firestore.")
                Toast.makeText(requireContext(), "Ingrediente eliminado de la despensa.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { ex ->
                Log.w(TAG, "Error al eliminar documento $documentId de despensa en Firestore", ex)
                Toast.makeText(requireContext(), "Error al eliminar: ${ex.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateEmptyViewVisibility() {
        if (::pantryAdapter.isInitialized) { // Solo actualiza si el adapter está listo
            val isEmpty = pantryAdapter.isEmpty()
            textViewPantryEmpty.isVisible = isEmpty
            recyclerViewPantryItems.isVisible = !isEmpty
            buttonSuggestRecipes.isVisible = !isEmpty
        } else { // Si el adapter no está listo, asume que está vacío
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
