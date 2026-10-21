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
}
