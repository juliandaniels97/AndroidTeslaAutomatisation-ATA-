package org.tfv.ata.ui.components.car

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
 * Minimal neon car: a few smooth lines (sills, belt line, roof, side profiles, windows, wheels, light bars).
 * Abstract on purpose, so it reads as "an electric car" without being a specific model.
 */
object CarModel {
  private val WHEEL_Z = listOf(1.48f, -1.48f)

  private fun v(x: Double, y: Double, z: Double) = V3(x.toFloat(), y.toFloat(), z.toFloat())

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

  /** Right half from the front centre to the rear centre, completed with its mirror image. */
  private fun symmetric(half: List<V3>): List<V3> = half + half.reversed().drop(1).dropLast(1).map { it.mirrored() }

  private fun profile(x: Double) = smooth(
    listOf(v(x, .74, 2.33), v(x, .90, 1.45), v(x, 1.20, .85), v(x, 1.44, .30), v(x, 1.45, -.55), v(x, 1.33, -1.30), v(x, 1.10, -1.95), v(x, 1.0, -2.30)),
  )

  private fun window(s: Double) = smooth(
    listOf(v(s * .95, .98, 1.20), v(s * .74, 1.30, .55), v(s * .62, 1.44, .05), v(s * .62, 1.44, -.65), v(s * .72, 1.30, -1.25), v(s * .92, 1.02, -1.85)),
  )

  fun circleYZ(x: Float, y: Float, z: Float, r: Float, n: Int = 28, from: Double = 0.0, to: Double = 2 * PI): List<V3> =
    (0..n).map { i ->
      val t = from + (to - from) * i / n
      V3(x, y + r * sin(t).toFloat(), z + r * cos(t).toFloat())
    }

  val body: List<List<V3>> = buildList {
    add(smooth(symmetric(listOf(v(0.0, .36, 2.30), v(.70, .36, 2.18), v(.93, .36, 1.70), v(.97, .36, 0.0), v(.95, .36, -1.80), v(.72, .36, -2.22), v(0.0, .36, -2.30))), closed = true))
    add(smooth(symmetric(listOf(v(0.0, .74, 2.36), v(.72, .80, 2.20), v(.95, .92, 1.50), v(.98, .98, 0.0), v(.96, 1.0, -1.6), v(.78, 1.0, -2.20), v(0.0, 1.0, -2.32))), closed = true))
    add(smooth(symmetric(listOf(v(0.0, 1.44, .35), v(.58, 1.45, .10), v(.60, 1.45, -.65), v(0.0, 1.42, -.95))), closed = true))
    add(profile(.55))
    add(profile(-.55))
    add(window(1.0))
    add(window(-1.0))
    for (s in listOf(1f, -1f)) for (zc in WHEEL_Z) add(circleYZ(s * .99f, .36f, zc, .42f, 16, 0.0, PI))
  }

  val wheels: List<List<V3>> = buildList {
    for (s in listOf(1f, -1f)) for (zc in WHEEL_Z) {
      add(circleYZ(s * 1.0f, .34f, zc, .33f))
      add(circleYZ(s * 1.0f, .34f, zc, .17f, 16))
    }
  }

  val headlights: List<List<V3>> = listOf(
    smooth(listOf(v(.30, .80, 2.30), v(.62, .83, 2.20), v(.88, .88, 1.85))),
    smooth(listOf(v(-.30, .80, 2.30), v(-.62, .83, 2.20), v(-.88, .88, 1.85))),
  )
  val taillight: List<V3> = smooth(listOf(v(-.78, 1.0, -2.22), v(0.0, 1.01, -2.33), v(.78, 1.0, -2.22)))

  /** Front doors (closed): panel outline on the side; hinge at the front edge. */
  fun frontDoor(side: Float): List<List<V3>> {
    val s = side.toDouble()
    return listOf(
      listOf(v(s * .97, .38, 1.12), v(s * .97, .97, 1.12)),
      listOf(v(s * .98, .38, -.28), v(s * .98, .98, -.28)),
      listOf(v(s * .97, .38, 1.12), v(s * .98, .38, -.28)),
      smooth(listOf(v(s * .95, .98, 1.12), v(s * .74, 1.30, .55), v(s * .63, 1.44, .05), v(s * .63, 1.44, -.28))),
    )
  }
  fun frontDoorHinge(side: Float) = V3(side * .97f, .6f, 1.12f)

