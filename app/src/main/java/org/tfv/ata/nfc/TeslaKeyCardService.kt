package org.tfv.ata.nfc

import android.app.KeyguardManager
import android.content.Context
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tfv.ata.core.nfc.KeyCardApdu
import org.tfv.ata.data.AtaStore

/**
 * Emulates a Tesla NFC key card over HCE with the phone's own card key.
 * Only responds when the user turned on "NFC key card" in settings and the phone is unlocked.
 */
class TeslaKeyCardService : HostApduService() {
  @Volatile private var enabled = false
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  override fun onCreate() {
    super.onCreate()
    val store = AtaStore.get(this)
    runCatching { runBlocking { enabled = store.settings.first().nfcKeyCard } }
    scope.launch { store.settings.collect { enabled = it.nfcKeyCard } }
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray =
    KeyCardApdu.process(this, commandApdu, enabled, isLocked())

  override fun onDeactivated(reason: Int) = KeyCardApdu.deactivated(this, reason)

  private fun isLocked(): Boolean {
    val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    return km != null && km.isDeviceLocked
  }
}
