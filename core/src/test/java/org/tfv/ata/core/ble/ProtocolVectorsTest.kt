package org.tfv.ata.core.ble

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPrivateKeySpec

/** Test vectors from teslamotors/vehicle-command pkg/protocol/protocol.md. */
class ProtocolVectorsTest {

  private val vin = "5YJ30123456789ABC".toByteArray(Charsets.US_ASCII)
  private val clientScalar = "2538CDC29A97C19C1E99A637D6CF4F8C970C118B56EDE1E6323E6D162C4B30DB"
  private val clientPublic = hex(
    "04b2b6bc68c2da0665ce656815594996c62394edd8bea905fe781a754fe6a845a7" +
      "14330902f225e9269d466e05b349981fda9d85cc23c6fb444aa73b629105dc6e",
  )
  private val vehiclePublic = hex(
    "04c7a1f47138486aa4729971494878d33b1a24e39571f748a6e16c5955b3d877d3" +
      "a6aaa0e955166474af5d32c410f439a2234137ad1bb085fd4e8813c958f11d97",
  )
  private val sharedK = hex("1b2fce19967b79db696f909cff89ea9a")
  private val uuid = hex("1588d5a30eabc6f8fc9a951b11f6fd11")
  private val sessionInfo = hex(
    "0806124104c7a1f47138486aa4729971494878d33b1a24e39571f748a6e16c5955b3d877d3" +
      "a6aaa0e955166474af5d32c410f439a2234137ad1bb085fd4e8813c958f11d971a104c463f9c" +
      "c0d3d26906e982ed224adde6255a0a0000",
  )
  private val sessionInfoTag = hex("996c1fe38331be138f8039c194b14db2198846ed7d8251e6749284d7b32ea002")

  @Test
  fun keyAgreementMatchesSpec() {
    val params = AlgorithmParameters.getInstance("EC").apply { init(ECGenParameterSpec("secp256r1")) }
      .getParameterSpec(ECParameterSpec::class.java)
    val privateKey = KeyFactory.getInstance("EC").generatePrivate(ECPrivateKeySpec(BigInteger(clientScalar, 16), params))
    assertArrayEquals(sharedK, Crypto.sharedKey(privateKey, vehiclePublic))
  }

  @Test
  fun sessionInfoKeyMatchesSpec() {
    assertArrayEquals(
      hex("fceb679ee7bca756fcd441bf238bf2f338629b41d9eb9c67be1b32c9672ce300"),
      Crypto.subkey(sharedK, "session info"),
    )
  }

  @Test
  fun sessionInfoMetadataMatchesSpec() {
    assertArrayEquals(
      hex("000106021135594a333031323334353637383941424306101588d5a30eabc6f8fc9a951b11f6fd11ff"),
      Session.sessionInfoMetadata(vin, uuid),
    )
  }

  @Test
  fun sessionInfoParsesAndVerifies() {
    val info = SessionInfo(sessionInfo)
    assertEquals(6, info.counter)
    assertEquals(2650, info.clockTime)
    assertArrayEquals(hex("4c463f9cc0d3d26906e982ed224adde6"), info.epoch)
    assertArrayEquals(vehiclePublic, info.publicKey)
    assertEquals(0, info.status)
    assertNotNull(Session.fromSharedKey(3, vin, clientPublic, sharedK, uuid, sessionInfo, sessionInfoTag))
  }

  @Test
  fun tamperedSessionInfoIsRejected() {
    val wrongTag = sessionInfoTag.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }
    assertNull(Session.fromSharedKey(3, vin, clientPublic, sharedK, uuid, sessionInfo, wrongTag))
    val wrongChallenge = uuid.copyOf().also { it[15] = 0 }
    assertNull(Session.fromSharedKey(3, vin, clientPublic, sharedK, wrongChallenge, sessionInfo, sessionInfoTag))
  }

  @Test
  fun commandMetadataMatchesSpec() {
    val expected = hex(
      "000105010103021135594a333031323334353637383941424303104c463f9cc0d3d26906e982ed224adde6" +
        "040400000a5f050400000007070400000002ff",
    )
    val actual = Session.commandMetadata(
      domain = 3,
      vin = vin,
      epoch = hex("4c463f9cc0d3d26906e982ed224adde6"),
      expiresAt = 2655,
      counter = 7,
      flags = 2,
    )
    assertArrayEquals(expected, actual)
  }

  @Test
  fun commandMetadataOmitsZeroFlags() {
    val withoutFlags = Session.commandMetadata(3, vin, ByteArray(16), 1, 1, 0)
    assertEquals(-1, withoutFlags.indexOfTag(Tag.FLAGS))
  }

  @Test(expected = IllegalArgumentException::class)
  fun metadataRejectsDuplicateTags() {
    Metadata().add(Tag.DOMAIN, byteArrayOf(1)).add(Tag.DOMAIN, byteArrayOf(2))
  }

  /** Index of [tag] when walking the TLV string, or -1. */
  private fun ByteArray.indexOfTag(tag: Int): Int {
    var i = 0
    while (i < size) {
      val t = this[i].toInt() and 0xFF
      if (t == Tag.END) return -1
      if (t == tag) return i
      i += 2 + (this[i + 1].toInt() and 0xFF)
    }
    return -1
  }
}

internal fun hex(s: String): ByteArray = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
