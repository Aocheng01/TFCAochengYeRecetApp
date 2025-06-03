// --- File: com/example/recetapp/fragments/ShoppingListFragment.kt ---
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
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetapp.R
import com.example.recetapp.adapters.ShoppingListAdapter
import com.example.recetapp.data.ShoppingDisplayItem
import com.example.recetapp.data.ShoppingListItem
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import java.util.Date

class ShoppingListFragment : Fragment() {

    private val TAG = "ShoppingListFragment"

    private lateinit var editTextShoppingItem: TextInputEditText
    private lateinit var buttonAddShoppingItem: Button
    private lateinit var recyclerViewShoppingListItems: RecyclerView
    private lateinit var textViewShoppingListEmpty: TextView
    private lateinit var buttonClearAllShoppingList: Button
    private lateinit var shoppingListAdapter: ShoppingListAdapter

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var shoppingListListenerRegistration: ListenerRegistration? = null

    private val recipeExpandedStates = mutableMapOf<String, Boolean>()
    private var lastRawShoppingListItems: List<ShoppingListItem>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_shopping_list, container, false)
        auth = Firebase.auth
        db = Firebase.firestore

        editTextShoppingItem = view.findViewById(R.id.editTextShoppingItem)
        buttonAddShoppingItem = view.findViewById(R.id.buttonAddShoppingItem)
        recyclerViewShoppingListItems = view.findViewById(R.id.recyclerViewShoppingListItems)
        textViewShoppingListEmpty = view.findViewById(R.id.textViewShoppingListEmpty)
        buttonClearAllShoppingList = view.findViewById(R.id.buttonClearAllShoppingList)

        setupRecyclerView()
        setupListeners()
        Log.d(TAG, "onCreateView: Fragmento Lista Compra Creado")
        return view
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: Adjuntando listener si el usuario está logueado.")
        if (auth.currentUser != null) {
            lastRawShoppingListItems = null // Reset cache
            attachShoppingListListener()
        } else {
            Log.w(TAG, "onStart: Usuario no logueado. Limpiando lista.")
            if (::shoppingListAdapter.isInitialized) {
                shoppingListAdapter.submitList(emptyList())
            }
            lastRawShoppingListItems = emptyList()
            updateEmptyViewAndClearButtonVisibility(emptyList())
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: Desregistrando listener de la lista de compra.")
        shoppingListListenerRegistration?.remove()
    }

    private fun setupRecyclerView() {
        shoppingListAdapter = ShoppingListAdapter(
            onItemCheckedChanged = { shoppingListItem, documentId, isChecked ->
                updateItemPurchasedStatusInFirestore(documentId, isChecked)
            },
            onDeleteClick = { shoppingListItem, documentId ->
                showDeleteConfirmationDialog(shoppingListItem, documentId)
            },
            onRecipeHeaderClick = { recipeId, recipeName ->
                // El click en el cuerpo del header también expande/colapsa
                toggleRecipeExpandedState(recipeId)
            },
            // Nuevos Callbacks
            onToggleRecipeExpandClick = { recipeId ->
                toggleRecipeExpandedState(recipeId)
            },
            onDeleteRecipeClick = { recipeId, recipeName ->
                showDeleteRecipeConfirmationDialog(recipeId, recipeName)
            }
        )
        recyclerViewShoppingListItems.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewShoppingListItems.adapter = shoppingListAdapter
    }

    private fun setupListeners() {
        buttonAddShoppingItem.setOnClickListener { addItemToShoppingList() }
        editTextShoppingItem.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { addItemToShoppingList(); true } else false
        }
        buttonClearAllShoppingList.setOnClickListener { showClearAllConfirmationDialog() }
    }

    private fun showDeleteConfirmationDialog(item: ShoppingListItem, documentId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Eliminar '${item.name}' de la lista?")
            .setIcon(R.drawable.ic_delete) // Asegúrate que ic_delete exista
            .setPositiveButton("Eliminar") { _, _ -> deleteShoppingItemFromFirestore(documentId) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showClearAllConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Borrar Toda la Lista")
            .setMessage("¿Estás seguro de que quieres eliminar TODOS los ítems? Esta acción no se puede deshacer.")
            .setIcon(R.drawable.ic_delete) // Puedes usar ic_delete_sweep o ic_delete
            .setPositiveButton("Sí, Borrar Todo") { _, _ -> clearAllShoppingListItems() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addItemToShoppingList() {
        val itemName = editTextShoppingItem.text.toString().trim()
        val userId = auth.currentUser?.uid

        if (itemName.isBlank()) {
            Toast.makeText(requireContext(), "El nombre no puede estar vacío.", Toast.LENGTH_SHORT).show()
            return
        }
        if (userId == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión.", Toast.LENGTH_SHORT).show()
            return
        }

        val shoppingItemData = hashMapOf(
            "name" to itemName,
            "isPurchased" to false,
            "recipeId" to null,
            "recipeName" to null,
            "addedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users").document(userId).collection("shoppingListItems")
            .add(shoppingItemData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Ítem manual '$itemName' añadido a Firestore con ID: ${documentReference.id}.")
                Toast.makeText(requireContext(), "$itemName añadido.", Toast.LENGTH_SHORT).show()
                editTextShoppingItem.text?.clear()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al añadir ítem manual '$itemName': ", e)
                Toast.makeText(requireContext(), "Error al añadir: ${e.message}", Toast.LENGTH_LONG).show()
            }
        hideKeyboard()
    }

    private fun attachShoppingListListener() {
        val userId = auth.currentUser?.uid ?: return
        shoppingListListenerRegistration?.remove()

        Log.d(TAG, "attachShoppingListListener: Adjuntando SnapshotListener para usuario $userId")
        db.collection("users").document(userId).collection("shoppingListItems")
            .orderBy("isPurchased", Query.Direction.ASCENDING)
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Error en SnapshotListener: ", e)
                    Toast.makeText(requireContext(), "Error al cargar lista: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val rawShoppingListItems = mutableListOf<ShoppingListItem>()
                snapshots?.forEach { document ->
                    try {
                        val item = document.toObject<ShoppingListItem>()
                        item.documentId = document.id
                        rawShoppingListItems.add(item)
                    } catch (ex: Exception) {
                        Log.e(TAG, "Error al parsear ShoppingListItem ID: ${document.id}", ex)
                    }
                }
                Log.d(TAG, "Total ítems crudos leídos de Firestore: ${rawShoppingListItems.size}")
                this.lastRawShoppingListItems = rawShoppingListItems
                processAndDisplayShoppingList(rawShoppingListItems)
            }
    }

    private fun processAndDisplayShoppingList(rawItems: List<ShoppingListItem>) {
        Log.d(TAG, "processAndDisplayShoppingList: Procesando ${rawItems.size} ítems crudos.")
        val displayList = mutableListOf<ShoppingDisplayItem>()

        val itemsFromRecipesGrouped = rawItems
            .filter { !it.recipeId.isNullOrBlank() }
            .groupBy { it.recipeId }

        val standaloneItems = rawItems.filter { it.recipeId.isNullOrBlank() }

        val sortableUnits = mutableListOf<Triple<Boolean, Date, ShoppingDisplayItem>>()

        itemsFromRecipesGrouped.forEach { (recipeId, ingredientsInRecipe) ->
            if (recipeId != null && ingredientsInRecipe.isNotEmpty()) {
                val allIngredientsPurchased = ingredientsInRecipe.all { it.isPurchased }
                val representativeDate = ingredientsInRecipe.minOfOrNull { it.addedAt?.time ?: Long.MAX_VALUE }?.let { Date(it) } ?: Date(Long.MAX_VALUE)
                val isExpanded = recipeExpandedStates.getOrPut(recipeId) { true } // Default a expandido
                sortableUnits.add(Triple(allIngredientsPurchased, representativeDate,
                    ShoppingDisplayItem.RecipeHeader(recipeId, ingredientsInRecipe.first().recipeName ?: "Receta", isExpanded)
                ))
            }
        }

        standaloneItems.forEach { item ->
            val sortDate = item.addedAt ?: Date(Long.MAX_VALUE) // Ítems nuevos (addedAt=null) arriba
            sortableUnits.add(Triple(item.isPurchased, sortDate, ShoppingDisplayItem.StandaloneItem(item, item.documentId!!)))
        }

        sortableUnits.sortWith(compareBy<Triple<Boolean, Date, ShoppingDisplayItem>> { it.first }.thenByDescending { it.second })

        sortableUnits.forEach { unit ->
            val displayItem = unit.third
            displayList.add(displayItem) // Añadir Header o StandaloneItem

            if (displayItem is ShoppingDisplayItem.RecipeHeader && displayItem.isExpanded) {
                itemsFromRecipesGrouped[displayItem.recipeId]
                    ?.sortedWith(compareBy({ it.isPurchased }, { it.addedAt ?: Date(Long.MAX_VALUE) }))
                    ?.forEach { ingredient ->
                        ingredient.documentId?.let { docId ->
                            displayList.add(ShoppingDisplayItem.RecipeIngredient(ingredient, docId))
                        }
                    }
            }
        }

        Log.d(TAG, "DisplayList final para el adaptador (${displayList.size} ítems).")
        // displayList.forEachIndexed { index, dItem -> Log.d(TAG, " Item $index: $dItem") }


        if (::shoppingListAdapter.isInitialized) {
            shoppingListAdapter.submitList(displayList.toList()) // Enviar una nueva lista (inmutable)
        }
        updateEmptyViewAndClearButtonVisibility(displayList)
        Log.d(TAG, "processAndDisplayShoppingList: Lista actualizada en el adaptador.")
    }


    private fun toggleRecipeExpandedState(recipeId: String) {
        val currentState = recipeExpandedStates.getOrPut(recipeId) { true }
        recipeExpandedStates[recipeId] = !currentState
        Log.d(TAG, "Toggled recipe $recipeId expanded state to: ${!currentState}")
        lastRawShoppingListItems?.let {
            processAndDisplayShoppingList(it)
        } ?: run {
            Log.w(TAG, "lastRawShoppingListItems es null al intentar toggleRecipeExpandedState. Re-adjuntando listener.")
            if (auth.currentUser != null) attachShoppingListListener()
        }
    }

    private fun showDeleteRecipeConfirmationDialog(recipeId: String, recipeName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Receta de la Lista")
            .setMessage("¿Eliminar todos los ingredientes de la receta '$recipeName' de la lista de la compra?")
            .setIcon(R.drawable.ic_delete) // O el que prefieras
            .setPositiveButton("Sí, Eliminar") { _, _ ->
                deleteRecipeIngredientsFromFirestore(recipeId, recipeName)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteRecipeIngredientsFromFirestore(recipeId: String, recipeName: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Intentando eliminar todos los ingredientes de la receta ID: $recipeId ($recipeName)")
        val shoppingListRef = db.collection("users").document(userId).collection("shoppingListItems")

        shoppingListRef.whereEqualTo("recipeId", recipeId).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Log.d(TAG, "No se encontraron ingredientes para la receta ID: $recipeId para eliminar.")
                    // No es necesariamente un error, podría haberse eliminado ya.
                    // Toast.makeText(requireContext(), "No hay ingredientes para esta receta.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val batch = db.batch()
                querySnapshot.documents.forEach { document ->
                    batch.delete(document.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        Log.d(TAG, "Todos los ingredientes de la receta '$recipeName' eliminados.")
                        Toast.makeText(requireContext(), "Ingredientes de '$recipeName' eliminados.", Toast.LENGTH_SHORT).show()
                        // El SnapshotListener actualizará la UI.
                        // También eliminar el estado de expansión de la receta si ya no tiene items.
                        recipeExpandedStates.remove(recipeId)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error al eliminar ingredientes de la receta '$recipeName' en batch:", e)
                        Toast.makeText(requireContext(), "Error al eliminar receta: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al obtener ingredientes para eliminar receta '$recipeName':", e)
                Toast.makeText(requireContext(), "Error al buscar ingredientes de receta: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // --- Métodos existentes sin cambios ---
    private fun updateItemPurchasedStatusInFirestore(documentId: String, isPurchased: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("shoppingListItems").document(documentId)
            .update("isPurchased", isPurchased)
            .addOnSuccessListener { Log.d(TAG, "Ítem $documentId actualizado a isPurchased=$isPurchased") }
            .addOnFailureListener { e -> Log.w(TAG, "Error al actualizar 'isPurchased' para $documentId", e) }
    }

    private fun deleteShoppingItemFromFirestore(documentId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("shoppingListItems").document(documentId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Ítem $documentId eliminado.")
                Toast.makeText(requireContext(), "Ítem eliminado.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al eliminar ítem $documentId", e)
                Toast.makeText(requireContext(), "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun clearAllShoppingListItems() {
        val userId = auth.currentUser?.uid ?: return
        val collectionRef = db.collection("users").document(userId).collection("shoppingListItems")

        Log.d(TAG, "clearAllShoppingListItems: Intentando borrar todos los ítems.")
        collectionRef.get().addOnSuccessListener { querySnapshot ->
            if (querySnapshot.isEmpty) {
                Toast.makeText(requireContext(), "La lista ya está vacía.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            val batch = db.batch()
            querySnapshot.documents.forEach { document -> batch.delete(document.reference) }
            batch.commit()
                .addOnSuccessListener {
                    Log.d(TAG, "Todos los ítems borrados de Firestore.")
                    Toast.makeText(requireContext(), "Lista de compra borrada.", Toast.LENGTH_SHORT).show()
                    recipeExpandedStates.clear() // Limpiar todos los estados de expansión
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al ejecutar batch delete:", e)
                    Toast.makeText(requireContext(), "Error al borrar lista: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al obtener ítems para borrar todo:", e)
                Toast.makeText(requireContext(), "Error al obtener ítems para borrar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateEmptyViewAndClearButtonVisibility(displayItems: List<ShoppingDisplayItem>) {
        if (view == null) return // Fragment puede estar destruyéndose
        val isEmpty = displayItems.isEmpty()
        textViewShoppingListEmpty.isVisible = isEmpty
        recyclerViewShoppingListItems.isVisible = !isEmpty
        buttonClearAllShoppingList.isVisible = !isEmpty
        Log.d(TAG, "updateEmptyViewAndClearButtonVisibility: Lista vacía = $isEmpty")
    }

    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}