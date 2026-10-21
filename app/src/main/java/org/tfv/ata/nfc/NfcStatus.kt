package org.tfv.ata.nfc

import android.content.ComponentName
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation

/** Live NFC state for the key-card assistant, the NFC tag screen and the bug report. */
object NfcStatus {
  /** Tesla key card AID ("teslaLogic"). */
  const val TESLA_AID = "7465736C614C6F676963"

  fun hasNfc(context: Context): Boolean = org.tfv.ata.core.nfc.NfcInfo.hasNfc(context)

  fun isEnabled(context: Context): Boolean = org.tfv.ata.core.nfc.NfcInfo.isEnabled(context)

  fun hasCardEmulation(context: Context): Boolean = org.tfv.ata.core.nfc.NfcInfo.hasCardEmulation(context)

  /** True if the car's card reader currently reaches ATA (no other app answers instead). */
  fun ataAnswersReader(context: Context): Boolean {
    val adapter = NfcAdapter.getDefaultAdapter(context) ?: return false
    val emulation = runCatching { CardEmulation.getInstance(adapter) }.getOrNull() ?: return false
    val component = ComponentName(context, TeslaKeyCardService::class.java)
    return runCatching { emulation.isDefaultServiceForAid(component, TESLA_AID) }.getOrDefault(false)
  }

  fun summary(context: Context): String =
    "nfc=${hasNfc(context)} on=${isEnabled(context)} hce=${hasCardEmulation(context)} " +
      "registered=${KeyCardRegistration.active(context)} ataAnswers=${ataAnswersReader(context)} " +
      "teslaApp=${KeyCardRegistration.teslaAppInstalled(context)}"
}
