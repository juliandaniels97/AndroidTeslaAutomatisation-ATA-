package org.tfv.ata.ui.tiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import org.tfv.ata.core.VehicleSnapshot
import org.tfv.ata.core.projected

/**
 * [snapshot] carried forward to now while the car charges (time to full counts down, battery, range and energy
 * rise), refreshed every 30 s; unchanged otherwise.
 */
@Composable
fun rememberLiveSnapshot(snapshot: VehicleSnapshot?): VehicleSnapshot? {
  val now by produceState(System.currentTimeMillis(), snapshot?.charging) {
    while (snapshot?.charging == true) {
      value = System.currentTimeMillis()
      delay(30_000)
    }
  }
  return remember(snapshot, now) { snapshot?.projected(now) }
}

/** "20.4 kWh" with one decimal in the phone's language (20,4 in German). */
fun formatKwh(kwh: Float): String = String.format(java.util.Locale.getDefault(), "%.1f kWh", kwh)
