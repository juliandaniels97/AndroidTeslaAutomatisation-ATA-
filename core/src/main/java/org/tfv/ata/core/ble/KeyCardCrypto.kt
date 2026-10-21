package org.tfv.ata.core.ble

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.SecretKeySpec

/**
 * Tesla NFC key-card authentication, reusing the phone's enrolled EC key (the same
 * key as the BLE phone key). Protocol: ECDH(P-256) with the reader's ephemeral public
 * key, session key = SHA1(sharedSecret)[:16], response = AES-128-ECB(challenge).
 *
 * Ref: github.com/pccr10001/TeslaWearKey, gist darconeous/2cd2de11148e3a75685940158bddf933
 */
object KeyCardCrypto {

  /** 65-byte uncompressed public key (0x04 || X || Y) presented to the reader. */
  fun publicKey(): ByteArray = VehicleKey.publicKeyBytes()

  /**
   * @param readerPublicKey 65-byte uncompressed EC public key from the car's reader.
   * @param challenge16 the 16-byte challenge sent by the reader (after its 65-byte ephemeral key).
   * @return 16-byte AES-ECB ciphertext.
   */
  fun authenticate(readerPublicKey: ByteArray, challenge16: ByteArray): ByteArray {
    val ka = KeyAgreement.getInstance("ECDH", "AndroidKeyStore")
    ka.init(VehicleKey.privateKey())
    ka.doPhase(Ec.publicKeyFromUncompressed(readerPublicKey), true)
    val shared = ka.generateSecret()
    val key = MessageDigest.getInstance("SHA-1").digest(shared).copyOf(16)
    val cipher = Cipher.getInstance("AES/ECB/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
    return cipher.doFinal(challenge16)
  }
}
