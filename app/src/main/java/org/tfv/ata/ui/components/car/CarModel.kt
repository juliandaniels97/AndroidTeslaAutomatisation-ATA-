package org.tfv.ata.ui.components.car

import org.tfv.ata.core.BodyStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A point in model space: x right, y up, z forward (front of the car), metres-ish, car centred on the origin. */
data class V3(val x: Float, val y: Float, val z: Float) {
  operator fun plus(o: V3) = V3(x + o.x, y + o.y, z + o.z)
  operator fun minus(o: V3) = V3(x - o.x, y - o.y, z - o.z)
  fun mirrored() = V3(-x, y, z)
}

/**
 * Proportions of one body style (half length/width, heights, glass and wheel positions). Every line of the neon
 * car is derived from these, so all styles share doors, lids, effects and tappable spots.
 */
data class BodySpec(
  val length: Float,
  val width: Float,
  val sill: Float,
  /** Side profile (z, y) from the nose to the tail, drawn at ±[profileX]. */
  val profile: List<Pair<Float, Float>>,
  val profileX: Float,
  val beltFront: Float,
  val beltMid: Float,
  val beltRear: Float,
  val roofFront: Float,
  val roofRear: Float,
  val roofWidth: Float,
  val roofHeight: Float,
  val windscreenBase: Float,
  val rearGlassBase: Float,
  val wheelFront: Float,
  val wheelRear: Float,
  val wheelRadius: Float,
  val doorRear: Float,
  /** Straight edges instead of smooth curves (Cybertruck). */
  val angular: Boolean = false,
)

object CarModel {
  private val specs = mapOf(
    BodyStyle.MODEL_3 to BodySpec(
      2.3f, .97f, .36f,
      listOf(2.33f to .74f, 1.45f to .90f, .85f to 1.20f, .30f to 1.44f, -.55f to 1.45f, -1.30f to 1.33f, -1.95f to 1.10f, -2.30f to 1.0f), .55f,
      .74f, .98f, 1.0f, .35f, -.95f, .60f, 1.45f, 1.20f, -1.85f, 1.48f, -1.48f, .33f, -.28f,
    ),
    BodyStyle.MODEL_Y to BodySpec(
      2.37f, .98f, .40f,
      listOf(2.40f to .80f, 1.50f to .98f, .90f to 1.30f, .30f to 1.58f, -.70f to 1.60f, -1.45f to 1.52f, -2.05f to 1.25f, -2.37f to 1.08f), .56f,
      .80f, 1.04f, 1.08f, .30f, -1.35f, .62f, 1.60f, 1.25f, -2.0f, 1.45f, -1.45f, .36f, -.28f,
    ),
    BodyStyle.MODEL_S to BodySpec(
      2.5f, .98f, .35f,
      listOf(2.52f to .72f, 1.60f to .88f, .95f to 1.18f, .30f to 1.42f, -.75f to 1.43f, -1.55f to 1.28f, -2.15f to 1.06f, -2.50f to .98f), .55f,
      .72f, .96f, .98f, .32f, -1.05f, .60f, 1.43f, 1.30f, -2.0f, 1.60f, -1.60f, .35f, -.35f,
    ),
    BodyStyle.MODEL_X to BodySpec(
      2.53f, 1.0f, .42f,
      listOf(2.55f to .82f, 1.65f to 1.0f, .95f to 1.45f, .25f to 1.68f, -.90f to 1.69f, -1.65f to 1.56f, -2.22f to 1.26f, -2.53f to 1.10f), .57f,
      .82f, 1.06f, 1.10f, .20f, -1.55f, .64f, 1.68f, 1.30f, -2.15f, 1.60f, -1.60f, .38f, -.40f,
    ),
    BodyStyle.CYBERTRUCK to BodySpec(
      2.85f, 1.0f, .48f,
      listOf(2.88f to .98f, 1.95f to 1.12f, .05f to 1.92f, -2.85f to 1.22f), .62f,
      .98f, 1.08f, 1.22f, .30f, -.25f, .55f, 1.90f, 1.95f, -.90f, 1.85f, -1.85f, .42f, -.45f, angular = true,
    ),
    BodyStyle.ROADSTER to BodySpec(
      2.15f, .98f, .32f,
      listOf(2.17f to .60f, 1.30f to .78f, .65f to 1.02f, .05f to 1.15f, -.65f to 1.13f, -1.45f to .98f, -2.15f to .82f), .52f,
      .60f, .86f, .84f, .10f, -.60f, .52f, 1.15f, .95f, -1.45f, 1.35f, -1.35f, .34f, -.45f,
    ),
  )
  private val shapes = HashMap<BodyStyle, CarShape>()

