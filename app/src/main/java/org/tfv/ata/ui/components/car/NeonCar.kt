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
  /** Battery level 0 … 100, null = unknown. */
  val battery: Int? = null,
)

/** Colours of the neon drawing; [glow] adds light (for dark backgrounds); [solid] draws a painted body instead. */
data class NeonStyle(
  val line: Color,
  val glow: Boolean,
  val warm: Color = Color(0xFFFF8A3D),
  val green: Color = Color(0xFF39FF88),
  val red: Color = Color(0xFFFF3B30),
  val white: Color = Color(0xFFE8F6FF),
  val lineWidth: Float = 2.2f,
  val solid: SolidColors? = null,
)

/** Material design: a solid, flat-shaded body in tonal colours with a soft shadow, without glow. */
data class SolidColors(
  val body: Color,
  val glass: Color,
  val roof: Color,
  val tyre: Color,
  val rim: Color,
  /** Inside of an opened door or lid. */
  val interior: Color,
  /** Panel gaps and details. */
  val seam: Color,
  val shadow: Color,
  /** Black plastic: rocker panels, bumper intakes, light lenses. */
  val trim: Color = Color(0xFF17181B),
)

private fun shade(base: Color, k: Float) = Color(
  (base.red * k).coerceIn(0f, 1f), (base.green * k).coerceIn(0f, 1f), (base.blue * k).coerceIn(0f, 1f), base.alpha,
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
  val solid = style.solid
  val blend = if (style.glow && solid == null) BlendMode.Plus else BlendMode.SrcOver
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
      if (solid != null) listOf(solid.shadow, solid.shadow.copy(alpha = solid.shadow.alpha * .5f), Color.Transparent)
      else listOf(style.line.copy(alpha = if (style.glow) .22f else .10f), Color.Transparent),
      center = Offset((minX + maxX) / 2, (minY + maxY) / 2),
      radius = (maxX - minX) / 2,
    ),
    topLeft = Offset(minX, minY),
    size = Size(maxX - minX, maxY - minY),
  )

  // Showroom floor: a faint grid that fades out with distance.
  if (style.glow && solid == null) {
    for (k in -3..3) {
      floorLine(camera, V3(k * 1f, 0f, -4.5f), V3(k * 1f, 0f, 4.5f), style.line.copy(alpha = .05f), blend)
    }
    for (k in -4..4) {
      floorLine(camera, V3(-3f, 0f, k * 1f), V3(3f, 0f, k * 1f), style.line.copy(alpha = .05f), blend)
    }
  }

  // Sentry: a red radar beam sweeping over the floor.
  val sentry = maxOf(fx.openness(CarEffect.SENTRY, now, 800), if (status.sentry) 1f else 0f)
  if (sentry > 0f) {
    val a0 = (t * 1.6f) % (2 * PI.toFloat())
    val beam = Path()
    val origin = camera.project(V3(0f, 0f, 0f)).offset
    beam.moveTo(origin.x, origin.y)
    for (k in 0..8) {
      val a = a0 + k * .05f
      val p = camera.project(V3(3.2f * cos(a), 0f, 3.2f * shape.stretch * sin(a))).offset
      beam.lineTo(p.x, p.y)
    }
    beam.close()
    drawPath(beam, style.red.copy(alpha = .16f * sentry), blendMode = blend)
  }

  // Remote drive: speed lines streaming backwards past the car.
  fx.pulse(CarEffect.DRIVE, now, 3000)?.let { p ->
    val strength = sin(p * PI).toFloat()
    for (k in 0 until 6) {
      val side = if (k % 2 == 0) 1f else -1f
      val x = side * (1.3f + .25f * (k / 2))
      val z = 3.5f - ((t * 4f + k * 1.3f) % 7f)
      floorLine(camera, V3(x, 0f, z), V3(x, 0f, z - .9f), style.white.copy(alpha = .5f * strength), blend, style.lineWidth)
    }
  }

  // Wake: a ring running out over the floor.
  fx.pulse(CarEffect.WAKE, now, 1600)?.let { p -> floorRing(camera, 1.2f + 3.5f * p, style.line.copy(alpha = (1 - p) * .9f), blend, style.lineWidth * 1.5f) }

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
  if (solid == null) (shape.body + shape.wheels).forEach { line ->
    val path = Path()
    line.forEachIndexed { i, p -> camera.projectReflection(p).offset.let { if (i == 0) path.moveTo(it.x, it.y) else path.lineTo(it.x, it.y) } }
    drawPath(path, reflection, style = Stroke(style.lineWidth))
  }

  fun polyline(points: List<V3>, color: Color, width: Float = style.lineWidth, alpha: Float = 1f) {
    if (points.size < 2 || alpha <= 0f) return
    val projected = points.map { camera.project(it) }
    val path = Path()
    projected.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.offset.x, p.offset.y) else path.lineTo(p.offset.x, p.offset.y) }
    if (solid != null) {
      drawPath(path, color.copy(alpha = alpha), style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
      return
    }
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
      // Lines further back fade (less on light backgrounds, where faint lines would vanish).
      val near = if (style.glow) .38f else .62f
      drawLine(c.copy(alpha = alpha * (near + (1 - near) * depth.coerceIn(0f, 1f))), a.offset, b.offset, width, StrokeCap.Round)
    }
  }

  fun fill(points: List<V3>, color: Color) {
    if (points.size < 3) return
    val path = Path()
    points.forEachIndexed { i, v -> camera.project(v).offset.let { if (i == 0) path.moveTo(it.x, it.y) else path.lineTo(it.x, it.y) } }
    path.close()
    drawPath(path, color)
  }
  /** Paint colour of a surface turned towards [normal]: lighter facing the viewer and the sky. */
  fun lit(base: Color, normal: V3): Color {
    val facing = camera.facing(normal).coerceAtLeast(0f)
    val k = .52f + .34f * facing + .24f * normal.y.coerceAtLeast(0f)
    // Clear-coat sheen on surfaces facing the viewer and the sky.
    val sheen = (facing * facing * .18f + normal.y.coerceAtLeast(0f) * .08f).coerceIn(0f, .25f)
    return lerp(shade(base, k), Color.White, sheen)
  }

  // Battery pack in the floor: cells light up with the charge level, a wave runs through them while charging.
  val charging = maxOf(fx.openness(CarEffect.CHARGING, now, 800), if (status.charging) 1f else 0f)
  if (solid == null) run {
    val cells = 10
    val level = (status.battery ?: 0).coerceIn(0, 100) / 100f
    polyline(shape.pack, style.line, width = style.lineWidth * .7f, alpha = .22f)
    for (i in 0 until cells) {
      val z0 = shape.packFront + (shape.packRear - shape.packFront) * i / cells
      val z1 = shape.packFront + (shape.packRear - shape.packFront) * (i + .8f) / cells
      val lit = status.battery != null && (cells - i) <= (level * cells + .5f)
      val wave = if (charging > 0f) .5f + .5f * sin(t * 4f + i * .7f) else 0f
      val color = if (charging > 0f) CHARGE_GREEN else style.line
      val a = if (lit) .30f + .35f * wave * charging else .06f
      polyline(listOf(V3(-shape.packHalfWidth * .92f, shape.packY, z0), V3(shape.packHalfWidth * .92f, shape.packY, z0), V3(shape.packHalfWidth * .92f, shape.packY, z1), V3(-shape.packHalfWidth * .92f, shape.packY, z1), V3(-shape.packHalfWidth * .92f, shape.packY, z0)), color, width = style.lineWidth * .6f, alpha = a)
    }
  }

  // Cabin: seats and dashboard seen through the glass.
  if (solid == null) {
    shape.seats.forEach { polyline(it, style.line, width = style.lineWidth * .7f, alpha = .3f) }
    polyline(shape.dashboard, style.line, width = style.lineWidth * .8f, alpha = .45f)
  }

  val climate = maxOf(fx.openness(CarEffect.CLIMATE, now, 1200), if (status.climate) 1f else 0f)
  val wheelHeat = fx.openness(CarEffect.WHEEL_HEAT, now, 1200)
  val defrost = fx.openness(CarEffect.DEFROST, now, 1500)

  // Solid body (Material): far wheels, painted surfaces back to front, near wheels with their rims.
  if (solid != null) {
    val nearSides = listOf(-1f, 1f).filter { camera.facing(V3(it, 0f, 0f)) > 0f }
    fun wheels(sides: List<Float>) {
      for ((side, zc) in shape.wheelCenters) if (side in sides) {
        fill(shape.wheelDisc(side, zc, shape.spec.wheelRadius), solid.tyre)
        fill(shape.wheelDisc(side, zc, shape.spec.wheelRadius * .66f), solid.rim)
      }
    }
    wheels(listOf(-1f, 1f) - nearSides.toSet())
    val glass = lerp(solid.glass, style.line, .35f * climate * (.8f + .2f * sin(t * 2)))
    shape.facets
      .filter { camera.facing(it.normal) > -.05f || it.surface == CarShape.Surface.ROOF }
      .sortedBy { camera.view(it.center).z }
      .forEach { f ->
        val base = when (f.surface) {
          CarShape.Surface.TRIM -> solid.trim
          CarShape.Surface.BODY -> solid.body
          CarShape.Surface.GLASS -> glass
          CarShape.Surface.ROOF -> lerp(solid.roof, style.line, .25f * climate)
        }
        fill(f.points, lit(base, f.normal))
      }
    for (side in nearSides) {
      for ((s2, zc) in shape.wheelCenters) if (s2 == side) fill(shape.archLiner(side, zc), solid.trim)
    }
    wheels(nearSides)
    for (side in nearSides) polyline(shape.sideCamera(side), solid.trim, width = style.lineWidth * 1.6f)
  }

  // Interior effects (neon: seen through the glass, drawn first; solid: on top of the glass).
  fun interior() {
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
    // Air streaming out of the vents (climate on, temperature set).
    val airPulse = fx.pulse(CarEffect.AIRFLOW, now, 2500)?.let { sin(it * PI).toFloat() } ?: 0f
    val air = maxOf(climate * .6f, airPulse)
    if (air > 0f) {
      for (i in 0 until 8) {
        val q = ((t * .45f + i / 8f) % 1f)
        val x = (-.6f + 1.2f * (i % 4) / 3f) * shape.spec.width
        val p = camera.project(V3(x, shape.spec.beltMid + .03f + .12f * q, shape.ventZ - 1.3f * q))
        val r = area.minDimension * (.008f + .014f * q)
        drawCircle(style.white.copy(alpha = .55f * air * (1 - q)), r, p.offset, blendMode = blend)
      }
    }
    // Music: equaliser bars on the dashboard and rings rising out of the cabin.
    fx.pulse(CarEffect.MEDIA, now, 2600)?.let { p ->
      val fade = sin(p * PI).toFloat()
      for (i in 0 until 5) {
        val x = (i - 2) * .09f
        val h = .06f + .14f * (.5f + .5f * sin(t * 9f + i * 1.9f))
        val base = V3(x, shape.spec.beltMid + .02f, shape.ventZ + .02f)
        polyline(listOf(base, base.copy(y = base.y + h)), style.white, width = style.lineWidth * 1.4f, alpha = fade)
      }
      for (k in 0..2) {
        val q = (p * 1.4f - k * .2f).coerceIn(0f, 1f)
        if (q > 0f && q < 1f) {
          val c = camera.project(shape.cabinCenter + V3(0f, .5f + q * 1.2f, 0f))
          drawCircle(style.line.copy(alpha = (1 - q) * .7f), area.minDimension * (.02f + .08f * q), c.offset, style = Stroke(style.lineWidth), blendMode = blend)
        }
      }
    }
    if (solid == null) {
      polyline(shape.steeringWheel, lerp(style.line, style.warm, wheelHeat), alpha = .45f + .55f * wheelHeat * (.75f + .25f * sin(t * 3)))
    } else if (wheelHeat > 0f) {
      val c = camera.project(shape.steeringWheel[0] + V3(-.17f, 0f, 0f))
      val r = area.minDimension * .08f
      drawCircle(Brush.radialGradient(listOf(style.warm.copy(alpha = .75f * wheelHeat * (.75f + .25f * sin(t * 3))), Color.Transparent), center = c.offset, radius = r), r, c.offset)
    }
    if (defrost > 0f) shape.defrostLines.forEachIndexed { i, line -> polyline(line, style.warm, alpha = defrost * (.55f + .45f * sin(t * 2.5f + i))) }
  }
  if (solid == null) interior()

  // Body.
  if (solid == null) {
    shape.body.forEach { polyline(it, style.line) }
    shape.wheels.forEach { polyline(it, style.line, alpha = .85f) }
  }
  // Rims: they turn while the car is driven remotely.
  val drive = fx.pulse(CarEffect.DRIVE, now, 3000)
  val spin = drive?.let { -(it * it * (3 - 2 * it)) * 6f * PI.toFloat() } ?: 0f
  val wakeP = fx.pulse(CarEffect.WAKE, now, 1600)
  val detail = solid?.seam ?: style.line
  for ((side, zc) in shape.wheelCenters) {
    if (solid != null && camera.facing(V3(side, 0f, 0f)) <= 0f) continue
    val flash = wakeP?.let { ((it - .55f) / .45f).coerceIn(0f, 1f).let { q -> sin(q * PI).toFloat() } } ?: 0f
    shape.rim(side, zc, spin).forEach { polyline(it, lerp(detail, style.white, flash), width = style.lineWidth * .8f, alpha = .7f + .3f * flash) }
  }
  shape.bumpers.forEach { if (solid != null) polyline(it, solid.trim, width = style.lineWidth * 3.2f) else polyline(it, detail, width = style.lineWidth * .8f, alpha = .6f) }
  shape.pillars.forEach { polyline(it, detail, width = style.lineWidth * .8f, alpha = .55f) }
  for (side in listOf(-1f, 1f)) polyline(shape.rearDoorSeam(side), detail, width = style.lineWidth * .8f, alpha = .4f)
  if (solid != null) interior()

  // Mirrors fold in when locking and out again when unlocking.
  val folded = fx.openness(CarEffect.MIRRORS_FOLDED, now, 600)
  for (side in listOf(-1f, 1f)) {
    val mount = shape.mirrorMount(side)
    val mirror = shape.mirror(side).map { rotateY(it, mount, side * 70f * folded) }
    if (solid != null) fill(mirror, lit(solid.body, V3(side, .3f, .2f)))
    polyline(mirror, detail)
  }
  // Door handles light up when unlocking; puddle lights shine on the floor under the mirrors.
  val unlockGlow = fx.pulse(CarEffect.UNLOCK, now, 2600)?.let { sin(it * PI).toFloat() } ?: 0f
  for (side in listOf(-1f, 1f)) shape.handles(side).forEach { polyline(it, lerp(detail, style.white, unlockGlow), width = style.lineWidth * (1f + unlockGlow), alpha = .6f + .4f * unlockGlow) }
  if (unlockGlow > 0f) {
    for (side in listOf(-1f, 1f)) {
      val m = shape.mirrorMount(side)
      val c = camera.project(V3(m.x + side * .45f, 0f, m.z - .2f)).offset
      val r = area.minDimension * .09f
      drawOval(
        Brush.radialGradient(listOf(style.white.copy(alpha = .45f * unlockGlow), Color.Transparent), center = c, radius = r),
        topLeft = Offset(c.x - r, c.y - r * .45f),
        size = Size(r * 2, r * .9f),
        blendMode = blend,
      )
    }
  }

  // A light running along the belt line now and then.
  val sweepCycle = (t % 9f) / 2.2f
  if (sweepCycle < 1f && solid == null) {
    for (side in listOf(-1f, 1f)) {
      val line = shape.beltLine(side)
      val head = (sweepCycle * (line.size + 8)).toInt()
      for (k in 0 until 8) {
        val i = head - k
        if (i in 1 until line.size) {
          val a = camera.project(line[i - 1]).offset
          val b = camera.project(line[i]).offset
          drawLine(style.white.copy(alpha = (1 - k / 8f) * .8f), a, b, style.lineWidth * 1.6f, StrokeCap.Round, blendMode = blend)
        }
      }
    }
  }

  // Doors: seams when closed, the door swung open around its hinge when opened.
  for ((effect, side) in listOf(CarEffect.DOOR_DRIVER to -1f, CarEffect.DOOR_PASSENGER to 1f)) {
    val open = fx.openness(effect, now)
    val angle = -side * 60f * open
    val hinge = shape.frontDoorHinge(side)
    if (solid != null && open > 0f) {
      // Opening in the body, then the painted door panel with its window swinging out.
      fill(shape.doorPanel(side), solid.interior)
      fill(shape.doorPanel(side).map { rotateY(it, hinge, angle) }, lit(solid.body, rotateY(V3(side, 0f, 0f), V3(0f, 0f, 0f), angle)))
      fill(shape.doorWindow(side).map { rotateY(it, hinge, angle) }, lit(solid.glass, rotateY(V3(side, .4f, 0f), V3(0f, 0f, 0f), angle)))
    }
    shape.frontDoor(side).forEach { line ->
      polyline(line.map { rotateY(it, hinge, angle) }, detail, alpha = if (solid != null) .5f + .5f * open else .35f + .65f * open)
    }
  }
  // Frunk and trunk lids.
  val frunk = fx.openness(CarEffect.FRUNK, now)
  val hood = shape.hood.map { rotateX(it, shape.hoodHinge, shape.frunkAngle * frunk) }
  if (solid != null && frunk > 0f) {
    fill(shape.hood, solid.interior)
    fill(hood, lit(solid.body, rotateX(V3(0f, 1f, .3f), V3(0f, 0f, 0f), shape.frunkAngle * frunk)))
  }
  polyline(hood, detail, alpha = .35f + .65f * frunk)
  val trunk = fx.openness(CarEffect.TRUNK, now)
  val lid = shape.trunkLid.map { rotateX(it, shape.trunkHinge, shape.trunkAngle * trunk) }
  if (solid != null && trunk > 0f) {
    fill(shape.trunkLid, solid.interior)
    fill(lid, lit(solid.body, rotateX(V3(0f, 1f, -.3f), V3(0f, 0f, 0f), shape.trunkAngle * trunk)))
  }
  polyline(lid, detail, alpha = .35f + .65f * trunk)
  // Charge port flap.
  val port = fx.openness(CarEffect.CHARGE_PORT, now)
  val flap = shape.chargePort.map { rotateY(it, shape.chargePortHinge, 80f * port) }
  if (solid != null) {
    if (port > 0f) fill(shape.chargePort, solid.interior)
    fill(flap, lit(solid.body, rotateY(V3(-1f, 0f, 0f), V3(0f, 0f, 0f), 80f * port)))
  }
  polyline(flap, if (charging > 0f) style.green else detail, alpha = .5f + .5f * port)
  if (charging > 0f || port > 0f) {
    val c = camera.project(shape.chargePortCenter)
    val pulse = if (charging > 0f) (.55f + .45f * sin(t * 3)) * charging else port * .6f
    val r = area.minDimension * .07f
    drawCircle(Brush.radialGradient(listOf(style.green.copy(alpha = .8f * pulse), Color.Transparent), center = c.offset, radius = r), r, c.offset, blendMode = blend)
  }
  // Charging: cable plugged in, energy dots running along it into the car.
  if (charging > 0f) {
    if (solid != null) polyline(shape.chargeCable, solid.tyre, width = style.lineWidth * 2.4f, alpha = charging)
    else polyline(shape.chargeCable, CHARGE_GREEN, alpha = .8f * charging)
    val cable = shape.chargeCable
    for (k in 0 until 4) {
      val q = 1f - ((t * .6f + k / 4f) % 1f)
      val p = camera.project(cable[(q * (cable.size - 1)).toInt()])
      drawCircle(style.green.copy(alpha = .9f * charging), area.minDimension * .012f, p.offset, blendMode = blend)
    }
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
  val breathe = if (solid != null) 1f else .72f + .28f * sin(t * 1.3f)
  // Wake: the light bars light up from the middle outwards.
  val wakeLight = wakeP?.let { (it * 2.2f).coerceIn(0f, 1f) }
  // The real cars' light signature (lenses, strips, light bars). Neon draws it as glowing lines (seen through
  // the car), the solid body only on the side facing the viewer.
  val front = V3(0f, .2f, 1f)
  val back = V3(0f, .2f, -1f)
  fun visible(normal: V3, limit: Float) = solid == null || camera.facing(normal) > limit
  val headLines: List<List<V3>> = buildList {
    shape.headLenses.filter { visible(it.normal, -.15f) }.forEach { add(it.strip) }
    shape.frontBar?.takeIf { visible(front, -.2f) }?.let { add(it) }
  }
  val tailLines: List<List<V3>> = buildList {
    shape.tailLenses.filter { visible(it.normal, -.15f) }.forEach { add(it.strip) }
    shape.rearBar?.takeIf { visible(back, -.2f) }?.let { add(it) }
  }
  if (solid != null) {
    shape.headLenses.filter { visible(it.normal, -.15f) }.forEach { fill(it.outline, solid.trim) }
    shape.tailLenses.filter { visible(it.normal, -.15f) }.forEach { fill(it.outline, Color(0xFF5A0A10)) }
  } else {
    // Lens outlines as faint lines around the light strips.
    shape.headLenses.forEach { polyline(it.outline + it.outline.first(), style.white, width = style.lineWidth * .7f, alpha = .35f) }
    shape.tailLenses.forEach { polyline(it.outline + it.outline.first(), style.red, width = style.lineWidth * .7f, alpha = .35f) }
  }
  headLines.forEach { polyline(it, style.white, width = style.lineWidth * 1.3f, alpha = breathe) }
  tailLines.forEach { polyline(it, style.red, width = style.lineWidth * 1.3f, alpha = (.75f + .25f * breathe) + .1f * lightBoost) }
  if (wakeLight != null) {
    val fade = 1f - (((wakeP ?: 1f) - .5f) / .5f).coerceIn(0f, 1f)
    headLines.forEach { polyline(it.take((it.size * wakeLight).toInt().coerceAtLeast(2)), style.white, width = style.lineWidth * 2.4f, alpha = fade) }
    tailLines.forEach { tail ->
      val mid = tail.size / 2
      val half = (mid * wakeLight).toInt()
      polyline(tail.subList((mid - half).coerceAtLeast(0), (mid + half + 1).coerceAtMost(tail.size)), style.red, width = style.lineWidth * 2.4f, alpha = fade)
    }
  }
  if (drive != null) headLines.forEach { polyline(it, style.white, width = style.lineWidth * 2.2f, alpha = sin(drive * PI).toFloat()) }
  if (lightBoost > 0f && (solid == null || camera.facing(front) > -.1f)) {
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
  // Sentry: a red eye on the B-pillars.
  if (sentry > 0f) {
    for (side in listOf(-1f, 1f)) {
      val c = camera.project(shape.sentryEye(side))
      val r = area.minDimension * .014f * (1.2f + .3f * sin(t * 4))
      drawCircle(style.red.copy(alpha = .9f * sentry), r, c.offset, blendMode = blend)
    }
  }
}

private fun DrawScope.floorLine(camera: CarCamera, from: V3, to: V3, color: Color, blend: BlendMode, width: Float = 1.2f) {
  drawLine(color, camera.project(from).offset, camera.project(to).offset, width, StrokeCap.Round, blendMode = blend)
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
