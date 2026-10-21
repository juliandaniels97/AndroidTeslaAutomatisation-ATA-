package org.tfv.ata.core.ble

import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.ECGenParameterSpec

internal object Ec {
  private val params: ECParameterSpec by lazy {
    val ap = AlgorithmParameters.getInstance("EC")
    ap.init(ECGenParameterSpec("secp256r1"))
    ap.getParameterSpec(ECParameterSpec::class.java)
  }

  fun publicKeyFromUncompressed(bytes: ByteArray): ECPublicKey {
    require(bytes.size == 65 && bytes[0].toInt() == 0x04) { "invalid uncompressed public key" }
    val x = BigInteger(1, bytes.copyOfRange(1, 33))
    val y = BigInteger(1, bytes.copyOfRange(33, 65))
    val spec = ECPublicKeySpec(ECPoint(x, y), params)
    return KeyFactory.getInstance("EC").generatePublic(spec) as ECPublicKey
  }

  fun encodeUncompressed(key: ECPublicKey): ByteArray {
    val x = toFixed(key.w.affineX, 32)
    val y = toFixed(key.w.affineY, 32)
    return byteArrayOf(0x04) + x + y
  }

  private fun toFixed(value: BigInteger, size: Int): ByteArray {
    val raw = value.toByteArray()
    val out = ByteArray(size)
    when {
      raw.size == size -> System.arraycopy(raw, 0, out, 0, size)
      raw.size == size + 1 && raw[0].toInt() == 0 ->
        System.arraycopy(raw, 1, out, 0, size)
      raw.size < size ->
        System.arraycopy(raw, 0, out, size - raw.size, raw.size)
      else ->
        System.arraycopy(raw, raw.size - size, out, 0, size)
    }
    return out
  }
}
