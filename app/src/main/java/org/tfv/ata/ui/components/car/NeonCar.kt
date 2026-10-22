package org.tfv.ata.ui.components.car

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/** Car state shown in the drawing (from the last status read). */
data class CarStatus(
  val charging: Boolean = false,
  val climate: Boolean = false,
  val sentry: Boolean = false,
)

/** Colours of the neon drawing; [glow] adds light (for dark backgrounds). */
data class NeonStyle(
  val line: Color,
  val glow: Boolean,
  val warm: Color = Color(0xFFFF8A3D),
  val green: Color = Color(0xFF39FF88),
  val red: Color = Color(0xFFFF3B30),
  val white: Color = Color(0xFFE8F6FF),
  val lineWidth: Float = 2.2f,
)

private val CHARGE_GREEN = Color(0xFF39FF88)

/**
 * Draws the neon car seen through [camera]: floor glow and reflection, body lines with a light band that moves
 * over the car while it turns, opened doors and lids, and effects such as flashing lights, heating or the
 * bioweapon defence fog around (never inside) the car.
 */
fun DrawScope.drawNeonCar(
  shape: CarShape,
  camera: CarCamera,
  yawDeg: Float,
  fx: CarFxState,
  status: CarStatus,
  now: Long,
  style: NeonStyle,
  area: Size = size,
) {
  val t = now / 1000f
  val blend = if (style.glow) BlendMode.Plus else BlendMode.SrcOver
  val highlightX = area.width / 2f + sin(Math.toRadians(yawDeg.toDouble()) * 1.7).toFloat() * area.width * 0.38f

  // Floor glow.
  val floor = (0..32).map { i ->
    val a = 2 * PI * i / 32
    camera.project(V3((1.6 * cos(a)).toFloat(), 0f, (2.9 * shape.stretch * sin(a)).toFloat())).offset
  }
  val minX = floor.minOf { it.x }; val maxX = floor.maxOf { it.x }
  val minY = floor.minOf { it.y }; val maxY = floor.maxOf { it.y }
  drawOval(
    Brush.radialGradient(
      listOf(style.line.copy(alpha = if (style.glow) .22f else .10f), Color.Transparent),
      center = Offset((minX + maxX) / 2, (minY + maxY) / 2),
      radius = (maxX - minX) / 2,
    ),
    topLeft = Offset(minX, minY),
    size = Size(maxX - minX, maxY - minY),
  )

  // Wake: a ring running out over the floor.
  fx.pulse(CarEffect.WAKE, now, 1600)?.let { p -> floorRing(camera, 1.2f + 3.5f * p, style.line.copy(alpha = (1 - p) * .9f), blend, style.lineWidth * 1.5f) }
  // Locate sound: three rings.
  fx.pulse(CarEffect.LOCATE, now, 2400)?.let { p ->
    for (k in 0..2) {
      val q = ((p * 1.5f) - k * .25f).coerceIn(0f, 1f)
      if (q > 0f && q < 1f) floorRing(camera, 1.0f + 4f * q, style.white.copy(alpha = (1 - q) * .8f), blend, style.lineWidth)
    }
  }

  // Bioweapon defence: green fog drifting around the car, outside the body.
  val bio = fx.openness(CarEffect.BIOWEAPON, now, 1500)
  if (bio > 0f) {
    for (i in 0 until 18) {
      val a = 2 * PI * i / 18 + t * .15 + sin(t * .4 + i) * .2
      val ring = 1.18f + .12f * sin(t * .7f + i * 1.3f)
      val p = camera.project(V3((1.05 * ring * cos(a)).toFloat(), .35f + .55f * ((i % 3) / 2f), (2.45 * shape.stretch * ring * sin(a)).toFloat()))
      val r = area.minDimension * (.10f + .03f * sin(t * .9f + i))
      drawCircle(
        Brush.radialGradient(listOf(style.green.copy(alpha = .20f * bio), Color.Transparent), center = p.offset, radius = r),
        radius = r,
        center = p.offset,
        blendMode = blend,
      )
    }
  }

  // Reflection on the floor.
  val reflection = style.line.copy(alpha = if (style.glow) .13f else .07f)
  (shape.body + shape.wheels).forEach { line ->
    val path = Path()
    line.forEachIndexed { i, p -> camera.projectReflection(p).offset.let { if (i == 0) path.moveTo(it.x, it.y) else path.lineTo(it.x, it.y) } }
    drawPath(path, reflection, style = Stroke(style.lineWidth))
  }

  fun polyline(points: List<V3>, color: Color, width: Float = style.lineWidth, alpha: Float = 1f) {
    if (points.size < 2 || alpha <= 0f) return
    val projected = points.map { camera.project(it) }
    val path = Path()
    projected.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.offset.x, p.offset.y) else path.lineTo(p.offset.x, p.offset.y) }
    if (style.glow) {
      drawPath(path, color.copy(alpha = .10f * alpha), style = Stroke(width * 6f, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = blend)
      drawPath(path, color.copy(alpha = .24f * alpha), style = Stroke(width * 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = blend)
    }
    for (i in 0 until projected.size - 1) {
      val a = projected[i]
      val b = projected[i + 1]
      val depth = ((a.depth + b.depth) / 2f + 2.6f) / 5.2f
      val mid = (a.offset.x + b.offset.x) / 2f
      val spec = exp(-((mid - highlightX) / (area.width * .11f)).let { it * it })
      val c = lerp(color, style.white, spec * .65f)
      drawLine(c.copy(alpha = alpha * (.38f + .62f * depth.coerceIn(0f, 1f))), a.offset, b.offset, width, StrokeCap.Round)
    }
  }

  // Interior effects first (seen through the glass).
  val climate = maxOf(fx.openness(CarEffect.CLIMATE, now, 1200), if (status.climate) 1f else 0f)
  if (climate > 0f) {
    val c = camera.project(shape.cabinCenter)
    val r = area.minDimension * .22f
    drawCircle(
      Brush.radialGradient(listOf(style.line.copy(alpha = .30f * climate * (.8f + .2f * sin(t * 2))), Color.Transparent), center = c.offset, radius = r),
      radius = r,
      center = c.offset,
      blendMode = blend,
    )
  }
  val wheelHeat = fx.openness(CarEffect.WHEEL_HEAT, now, 1200)
  polyline(shape.steeringWheel, lerp(style.line, style.warm, wheelHeat), alpha = .45f + .55f * wheelHeat * (.75f + .25f * sin(t * 3)))
  val defrost = fx.openness(CarEffect.DEFROST, now, 1500)
  if (defrost > 0f) shape.defrostLines.forEachIndexed { i, line -> polyline(line, style.warm, alpha = defrost * (.55f + .45f * sin(t * 2.5f + i))) }

  // Body.
  shape.body.forEach { polyline(it, style.line) }
  shape.wheels.forEach { polyline(it, style.line, alpha = .85f) }

  // Doors: seams when closed, the door swung open around its hinge when opened.
  for ((effect, side) in listOf(CarEffect.DOOR_DRIVER to -1f, CarEffect.DOOR_PASSENGER to 1f)) {
    val open = fx.openness(effect, now)
    val angle = -side * 60f * open
    val hinge = shape.frontDoorHinge(side)
    shape.frontDoor(side).forEach { line ->
      polyline(line.map { rotateY(it, hinge, angle) }, style.line, alpha = .35f + .65f * open)
    }
  }
  // Frunk and trunk lids.
  val frunk = fx.openness(CarEffect.FRUNK, now)
  polyline(shape.hood.map { rotateX(it, shape.hoodHinge, -55f * frunk) }, style.line, alpha = .35f + .65f * frunk)
  val trunk = fx.openness(CarEffect.TRUNK, now)
  polyline(shape.trunkLid.map { rotateX(it, shape.trunkHinge, 50f * trunk) }, style.line, alpha = .35f + .65f * trunk)
  // Charge port flap.
  val port = fx.openness(CarEffect.CHARGE_PORT, now)
  polyline(shape.chargePort.map { rotateY(it, shape.chargePortHinge, 80f * port) }, if (status.charging) CHARGE_GREEN else style.line, alpha = .5f + .5f * port)
  if (status.charging || port > 0f) {
    val c = camera.project(shape.chargePortCenter)
    val pulse = if (status.charging) .55f + .45f * sin(t * 3) else port * .6f
    val r = area.minDimension * .07f
    drawCircle(Brush.radialGradient(listOf(CHARGE_GREEN.copy(alpha = .8f * pulse), Color.Transparent), center = c.offset, radius = r), r, c.offset, blendMode = blend)
  }
  // Windows lowered: the glass edge drops to the belt line.
  val windows = fx.openness(CarEffect.WINDOWS, now)
  if (windows > 0f) for (side in listOf(-1f, 1f)) polyline(shape.windowDown(side), style.white, alpha = windows)

  // Lights.
  val lockPulse = fx.pulse(CarEffect.LOCK, now, 700)
  val unlockPulse = fx.pulse(CarEffect.UNLOCK, now, 1200)
  val flash = fx.pulse(CarEffect.LIGHTS, now, 1400)
  val lightBoost = when {
    flash != null -> if ((flash * 6).toInt() % 2 == 0) 1f else 0f
    unlockPulse != null -> if ((unlockPulse * 4).toInt() % 2 == 0) 1f else 0f
    lockPulse != null -> 1f - lockPulse
    else -> 0f
  }
  shape.headlights.forEach { polyline(it, style.white, width = style.lineWidth * 1.3f) }
  polyline(shape.taillight, style.red, width = style.lineWidth * 1.3f, alpha = .9f + .1f * lightBoost)
  if (lightBoost > 0f) {
    for (p in listOf(shape.headlightLeft, shape.headlightRight)) {
      val c = camera.project(p)
      val r = area.minDimension * .16f
      drawCircle(Brush.radialGradient(listOf(style.white.copy(alpha = .85f * lightBoost), Color.Transparent), center = c.offset, radius = r), r, c.offset, blendMode = blend)
    }
  }
  // Horn: sound arcs in front of the car.
  fx.pulse(CarEffect.HONK, now, 1200)?.let { p ->
    val c = camera.project(shape.frontCenter).offset
    for (k in 0..2) {
      val q = (p * 1.6f - k * .3f).coerceIn(0f, 1f)
      if (q > 0f && q < 1f) {
        val r = area.minDimension * (.06f + .25f * q)
        drawCircle(style.white.copy(alpha = (1 - q) * .8f), r, c, style = Stroke(style.lineWidth * 1.4f), blendMode = blend)
      }
    }
  }
  // Boombox: little clouds puffing out at the back.
  fx.pulse(CarEffect.PUFF, now, 2200)?.let { p ->
    val rear = shape.rearCenter
    for (k in 0 until 6) {
      val q = (p * 1.3f - k * .06f).coerceIn(0f, 1f)
      val drift = V3(sin(k * 1.7f) * .5f * q, .2f * q + k * .05f, -1.2f * q)
      val c = camera.project(rear + drift)
      val r = area.minDimension * (.03f + .08f * q)
      drawCircle(Brush.radialGradient(listOf(style.green.copy(alpha = .35f * (1 - q)), Color.Transparent), center = c.offset, radius = r), r, c.offset, blendMode = blend)
    }
  }
  // Sentry: a red eye on the B-pillars.
  if (status.sentry) {
    for (side in listOf(-1f, 1f)) {
      val c = camera.project(shape.sentryEye(side))
      val r = area.minDimension * .025f * (1.2f + .3f * sin(t * 4))
      drawCircle(style.red.copy(alpha = .9f), r, c.offset, blendMode = blend)
    }
  }
}

