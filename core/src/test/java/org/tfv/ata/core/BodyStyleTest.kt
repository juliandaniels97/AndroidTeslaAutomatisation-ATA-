package org.tfv.ata.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BodyStyleTest {
  @Test
  fun detectsModelFromVin() {
    assertEquals(BodyStyle.MODEL_3, BodyStyle.fromVin("5YJ3E7EB2KF123456"))
    assertEquals(BodyStyle.MODEL_Y, BodyStyle.fromVin("LRWYGCEK1NC123456"))
    assertEquals(BodyStyle.MODEL_Y, BodyStyle.fromVin("XP7YGCEK1PB123456"))
    assertEquals(BodyStyle.MODEL_S, BodyStyle.fromVin("5YJSA1E26HF123456"))
    assertEquals(BodyStyle.MODEL_X, BodyStyle.fromVin("5yjxcae2xgf123456"))
    assertEquals(BodyStyle.CYBERTRUCK, BodyStyle.fromVin("7G2CEHED1RA123456"))
    assertEquals(BodyStyle.ROADSTER, BodyStyle.fromVin("5YJRE1A31A1123456"))
    assertNull(BodyStyle.fromVin("WVWZZZ1KZAW123456"))
    assertNull(BodyStyle.fromVin(""))
  }

  @Test
  fun manualChoiceWinsOverVin() {
    val v = Vehicle("1", "Car", "5YJ3E7EB2KF123456")
    assertEquals(BodyStyle.MODEL_3, v.body)
    assertEquals(BodyStyle.CYBERTRUCK, v.copy(bodyStyle = "CYBERTRUCK").body)
    assertEquals(BodyStyle.MODEL_3, Vehicle("2", "Car", "WVWZZZ1KZAW123456").body)
  }
}
