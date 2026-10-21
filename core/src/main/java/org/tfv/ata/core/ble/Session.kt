package org.tfv.ata.core.ble

import java.security.PrivateKey
import java.security.SecureRandom

/** Outcome of reading a reply payload, see protocol.md "Response decryption". */
internal sealed interface ReplyPayload {
  /** Plaintext reply (vehicle firmware without response encryption) or no payload at all. */
  data class Plain(val bytes: ByteArray?) : ReplyPayload
  data class Decrypted(val bytes: ByteArray) : ReplyPayload
  /** The AES-GCM tag didn't verify: the reply must be ignored. */
  data object AuthFailed : ReplyPayload
  /** A response counter that was already used for this request (replay): the reply must be ignored. */
  data object Replayed : ReplyPayload
}

internal class Session(
  private val domain: Int,
  private val vin: ByteArray,
  private val publicKey: ByteArray,
  private val sharedKey: ByteArray,
  private val epoch: ByteArray,
  private var counter: Int,
  private val timeZeroMs: Long,
) {
  private val random = SecureRandom()
  private var lastRequestHash: ByteArray = ByteArray(0)
  private val seenResponseCounters = HashSet<Int>()

  fun signedCommand(payload: ByteArray, routing: ByteArray, ttlSeconds: Int = DEFAULT_TTL): ByteArray {
    counter += 1
    val expiresAt = ((System.currentTimeMillis() - timeZeroMs) / 1000L).toInt() + ttlSeconds
    val flags = FLAG_ENCRYPT_RESPONSE_MASK

    val metadata = commandMetadata(domain, vin, epoch, expiresAt, counter, flags)
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
    seenResponseCounters.clear()

    return buildProto {
      message(Rm.TO_DESTINATION, buildProto { enum(DestF.DOMAIN, domain) })
      message(Rm.FROM_DESTINATION, buildProto { bytes(DestF.ROUTING_ADDRESS, routing) })
      bytes(Rm.PROTOBUF_BYTES, ciphertext)
      message(Rm.SIGNATURE_DATA, signatureData)
      varint(Rm.FLAGS, flags.toLong())
      bytes(Rm.UUID, uuid)
    }
  }

  fun readReply(response: ParsedResponse): ReplyPayload {
    val nonce = response.responseNonce ?: return ReplyPayload.Plain(response.protobufBytes)
    val tag = response.responseTag ?: return ReplyPayload.AuthFailed
    val ciphertext = response.protobufBytes ?: ByteArray(0)
    if (response.responseCounter in seenResponseCounters) return ReplyPayload.Replayed
    val metadata = responseMetadata(
      response.fromDomain, vin, response.responseCounter, response.flags, lastRequestHash, response.fault,
    )
    val aad = Crypto.sha256(metadata)
    val plain = runCatching { Crypto.aesGcmDecrypt(sharedKey, nonce, ciphertext, tag, aad) }.getOrNull()
      ?: return ReplyPayload.AuthFailed
    seenResponseCounters += response.responseCounter
    return ReplyPayload.Decrypted(plain)
  }

  companion object {
    const val DEFAULT_TTL = 30

    fun commandMetadata(domain: Int, vin: ByteArray, epoch: ByteArray, expiresAt: Int, counter: Int, flags: Int): ByteArray {
      val m = Metadata()
        .add(Tag.SIGNATURE_TYPE, byteArrayOf(SignatureType.AES_GCM_PERSONALIZED.toByte()))
        .add(Tag.DOMAIN, byteArrayOf(domain.toByte()))
        .add(Tag.PERSONALIZATION, vin)
        .add(Tag.EPOCH, epoch)
        .addUint32(Tag.EXPIRES_AT, expiresAt)
        .addUint32(Tag.COUNTER, counter)
      // Flags are only part of the request metadata when non-zero.
      if (flags != 0) m.addUint32(Tag.FLAGS, flags)
      return m.serialize()
    }

    fun responseMetadata(domain: Int, vin: ByteArray, counter: Int, flags: Int, requestHash: ByteArray, fault: Int): ByteArray =
      Metadata()
        .add(Tag.SIGNATURE_TYPE, byteArrayOf(SignatureType.AES_GCM_RESPONSE.toByte()))
        .add(Tag.DOMAIN, byteArrayOf(domain.toByte()))
        .add(Tag.PERSONALIZATION, vin)
        .addUint32(Tag.COUNTER, counter)
        .addUint32(Tag.FLAGS, flags)
        .add(Tag.REQUEST_HASH, requestHash)
        .addUint32(Tag.FAULT, fault)
        .serialize()

    fun sessionInfoMetadata(vin: ByteArray, challenge: ByteArray): ByteArray =
      Metadata()
        .add(Tag.SIGNATURE_TYPE, byteArrayOf(SignatureType.HMAC.toByte()))
        .add(Tag.PERSONALIZATION, vin)
        .add(Tag.CHALLENGE, challenge)
        .serialize()

    /** Verifies the session info HMAC and derives the session; null if the tag or the key is invalid. */
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
      return fromSharedKey(domain, vin, publicKey, sharedKey, requestUuid, sessionInfoBytes, sessionInfoTag)
    }

    fun fromSharedKey(
      domain: Int,
      vin: ByteArray,
      publicKey: ByteArray,
      sharedKey: ByteArray,
      requestUuid: ByteArray,
      sessionInfoBytes: ByteArray,
      sessionInfoTag: ByteArray,
    ): Session? {
      val info = SessionInfo(sessionInfoBytes)
      val sessionInfoKey = Crypto.subkey(sharedKey, "session info")
      val expected = Crypto.hmacSha256(sessionInfoKey, sessionInfoMetadata(vin, requestUuid) + sessionInfoBytes)
      if (!Crypto.constantTimeEquals(expected, sessionInfoTag)) return null
      val timeZeroMs = System.currentTimeMillis() - info.clockTime.toLong() * 1000L
      return Session(domain, vin, publicKey, sharedKey, info.epoch, info.counter, timeZeroMs)
    }
  }
}
