package org.tfv.ata.core.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatusCombineTest {
  private fun response(field: Int, inner: ByteArray) =
    buildProto { message(1, buildProto { enum(1, 0) }); message(2, buildProto { message(field, inner) }) }

  @Test
  fun categoriesAreMergedIntoOneVehicleData() {
    val charge = response(3, buildProto { enum(114, 77) })
    val climate = response(4, buildProto { fixed32(101, java.lang.Float.floatToIntBits(21.5f)) })
    val closures = response(9, buildProto { enum(113, 1) })
    val snapshot = StatusParser.parse(StatusParser.combine(listOf(charge, climate, closures))!!)!!
    assertEquals(77, snapshot.batteryPercent)
    assertEquals(21.5f, snapshot.insideTempC, 0.01f)
    assertEquals(true, snapshot.locked)
  }

  @Test
  fun noAnswersGiveNothing() {
    assertNull(StatusParser.combine(emptyList()))
  }

  @Test
  fun chargingPowerSeatsDoorsAndVolumeAreRead() {
    val charge = response(3, buildProto {
      message(1, buildProto { message(5, ByteArray(0)) })
      enum(114, 60)
      enum(122, 48)
      enum(104, 80)
    })
    val climate = response(4, buildProto { enum(113, 3); enum(116, 1); bool(125, true) })
    val closures = response(9, buildProto { bool(106, true); bool(108, true) })
    val media = response(20, buildProto { fixed32(5, java.lang.Float.floatToIntBits(6.3f)); fixed32(7, java.lang.Float.floatToIntBits(11f)) })
    val s = StatusParser.parse(StatusParser.combine(listOf(charge, climate, closures, media))!!)!!
    assertEquals(true, s.charging)
    assertEquals(48, s.chargingPowerKw)
    assertEquals(80, s.chargeLimit)
    assertEquals(3, s.seatHeatFrontLeft)
    assertEquals(1, s.seatHeatRearRight)
    assertEquals(null, s.seatHeatRearCenter)
    assertEquals(true, s.steeringHeater)
    assertEquals(true, s.trunkOpen)
    assertEquals(true, s.windowsOpen)
    assertEquals(6.3f, s.volume!!, 0.01f)
    assertEquals(11f, s.volumeMax, 0.01f)
  }

  @Test
  fun lowAcPowerFallsBackToVoltsTimesAmps() {
    val charge = response(3, buildProto {
      message(1, buildProto { message(5, ByteArray(0)) })
      enum(119, 230); enum(121, 6); enum(134, 3)
    })
    assertEquals(4, StatusParser.parse(StatusParser.combine(listOf(charge))!!)!!.chargingPowerKw)
  }
}
