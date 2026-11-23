package com.steefwel28.consumoenergetico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.steefwel28.consumoenergetico.data.worker.MaintenanceReminderScheduler
import com.steefwel28.consumoenergetico.ui.navigation.ConsumoApp
import com.steefwel28.consumoenergetico.ui.theme.ConsumoEnergeticoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MaintenanceReminderScheduler.enqueue(this)

        val container = (application as ConsumoEnergeticoApp).container
        setContent {
            ConsumoEnergeticoTheme {
                ConsumoApp(container)
            }
        }
    }
}