  /** The (cached) neon shape of [style]. */
  fun shape(style: BodyStyle): CarShape = shapes.getOrPut(style) { CarShape(specs.getValue(style)) }

  /** Catmull-Rom smoothing of a control polygon. */
  fun smooth(points: List<V3>, closed: Boolean = false, steps: Int = 8): List<V3> {
    val out = ArrayList<V3>()
    val m = points.size
    val segments = if (closed) m else m - 1
    for (i in 0 until segments) {
      val p0 = if (closed) points[(i - 1 + m) % m] else points[maxOf(i - 1, 0)]
      val p1 = points[i % m]
      val p2 = points[(i + 1) % m]
      val p3 = if (closed) points[(i + 2) % m] else points[minOf(i + 2, m - 1)]
      for (k in 0 until steps) {
        val t = k / steps.toFloat()
        val t2 = t * t
        val t3 = t2 * t
        fun c(a: Float, b: Float, c: Float, d: Float) =
          0.5f * (2 * b + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t2 + (-a + 3 * b - 3 * c + d) * t3)
        out.add(V3(c(p0.x, p1.x, p2.x, p3.x), c(p0.y, p1.y, p2.y, p3.y), c(p0.z, p1.z, p2.z, p3.z)))
      }
    }
    out.add(if (closed) out[0] else points.last())
    return out
  }

  fun circleYZ(x: Float, y: Float, z: Float, r: Float, n: Int = 28, from: Double = 0.0, to: Double = 2 * PI): List<V3> =
    (0..n).map { i ->
      val t = from + (to - from) * i / n
      V3(x, y + r * sin(t).toFloat(), z + r * cos(t).toFloat())
    }
}

/**
 * Minimal neon car of one body style: a few lines (sills, belt line, roof, side profiles, windows, wheels, light
 * bars) plus the movable parts and anchor points used by the effects.
 */
class CarShape(private val s: BodySpec) {
  private val l = s.length
  private val w = s.width

  /** Length relative to the Model 3; floor effects stretch with it. */
  val stretch = l / 2.3f
  /** Zoom factor that makes every style fill the frame like the Model 3. */
  val fit = 2.3f / l

  /** Smooth line, or straight segments for angular bodies. */
  private fun line(points: List<V3>, closed: Boolean = false, steps: Int = 8): List<V3> =
    if (s.angular) if (closed) points + points.first() else points else CarModel.smooth(points, closed, steps)

  /** Right half from the front centre to the rear centre, completed with its mirror image. */
  private fun symmetric(half: List<V3>): List<V3> = half + half.reversed().drop(1).dropLast(1).map { it.mirrored() }

  private fun beltY(z: Float) = if (z > -l * .3f) s.beltMid else s.beltRear

  /** Height of the side profile at [z]. */
  private fun profileY(z: Float): Float {
    val p = s.profile
    if (z >= p.first().first) return p.first().second
    for (i in 0 until p.size - 1) {
      val (z0, y0) = p[i]
      val (z1, y1) = p[i + 1]
      if (z <= z0 && z >= z1) return y0 + (y1 - y0) * (z0 - z) / (z0 - z1)
    }
    return p.last().second
  }

  private val glassMidX = (w + s.roofWidth) / 2f + .02f

