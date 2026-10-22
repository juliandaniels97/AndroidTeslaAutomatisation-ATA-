package org.tfv.ata.core.proximity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProximityDetectorTest {
  private val near = ProximityDetector.rssiForMeters(5)

  /** Feeds [rssi] every second from [from] for [seconds]; returns how often it fired. */
  private fun ProximityDetector.walk(rssi: Int, from: Long, seconds: Int): Int =
    (0 until seconds).count { onSample(rssi, from + it * 1000L) }

  @Test
  fun distanceModel() {
    assertEquals(-59, ProximityDetector.rssiForMeters(1))
    assertEquals(-73, ProximityDetector.rssiForMeters(5))
    assertEquals(-79, ProximityDetector.rssiForMeters(10))
  }

  @Test
  fun unlocksOnceWhenWalkingUp() {
    val d = ProximityDetector(near)
    assertEquals(0, d.walk(-90, 0, 10))
    assertEquals(1, d.walk(-60, 10_000, 20))
    assertEquals(ProximityDetector.State.NEAR, d.state)
  }

  @Test
  fun neverFiresWhenStartedNextToTheCar() {
    val d = ProximityDetector(near)
    assertEquals(0, d.walk(-60, 0, 60))
  }

  @Test
  fun firesAgainOnlyAfterBeingAway() {
    val d = ProximityDetector(near, cooldownMs = 0)
    d.walk(-90, 0, 5)
    assertEquals(1, d.walk(-60, 5_000, 10))
    // Short dip (e.g. turning around next to the car): no second unlock.
    d.walk(-90, 15_000, 10)
    assertEquals(0, d.walk(-60, 25_000, 10))
    // Really away for a while, then back.
    d.walk(-95, 35_000, 60)
    assertEquals(ProximityDetector.State.AWAY, d.state)
    assertEquals(1, d.walk(-60, 95_000, 10))
  }

  @Test
  fun carOutOfRangeCountsAsAway() {
    val d = ProximityDetector(near, cooldownMs = 0)
    d.walk(-90, 0, 3)
    assertEquals(1, d.walk(-60, 3_000, 5))
    d.onTick(8_000 + 60_000)
    assertEquals(ProximityDetector.State.AWAY, d.state)
    assertTrue(d.walk(-60, 70_000, 5) > 0)
  }

  @Test
  fun cooldownBlocksQuickRepeats() {
    val d = ProximityDetector(near, cooldownMs = 10 * 60_000L)
    d.walk(-90, 0, 3)
    assertEquals(1, d.walk(-60, 3_000, 5))
    d.walk(-95, 8_000, 60)
    assertEquals(0, d.walk(-60, 68_000, 5))
  }

  @Test
  fun singleSpikeDoesNotUnlock() {
    val d = ProximityDetector(near)
    d.walk(-90, 0, 5)
    assertFalse(d.onSample(-50, 5_000))
    assertEquals(0, d.walk(-90, 6_000, 5))
  }
}