private fun DrawScope.floorRing(camera: CarCamera, radius: Float, color: Color, blend: BlendMode, width: Float) {
  val path = Path()
  for (i in 0..48) {
    val a = 2 * PI * i / 48
    val p = camera.project(V3((radius * .8f * cos(a)).toFloat(), 0f, (radius * 1.3f * sin(a)).toFloat())).offset
    if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
  }
  drawPath(path, color, style = Stroke(width), blendMode = blend)
}

fun rotateY(p: V3, pivot: V3, degrees: Float): V3 {
  if (abs(degrees) < 0.01f) return p
  val a = Math.toRadians(degrees.toDouble())
  val x = p.x - pivot.x
  val z = p.z - pivot.z
  return V3((x * cos(a) + z * sin(a)).toFloat() + pivot.x, p.y, (-x * sin(a) + z * cos(a)).toFloat() + pivot.z)
}

fun rotateX(p: V3, pivot: V3, degrees: Float): V3 {
  if (abs(degrees) < 0.01f) return p
  val a = Math.toRadians(degrees.toDouble())
  val y = p.y - pivot.y
  val z = p.z - pivot.z
  return V3(p.x, (y * cos(a) - z * sin(a)).toFloat() + pivot.y, (y * sin(a) + z * cos(a)).toFloat() + pivot.z)
}