  /** Side glass outline from the windscreen base over the roof edge to the rear glass. */
  private fun window(side: Float) = line(
    listOf(
      V3(side * (w - .02f), beltY(s.windscreenBase), s.windscreenBase),
      V3(side * glassMidX, (s.beltMid + s.roofHeight) / 2f + .05f, (s.windscreenBase + s.roofFront) / 2f),
      V3(side * (s.roofWidth + .02f), s.roofHeight, s.roofFront - .3f),
      V3(side * (s.roofWidth + .02f), s.roofHeight, s.roofRear + .35f),
      V3(side * glassMidX, (s.beltRear + s.roofHeight) / 2f + .04f, (s.roofRear + s.rearGlassBase) / 2f),
      V3(side * (w - .05f), beltY(s.rearGlassBase), s.rearGlassBase),
    ),
  )

  private val wheelZ = listOf(s.wheelFront, s.wheelRear)

  val body: List<List<V3>> = buildList {
    add(line(symmetric(listOf(V3(0f, s.sill, l), V3(w * .72f, s.sill, l - .12f), V3(w * .96f, s.sill, l * .74f), V3(w, s.sill, 0f), V3(w * .98f, s.sill, -l * .78f), V3(w * .74f, s.sill, -l + .08f), V3(0f, s.sill, -l))), closed = true))
    add(line(symmetric(listOf(V3(0f, s.beltFront, l + .04f), V3(w * .74f, s.beltFront + .06f, l - .12f), V3(w * .98f, s.beltMid - .05f, l * .62f), V3(w + .01f, s.beltMid, 0f), V3(w - .01f, s.beltRear, -l * .7f), V3(w * .8f, s.beltRear, -l + .1f), V3(0f, s.beltRear, -l - .02f))), closed = true))
    add(line(symmetric(listOf(V3(0f, s.roofHeight, s.roofFront), V3(s.roofWidth, s.roofHeight + .01f, s.roofFront - .22f), V3(s.roofWidth, s.roofHeight + .01f, s.roofRear + .28f), V3(0f, s.roofHeight - .02f, s.roofRear))), closed = true))
    for (side in listOf(1f, -1f)) add(line(s.profile.map { (z, y) -> V3(side * s.profileX, y, z) }))
    add(window(1f))
    add(window(-1f))
    // Wheel arches: round, or trapezoids on angular bodies.
    val arch = s.wheelRadius + .09f
    for (side in listOf(1f, -1f)) for (zc in wheelZ) {
      val x = side * .99f * w
      add(
        if (s.angular) listOf(V3(x, s.sill, zc + arch * 1.1f), V3(x, s.sill + arch * .9f, zc + arch * .6f), V3(x, s.sill + arch * .9f, zc - arch * .6f), V3(x, s.sill, zc - arch * 1.1f))
        else CarModel.circleYZ(x, s.wheelRadius + .03f, zc, arch, 16, 0.0, PI),
      )
    }
  }

  val wheels: List<List<V3>> = buildList {
    for (side in listOf(1f, -1f)) for (zc in wheelZ) {
      add(CarModel.circleYZ(side * w * 1.02f, s.wheelRadius + .01f, zc, s.wheelRadius))
      add(CarModel.circleYZ(side * w * 1.02f, s.wheelRadius + .01f, zc, s.wheelRadius * .5f, 16))
    }
  }

  val headlights: List<List<V3>> =
    if (s.angular) listOf(listOf(V3(-w * .7f, s.beltFront + .02f, l + .02f), V3(w * .7f, s.beltFront + .02f, l + .02f)))
    else listOf(1f, -1f).map { side ->
      CarModel.smooth(listOf(V3(side * .30f, s.beltFront + .06f, l), V3(side * .64f * w, s.beltFront + .09f, l - .1f), V3(side * .9f * w, s.beltFront + .14f, l - .45f)))
    }
  val taillight: List<V3> = line(listOf(V3(-w * .8f, s.beltRear, -l + .08f), V3(0f, s.beltRear + .01f, -l - .03f), V3(w * .8f, s.beltRear, -l + .08f)))

