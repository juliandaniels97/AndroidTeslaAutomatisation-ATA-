package org.tfv.ata.geo

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Where the phone points: [degrees] from true north (0 … 360), [reliable] false while the compass needs calibrating. */
data class CompassReading(val degrees: Float, val reliable: Boolean, val available: Boolean = true)

/**
 * Live heading of the phone relative to true north, updated while [enabled].
 *
 * - Lying flat, the top edge of the screen is the direction; held upright (like a camera), the back of the phone.
 * - Follows the screen rotation.
 * - Corrected by the magnetic declination at [location] (the compass knows magnetic north, GPS bearings are true).
 * - Smoothed, so the arrow doesn't jitter.
 */
@Composable
fun rememberCompass(enabled: Boolean, location: android.location.Location? = null): State<CompassReading> {
  val context = LocalContext.current
  val reading = remember { mutableStateOf(CompassReading(0f, reliable = true, available = true)) }
  val where by rememberUpdatedState(location ?: Geo.lastLocation(context))

  DisposableEffect(enabled) {
    if (!enabled) return@DisposableEffect onDispose { }
    val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val sensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    if (sm == null || sensor == null) {
      reading.value = CompassReading(0f, reliable = false, available = false)
      return@DisposableEffect onDispose { }
    }
    val listener = object : SensorEventListener {
      private val rotation = FloatArray(9)
      private val remapped = FloatArray(9)
      private val orientation = FloatArray(3)
      private var sinSmooth = 0.0
      private var cosSmooth = 1.0
      private var started = false
      private var reliable = true

      override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rotation, event.values)
        // Upright phone (screen facing the user): use the direction the back of the phone faces.
        val upright = abs(rotation[7]) > .7f
        val (axisX, axisY) = axesFor(screenRotation(context), upright)
        SensorManager.remapCoordinateSystem(rotation, axisX, axisY, remapped)
        SensorManager.getOrientation(remapped, orientation)
        var heading = Math.toDegrees(orientation[0].toDouble())
        where?.let { heading += GeomagneticField(it.latitude.toFloat(), it.longitude.toFloat(), it.altitude.toFloat(), System.currentTimeMillis()).declination }
        // Low-pass on the unit circle, so 359° → 1° doesn't swing through 180°.
        val r = Math.toRadians(heading)
        if (!started) {
          sinSmooth = sin(r)
          cosSmooth = cos(r)
          started = true
        } else {
          sinSmooth += SMOOTHING * (sin(r) - sinSmooth)
          cosSmooth += SMOOTHING * (cos(r) - cosSmooth)
        }
        val smoothed = ((Math.toDegrees(atan2(sinSmooth, cosSmooth)) + 360) % 360).toFloat()
        reading.value = CompassReading(smoothed, reliable)
      }

      override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        reliable = accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
        reading.value = reading.value.copy(reliable = reliable)
      }
    }
    sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
    onDispose { sm.unregisterListener(listener) }
  }
  return reading
}

/** Device heading in degrees (true north), for places that only need the angle. */
@Composable
fun rememberDeviceHeading(enabled: Boolean): State<Float> {
  val compass = rememberCompass(enabled)
  return remember { derivedFloat(compass) }
}

private fun derivedFloat(compass: State<CompassReading>): State<Float> =
  androidx.compose.runtime.derivedStateOf { compass.value.degrees }

private const val SMOOTHING = .18

private fun screenRotation(context: Context): Int =
  runCatching { context.display?.rotation }.getOrNull() ?: Surface.ROTATION_0

/** Axes for SensorManager.remapCoordinateSystem: follow the screen rotation, and use the camera axis when upright. */
private fun axesFor(rotation: Int, upright: Boolean): Pair<Int, Int> {
  val x = SensorManager.AXIS_X
  val y = if (upright) SensorManager.AXIS_Z else SensorManager.AXIS_Y
  val minusX = SensorManager.AXIS_MINUS_X
  val minusY = if (upright) SensorManager.AXIS_MINUS_Z else SensorManager.AXIS_MINUS_Y
  return when (rotation) {
    Surface.ROTATION_90 -> y to minusX
    Surface.ROTATION_180 -> minusX to minusY
    Surface.ROTATION_270 -> minusY to x
    else -> x to y
  }
}

/** Shortest turn from [from] to [to] in degrees (-180 … 180). */
fun shortestTurn(from: Float, to: Float): Float = (((to - from) % 360f) + 540f) % 360f - 180f
