package org.tfv.ata.core.ble

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.interfaces.ECPublicKey

object VehicleKey {
  private const val KEYSTORE = "AndroidKeyStore"
  private const val ALIAS = "ata-vehicle-ble-key"

  /** ECDH keys in the Android Keystore (PURPOSE_AGREE_KEY) need Android 12 / API 31. */
  val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

  @Synchronized
  private fun ensure() {
    if (!isSupported) throw UnsupportedOperationException("Direct BLE control needs Android 12 (API 31) or newer")
    val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
    if (ks.containsAlias(ALIAS)) return
    val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE)
    generator.initialize(
      KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_AGREE_KEY)
        .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
        .setDigests(KeyProperties.DIGEST_SHA256)
        .build(),
    )
    generator.generateKeyPair()
  }

  internal fun privateKey(): PrivateKey {
    ensure()
    val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
    return ks.getKey(ALIAS, null) as PrivateKey
  }

  fun publicKeyBytes(): ByteArray {
    ensure()
    val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
    val pub = ks.getCertificate(ALIAS).publicKey as ECPublicKey
    return Ec.encodeUncompressed(pub)
  }

  fun publicKeyFingerprint(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(publicKeyBytes())
    return digest.joinToString(":") { "%02X".format(it) }
  }
}
