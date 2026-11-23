package com.steefwel28.consumoenergetico.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import com.steefwel28.consumoenergetico.core.signature.CraftedBy

/**
 * Emblema de la app: tres fichas apiladas con ligero desplazamiento y la
 * chispa de cuatro puntas, en silueta monocromática. [appear] (0..1) escalona
 * la entrada: primero la chispa y luego las fichas de arriba hacia abajo.
 * Compartido por el splash y el easter egg de Ajustes.
 */
@CraftedBy("steefwel28")
@Composable
fun PixelEmblem(
    color: Color,
    modifier: Modifier = Modifier,
    appear: Float = 1f,
) {
    Canvas(modifier = modifier) {
        val scale = size.minDimension / 108f
        val spark = Path().apply {
            moveTo(54f, 23f)
            cubicTo(55.5f, 29f, 57f, 30.5f, 63f, 33f)
            cubicTo(57f, 35.5f, 55.5f, 37f, 54f, 43f)
            cubicTo(52.5f, 37f, 51f, 35.5f, 45f, 33f)
            cubicTo(51f, 30.5f, 52.5f, 29f, 54f, 23f)
            close()
        }
        val capsules = listOf(47f to 28f, 59f to 31f, 71f to 34f).map { (top, left) ->
            Path().apply {
                addRoundRect(RoundRect(Rect(left, top, left + 46f, top + 11f), CornerRadius(5.5f)))
            }
        }

        fun drawElement(path: Path, index: Int, pivotX: Float, pivotY: Float) {
            val p = ((appear - index * 0.18f) / 0.4f).coerceIn(0f, 1f)
            if (p <= 0f) return
            withTransform({
                scale(scale, scale)
                scale(0.6f + 0.4f * p, 0.6f + 0.4f * p, androidx.compose.ui.geometry.Offset(pivotX, pivotY))
            }) {
                drawPath(path, color.copy(alpha = p))
            }
        }

        drawElement(spark, 0, 54f, 33f)
        capsules.forEachIndexed { index, path ->
            drawElement(path, index + 1, 54f, 52.5f + index * 12f)
        }
    }
}