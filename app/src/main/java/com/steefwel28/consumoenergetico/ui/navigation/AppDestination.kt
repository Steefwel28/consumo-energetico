package com.steefwel28.consumoenergetico.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.ui.graphics.vector.ImageVector

/** Los cuatro destinos de la barra inferior estilo Pixel. */
enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    CALCULATE("calcular", "Calcular", Icons.Rounded.Calculate),
    RECOMMEND("recomendar", "Recomendar", Icons.Rounded.TipsAndUpdates),
    HOME("mi-casa", "Mi Casa", Icons.Rounded.Home),
    SETTINGS("ajustes", "Ajustes", Icons.Rounded.Settings),
}