  private val doorFront = s.windscreenBase - .08f

  /** Front doors (closed): panel outline on the side; hinge at the front edge. */
  fun frontDoor(side: Float): List<List<V3>> {
    val x = side * (w - .01f)
    return listOf(
      listOf(V3(x, s.sill + .02f, doorFront), V3(x, beltY(doorFront), doorFront)),
      listOf(V3(x, s.sill + .02f, s.doorRear), V3(x, beltY(s.doorRear), s.doorRear)),
      listOf(V3(x, s.sill + .02f, doorFront), V3(x, s.sill + .02f, s.doorRear)),
      line(
        listOf(
          V3(side * (w - .02f), beltY(doorFront), doorFront),
          V3(side * glassMidX, (s.beltMid + s.roofHeight) / 2f + .05f, (s.windscreenBase + s.roofFront) / 2f),
          V3(side * (s.roofWidth + .03f), s.roofHeight, s.roofFront - .3f),
          V3(side * (s.roofWidth + .03f), s.roofHeight, s.doorRear),
        ),
      ),
    )
  }
  fun frontDoorHinge(side: Float) = V3(side * (w - .01f), (s.sill + s.beltMid) / 2f, doorFront)

  val hood: List<V3> = line(
    listOf(
      V3(-.6f * w, s.beltFront + .06f, l - .12f), V3(0f, s.beltFront + .04f, l - .04f), V3(.6f * w, s.beltFront + .06f, l - .12f),
      V3(.88f * w, s.beltMid - .06f, s.windscreenBase + .05f), V3(0f, s.beltMid - .03f, s.windscreenBase - .02f), V3(-.88f * w, s.beltMid - .06f, s.windscreenBase + .05f),
    ),
    closed = true, steps = 6,
  )
  val hoodHinge = V3(0f, s.beltMid - .03f, s.windscreenBase)

  private val lidTopZ = s.roofRear - .3f
  private val lidTopY = profileY(lidTopZ) - .08f
  val trunkLid: List<V3> = line(
    listOf(
      V3(-.78f * w, s.beltRear, -l + .08f), V3(0f, s.beltRear + .01f, -l - .01f), V3(.78f * w, s.beltRear, -l + .08f),
      V3(.57f * w, lidTopY, lidTopZ), V3(0f, lidTopY + .02f, lidTopZ + .05f), V3(-.57f * w, lidTopY, lidTopZ),
    ),
    closed = true, steps = 6,
  )
  val trunkHinge = V3(0f, lidTopY + .04f, lidTopZ + .2f)

  /** Charge port flap on the rear left side, above the rear wheel. */
  val chargePortCenter = V3(-w, beltY(s.wheelRear - s.wheelRadius) - .13f, s.wheelRear - s.wheelRadius - .01f)
  val chargePort: List<V3> = chargePortCenter.let { c ->
    listOf(V3(c.x, c.y - .07f, c.z + .1f), V3(c.x, c.y - .07f, c.z - .1f), V3(c.x + .005f, c.y + .07f, c.z - .1f), V3(c.x + .005f, c.y + .07f, c.z + .1f), V3(c.x, c.y - .07f, c.z + .1f))
  }
  val chargePortHinge = V3(chargePortCenter.x, chargePortCenter.y, chargePortCenter.z + .1f)

  /** Side windows (glass top edge when lowered). */
  fun windowDown(side: Float): List<V3> =
    CarModel.smooth(listOf(V3(side * w, s.beltMid + .04f, s.windscreenBase - .2f), V3(side * (w + .01f), s.beltMid + .06f, s.doorRear), V3(side * (w - .02f), s.beltRear + .06f, s.rearGlassBase + .25f)))

