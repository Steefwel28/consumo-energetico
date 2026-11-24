package com.steefwel28.consumoenergetico.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.steefwel28.consumoenergetico.ui.theme.LocalLiquidGlass

/**
 * Lente líquido AGSL estilo iOS 26/27 para los controles flotantes (barra
 * inferior, menú `☰`, hojas modales y diálogos).
 *
 * Dibuja sobre el fondo ya desenfocado por `haze`: refracción de borde,
 * dispersión cromática del canto y brillo especular superior. Las tarjetas
 * principales no lo usan, para que el contenido quede limpio y legible.
 *
 * AGSL no puede muestrear el backdrop, así que la refracción se simula con
 * bandas desplazadas sobre el SDF del rectángulo redondeado.
 *
 * shortcut: requiere Android 13+ (RuntimeShader); en versiones previas queda
 * el tinte + desenfoque de `haze`. Se usa shader nativo en vez de
 * `io.github.kyant0:backdrop` para no arrastrar un baseline Kotlin/Compose
 * distinto al del proyecto (Kotlin 2.3.21, AGP 9.5.1, SDK 37).
 */
@Composable
fun Modifier.liquidGlassLens(
    cornerRadius: Dp = 24.dp,
    intensity: Float = 1f,
): Modifier {
    if (!LocalLiquidGlass.current) return this
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this
    return liquidLensShader(cornerRadius, intensity)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun Modifier.liquidLensShader(
    cornerRadius: Dp,
    intensity: Float,
): Modifier {
    val radiusPx = with(LocalDensity.current) { cornerRadius.toPx() }
    return drawWithCache {
        val shader = RuntimeShader(LIQUID_LENS_AGSL).apply {
            setFloatUniform("uSize", size.width, size.height)
            setFloatUniform(
                "uCorner",
                radiusPx.coerceAtMost(size.minDimension / 2f),
            )
            setFloatUniform("uIntensity", intensity.coerceIn(0f, 1f))
        }
        val brush = ShaderBrush(shader)
        onDrawBehind { drawRect(brush) }
    }
}

private const val LIQUID_LENS_AGSL = """
uniform float2 uSize;
uniform float uCorner;
uniform float uIntensity;

float sdRoundRect(float2 p, float2 halfSize, float r) {
    float2 q = abs(p) - (halfSize - r);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 fragCoord) {
    float2 halfSize = max(uSize * 0.5, float2(1.0));
    float2 p = fragCoord - halfSize;
    float d = sdRoundRect(p, halfSize, min(uCorner, min(halfSize.x, halfSize.y)));

    // Canto refractado: tres bandas desplazadas simulan dispersión cromática.
    float rimR = 1.0 - smoothstep(0.0, 9.0, abs(d + 3.5));
    float rimG = 1.0 - smoothstep(0.0, 9.0, abs(d));
    float rimB = 1.0 - smoothstep(0.0, 9.0, abs(d - 3.5));
    half3 dispersion = half3(rimR, rimG, rimB) * (0.42 * uIntensity);

    // Brillo especular del canto superior izquierdo (luz del sistema).
    float2 lightPos = float2(p.x + halfSize.x * 0.55, p.y + halfSize.y * 0.75);
    float spec = pow(clamp(1.0 - length(lightPos) / (halfSize.x * 1.15), 0.0, 1.0), 3.0);

    // Tinte translúcido esmeralda: más denso en la base de la superficie.
    float vertical = clamp(fragCoord.y / max(uSize.y, 1.0), 0.0, 1.0);
    half3 tint = mix(half3(0.086, 0.196, 0.180), half3(0.039, 0.122, 0.110), vertical);

    half3 color = tint + dispersion + half3(spec * 0.20);
    float alpha = clamp(0.46 * uIntensity + rimG * 0.22 + spec * 0.16, 0.0, 0.82);
    return half4(color, alpha);
}
"""
