package org.tfv.ata.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProjectionTest {
  private val start = DemoCar.start(0L).copy(updatedAt = 1_000L)

  @Test
  fun halfTheTimeLaterHalfTheWayToTheLimit() {
    val p = start.projected(1_000L + 30 * 60_000L)
    assertEquals(30, p.minutesToFull)
    assertEquals(65, p.batteryPercent)
    assertEquals(31f, p.chargeEnergyAddedKwh, .01f)
    assertEquals(265 * 65 / 50, p.rangeKm)
  }

  @Test
  fun afterTheTimeToFullItStopsAtTheLimit() {
    val p = start.projected(1_000L + 3 * 60 * 60_000L)
    assertEquals(80, p.batteryPercent)
    assertEquals(0, p.minutesToFull)
    assertFalse(p.charging)
    assertEquals(42f, p.chargeEnergyAddedKwh, .01f)
  }

  @Test
  fun notChargingStaysAsItIs() {
    val idle = start.copy(charging = false)
    assertEquals(idle, idle.projected(1_000L + 60 * 60_000L))
  }

  @Test
  fun intervalStepsAre120From5MinTo3h() {
    assertEquals(120, StatusIntervals.steps.size)
    assertEquals(5, StatusIntervals.steps.first())
    assertEquals(180, StatusIntervals.steps.last())
    assertEquals(StatusIntervals.steps.sorted().distinct(), StatusIntervals.steps)
  }
}
