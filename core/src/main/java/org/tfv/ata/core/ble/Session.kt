package org.tfv.ata.core.ble

import java.security.PrivateKey
import java.security.SecureRandom

internal class Session(
  private val domain: Int,
  private val vin: ByteArray,
  private val privateKey: PrivateKey,
  private val publicKey: ByteArray,
  private val sharedKey: ByteArray,
  private val epoch: ByteArray,
  private var counter: Int,
  private val timeZeroMs: Long,
) {
  private val random = SecureRandom()

  fun signedCommand(payload: ByteArray, routing: ByteArray, ttlSeconds: Int = DEFAULT_TTL): ByteArray {
    counter += 1
    val expiresAt = ((System.currentTimeMillis() - timeZeroMs) / 1000L).toInt() + ttlSeconds
    val flags = FLAG_ENCRYPT_RESPONSE_MASK

    val metadata = Metadata()
      .add(Tag.SIGNATURE_TYPE, byteArrayOf(SignatureType.AES_GCM_PERSONALIZED.toByte()))
      .add(Tag.DOMAIN, byteArrayOf(domain.toByte()))
      .add(Tag.PERSONALIZATION, vin)
      .add(Tag.EPOCH, epoch)
      .addUint32(Tag.EXPIRES_AT, expiresAt)
      .addUint32(Tag.COUNTER, counter)
      .addUint32(Tag.FLAGS, flags)
      .serialize()
    val aad = Crypto.sha256(metadata)

    val nonce = ByteArray(12).also { random.nextBytes(it) }
    val sealed = Crypto.aesGcmEncrypt(sharedKey, nonce, payload, aad)
    val ciphertext = sealed.copyOfRange(0, sealed.size - 16)
    val tag = sealed.copyOfRange(sealed.size - 16, sealed.size)

    val uuid = ByteArray(16).also { random.nextBytes(it) }

    val gcm = buildProto {
      bytes(GcmF.EPOCH, epoch)
      bytes(GcmF.NONCE, nonce)
      varint(GcmF.COUNTER, counter.toLong())
      fixed32(GcmF.EXPIRES_AT, expiresAt)
      bytes(GcmF.TAG, tag)
    }
    val signatureData = buildProto {
      message(SigF.SIGNER_IDENTITY, buildProto { bytes(KeyIdF.PUBLIC_KEY, publicKey) })
      message(SigF.AES_GCM_PERSONALIZED, gcm)
    }

    lastRequestHash = byteArrayOf(SignatureType.AES_GCM_PERSONALIZED.toByte()) + tag

    return buildProto {
      message(Rm.TO_DESTINATION, buildProto { enum(DestF.DOMAIN, domain) })
      message(Rm.FROM_DESTINATION, buildProto { bytes(DestF.ROUTING_ADDRESS, routing) })
      bytes(Rm.PROTOBUF_BYTES, ciphertext)
      message(Rm.SIGNATURE_DATA, signatureData)
      varint(Rm.FLAGS, flags.toLong())
      bytes(Rm.UUID, uuid)
    }
  }

  private var lastRequestHash: ByteArray = ByteArray(0)

  fun decryptResponse(response: ParsedResponse): ByteArray? {
    val nonce = response.responseNonce ?: return response.protobufBytes
    val tag = response.responseTag ?: return null
    val ciphertext = response.protobufBytes ?: return null
    val metadata = Metadata()
      .add(Tag.SIGNATURE_TYPE, byteArrayOf(SignatureType.AES_GCM_RESPONSE.toByte()))
      .add(Tag.DOMAIN, byteArrayOf(response.fromDomain.toByte()))
      .add(Tag.PERSONALIZATION, vin)
      .addUint32(Tag.COUNTER, response.responseCounter)
      .addUint32(Tag.FLAGS, response.flags)
      .add(Tag.REQUEST_HASH, lastRequestHash)
      .addUint32(Tag.FAULT, response.fault)
      .serialize()
    val aad = Crypto.sha256(metadata)
    return runCatching { Crypto.aesGcmDecrypt(sharedKey, nonce, ciphertext, tag, aad) }.getOrNull()
  }

  companion object {
    const val DEFAULT_TTL = 30

    fun fromHandshake(
      domain: Int,
      vin: ByteArray,
      privateKey: PrivateKey,
      publicKey: ByteArray,
      requestUuid: ByteArray,
      sessionInfoBytes: ByteArray,
      sessionInfoTag: ByteArray,
    ): Session? {
      val info = SessionInfo(sessionInfoBytes)
      if (info.publicKey.size != 65) return null
      val sharedKey = Crypto.sharedKey(privateKey, info.publicKey)

      val sessionInfoKey = Crypto.subkey(sharedKey, "session info")
      val metadata = Metadata()
        .add(Tag.SIGNATURE_TYPE, byteArrayOf(SignatureType.HMAC.toByte()))
        .add(Tag.PERSONALIZATION, vin)
        .add(Tag.CHALLENGE, requestUuid)
        .serialize()
      val expected = Crypto.hmacSha256(sessionInfoKey, metadata + sessionInfoBytes)
      if (!Crypto.constantTimeEquals(expected, sessionInfoTag)) return null

      val timeZeroMs = System.currentTimeMillis() - info.clockTime.toLong() * 1000L
      return Session(domain, vin, privateKey, publicKey, sharedKey, info.epoch, info.counter, timeZeroMs)
    }
  }
}
