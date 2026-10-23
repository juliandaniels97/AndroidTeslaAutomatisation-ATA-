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
 * Dashed while the level is unknown. While charging, green dots climb slowly along the ring up to the current
 * level (a little faster with more [chargeKw]), the tip breathes and a faint dashed arc shows the way to the
 * [chargeLimit].
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
  /** Material design: an even, solid arc with round ends instead of the flowing neon light. */
  material: Boolean = false,
  chargeKw: Int = 0,
  chargeLimit: Int? = null,
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
  if (material) {
    drawCircle(color.copy(alpha = .16f), radius, center, style = Stroke(width))
    val sweepAll = 360f * level.coerceIn(0, 100) / 100f
    drawArc(color, -90f, sweepAll, false, topLeft, box, style = Stroke(width, cap = StrokeCap.Round))
    if (charging) drawChargeFlow(center, radius, level, chargeKw, chargeLimit, t, width, color, glow, true, topLeft, box)
    return
  }
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
    val alpha = light.coerceIn(0f, 1f)
    if (glow) {
      drawArc(color.copy(alpha = .10f * alpha), a, step, false, topLeft, box, style = Stroke(width * 3.2f), blendMode = blend)
      drawArc(color.copy(alpha = .22f * alpha), a, step, false, topLeft, box, style = Stroke(width * 1.7f), blendMode = blend)
    }
    drawArc(color.copy(alpha = alpha), a, step + .6f, false, topLeft, box, style = Stroke(width, cap = if (i == 0 || i == segments - 1) StrokeCap.Round else StrokeCap.Butt))
  }
  if (charging) drawChargeFlow(center, radius, level, chargeKw, chargeLimit, t, width, color, glow, false, topLeft, box)
}

/** Charging animation under the ring: target arc to the limit, flowing drops up to the level, breathing tip. */
private fun DrawScope.drawChargeFlow(
  center: Offset,
  radius: Float,
  level: Int,
  kw: Int,
  limit: Int?,
  t: Float,
  width: Float,
  color: Color,
  glow: Boolean,
  material: Boolean,
  topLeft: Offset,
  box: Size,
) {
  val sweep = 360f * level.coerceIn(0, 100) / 100f
  val target = limit?.coerceIn(level, 100)
  if (target != null && target > level) {
    drawArc(
      color.copy(alpha = .35f), -90f + sweep, 360f * (target - level) / 100f, false, topLeft, box,
      style = Stroke(width * .35f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 9f))),
    )
  }
  if (sweep < 8f) return
  // Green dots climb slowly along the charged part of the ring towards the tip, like energy flowing in; a little
  // faster with more power, never hectic. Each fades in at the start and out at the tip.
  val green = if (material) Color(0xFF2E9E5B) else Color(0xFF39FF88)
  val speed = (14f + kw * .12f).coerceAtMost(40f)
  val spacing = 26f
  val count = (sweep / spacing).toInt().coerceAtLeast(1)
  val shift = (t * speed) % spacing
  for (k in 0..count) {
    val pos = k * spacing + shift
    if (pos > sweep) continue
    val f = pos / sweep
    val fade = minOf(1f, f * 6f, (1f - f) * 6f).coerceIn(0f, 1f)
    val rad = Math.toRadians((-90f + pos).toDouble())
    val p = Offset(center.x + radius * kotlin.math.cos(rad).toFloat(), center.y + radius * kotlin.math.sin(rad).toFloat())
    if (glow) drawCircle(green.copy(alpha = .28f * fade), width * .9f, p, blendMode = BlendMode.Plus)
    drawCircle(green.copy(alpha = .95f * fade), width * .3f, p)
  }
  // Breathing tip at the current level.
  val breathe = .5f + .5f * sin(t * 3.2f)
  val a = Math.toRadians((-90f + sweep).toDouble())
  val tip = Offset(center.x + radius * kotlin.math.cos(a).toFloat(), center.y + radius * kotlin.math.sin(a).toFloat())
  drawCircle(color.copy(alpha = .18f + .22f * breathe), width * (1.1f + .9f * breathe), tip)
  drawCircle(if (material) color else Color.White.copy(alpha = .85f), width * .32f, tip)
}
