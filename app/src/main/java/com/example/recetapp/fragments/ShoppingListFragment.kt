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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetapp.R
import com.example.recetapp.adapters.ShoppingListAdapter
import com.example.recetapp.data.ShoppingListItem // Asegúrate que la importación sea desde .data
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

class ShoppingListFragment : Fragment() {

    private val TAG = "ShoppingListFragment"

    private lateinit var editTextShoppingItem: TextInputEditText
    private lateinit var buttonAddShoppingItem: Button
    private lateinit var recyclerViewShoppingListItems: RecyclerView
    private lateinit var textViewShoppingListEmpty: TextView
    private lateinit var shoppingListAdapter: ShoppingListAdapter

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val shoppingListLocal = mutableListOf<ShoppingListItem>()
    private var shoppingListListenerRegistration: ListenerRegistration? = null

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

        setupRecyclerView()
        setupListeners()

        return view
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            attachShoppingListListener()
        } else {
            Log.w(TAG, "Usuario no logueado, no se puede cargar la lista de compra.")
            shoppingListLocal.clear()
            if (::shoppingListAdapter.isInitialized) {
                shoppingListAdapter.submitList(shoppingListLocal)
            }
            updateEmptyViewVisibility()
        }
    }

    override fun onStop() {
        super.onStop()
        shoppingListListenerRegistration?.remove()
        Log.d(TAG, "ShoppingList listener desregistrado.")
    }

    private fun setupRecyclerView() {
        shoppingListAdapter = ShoppingListAdapter(
            shoppingListLocal,
            onItemCheckedChanged = { item, isChecked, position ->
                Log.d(TAG, "Item '${item.name}' checked: $isChecked en posición $position")
                updateItemPurchasedStatusInFirestore(item.id, isChecked)
            },
            onDeleteClick = { item, position ->
                Log.d(TAG, "Eliminar item: ${item.name} (ID: ${item.id}) en posición $position")
                showDeleteConfirmationDialog(item)
            }
        )
        recyclerViewShoppingListItems.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewShoppingListItems.adapter = shoppingListAdapter
    }

    private fun setupListeners() {
        buttonAddShoppingItem.setOnClickListener {
            addItemToShoppingList()
        }
        editTextShoppingItem.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addItemToShoppingList()
                true
            } else {
                false
            }
        }
    }

    private fun showDeleteConfirmationDialog(item: ShoppingListItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que quieres eliminar '${item.name}' de la lista de la compra?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteShoppingItemFromFirestore(item.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addItemToShoppingList() {
        val itemName = editTextShoppingItem.text.toString().trim()
        val userId = auth.currentUser?.uid

        if (itemName.isBlank()) {
            Toast.makeText(requireContext(), "El nombre del ítem no puede estar vacío.", Toast.LENGTH_SHORT).show()
            return
        }
        if (userId == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión para añadir ítems.", Toast.LENGTH_SHORT).show()
            return
        }
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
                Log.d(TAG, "$itemName añadido a la lista de compra en Firestore con ID: $normalizedItemNameId")
                Toast.makeText(requireContext(), "$itemName añadido a la lista", Toast.LENGTH_SHORT).show()
                editTextShoppingItem.text?.clear()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al añadir $itemName a Firestore", e)
                Toast.makeText(requireContext(), "Error al añadir ítem: ${e.message}", Toast.LENGTH_LONG).show()
            }
        hideKeyboard()
    }

    private fun attachShoppingListListener() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "Intento de adjuntar listener de lista de compra sin usuario logueado.")
            shoppingListListenerRegistration?.remove()
            shoppingListLocal.clear()
            if (::shoppingListAdapter.isInitialized) shoppingListAdapter.submitList(shoppingListLocal)
            updateEmptyViewVisibility()
            return
        }

        shoppingListListenerRegistration?.remove()

        Log.d(TAG, "Adjuntando SnapshotListener para ítems de lista de compra del usuario: $userId")
        shoppingListListenerRegistration = db.collection("users").document(userId)
            .collection("shoppingListItems")
            .orderBy("isPurchased", Query.Direction.ASCENDING)
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Error en SnapshotListener de lista de compra.", e)
                    Toast.makeText(requireContext(), "Error al cargar la lista: ${e.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                Log.d(TAG, "SnapshotListener (Compra): Callback recibido.") // Log para saber que el listener se disparó
                shoppingListLocal.clear()
                if (snapshots != null && !snapshots.isEmpty) {
                    Log.d(TAG, "SnapshotListener (Compra): Recibidos ${snapshots.size()} documentos.")
                    for (document in snapshots.documents) {
                        val name = document.getString("name")
                        val isPurchased = document.getBoolean("isPurchased") ?: false
                        Log.d(TAG, "SnapshotListener (Compra): Procesando Doc ID: ${document.id}, Name: $name, Purchased: $isPurchased")
                        if (name != null) {
                            shoppingListLocal.add(ShoppingListItem(id = document.id, name = name, isPurchased = isPurchased))
                            Log.d(TAG, "SnapshotListener (Compra): Añadido '${name}' a shoppingListLocal. Tamaño actual: ${shoppingListLocal.size}") // <-- LOG ADICIONAL
                        } else {
                            Log.w(TAG, "SnapshotListener (Compra): Documento ${document.id} tiene campo 'name' nulo. Data: ${document.data}")
                        }
                    }
                } else {
                    Log.d(TAG, "SnapshotListener (Compra): Snapshots es nulo o vacío.")
                }

                if (::shoppingListAdapter.isInitialized) {
                    Log.d(TAG, "SnapshotListener (Compra): Actualizando adaptador con ${shoppingListLocal.size} items.")
                    shoppingListAdapter.submitList(shoppingListLocal.toList()) // Enviar una copia para evitar problemas de referencia
                } else {
                    Log.w(TAG, "SnapshotListener (Compra): shoppingListAdapter no inicializado al intentar submitList.")
                }
                updateEmptyViewVisibility()
                Log.d(TAG, "SnapshotListener (Compra): FIN de callback. Items locales finales: ${shoppingListLocal.size}")
            }
    }

    private fun updateItemPurchasedStatusInFirestore(documentId: String, isPurchased: Boolean) {
        val userId = auth.currentUser?.uid
        if (userId == null || documentId.isBlank()) { /* ... */ return }
        Log.d(TAG, "Actualizando estado 'isPurchased' a $isPurchased para item ID: $documentId")
        db.collection("users").document(userId)
            .collection("shoppingListItems").document(documentId)
            .update("isPurchased", isPurchased)
            .addOnSuccessListener {
                Log.d(TAG, "Estado 'isPurchased' actualizado en Firestore para $documentId.")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al actualizar 'isPurchased' en Firestore para $documentId", e)
                Toast.makeText(requireContext(), "Error al actualizar estado: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun deleteShoppingItemFromFirestore(documentId: String) {
        val userId = auth.currentUser?.uid
        if (userId == null || documentId.isBlank()) { /* ... */ return }
        Log.d(TAG, "Intentando eliminar ítem de compra con ID: $documentId")
        db.collection("users").document(userId)
            .collection("shoppingListItems").document(documentId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Documento $documentId eliminado de la lista de compra en Firestore.")
                Toast.makeText(requireContext(), "Ítem eliminado.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al eliminar documento $documentId de Firestore", e)
                Toast.makeText(requireContext(), "Error al eliminar ítem: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateEmptyViewVisibility() {
        if (::shoppingListAdapter.isInitialized && shoppingListAdapter.isEmpty()) {
            textViewShoppingListEmpty.visibility = View.VISIBLE
            recyclerViewShoppingListItems.visibility = View.GONE
        } else {
            textViewShoppingListEmpty.visibility = View.GONE
            recyclerViewShoppingListItems.visibility = View.VISIBLE
        }
    }

    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDetach() {
        super.onDetach()
        // No hay listener de fragmento a actividad en este fragmento por ahora
    }
}
