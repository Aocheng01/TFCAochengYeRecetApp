package com.example.recetapp.data

// Representa el objeto "next" dentro de "_links"
data class NextPageLink(
    val href: String?, // La URL completa para la siguiente página
    val title: String?
)