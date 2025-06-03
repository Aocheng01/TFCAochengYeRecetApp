package com.example.recetapp.utils // O el paquete donde lo hayas creado

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs
import kotlin.math.max

private const val MIN_SCALE = 0.85f
private const val MIN_ALPHA = 0.5f

class ZoomOutPageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            val pageHeight = height
            when {
                position < -1 -> { // [-Infinity,-1)
                    // Esta página está fuera de la pantalla hacia la izquierda.
                    alpha = 0f
                }
                position <= 1 -> { // [-1,1]
                    // Modifica la transición de encogimiento por defecto
                    val scaleFactor = max(MIN_SCALE, 1 - abs(position))
                    val vertMargin = pageHeight * (1 - scaleFactor) / 2
                    val horzMargin = pageWidth * (1 - scaleFactor) / 2
                    translationX = if (position < 0) {
                        horzMargin - vertMargin / 2
                    } else {
                        horzMargin + vertMargin / 2
                    }

                    // Escalar la página hacia abajo (entre MIN_SCALE y 1)
                    scaleX = scaleFactor
                    scaleY = scaleFactor

                    // Difuminar la página a medida que se aleja.
                    alpha = (MIN_ALPHA +
                            (((scaleFactor - MIN_SCALE) / (1 - MIN_SCALE)) * (1 - MIN_ALPHA)))
                }
                else -> { // (1,+Infinity]
                    // Esta página está fuera de la pantalla hacia la derecha.
                    alpha = 0f
                }
            }
        }
    }
}