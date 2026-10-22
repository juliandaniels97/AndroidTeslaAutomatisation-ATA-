package org.tfv.ata.ui.components.car

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis

/** Wall-clock time, updated every frame while the caller is shown (for the car's animations). */
@Composable
fun rememberFrameClock(): State<Long> = produceState(System.currentTimeMillis()) {
  while (true) withFrameMillis { value = System.currentTimeMillis() }
}
