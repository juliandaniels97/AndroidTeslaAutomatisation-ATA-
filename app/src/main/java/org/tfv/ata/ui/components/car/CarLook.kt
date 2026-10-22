package org.tfv.ata.ui.components.car

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import org.tfv.ata.core.BodyStyle
import org.tfv.ata.core.Vehicle
import org.tfv.ata.ui.theme.AtaColors

/** How the car is drawn for the current design, theme and the vehicle's colour choices. */
data class CarLook(val style: NeonStyle, val flat: FlatStyle, val material: Boolean)

/**
 * Neon: glowing lines in the vehicle's neon colour (darker on light backgrounds). Material: a solid car in the
 * vehicle's Tesla paint with black glass roof, trim and wheels, effects in the theme's colours.
 * [dark] overrides the theme's brightness (the neon full-screen view is always dark).
 */
@Composable
fun rememberCarLook(vehicle: Vehicle?, dark: Boolean = AtaColors.current.isDark, lineWidth: Float = 2.4f): CarLook {
  val palette = AtaColors.current
  val scheme = MaterialTheme.colorScheme
  if (palette.material) {
    val paint = Color((vehicle?.carPaint ?: org.tfv.ata.core.CarPaint.defaultFor(vehicle?.body ?: BodyStyle.MODEL_3)).argb)
    val solid = SolidColors(
      body = paint,
      glass = Color(0xFF14181E),
      roof = Color(0xFF0B0D10),
      tyre = Color(0xFF141518),
      rim = Color(0xFF3B3F45),
      interior = Color(0xFF0C0D0F),
      seam = lerp(paint, Color.Black, .45f),
      shadow = Color.Black.copy(alpha = if (dark) .45f else .28f),
    )
    return CarLook(
      style = NeonStyle(
        line = scheme.primary,
        glow = false,
        warm = Color(0xFFFF8A3D),
        green = palette.success,
        red = Color(0xFFE0262F),
        white = Color(0xFFF7FAFF),
        lineWidth = lineWidth,
        solid = solid,
      ),
      flat = FlatStyle(paint, Color(0xFF1E242C), scheme.primary, dark, green = palette.success),
      material = true,
    )
  }
  val neon = Color((vehicle?.carNeon ?: org.tfv.ata.core.NeonColor.BLUE).argb)
  return CarLook(
    // On light backgrounds the lines get darker and a bit stronger, so they read like ink instead of light.
    style = NeonStyle(line = if (dark) neon else lerp(neon, Color(0xFF06121F), .5f), glow = dark, lineWidth = if (dark) lineWidth else lineWidth + 1f),
    flat = FlatStyle(palette.carBody, palette.carGlass, palette.accent, dark),
    material = false,
  )
}
