package org.tfv.ata.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.tfv.ata.core.VehicleSnapshot
import org.tfv.ata.ui.theme.AtaColors
import org.tfv.ata.ui.theme.AtaPalette
import kotlin.math.min

/**
 * The car seen from above inside a battery ring. Charging lights the charge port and runs a glow around the
 * ring, climate tints the glass roof, and the chips below show locks, charging, climate and Sentry.
 */
@Composable
fun VehicleArt(snapshot: VehicleSnapshot?, modifier: Modifier = Modifier) {
  val battery = snapshot?.batteryPercent ?: 0
  val known = snapshot != null && snapshot.updatedAt > 0
  val charging = snapshot?.charging == true
  val climate = snapshot?.climateOn == true
  val palette = AtaColors.current

  val transition = rememberInfiniteTransition(label = "vehicle")
  val pulse by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "pulse",
  )
  val sweep by transition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
    label = "sweep",
  )
  val animatedBattery by animateFloatAsState(
    targetValue = if (known) battery.coerceIn(0, 100) / 100f else 0f,
    animationSpec = tween(900, easing = FastOutSlowInEasing),
    label = "battery",
  )
  val ringColor by animateColorAsState(
    targetValue = when {
      !known -> palette.muted
      charging -> palette.success
      battery <= 15 -> palette.danger
      battery <= 30 -> palette.warning
      else -> palette.accent
    },
    animationSpec = tween(600),
    label = "ring",
  )

  Box(modifier = modifier.fillMaxWidth().height(270.dp).padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxWidth().height(250.dp)) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = min(size.width, size.height) / 2f - 12f
      val ring = 14f

      // Battery ring: dashed track while the level is unknown.
      drawCircle(
        color = ringColor.copy(alpha = if (known) 0.14f else 0.35f),
        radius = radius,
        center = center,
        style = Stroke(width = if (known) ring else 4f, pathEffect = if (known) null else PathEffect.dashPathEffect(floatArrayOf(10f, 14f))),
      )
      if (known) {
        drawArc(
          brush = Brush.sweepGradient(listOf(ringColor.copy(alpha = 0.6f), ringColor, ringColor.copy(alpha = 0.6f)), center),
          startAngle = -90f,
          sweepAngle = 360f * animatedBattery,
          useCenter = false,
          topLeft = Offset(center.x - radius, center.y - radius),
          size = Size(radius * 2, radius * 2),
          style = Stroke(width = ring, cap = StrokeCap.Round),
        )
      }
      if (charging) {
        drawArc(
          color = palette.success.copy(alpha = 0.55f),
          startAngle = sweep,
          sweepAngle = 40f,
          useCenter = false,
          topLeft = Offset(center.x - radius, center.y - radius),
          size = Size(radius * 2, radius * 2),
          style = Stroke(width = ring, cap = StrokeCap.Round),
        )
      }

      drawCar(center, length = radius * 1.42f, palette = palette, climateGlow = if (climate) 0.35f + 0.35f * pulse else 0f, chargeGlow = if (charging) 0.4f + 0.6f * pulse else 0f)
    }

    // State chips below the car.
    if (known) {
      Row(
        Modifier.align(Alignment.BottomCenter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        snapshot?.locked?.let { locked ->
          StateChip(if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen, if (locked) palette.success else palette.warning)
        }
        if (charging) StateChip(Icons.Filled.Bolt, palette.success)
        if (climate) StateChip(Icons.Filled.AcUnit, palette.accent)
        if (snapshot?.sentry == true) StateChip(Icons.Filled.Shield, palette.critical)
      }
    }
  }
}

@Composable
private fun StateChip(icon: ImageVector, tint: Color) {
  Box(
    Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp),
    contentAlignment = Alignment.Center,
  ) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
  }
}

