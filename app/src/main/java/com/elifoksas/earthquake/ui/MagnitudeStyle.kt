package com.elifoksas.earthquake.ui

import android.content.res.ColorStateList
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.elifoksas.earthquake.R

object MagnitudeStyle {

    fun applyBackground(view: View, magnitude: Double?) {
        val color = ContextCompat.getColor(view.context, getColorRes(magnitude))
        ViewCompat.setBackgroundTintList(view, ColorStateList.valueOf(color))
    }

    fun getStatus(magnitude: Double?): String {
        val value = magnitude ?: return "Magnitude unavailable"

        return when {
            value < 3.0 -> "Light shaking"
            value < 4.0 -> "Noticeable shaking"
            value < 5.0 -> "Moderate shaking"
            value < 6.0 -> "Strong shaking"
            else -> "Severe shaking"
        }
    }

    @ColorRes
    private fun getColorRes(magnitude: Double?): Int {
        val value = magnitude ?: return R.color.earthquake_magnitude_unknown

        return when {
            value < 3.0 -> R.color.earthquake_magnitude_low
            value < 4.0 -> R.color.earthquake_magnitude_noticeable
            value < 5.0 -> R.color.earthquake_magnitude_moderate
            value < 6.0 -> R.color.earthquake_magnitude_strong
            else -> R.color.earthquake_magnitude_severe
        }
    }
}
