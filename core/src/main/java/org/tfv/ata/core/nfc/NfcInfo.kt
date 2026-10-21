package org.tfv.ata.core.nfc

import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter

/** NFC capabilities of this device (phone or watch). */
object NfcInfo {
  fun hasNfc(context: Context): Boolean = NfcAdapter.getDefaultAdapter(context) != null

  fun isEnabled(context: Context): Boolean = NfcAdapter.getDefaultAdapter(context)?.isEnabled == true

  fun hasCardEmulation(context: Context): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
}
