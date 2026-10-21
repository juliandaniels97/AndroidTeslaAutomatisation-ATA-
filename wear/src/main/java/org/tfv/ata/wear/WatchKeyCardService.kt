package org.tfv.ata.wear

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import org.tfv.ata.core.nfc.KeyCardApdu

/**
 * The watch as a Tesla NFC key card (own key, separate from the watch's BLE key). Registered only while the user
 * turned it on, and it never answers while the watch is locked (e.g. taken off the wrist).
 */
class WatchKeyCardService : HostApduService() {
  override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray =
    KeyCardApdu.process(this, commandApdu, WearPrefs.keyCard(this), isLocked())

  override fun onDeactivated(reason: Int) = KeyCardApdu.deactivated(this, reason)

  private fun isLocked(): Boolean =
    (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isDeviceLocked == true

  companion object {
    /** Registers the card service with Android's NFC stack only while the key card is on. */
    fun apply(context: Context) {
      val wanted = if (WearPrefs.keyCard(context)) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
      else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
      runCatching {
        context.packageManager.setComponentEnabledSetting(
          ComponentName(context, WatchKeyCardService::class.java),
          wanted,
          PackageManager.DONT_KILL_APP,
        )
      }
    }
  }
}