  /** Steering wheel (left-hand drive), visible through the glass roof. */
  val steeringWheel: List<V3> = (0..24).map { i ->
    val t = 2 * PI * i / 24
    val r = .17
    // Circle tilted towards the driver.
    V3((-.37 * w + r * cos(t)).toFloat(), (s.beltMid + r * sin(t) * .8).toFloat(), (s.windscreenBase - .58 - r * sin(t) * .6).toFloat())
  }

  /** Heating lines on the windscreen and the rear window (defrost). */
  val defrostLines: List<List<V3>> = buildList {
    val frontRise = s.roofHeight - s.beltMid
    val frontRun = s.windscreenBase - s.roofFront
    val rearRise = s.roofHeight - s.beltRear
    val rearRun = s.roofRear - s.rearGlassBase
    for (k in 0..3) {
      val f = k / 4f
      val fy = s.beltMid + .04f + frontRise * .77f * f
      val fz = s.windscreenBase - .12f - frontRun * .73f * f
      add(CarModel.smooth(listOf(V3(-.72f * w + .12f * f, fy, fz), V3(0f, fy + .01f, fz + .04f), V3(.72f * w - .12f * f, fy, fz))))
      val ry = s.beltRear + .03f + rearRise * .67f * f
      val rz = s.rearGlassBase + .03f + rearRun * .61f * f
      add(CarModel.smooth(listOf(V3(-.74f * w + .14f * f, ry, rz), V3(0f, ry + .01f, rz - .04f), V3(.74f * w - .14f * f, ry, rz))))
    }
  }

  val cabinCenter = V3(0f, s.beltMid, (s.roofFront + s.roofRear) / 2f)
  val frontCenter = V3(0f, s.beltFront + .06f, l + .05f)
  val rearCenter = V3(0f, s.beltRear - .2f, -l - .05f)
  val headlightLeft = V3(-.72f * w, s.beltFront + .11f, l - .2f)
  val headlightRight = V3(.72f * w, s.beltFront + .11f, l - .2f)
  fun sentryEye(side: Float) = V3(side * w, s.beltMid + .02f, s.doorRear)

  /** Where the tappable spot of [spot] sits on this body. */
  fun anchor(spot: CarHotspot): V3 = when (spot) {
    CarHotspot.DRIVER_DOOR -> V3(-w - .03f, (s.sill + s.beltMid) / 2f + .05f, (doorFront + s.doorRear) / 2f)
    CarHotspot.PASSENGER_DOOR -> V3(w + .03f, (s.sill + s.beltMid) / 2f + .05f, (doorFront + s.doorRear) / 2f)
    CarHotspot.FRUNK -> V3(0f, s.beltFront + .18f, (l + s.windscreenBase) / 2f)
    CarHotspot.TRUNK -> V3(0f, s.beltRear + .2f, (s.rearGlassBase - l) / 2f + .3f)
    CarHotspot.CHARGE_PORT -> chargePortCenter.copy(x = -w - .03f)
    CarHotspot.WINDOWS -> V3(-w - .03f, (s.beltRear + s.roofHeight) / 2f, (s.doorRear + s.roofRear) / 2f)
    CarHotspot.LIGHTS -> V3(0f, s.sill + .12f, l + .07f)
    CarHotspot.CLIMATE -> V3(0f, s.roofHeight + .03f, (s.roofFront + s.roofRear) / 2f)
  }
}

/** Places on the car that can be tapped in the full-screen view; [normal] tells which way the spot faces. */
enum class CarHotspot(val normal: V3) {
  DRIVER_DOOR(V3(-1f, 0f, 0f)),
  PASSENGER_DOOR(V3(1f, 0f, 0f)),
  FRUNK(V3(0f, .7f, .7f)),
  TRUNK(V3(0f, .6f, -.8f)),
  CHARGE_PORT(V3(-1f, 0f, 0f)),
  WINDOWS(V3(-1f, .3f, 0f)),
  LIGHTS(V3(0f, 0f, 1f)),
  CLIMATE(V3(0f, 1f, 0f)),
}
