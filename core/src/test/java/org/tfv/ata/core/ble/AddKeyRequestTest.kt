package org.tfv.ata.core.ble

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddKeyRequestTest {
  private val key = ByteArray(65) { if (it == 0) 0x04 else it.toByte() }

  @Test
  fun bareToVcsecMessageWithPresentKeySignature() {
    val msg = ProtoReader(addKeyRequest(key, owner = false))
    // Not a RoutableMessage: no destination / protobuf_message_as_bytes fields (fault 10 on the car).
    assertFalse(msg.has(6))
    assertFalse(msg.has(10))
    val signed = msg.message(1)!!
    assertEquals(2L, signed.varint(3)) // SIGNATURE_TYPE_PRESENT_KEY
    val whitelistOp = ProtoReader(signed.bytes(2)!!).message(16)!!
    val change = whitelistOp.message(5)!!
    assertArrayEquals(key, change.message(1)!!.bytes(1))
    assertEquals(3L, change.varint(4)) // ROLE_DRIVER
    assertEquals(7L, whitelistOp.message(6)!!.varint(1)) // KEY_FORM_FACTOR_ANDROID_DEVICE
  }

  @Test
  fun ownerRole() {
    val signed = ProtoReader(addKeyRequest(key, owner = true)).message(1)!!
    val change = ProtoReader(signed.bytes(2)!!).message(16)!!.message(5)!!
    assertEquals(2L, change.varint(4))
  }

  @Test
  fun faultNamesAreReadable() {
    assertTrue(faultName(10).contains("decoding"))
    assertTrue(faultName(14).contains("full"))
    assertTrue(faultName(25).contains("exceeds MTU"))
    assertTrue(faultName(99).contains("99"))
  }
}
