package org.tfv.ata.ui.components.car

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

/**
 * Perspective camera looking at the car. [yawDeg] turns the car (0 = front towards the viewer), [pitchDeg]
 * raises the camera. Projects model points to screen pixels inside a [width] × [height] area. A larger
 * [distance] flattens the perspective (same size on screen), used for the static side view; [pan] shifts the picture.
 */
class CarCamera(
  yawDeg: Float,
  pitchDeg: Float,
  private val width: Float,
  private val height: Float,
  zoom: Float = 1f,
  private val distance: Float = 9f,
  private val pan: Offset = Offset.Zero,
) {
  private val yaw = Math.toRadians(yawDeg.toDouble())
  private val pitch = Math.toRadians(pitchDeg.toDouble())
  private val cy = cos(yaw).toFloat()
  private val sy = sin(yaw).toFloat()
  private val cp = cos(pitch).toFloat()
  private val sp = sin(pitch).toFloat()
  private val focal = minOf(width * 1.25f, height * 1.9f) * zoom * distance / 9f

  data class Projected(val offset: Offset, val depth: Float)

  /** Rotates a model point into camera space; z grows towards the viewer. */
  fun view(p: V3): V3 {
    val y = p.y - .8f
    val x1 = p.x * cy - p.z * sy
    val z1 = p.x * sy + p.z * cy
    val y2 = y * cp - z1 * sp
    val z2 = y * sp + z1 * cp
    return V3(x1, y2, z2)
  }

  fun project(p: V3): Projected {
    val c = view(p)
    val d = distance - c.z
    // Screen x is mirrored: the model's -x side (driver, steering wheel, charge port) is the car's left, and the
    // car follows the finger when turned.
    return Projected(Offset(width / 2f + pan.x - focal * c.x / d, height / 2f + pan.y - focal * c.y / d), c.z)
  }

  companion object {
    const val SIDE_YAW = -90f
    const val SIDE_PITCH = 3f
    const val SIDE_DISTANCE = 40f

    /** Static side view (2D mode): driver's (left) side, front to the left, almost no perspective. */
    fun side(width: Float, height: Float, zoom: Float = 1f, pan: Offset = Offset.Zero) =
      CarCamera(SIDE_YAW, SIDE_PITCH, width, height, zoom, SIDE_DISTANCE, pan)
  }

  /** Floor reflection: the point mirrored below the ground plane. */
  fun projectReflection(p: V3): Projected = project(V3(p.x, -p.y, p.z))

  /** How much a surface with model-space [normal] faces the viewer (-1 … 1). */
  fun facing(normal: V3): Float {
    val x1 = normal.x * cy - normal.z * sy
    val z1 = normal.x * sy + normal.z * cy
    val z2 = normal.y * sp + z1 * cp
    val len = kotlin.math.sqrt(normal.x * normal.x + normal.y * normal.y + normal.z * normal.z).coerceAtLeast(1e-4f)
    return z2 / len
  }
}