/** Top-down car, front pointing up (Model 3/Y-like proportions). */
private fun DrawScope.drawCar(center: Offset, length: Float, palette: AtaPalette, climateGlow: Float, chargeGlow: Float) {
  val width = length * 0.46f
  val top = center.y - length / 2f
  val bottom = center.y + length / 2f
  val left = center.x - width / 2f
  val right = center.x + width / 2f
  fun x(f: Float) = center.x + f * width / 2f // f = -1 … 1
  fun y(f: Float) = top + f * length // f = 0 … 1

  // Soft shadow.
  val shadowAlpha = if (palette.isDark) 0.5f else 0.12f
  drawOval(
    brush = Brush.radialGradient(
      listOf(Color.Black.copy(alpha = shadowAlpha), Color.Transparent),
      center = Offset(center.x, center.y + length * 0.03f),
      radius = length * 0.55f,
    ),
    topLeft = Offset(left - width * 0.2f, top - length * 0.02f),
    size = Size(width * 1.4f, length * 1.1f),
  )

  // Wheels (peek out at the sides).
  val wheelW = width * 0.13f
  val wheelH = length * 0.15f
  val wheel = Color(0xFF15181D)
  listOf(0.16f, 0.71f).forEach { fy ->
    listOf(left - wheelW * 0.35f, right - wheelW * 0.65f).forEach { wx ->
      drawRoundRect(wheel, Offset(wx, y(fy)), Size(wheelW, wheelH), CornerRadius(wheelW * 0.4f))
    }
  }

  // Body.
  val body = Path().apply {
    moveTo(center.x, top)
    cubicTo(x(0.62f), top, x(0.96f), y(0.03f), x(0.98f), y(0.13f))
    cubicTo(x(1.02f), y(0.30f), x(0.97f), y(0.45f), x(0.99f), y(0.62f))
    cubicTo(x(1.0f), y(0.80f), x(0.98f), y(0.95f), x(0.70f), y(0.99f))
    cubicTo(x(0.40f), bottom, x(-0.40f), bottom, x(-0.70f), y(0.99f))
    cubicTo(x(-0.98f), y(0.95f), x(-1.0f), y(0.80f), x(-0.99f), y(0.62f))
    cubicTo(x(-0.97f), y(0.45f), x(-1.02f), y(0.30f), x(-0.98f), y(0.13f))
    cubicTo(x(-0.96f), y(0.03f), x(-0.62f), top, center.x, top)
    close()
  }
  drawPath(
    body,
    Brush.horizontalGradient(
      listOf(palette.carBody, lerp(palette.carBody, Color.White, 0.22f), palette.carBody),
      startX = left,
      endX = right,
    ),
  )

  // Mirrors.
  listOf(-1f, 1f).forEach { side ->
    drawOval(
      palette.carBody,
      topLeft = Offset(if (side < 0) left - width * 0.11f else right - width * 0.03f, y(0.30f)),
      size = Size(width * 0.14f, length * 0.035f),
    )
  }

  // Glass: windshield, panoramic roof, rear window in one piece.
  val glass = Path().apply {
    moveTo(x(-0.62f), y(0.31f))
    cubicTo(x(-0.40f), y(0.25f), x(0.40f), y(0.25f), x(0.62f), y(0.31f))
    cubicTo(x(0.78f), y(0.48f), x(0.78f), y(0.66f), x(0.66f), y(0.80f))
    cubicTo(x(0.35f), y(0.84f), x(-0.35f), y(0.84f), x(-0.66f), y(0.80f))
    cubicTo(x(-0.78f), y(0.66f), x(-0.78f), y(0.48f), x(-0.62f), y(0.31f))
    close()
  }
  val glassTop = lerp(palette.carGlass, palette.accent, climateGlow)
  drawPath(glass, Brush.verticalGradient(listOf(lerp(glassTop, Color.White, 0.12f), glassTop, palette.carGlass), startY = y(0.27f), endY = y(0.84f)))
  // Roof divider (B-pillar line) and reflection.
  drawLine(palette.carBody.copy(alpha = 0.6f), Offset(x(-0.72f), y(0.47f)), Offset(x(0.72f), y(0.47f)), strokeWidth = length * 0.008f)
  drawLine(Color.White.copy(alpha = 0.12f), Offset(x(-0.25f), y(0.36f)), Offset(x(-0.45f), y(0.70f)), strokeWidth = length * 0.012f, cap = StrokeCap.Round)

  // Headlights and taillight bar.
  val headlight = Color(0xFFDDF3FF)
  listOf(-1f, 1f).forEach { side ->
    rotate(if (side < 0) -28f else 28f, Offset(x(side * 0.66f), y(0.05f))) {
      drawRoundRect(headlight.copy(alpha = 0.9f), Offset(x(side * 0.66f) - width * 0.13f, y(0.05f) - length * 0.008f), Size(width * 0.26f, length * 0.016f), CornerRadius(length * 0.01f))
    }
  }
  val tail = Color(0xFFFF3B30)
  drawRoundRect(tail.copy(alpha = 0.85f), Offset(x(-0.78f), y(0.955f)), Size(width * 0.78f, length * 0.014f), CornerRadius(length * 0.01f))

  // Charge port (rear left) glows while charging.
  if (chargeGlow > 0f) {
    val port = Offset(x(-0.96f), y(0.86f))
    drawCircle(palette.success.copy(alpha = 0.25f * chargeGlow), radius = width * 0.16f, center = port)
    drawCircle(palette.success.copy(alpha = chargeGlow), radius = width * 0.05f, center = port)
  }
}
