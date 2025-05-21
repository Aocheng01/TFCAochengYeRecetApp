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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetapp.R
import com.example.recetapp.adapters.PantryAdapter
import com.example.recetapp.adapters.PantryItem // Importa el data class PantryItem
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

class PantryFragment : Fragment() {

    private val TAG = "PantryFragment"

    private lateinit var editTextPantryIngredient: TextInputEditText
    private lateinit var buttonAddPantryIngredient: Button
    private lateinit var recyclerViewPantryItems: RecyclerView
    private lateinit var textViewPantryEmpty: TextView
    private lateinit var pantryAdapter: PantryAdapter

    // Instancias de Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Lista local que se sincronizará con Firestore
    private val pantryItemListLocal = mutableListOf<PantryItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pantry, container, false)

        // Inicializar Firebase
        auth = Firebase.auth
        db = Firebase.firestore

        // Inicializar Vistas
        editTextPantryIngredient = view.findViewById(R.id.editTextPantryIngredient)
        buttonAddPantryIngredient = view.findViewById(R.id.buttonAddPantryIngredient)
        recyclerViewPantryItems = view.findViewById(R.id.recyclerViewPantryItems)
        textViewPantryEmpty = view.findViewById(R.id.textViewPantryEmpty)

        setupRecyclerView()
        setupListeners()
        // La carga de datos se hará en onResume o después de que el usuario esté verificado
        return view
    }

    override fun onResume() {
        super.onResume()
        // Cargar ingredientes cuando el fragmento se vuelve visible y el usuario está logueado
        if (auth.currentUser != null) {
            loadPantryItems()
        } else {
            // Manejar caso donde el usuario no está logueado (quizás mostrar mensaje o no hacer nada)
            Log.w(TAG, "Usuario no logueado, no se pueden cargar items de despensa.")
            pantryItemListLocal.clear()
            pantryAdapter.submitList(pantryItemListLocal) // Limpia la lista si el usuario se desloguea
            updateEmptyViewVisibility()
        }
    }

    private fun setupRecyclerView() {
        pantryAdapter = PantryAdapter(
            pantryItemListLocal, // Usa la lista local que se sincronizará
            onSearchClick = { ingredientName ->
                Log.d(TAG, "Buscar recetas con: $ingredientName")
                Toast.makeText(requireContext(), "Buscar con: $ingredientName", Toast.LENGTH_SHORT).show()
                // TODO: Implementar la lógica para navegar al SearchRecipesFragment
                // y pasar 'ingredientName' como la query de búsqueda.
                // Esto requerirá comunicación entre fragments.
                // Ejemplo: (activity as? MainActivity)?.viewPager?.setCurrentItem(1, true)
                // y usar un SharedViewModel o argumentos para pasar la query.
                (activity as? MainActivity)?.navigateToSearchWithQuery(ingredientName)

            },
            onAddToShoppingListClick = { ingredientName ->
                Log.d(TAG, "$ingredientName añadido a la lista de compra (TODO)")
                Toast.makeText(requireContext(), "$ingredientName a lista de compra (TODO)", Toast.LENGTH_SHORT).show()
                // TODO: Implementar la lógica para añadir a la lista de compra real.
                // Esto podría implicar escribir en otra colección de Firestore para la lista de compra
                // o comunicación con ShoppingListFragment o un ViewModel compartido.
            },
            onDeleteClick = { pantryItem, position ->
                Log.d(TAG, "Eliminar: ${pantryItem.name} (ID: ${pantryItem.id}) en posición $position")
                deletePantryItemFromFirestore(pantryItem.id, position)
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

        // Normalizar nombre para usar como ID y para comprobación de duplicados
        val normalizedIngredientName = ingredientName.lowercase(Locale.getDefault())

        // Comprobar si ya existe (basado en el ID normalizado)
        if (pantryItemListLocal.any { it.id == normalizedIngredientName }) {
            Toast.makeText(requireContext(), "$ingredientName ya está en la despensa.", Toast.LENGTH_SHORT).show()
            hideKeyboard()
            return
        }

        val pantryItemData = hashMapOf(
            "name" to ingredientName, // Guardamos el nombre original para mostrar
            "addedAt" to FieldValue.serverTimestamp() // Opcional: marca de tiempo
        )

        // Usamos el nombre normalizado como ID del documento para evitar duplicados
        db.collection("users").document(userId)
            .collection("pantryItems").document(normalizedIngredientName)
            .set(pantryItemData)
            .addOnSuccessListener {
                Log.d(TAG, "$ingredientName añadido a Firestore con ID: $normalizedIngredientName")
                Toast.makeText(requireContext(), "$ingredientName añadido a la despensa", Toast.LENGTH_SHORT).show()
                editTextPantryIngredient.text?.clear()
                // Volver a cargar la lista para reflejar el cambio (o añadir localmente si prefieres optimizar)
                loadPantryItems()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al añadir $ingredientName a Firestore", e)
                Toast.makeText(requireContext(), "Error al añadir ingrediente.", Toast.LENGTH_SHORT).show()
            }
        hideKeyboard()
    }

    private fun loadPantryItems() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "Intento de cargar despensa sin usuario logueado.")
            pantryItemListLocal.clear()
            pantryAdapter.submitList(pantryItemListLocal)
            updateEmptyViewVisibility()
            return
        }

        Log.d(TAG, "Cargando items de despensa para el usuario: $userId")
        db.collection("users").document(userId)
            .collection("pantryItems")
            // .orderBy("addedAt", Query.Direction.DESCENDING) // Opcional: ordenar
            .get()
            .addOnSuccessListener { documents ->
                pantryItemListLocal.clear()
                for (document in documents) {
                    val name = document.getString("name")
                    if (name != null) {
                        // Usamos el ID del documento de Firestore (que es el nombre normalizado)
                        pantryItemListLocal.add(PantryItem(id = document.id, name = name))
                    }
                }
                pantryAdapter.submitList(pantryItemListLocal)
                updateEmptyViewVisibility()
                Log.d(TAG, "Items de despensa cargados: ${pantryItemListLocal.size}")
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error al obtener items de despensa.", exception)
                Toast.makeText(requireContext(), "Error al cargar la despensa.", Toast.LENGTH_SHORT).show()
                updateEmptyViewVisibility() // Aún así actualiza la vista vacía
            }
    }

    private fun deletePantryItemFromFirestore(documentId: String, position: Int) {
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
                // No es necesario llamar a pantryAdapter.removeItem(position) aquí
                // si loadPantryItems() se llama y actualiza toda la lista.
                // Si prefieres una actualización local inmediata:
                // pantryAdapter.removeItem(position)
                // updateEmptyViewVisibility()
                // Por consistencia, recargamos:
                loadPantryItems()
                Toast.makeText(requireContext(), "Ingrediente eliminado.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al eliminar documento $documentId de Firestore", e)
                Toast.makeText(requireContext(), "Error al eliminar ingrediente.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateEmptyViewVisibility() {
        if (pantryAdapter.isEmpty()) {
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
}