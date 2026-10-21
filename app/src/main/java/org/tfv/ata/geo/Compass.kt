package org.tfv.ata.geo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Live device heading in degrees (0 = north), updated while [enabled]. */
@Composable
fun rememberDeviceHeading(enabled: Boolean): State<Float> {
  val context = LocalContext.current
  val heading = remember { mutableFloatStateOf(0f) }

  DisposableEffect(enabled) {
    if (!enabled) {
      onDispose { }
    } else {
      val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
      val sensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
      val listener = object : SensorEventListener {
        private val rotation = FloatArray(9)
        private val orientation = FloatArray(3)
        override fun onSensorChanged(event: SensorEvent) {
          SensorManager.getRotationMatrixFromVector(rotation, event.values)
          SensorManager.getOrientation(rotation, orientation)
          heading.floatValue = Math.toDegrees(orientation[0].toDouble()).toFloat()
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
      }
      if (sm != null && sensor != null) {
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
      }
      onDispose { sm?.unregisterListener(listener) }
    }
  }
  return heading
}
