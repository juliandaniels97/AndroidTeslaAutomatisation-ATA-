package org.tfv.ata.ui.components.car

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.exp
import kotlin.math.sin

/** Battery level colours: 100–80 % dark blue, 80–20 % blue, 20–10 % yellow, below 10 % red. */
fun batteryColor(level: Int): Color = when {
  level >= 80 -> Color(0xFF2E5BFF)
  level >= 20 -> Color(0xFF4FB4FF)
  level >= 10 -> Color(0xFFFFD60A)
  else -> Color(0xFFFF3B30)
}

/**
 * Neon battery ring with a slow, viscous glow: the brightness flows gently along the arc like thick liquid.
 * Dashed while the level is unknown; while charging a brighter drop runs around the ring.
 */
fun DrawScope.drawBatteryRing(
  center: Offset,
  radius: Float,
  level: Int?,
  charging: Boolean,
  now: Long,
  glow: Boolean,
  unknownColor: Color,
  width: Float = 13f,
) {
  val t = now / 1000f
  val blend = if (glow) BlendMode.Plus else BlendMode.SrcOver
  val topLeft = Offset(center.x - radius, center.y - radius)
  val box = Size(radius * 2, radius * 2)
  if (level == null) {
    drawCircle(unknownColor.copy(alpha = .4f), radius, center, style = Stroke(4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 14f))))
    return
  }
  val color = batteryColor(level)
  drawCircle(color.copy(alpha = .10f), radius, center, style = Stroke(width))
  val sweep = 360f * level.coerceIn(0, 100) / 100f
  val segments = maxOf(2, (sweep / 3f).toInt())
  val step = sweep / segments
  val blob = (t * 14f) % 360f
  for (i in 0 until segments) {
    val a = -90f + i * step
    val rel = i * step
    // Slow interference of two waves plus a soft drop moving along the arc: subtle, never flickering.
    var light = .80f + .10f * sin(rel / 57.3f * 3f + t * .5f) + .06f * sin(rel / 57.3f * 7f - t * .8f)
    val d = ((rel - blob + 540f) % 360f) - 180f
    light += .14f * exp(-(d / 28f) * (d / 28f))
    if (charging) {
      val c = ((rel - (t * 90f) % 360f + 540f) % 360f) - 180f
      light += .35f * exp(-(c / 14f) * (c / 14f))
    }
    val alpha = light.coerceIn(0f, 1f)
    if (glow) {
      drawArc(color.copy(alpha = .10f * alpha), a, step, false, topLeft, box, style = Stroke(width * 3.2f), blendMode = blend)
      drawArc(color.copy(alpha = .22f * alpha), a, step, false, topLeft, box, style = Stroke(width * 1.7f), blendMode = blend)
    }
    drawArc(color.copy(alpha = alpha), a, step + .6f, false, topLeft, box, style = Stroke(width, cap = if (i == 0 || i == segments - 1) StrokeCap.Round else StrokeCap.Butt))
  }
}
