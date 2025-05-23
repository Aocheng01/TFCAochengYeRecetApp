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
import com.example.recetapp.adapters.PantryAdapter
import com.example.recetapp.adapters.PantryItem
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration // Importa ListenerRegistration
import com.google.firebase.firestore.Query // Importa Query para ordenar
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

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val pantryItemListLocal = mutableListOf<PantryItem>()
    private var pantryListenerRegistration: ListenerRegistration? = null // Para el SnapshotListener

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

        setupRecyclerView()
        setupListeners()
        // La carga inicial se hará en onResume/onStart con el SnapshotListener
        return view
    }

    override fun onStart() { // Cambiado de onResume a onStart para registrar el listener
        super.onStart()
        if (auth.currentUser != null) {
            attachPantryListener() // Inicia la escucha de cambios en Firestore
        } else {
            Log.w(TAG, "Usuario no logueado, no se pueden cargar/escuchar items de despensa.")
            pantryItemListLocal.clear()
            if (::pantryAdapter.isInitialized) {
                pantryAdapter.submitList(pantryItemListLocal)
            }
            updateEmptyViewVisibility()
        }
    }

    override fun onStop() { // Importante desregistrar el listener
        super.onStop()
        pantryListenerRegistration?.remove()
        Log.d(TAG, "Pantry listener desregistrado.")
    }


    private fun setupRecyclerView() {
        pantryAdapter = PantryAdapter(
            pantryItemListLocal,
            onSearchClick = { ingredientName ->
                listener?.onSearchRequestedFromPantry(ingredientName)
            },
            onAddToShoppingListClick = { ingredientName ->
                Log.d(TAG, "$ingredientName añadido a la lista de compra (TODO)")
                Toast.makeText(requireContext(), "$ingredientName a lista de compra (TODO)", Toast.LENGTH_SHORT).show()
                // TODO: Lógica para añadir a la lista de compra real.
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
    }

    private fun showDeleteConfirmationDialog(pantryItem: PantryItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que quieres eliminar '${pantryItem.name}' de tu despensa?")
            .setPositiveButton("Eliminar") { dialog, which ->
                // Usuario confirma la eliminación
                deletePantryItemFromFirestore(pantryItem.id)
            }
            .setNegativeButton("Cancelar", null) // No hace nada si se cancela
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
        // La comprobación de duplicados ahora la manejará Firestore (set sobrescribe)
        // o podrías hacer una lectura antes si quieres un mensaje específico.
        // Por simplicidad, dejaremos que set() sobrescriba si ya existe con el mismo ID normalizado.

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
                // Ya no llamamos a loadPantryItems() aquí, el SnapshotListener se encargará
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al añadir $ingredientName a Firestore", e)
                Toast.makeText(requireContext(), "Error al añadir ingrediente.", Toast.LENGTH_SHORT).show()
            }
        hideKeyboard()
    }

    // ---- NUEVA FUNCIÓN para escuchar cambios en Firestore en tiempo real ----
    private fun attachPantryListener() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "Intento de adjuntar listener sin usuario logueado.")
            pantryListenerRegistration?.remove() // Asegura que no haya listeners antiguos
            pantryItemListLocal.clear()
            if (::pantryAdapter.isInitialized) pantryAdapter.submitList(pantryItemListLocal)
            updateEmptyViewVisibility()
            return
        }

        // Si ya hay un listener, lo quitamos antes de añadir uno nuevo
        pantryListenerRegistration?.remove()

        Log.d(TAG, "Adjuntando SnapshotListener para items de despensa del usuario: $userId")
        pantryListenerRegistration = db.collection("users").document(userId)
            .collection("pantryItems")
            .orderBy("name", Query.Direction.ASCENDING) // Ordenar por nombre ascendente
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Error en SnapshotListener de despensa.", e)
                    // ...
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    Log.d(TAG, "SnapshotListener: Documentos recibidos: ${snapshots.size()}") // Log del número de documentos
                    pantryItemListLocal.clear()
                    for (document in snapshots) {
                        Log.d(TAG, "SnapshotListener: Doc ID: ${document.id}, Data: ${document.data}") // Log de cada documento
                        val name = document.getString("name")
                        if (name != null) {
                            pantryItemListLocal.add(PantryItem(id = document.id, name = name))
                        } else {
                            Log.w(TAG, "SnapshotListener: Documento ${document.id} no tiene 'name'.")
                        }
                    }
                } else {
                    Log.d(TAG, "SnapshotListener: Snapshots es null.")
                    pantryItemListLocal.clear()
                }

                if (::pantryAdapter.isInitialized) {
                    pantryAdapter.submitList(pantryItemListLocal.toList()) // Envía una copia de la lista
                }
                updateEmptyViewVisibility()
                Log.d(TAG, "SnapshotListener: Items de despensa actualizados en UI: ${pantryItemListLocal.size}")
            }
    }
    // -----------------------------------------------------------------

    // La función loadPantryItems() ya no es necesaria si usamos SnapshotListener
    // private fun loadPantryItems() { ... }

    private fun deletePantryItemFromFirestore(documentId: String) { // Ya no necesita 'position'
        val userId = auth.currentUser?.uid
        if (userId == null || documentId.isBlank()) {
            Toast.makeText(requireContext(), "Error al eliminar.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Intentando eliminar item con ID: $documentId de Firestore para usuario $userId")
        db.collection("users").document(userId)
            .collection("pantryItems").document(documentId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Documento $documentId eliminado de Firestore.")
                Toast.makeText(requireContext(), "Ingrediente eliminado.", Toast.LENGTH_SHORT).show()
                // El SnapshotListener actualizará la UI automáticamente
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al eliminar documento $documentId de Firestore", e)
                Toast.makeText(requireContext(), "Error al eliminar ingrediente.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateEmptyViewVisibility() {
        if (::pantryAdapter.isInitialized && pantryAdapter.isEmpty()) {
            textViewPantryEmpty.visibility = View.VISIBLE
            recyclerViewPantryItems.visibility = View.GONE
        } else {
            textViewPantryEmpty.visibility = View.GONE
            recyclerViewPantryItems.visibility = View.VISIBLE
        }
    }

    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
        Log.d(TAG, "PantryFragmentListener desadjuntado.")
    }
}
