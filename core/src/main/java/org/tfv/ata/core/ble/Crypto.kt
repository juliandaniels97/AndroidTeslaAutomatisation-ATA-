package org.tfv.ata.core.ble

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.PrivateKey
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object Tag {
  const val SIGNATURE_TYPE = 0
  const val DOMAIN = 1
  const val PERSONALIZATION = 2
  const val EPOCH = 3
  const val EXPIRES_AT = 4
  const val COUNTER = 5
  const val CHALLENGE = 6
  const val FLAGS = 7
  const val REQUEST_HASH = 8
  const val FAULT = 9
  const val END = 255
}

internal object SignatureType {
  const val AES_GCM_PERSONALIZED = 5
  const val HMAC = 6
  const val AES_GCM_RESPONSE = 9
}

internal object Crypto {

  fun sharedKey(privateKey: PrivateKey, vehiclePublicKey: ByteArray): ByteArray {
    val agreement = KeyAgreement.getInstance("ECDH")
    agreement.init(privateKey)
    agreement.doPhase(Ec.publicKeyFromUncompressed(vehiclePublicKey), true)
    val secret = agreement.generateSecret()
    val x = if (secret.size > 32) secret.copyOfRange(secret.size - 32, secret.size) else secret
    return MessageDigest.getInstance("SHA-1").digest(x).copyOf(16)
  }

  fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(key, "HmacSHA256"))
    return mac.doFinal(data)
  }

  fun subkey(sharedKey: ByteArray, label: String): ByteArray =
    hmacSha256(sharedKey, label.toByteArray(Charsets.US_ASCII))

  fun aesGcmEncrypt(key: ByteArray, nonce: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
    cipher.updateAAD(aad)
    return cipher.doFinal(plaintext)
  }

  fun aesGcmDecrypt(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray, tag: ByteArray, aad: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
    cipher.updateAAD(aad)
    return cipher.doFinal(ciphertext + tag)
  }

  fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

  fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
    if (a.size != b.size) return false
    var result = 0
    for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
    return result == 0
  }
}

internal class Metadata {
  private val out = ByteArrayOutputStream()
  private var lastTag = -1

  fun add(tag: Int, value: ByteArray): Metadata {
    require(tag >= lastTag) { "metadata out of order" }
    require(value.size <= 255)
    lastTag = tag
    out.write(tag)
    out.write(value.size)
    out.write(value)
    return this
  }

  fun addUint32(tag: Int, value: Int): Metadata {
    val b = byteArrayOf(
      (value ushr 24).toByte(),
      (value ushr 16).toByte(),
      (value ushr 8).toByte(),
      value.toByte(),
    )
    return add(tag, b)
  }

  fun serialize(): ByteArray {
    out.write(Tag.END)
    return out.toByteArray()
  }
}
