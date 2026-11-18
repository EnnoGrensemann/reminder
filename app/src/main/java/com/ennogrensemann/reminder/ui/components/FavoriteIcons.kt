package com.ennogrensemann.reminder.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class FavoriteIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector
)

object FavoriteIcons {
    val options = listOf(
        FavoriteIconOption("Home", "Zuhause", Icons.Default.Home),
        FavoriteIconOption("Work", "Arbeit", Icons.Default.Work),
        FavoriteIconOption("Shopping", "Einkaufen", Icons.Default.ShoppingCart),
        FavoriteIconOption("Gym", "Sport", Icons.Default.FitnessCenter),
        FavoriteIconOption("School", "Uni / Schule", Icons.Default.School),
        FavoriteIconOption("Cafe", "Freizeit", Icons.Default.LocalCafe),
        FavoriteIconOption("Star", "Favorit", Icons.Default.Star),
        FavoriteIconOption("Place", "Ort", Icons.Default.Place)
    )

    fun iconFor(key: String): ImageVector {
        return options.firstOrNull { it.key.equals(key, ignoreCase = true) }?.icon
            ?: Icons.Default.Place
    }
}
