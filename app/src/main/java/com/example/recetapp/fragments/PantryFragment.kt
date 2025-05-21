package com.example.recetapp.fragments // Asegúrate que el paquete sea correcto

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
import com.example.recetapp.R // Asegúrate que esta importación sea correcta
import com.example.recetapp.adapters.PantryAdapter
import com.google.android.material.textfield.TextInputEditText

class PantryFragment : Fragment() {

    private val TAG = "PantryFragment"

    private lateinit var editTextPantryIngredient: TextInputEditText
    private lateinit var buttonAddPantryIngredient: Button
    private lateinit var recyclerViewPantryItems: RecyclerView
    private lateinit var textViewPantryEmpty: TextView
    private lateinit var pantryAdapter: PantryAdapter

    // Lista en memoria para los ingredientes de la despensa
    private val pantryItemList = mutableListOf<String>()
    // TODO: Reemplazar esta lista en memoria con persistencia (Room o Firestore)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el layout para este fragment
        val view = inflater.inflate(R.layout.fragment_pantry, container, false)

        // Inicializar Vistas
        editTextPantryIngredient = view.findViewById(R.id.editTextPantryIngredient)
        buttonAddPantryIngredient = view.findViewById(R.id.buttonAddPantryIngredient)
        recyclerViewPantryItems = view.findViewById(R.id.recyclerViewPantryItems)
        textViewPantryEmpty = view.findViewById(R.id.textViewPantryEmpty)

        setupRecyclerView()
        setupListeners()
        updateEmptyViewVisibility()

        return view
    }

    private fun setupRecyclerView() {
        pantryAdapter = PantryAdapter(
            pantryItemList,
            onSearchClick = { ingredient ->
                Log.d(TAG, "Buscar recetas con: $ingredient")
                Toast.makeText(requireContext(), "Buscar recetas con: $ingredient", Toast.LENGTH_SHORT).show()
                // TODO: Implementar la lógica para navegar al SearchRecipesFragment
            },
            onAddToShoppingListClick = { ingredient ->
                Log.d(TAG, "$ingredient añadido a la lista de compra")
                Toast.makeText(requireContext(), "$ingredient añadido a lista de compra", Toast.LENGTH_SHORT).show()
                // TODO: Implementar la lógica para añadir a la lista de compra real.
                // Esto podría implicar comunicación con ShoppingListFragment o un ViewModel compartido.
            },
            onDeleteClick = { ingredient, position ->
                Log.d(TAG, "$ingredient eliminado de la despensa en posición $position")
                pantryAdapter.removeItem(position)
                Toast.makeText(requireContext(), "$ingredient eliminado", Toast.LENGTH_SHORT).show()
                updateEmptyViewVisibility()
                // TODO: Eliminar de la base de datos persistente (Room o Firestore)
            }
        )
        recyclerViewPantryItems.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewPantryItems.adapter = pantryAdapter
    }

    private fun setupListeners() {
        buttonAddPantryIngredient.setOnClickListener {
            addIngredientToPantry()
        }

        // Para que al pulsar "Hecho" en el teclado también se añada el ingrediente
        editTextPantryIngredient.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addIngredientToPantry()
                true // Indica que el evento ha sido consumido
            } else {
                false
            }
        }
    }

    private fun addIngredientToPantry() {
        val ingredientName = editTextPantryIngredient.text.toString().trim()
        if (ingredientName.isNotBlank()) {
            // Evitar duplicados (sensible a mayúsculas/minúsculas por ahora)
            if (!pantryItemList.any { it.equals(ingredientName, ignoreCase = true) }) {
                pantryAdapter.addItem(ingredientName)
                editTextPantryIngredient.text?.clear()
                updateEmptyViewVisibility()
                Log.d(TAG, "$ingredientName añadido a la despensa")
                // TODO: Guardar en la base de datos persistente (Room o Firestore)
            } else {
                Toast.makeText(requireContext(), "$ingredientName ya está en la despensa.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "El nombre del ingrediente no puede estar vacío.", Toast.LENGTH_SHORT).show()
        }
        hideKeyboard()
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

    // --- TODO: Métodos para cargar/guardar desde persistencia ---
    // override fun onResume() {
    //     super.onResume()
    //     // TODO: Cargar pantryItemList desde Room o Firestore
    //     // pantryAdapter.notifyDataSetChanged() // Si la lista se actualiza
    //     // updateEmptyViewVisibility()
    // }
    //
    // override fun onPause() {
    //     super.onPause()
    //     // TODO: Guardar pantryItemList en Room o Firestore si es necesario aquí,
    //     // aunque es mejor guardar al añadir/eliminar.
    // }
}