  val hood: List<V3> = smooth(listOf(v(-.6, .80, 2.18), v(0.0, .78, 2.26), v(.6, .80, 2.18), v(.86, .92, 1.25), v(0.0, .95, 1.18), v(-.86, .92, 1.25), v(-.6, .80, 2.18)), steps = 6)
  val hoodHinge = V3(0f, .95f, 1.2f)

  val trunkLid: List<V3> = smooth(listOf(v(-.76, 1.0, -2.22), v(0.0, 1.01, -2.31), v(.76, 1.0, -2.22), v(.55, 1.36, -1.25), v(0.0, 1.38, -1.2), v(-.55, 1.36, -1.25), v(-.76, 1.0, -2.22)), steps = 6)
  val trunkHinge = V3(0f, 1.40f, -1.05f)

  /** Charge port flap on the rear left side. */
  val chargePort: List<V3> = listOf(v(-.97, .80, -1.72), v(-.97, .80, -1.92), v(-.965, .94, -1.92), v(-.965, .94, -1.72), v(-.97, .80, -1.72))
  val chargePortHinge = V3(-.97f, .87f, -1.72f)
  val chargePortCenter = V3(-.97f, .87f, -1.82f)

  /** Side windows (glass top edge when lowered). */
  fun windowDown(side: Float): List<V3> = smooth(listOf(v(side * .97, 1.02, 1.0), v(side * .98, 1.04, -.28), v(side * .95, 1.06, -1.6)))

  /** Steering wheel (left-hand drive), visible through the glass roof. */
  val steeringWheel: List<V3> = (0..24).map { i ->
    val t = 2 * PI * i / 24
    // Circle tilted towards the driver.
    val r = .17
    v(-.37 + r * cos(t), .98 + r * sin(t) * .8, .62 - r * sin(t) * .6)
  }

  /** Heating lines on the windscreen and the rear window (defrost). */
  val defrostLines: List<List<V3>> = buildList {
    for (k in 0..3) {
      val f = k / 4.0
      add(smooth(listOf(v(-.70 + .12 * f, 1.02 + .36 * f, 1.08 - .62 * f), v(0.0, 1.03 + .38 * f, 1.12 - .62 * f), v(.70 - .12 * f, 1.02 + .36 * f, 1.08 - .62 * f))))
      add(smooth(listOf(v(-.72 + .14 * f, 1.03 + .30 * f, -1.82 + .55 * f), v(0.0, 1.04 + .32 * f, -1.86 + .55 * f), v(.72 - .14 * f, 1.03 + .30 * f, -1.82 + .55 * f))))
    }
  }

  val cabinCenter = V3(0f, 1.0f, -.15f)
  val frontCenter = V3(0f, .8f, 2.35f)
  val rearCenter = V3(0f, .8f, -2.35f)
  val headlightLeft = V3(-.70f, .85f, 2.12f)
  val headlightRight = V3(.70f, .85f, 2.12f)

}

/** Places on the car that can be tapped in the full-screen view. */
enum class CarHotspot(val anchor: V3, val normal: V3) {
  DRIVER_DOOR(V3(-1.0f, .72f, .42f), V3(-1f, 0f, 0f)),
  PASSENGER_DOOR(V3(1.0f, .72f, .42f), V3(1f, 0f, 0f)),
  FRUNK(V3(0f, .92f, 1.75f), V3(0f, .7f, .7f)),
  TRUNK(V3(0f, 1.2f, -1.75f), V3(0f, .6f, -.8f)),
  CHARGE_PORT(V3(-1.0f, .87f, -1.82f), V3(-1f, 0f, 0f)),
  WINDOWS(V3(1.0f, 1.18f, -.25f), V3(1f, .3f, 0f)),
  LIGHTS(V3(0f, .8f, 2.4f), V3(0f, 0f, 1f)),
  CLIMATE(V3(0f, 1.48f, -.2f), V3(0f, 1f, 0f)),
}
