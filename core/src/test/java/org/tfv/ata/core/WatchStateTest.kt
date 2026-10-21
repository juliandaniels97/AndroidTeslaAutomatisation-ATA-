package org.tfv.ata.core

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchStateTest {
  @Test
  fun roundTrip() {
    val state = WatchState(
      model = "SM-R930", sdk = 34, appVersion = "1.4.1", supported = true, permission = true, bluetoothOn = true,
      locationPermission = true, locationOn = false,
      vin = "5YJ3E7EB0KF000000", directMode = false, running = "pair", step = "CONNECTING", outcome = "",
      code = "NOT_REACHABLE", probe = "ok", log = "  0.0s  line one\n  1.2s  line two\n\nafter blank",
    )
    assertEquals(state, WatchState.decode(state.encode()))
  }

  @Test
  fun emptyLog() {
    val state = WatchState(model = "x")
    assertEquals(state, WatchState.decode(state.encode()))
  }
}
