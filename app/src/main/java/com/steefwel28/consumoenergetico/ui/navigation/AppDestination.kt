package com.steefwel28.consumoenergetico.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.ui.graphics.vector.ImageVector
import com.steefwel28.consumoenergetico.R

/** Los cuatro destinos de la barra inferior estilo Pixel. */
enum class AppDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    CALCULATE("calcular", R.string.nav_calculate, Icons.Rounded.Calculate),
    RECOMMEND("recomendar", R.string.nav_recommend, Icons.Rounded.TipsAndUpdates),
    HOME("mi-casa", R.string.nav_home, Icons.Rounded.Home),
    SETTINGS("ajustes", R.string.nav_settings, Icons.Rounded.Settings),
}