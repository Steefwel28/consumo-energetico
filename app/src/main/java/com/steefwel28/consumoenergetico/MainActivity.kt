package com.steefwel28.consumoenergetico

import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steefwel28.consumoenergetico.data.worker.MaintenanceReminderScheduler
import com.steefwel28.consumoenergetico.domain.settings.AppearancePreferences
import com.steefwel28.consumoenergetico.domain.settings.ThemeMode
import com.steefwel28.consumoenergetico.ui.navigation.ConsumoApp
import com.steefwel28.consumoenergetico.ui.theme.ConsumoEnergeticoTheme
import com.steefwel28.consumoenergetico.ui.theme.LocalLiquidGlass
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MaintenanceReminderScheduler.enqueue(this)

        val container = (application as ConsumoEnergeticoApp).container
        // Lectura única y corta en el arranque: fija el fondo de ventana antes
        // del primer frame para que no aparezca el destello blanco.
        val storedAppearance = runBlocking {
            runCatching { container.preferences.appearance.first() }
                .getOrDefault(AppearancePreferences())
        }
        window.setBackgroundDrawable(
            ColorDrawable(windowBackgroundColor(storedAppearance)),
        )

        setContent {
            val appearance by container.preferences.appearance.collectAsStateWithLifecycle(
                initialValue = storedAppearance,
            )
            val darkTheme = appearance.mode.isDark()

            ConsumoEnergeticoTheme(
                darkTheme = darkTheme,
                oledPure = appearance.oledPure,
            ) {
                CompositionLocalProvider(LocalLiquidGlass provides appearance.liquidGlass) {
                    ConsumoApp(container = container, appearance = appearance)
                }
            }
        }
    }

    private fun ThemeMode.isDark(): Boolean = when (this) {
        ThemeMode.DAY -> false
        ThemeMode.NIGHT -> true
        ThemeMode.SYSTEM -> resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun windowBackgroundColor(appearance: AppearancePreferences): Int = when {
        appearance.liquidGlass -> LIQUID_GLASS_BACKGROUND
        appearance.mode.isDark() -> AndroidColor.BLACK
        else -> AndroidColor.WHITE
    }

    private companion object {
        const val LIQUID_GLASS_BACKGROUND = 0xFF04110F.toInt()
    }
}
