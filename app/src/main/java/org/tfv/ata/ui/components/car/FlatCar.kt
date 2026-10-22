package org.tfv.ata.ui.components.car

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Colours of the solid side view (2D mode), like the earlier car drawing: painted body, tinted glass, soft shadow. */
data class FlatStyle(
  val body: Color,
  val glass: Color,
  val accent: Color,
  val dark: Boolean,
  val tyre: Color = Color(0xFF15181D),
  val rim: Color = Color(0xFFB7C0CA),
  val light: Color = Color(0xFFDDF3FF),
  val red: Color = Color(0xFFFF3B30),
  val green: Color = Color(0xFF2ECC71),
  val warm: Color = Color(0xFFFF8A3D),
)

/**
 * Solid side view of [shape] (left side, front to the right) seen through a side [camera]: painted body, glass,
 * wheels with rims, lights. Opened doors and lids, charging, lights and the other effects are shown calmly,
 * without the neon glow of the 3D view.
 */
fun DrawScope.drawFlatCar(shape: CarShape, camera: CarCamera, fx: CarFxState, status: CarStatus, now: Long, style: FlatStyle) {
  val s = shape.spec
  val t = now / 1000f
  fun p(z: Float, y: Float): Offset = camera.project(V3(0f, y, z)).offset
  fun path(points: List<Offset>, close: Boolean = true) = Path().apply {
    points.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
    if (close) close()
  }
  val unit = (p(1f, 0f).x - p(0f, 0f).x)
  val l = s.length
  val bottom = s.sill - .1f
  val arch = s.wheelRadius + .07f
  val wheelY = s.wheelRadius + .01f

  // Top edge: the side profile, smooth unless the body is angular.
  val top: List<Pair<Float, Float>> =
    if (s.angular) s.profile
    else CarModel.smooth(s.profile.map { (z, y) -> V3(0f, y, z) }, steps = 10).map { it.z to it.y }
  fun topY(z: Float): Float {
    if (z >= top.first().first) return top.first().second
    for (i in 0 until top.size - 1) {
      val (z0, y0) = top[i]
      val (z1, y1) = top[i + 1]
      if (z <= z0 && z >= z1) return y0 + (y1 - y0) * (z0 - z) / (z0 - z1)
    }
    return top.last().second
  }
  fun beltY(z: Float) = if (z > -l * .3f) s.beltMid else s.beltRear

  // Shadow on the ground.
  val shadowCenter = p(0f, 0f)
  val shadowW = unit * l * 2.3f
  drawOval(
    Brush.radialGradient(listOf(Color.Black.copy(alpha = if (style.dark) .55f else .18f), Color.Transparent), center = shadowCenter, radius = shadowW / 2f),
    topLeft = Offset(shadowCenter.x - shadowW / 2f, shadowCenter.y - unit * .12f),
    size = Size(shadowW, unit * .24f),
  )

  // Bioweapon defence: a soft green haze around the body.
  val bio = fx.openness(CarEffect.BIOWEAPON, now, 1500)
  if (bio > 0f) {
    val c = p(0f, s.roofHeight / 2f)
    val r = unit * l * 1.25f
    drawOval(
      Brush.radialGradient(listOf(style.green.copy(alpha = .22f * bio * (.85f + .15f * sin(t))), Color.Transparent), center = c, radius = r),
      topLeft = Offset(c.x - r, c.y - r * .5f),
      size = Size(r * 2, r),
    )
  }

  // Body outline: profile, tail, sill with wheel arches, nose.
  val outline = ArrayList<Offset>()
  top.forEach { (z, y) -> outline.add(p(z, y)) }
  outline.add(p(-l + .02f, s.sill + .08f))
  outline.add(p(-l + .14f, bottom))
  for (zc in listOf(s.wheelRear, s.wheelFront)) {
    outline.add(p(zc - arch, bottom))
    for (k in 0..14) {
      val a = PI - PI * k / 14
      outline.add(p(zc + arch * cos(a).toFloat(), wheelY + arch * sin(a).toFloat()))
    }
    outline.add(p(zc + arch, bottom))
  }
  outline.add(p(l - .14f, bottom))
  outline.add(p(l + .02f, s.sill + .1f))
  val body = path(outline)
  val bodyTop = p(0f, s.roofHeight).y
  val bodyBottom = p(0f, bottom).y
  drawPath(
    body,
    Brush.verticalGradient(
      listOf(lerp(style.body, Color.White, .28f), style.body, lerp(style.body, Color.Black, .25f)),
      startY = bodyTop,
      endY = bodyBottom,
    ),
  )

  // Glass: from the windscreen base along the roof to the rear glass.
  val windows = fx.openness(CarEffect.WINDOWS, now)
  val climate = maxOf(fx.openness(CarEffect.CLIMATE, now, 1200), if (status.climate) 1f else 0f)
  val glassPts = ArrayList<Offset>()
  glassPts.add(p(s.windscreenBase - .05f, beltY(s.windscreenBase) + .02f))
  top.filter { (z, _) -> z < s.windscreenBase - .1f && z > s.rearGlassBase + .1f }.forEach { (z, y) -> glassPts.add(p(z, y - .06f)) }
  glassPts.add(p(s.rearGlassBase + .05f, beltY(s.rearGlassBase) + .02f))
  val glassColor = lerp(style.glass, style.accent, .35f * climate * (.8f + .2f * sin(t * 2)))
  drawPath(
    path(glassPts),
    Brush.verticalGradient(listOf(lerp(glassColor, Color.White, .14f), glassColor), startY = bodyTop, endY = p(0f, s.beltMid).y),
  )
  // Lowered side windows: the cabin shows dark behind the door glass, with the glass edge at the belt line.
  if (windows > 0f) {
    val sideGlass = glassPts.filter { it.x <= p(s.windscreenBase - .25f, 0f).x && it.x >= p(s.rearGlassBase + .3f, 0f).x }
    if (sideGlass.size >= 2) {
      val pts = listOf(p(s.windscreenBase - .25f, beltY(s.windscreenBase) + .02f)) + sideGlass + p(s.rearGlassBase + .3f, beltY(s.rearGlassBase) + .02f)
      drawPath(path(pts), Color.Black.copy(alpha = .5f * windows))
    }
    drawLine(style.light.copy(alpha = .7f * windows), p(s.windscreenBase - .25f, s.beltMid + .05f), p(s.rearGlassBase + .3f, s.beltRear + .05f), unit * .025f, StrokeCap.Round)
  }
  // Defrost: warm lines on the windscreen and the rear glass.
  val defrost = fx.openness(CarEffect.DEFROST, now, 1500)
  if (defrost > 0f) {
    for (k in 0..2) {
      val f = (k + 1) / 4f
      val zf = s.windscreenBase - (s.windscreenBase - s.roofFront) * f
      drawLine(style.warm.copy(alpha = .7f * defrost), p(zf, topY(zf) - .1f), p(zf + .08f, beltY(zf) + .08f), unit * .02f, StrokeCap.Round)
      val zr = s.rearGlassBase + (s.roofRear - s.rearGlassBase) * f
      drawLine(style.warm.copy(alpha = .7f * defrost), p(zr, topY(zr) - .1f), p(zr - .08f, beltY(zr) + .08f), unit * .02f, StrokeCap.Round)
    }
  }
  // Steering wheel heater: a warm glow behind the windscreen.
  val wheelHeat = fx.openness(CarEffect.WHEEL_HEAT, now, 1200)
  if (wheelHeat > 0f) {
    val c = p(s.windscreenBase - .58f, s.beltMid + .08f)
    val r = unit * .3f
    drawCircle(Brush.radialGradient(listOf(style.warm.copy(alpha = .6f * wheelHeat), Color.Transparent), center = c, radius = r), r, c)
  }

  val seam = lerp(style.body, Color.Black, .45f)
  val seamWidth = unit * .018f
  // B-pillar.
  drawLine(lerp(style.body, Color.Black, .15f), p(s.doorRear, beltY(s.doorRear)), p(s.doorRear - .02f, topY(s.doorRear) - .05f), unit * .07f)

  // Front door: swings towards the viewer around its front edge (seen from the side it gets narrower).
  val doorFront = shape.doorFront
  val door = fx.openness(CarEffect.DOOR_DRIVER, now)
  val doorPts = listOf(
    doorFront to bottom + .05f, s.doorRear to bottom + .05f, s.doorRear to topY(s.doorRear) - .05f,
    (doorFront + s.doorRear) / 2f to topY((doorFront + s.doorRear) / 2f) - .05f, doorFront to beltY(doorFront) + .02f,
  )
  if (door > 0f) {
    drawPath(path(doorPts.map { (z, y) -> p(z, y) }), Color.Black.copy(alpha = .55f * door))
    val squeeze = cos(Math.toRadians(60.0 * door)).toFloat()
    val open = doorPts.map { (z, y) -> p(doorFront + (z - doorFront) * squeeze, y) }
    drawPath(path(open), lerp(style.body, Color.White, .18f))
    drawPath(path(open), seam, style = Stroke(seamWidth))
  } else {
    drawLine(seam, p(doorFront, bottom + .05f), p(doorFront, beltY(doorFront)), seamWidth)
  }
  drawLine(seam, p(s.doorRear, bottom + .05f), p(s.doorRear, beltY(s.doorRear)), seamWidth)
  if (s.rearDoors) {
    val rearDoorRear = s.wheelRear + s.wheelRadius + .12f
    drawLine(seam, p(rearDoorRear, bottom + .12f), p(rearDoorRear, beltY(rearDoorRear)), seamWidth)
  }
  // Door handles: they light up when unlocking.
  val unlockGlow = fx.pulse(CarEffect.UNLOCK, now, 2600)?.let { sin(it * PI).toFloat() } ?: 0f
  val handleColor = lerp(lerp(style.body, Color.White, .4f), style.light, unlockGlow)
  val handleZ = buildList {
    if (door == 0f) add(s.doorRear + .32f)
    if (s.rearDoors) add(s.wheelRear + s.wheelRadius + .44f)
  }
  for (z in handleZ) drawLine(handleColor, p(z, beltY(z) - .1f), p(z - .18f, beltY(z) - .1f), unit * .035f, StrokeCap.Round)

  // Mirror (folds back when locking).
  val folded = fx.openness(CarEffect.MIRRORS_FOLDED, now, 600)
  val mz = s.windscreenBase - .12f
  val my = s.beltMid + .05f
  val mirrorLen = .22f * (1f - .5f * folded)
  drawPath(
    path(listOf(p(mz, my), p(mz - mirrorLen, my + .02f), p(mz - mirrorLen + .04f, my + .12f), p(mz - .02f, my + .1f))),
    lerp(style.body, Color.Black, .1f),
  )

  // Frunk and trunk lids: lifted around their hinges, the opening dark below.
  val frunk = fx.openness(CarEffect.FRUNK, now)
  if (frunk > 0f) {
    val hood = top.filter { (z, _) -> z > s.windscreenBase + .02f }.map { (z, y) -> z to y } + listOf(s.windscreenBase + .02f to beltY(s.windscreenBase) - .02f, l - .1f to s.beltFront - .05f)
    drawPath(path(hood.map { (z, y) -> p(z, y) }), Color.Black.copy(alpha = .5f * frunk))
    val pivot = p(s.windscreenBase, beltY(s.windscreenBase))
    val a = Math.toRadians(shape.frunkAngle * .7 * frunk)
    drawPath(path(hood.map { (z, y) -> rotate2d(p(z, y), pivot, a) }), lerp(style.body, Color.White, .12f))
  }
  val trunk = fx.openness(CarEffect.TRUNK, now)
  if (trunk > 0f) {
    val lidTop = s.roofRear - .3f
    val lid = top.filter { (z, _) -> z < lidTop }.map { (z, y) -> z to y } + listOf(-l + .04f to s.beltRear - .08f, lidTop to topY(lidTop) - .12f)
    drawPath(path(lid.map { (z, y) -> p(z, y) }), Color.Black.copy(alpha = .5f * trunk))
    val pivot = p(lidTop + .1f, topY(lidTop + .1f))
    val a = Math.toRadians(shape.trunkAngle * .84 * trunk)
    drawPath(path(lid.map { (z, y) -> rotate2d(p(z, y), pivot, a) }), lerp(style.body, Color.White, .12f))
  }

  // Charge port: small flap above the rear wheel, green while charging, with the cable plugged in.
  val charging = maxOf(fx.openness(CarEffect.CHARGING, now, 800), if (status.charging) 1f else 0f)
  val port = fx.openness(CarEffect.CHARGE_PORT, now)
  val pc = p(shape.chargePortCenter.z, shape.chargePortCenter.y)
  val portW = unit * .2f * (1f - .7f * port)
  drawRect(lerp(style.body, Color.Black, .2f), Offset(pc.x - unit * .1f, pc.y - unit * .07f), Size(portW, unit * .14f))
  if (charging > 0f || port > 0f) {
    val glow = if (charging > 0f) (.55f + .45f * sin(t * 3)) * charging else port * .6f
    val r = unit * .3f
    drawCircle(Brush.radialGradient(listOf(style.green.copy(alpha = .8f * glow), Color.Transparent), center = pc, radius = r), r, pc)
  }
  if (charging > 0f) {
    val cable = Path().apply {
      moveTo(pc.x, pc.y)
      val ground = p(shape.chargePortCenter.z - .9f, 0f)
      cubicTo(pc.x - unit * .5f, pc.y + unit * .1f, ground.x + unit * .2f, ground.y - unit * .3f, ground.x, ground.y)
    }
    drawPath(cable, Color(0xFF2A2F36).copy(alpha = charging), style = Stroke(unit * .05f, cap = StrokeCap.Round))
    for (k in 0 until 3) {
      val q = 1f - ((t * .6f + k / 3f) % 1f)
      val ground = p(shape.chargePortCenter.z - .9f, 0f)
      val d = Offset(ground.x + (pc.x - ground.x) * q, ground.y + (pc.y - ground.y) * q - unit * .15f * sin(q * PI).toFloat())
      drawCircle(style.green.copy(alpha = .9f * charging), unit * .03f, d)
    }
  }

  // Wheels with five-spoke rims; they turn while driving remotely.
  val drive = fx.pulse(CarEffect.DRIVE, now, 3000)
  val spin = drive?.let { (it * it * (3 - 2 * it)) * 6f * PI.toFloat() } ?: 0f
  for (zc in listOf(s.wheelFront, s.wheelRear)) {
    val c = p(zc, wheelY)
    val r = unit * s.wheelRadius
    drawCircle(style.tyre, r, c)
    drawCircle(Brush.radialGradient(listOf(lerp(style.rim, Color.White, .3f), style.rim, lerp(style.rim, Color.Black, .35f)), center = c, radius = r * .7f), r * .7f, c)
    for (k in 0 until 5) {
      val a = spin + 2 * PI.toFloat() * k / 5
      drawLine(lerp(style.rim, Color.Black, .45f), Offset(c.x + r * .2f * cos(a), c.y + r * .2f * sin(a)), Offset(c.x + r * .66f * cos(a + .2f), c.y + r * .66f * sin(a + .2f)), r * .1f, StrokeCap.Round)
    }
    drawCircle(lerp(style.rim, Color.Black, .3f), r * .14f, c)
  }

  // Lights: headlight at the nose, light bar at the tail; they flash for lights, unlock and lock.
  val lockPulse = fx.pulse(CarEffect.LOCK, now, 700)
  val unlockPulse = fx.pulse(CarEffect.UNLOCK, now, 1200)
  val flash = fx.pulse(CarEffect.LIGHTS, now, 1400)
  val wake = fx.pulse(CarEffect.WAKE, now, 1600)
  val boost = when {
    flash != null -> if ((flash * 6).toInt() % 2 == 0) 1f else 0f
    unlockPulse != null -> if ((unlockPulse * 4).toInt() % 2 == 0) 1f else 0f
    lockPulse != null -> 1f - lockPulse
    wake != null -> sin(wake * PI).toFloat()
    drive != null -> sin(drive * PI).toFloat()
    else -> 0f
  }
  val headA = p(l - .02f, s.beltFront + .04f)
  val headB = p(l - .42f, s.beltFront + .12f)
  drawLine(style.light, headA, headB, unit * .05f, StrokeCap.Round)
  drawLine(style.red, p(-l + .03f, s.beltRear - .02f), p(-l + .3f, s.beltRear + .01f), unit * .05f, StrokeCap.Round)
  if (boost > 0f) {
    val r = unit * .7f
    drawCircle(Brush.radialGradient(listOf(style.light.copy(alpha = .7f * boost), Color.Transparent), center = headA, radius = r), r, headA)
    val tail = p(-l + .1f, s.beltRear)
    drawCircle(Brush.radialGradient(listOf(style.red.copy(alpha = .5f * boost), Color.Transparent), center = tail, radius = r * .6f), r * .6f, tail)
  }

  // Sentry: a red camera dot on the front fender.
  val sentry = maxOf(fx.openness(CarEffect.SENTRY, now, 800), if (status.sentry) 1f else 0f)
  if (sentry > 0f) drawCircle(style.red.copy(alpha = sentry), unit * .04f * (1.1f + .3f * sin(t * 4)), p(s.doorRear + .2f + (s.windscreenBase - s.doorRear), s.beltMid - .05f))

  // Horn, locate and music: calm rings in front of / above the car.
  fx.pulse(CarEffect.HONK, now, 1200)?.let { rings(p(l + .2f, s.beltFront), unit, it, style.accent) }
  fx.pulse(CarEffect.LOCATE, now, 2400)?.let { rings(p(0f, s.roofHeight + .2f), unit, it, style.accent) }
  fx.pulse(CarEffect.MEDIA, now, 2600)?.let { rings(p((s.roofFront + s.roofRear) / 2f, s.roofHeight + .1f), unit, it, style.accent) }
  // Boombox: little clouds at the back.
  fx.pulse(CarEffect.PUFF, now, 2200)?.let { q0 ->
    for (k in 0 until 5) {
      val q = (q0 * 1.3f - k * .06f).coerceIn(0f, 1f)
      val c = p(-l - .2f - 1.0f * q, s.sill + .2f + .25f * q + k * .04f)
      drawCircle(style.green.copy(alpha = .35f * (1 - q)), unit * (.06f + .12f * q), c)
    }
  }
}

private fun DrawScope.rings(center: Offset, unit: Float, p: Float, color: Color) {
  for (k in 0..2) {
    val q = (p * 1.5f - k * .25f).coerceIn(0f, 1f)
    if (q > 0f && q < 1f) drawCircle(color.copy(alpha = (1 - q) * .7f), unit * (.15f + .6f * q), center, style = Stroke(unit * .03f))
  }
}

private fun rotate2d(o: Offset, pivot: Offset, radians: Double): Offset {
  val c = cos(radians).toFloat()
  val sn = sin(radians).toFloat()
  val dx = o.x - pivot.x
  val dy = o.y - pivot.y
  return Offset(pivot.x + dx * c - dy * sn, pivot.y + dx * sn + dy * c)
}
