package com.steefwel28.consumoenergetico.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Borde especular compartido por las superficies Liquid Glass (1 dp, blanco
 * degradado). Vive aquí para que `SectionCard` y las tarjetas de Ajustes
 * hablen el mismo idioma visual sin duplicar el degradado.
 */
@Composable
fun liquidGlassBorder(): BorderStroke = BorderStroke(
    width = 1.dp,
    brush = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.46f),
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.24f),
        ),
    ),
)

/**
 * Tarjeta tonal Pixel: jerarquía por `surfaceContainerLow`, borde de 24 dp y
 * aire interno constante en todas las pantallas. Con Liquid Glass activo la
 * superficie se vuelve translúcida y gana un borde especular de 1 dp.
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = null,
    ) { body() }
}
