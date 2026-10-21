package org.tfv.ata.debug

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.tfv.ata.BuildConfig
import org.tfv.ata.automation.BackgroundLaunch
import org.tfv.ata.automation.Notifier
import org.tfv.ata.core.Vin
import org.tfv.ata.core.WatchState
import org.tfv.ata.core.WearContract
import org.tfv.ata.core.ble.BleLog
import org.tfv.ata.core.ble.BleTransport
import org.tfv.ata.core.ble.TeslaController
import org.tfv.ata.core.ble.VehicleKey
import org.tfv.ata.data.AtaStore
import org.tfv.ata.nfc.KeyCardRegistration
import org.tfv.ata.wear.WatchLink
import java.net.URLEncoder

/**
 * Debug report for problem reports: app and device info, setup state and the Bluetooth logs of phone and watch.
 * The VIN is shortened; no keys or tokens are included.
 */
object DebugReport {
  private const val ISSUES_URL = "https://github.com/juliandaniels97/AndroidTeslaAutomatisation-ATA-/issues/new"
  /** Browsers and GitHub reject much longer URLs. */
  private const val MAX_URL = 7_500

  /** Asks the watch for a fresh report (if one is reachable) and waits briefly for the answer. */
  suspend fun refreshWatch(context: Context): WatchState? {
    val before = WatchLink.state.value
    if (!WatchLink.send(context, WearContract.WATCH_CMD_STATE)) return before
    return withTimeoutOrNull(4_000) { WatchLink.state.first { it != null && it !== before } } ?: before
  }

  suspend fun build(context: Context, watch: WatchState?, maxLogLines: Int = Int.MAX_VALUE): String {
    val store = AtaStore.get(context)
    val settings = store.settings.first()
    val vehicles = store.vehicles.first()
    val selectedId = store.selectedVehicleId.first()
    val selected = vehicles.firstOrNull { it.id == selectedId } ?: vehicles.firstOrNull()
    val fingerprint = if (VehicleKey.isSupported) runCatching { VehicleKey.publicKeyFingerprint().take(23) }.getOrNull() else null
    return buildString {
      appendLine("ATA debug report")
      appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
      appendLine("Phone: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
      appendLine("Tesla app installed: ${KeyCardRegistration.teslaAppInstalled(context)}")
      appendLine(
        "Settings: active=${settings.active} ble=${settings.experimentalBle} nfcKeyCard=${settings.nfcKeyCard} " +
          "theme=${settings.theme} poll=${settings.pollIntervalMinutes}min",
      )
      appendLine("Vehicles: ${vehicles.size}")
      selected?.let { v ->
        val vin = Vin.normalize(v.vin)
        appendLine("Selected VIN: ${maskVin(vin)} valid=${Vin.isValid(vin)} bleName=${Vin.bleLocalName(vin)}")
      }
      appendLine(
        "Bluetooth: on=${BleTransport.isBluetoothOn(context)} permission=${TeslaController.hasBlePermission(context)} " +
          "keySupported=${VehicleKey.isSupported} key=${fingerprint ?: "-"}",
      )
      appendLine(
        "Location: permission=${BleTransport.hasLocationPermission(context)} on=${BleTransport.isLocationOn(context)}",
      )
      appendLine("Notifications=${Notifier.canNotify(context)} overlay=${BackgroundLaunch.allowed(context)}")
      appendLine("NFC: ${org.tfv.ata.nfc.NfcStatus.summary(context)}")
      appendLine()
      appendLine("--- Phone Bluetooth log ---")
      appendLine(tail(BleLog.text(), maxLogLines).ifBlank { "(empty: run \"Test connection\" or pairing first)" })
      appendLine()
      appendLine("--- NFC key card log ---")
      appendLine(tail(org.tfv.ata.core.nfc.NfcLog.text(context), maxLogLines).ifBlank { "(empty: no card reader contact yet)" })
      appendLine()
      appendLine("--- Watch ---")
      when {
        watch == null -> appendLine("No answer from a watch (${WatchLink.reach.value}).")
        else -> {
          appendLine("Watch: ${watch.model}, SDK ${watch.sdk}, app ${watch.appVersion}")
          appendLine(
            "supported=${watch.supported} permission=${watch.permission} bluetooth=${watch.bluetoothOn} " +
              "location=${watch.locationPermission}/${watch.locationOn} " +
              "direct=${watch.directMode} vin=${maskVin(watch.vin)}",
          )
          appendLine("last: running=${watch.running} step=${watch.step} outcome=${watch.outcome} code=${watch.code} probe=${watch.probe}")
          appendLine("--- Watch Bluetooth log ---")
          appendLine(tail(watch.log, maxLogLines).ifBlank { "(empty)" })
          appendLine(
            "Watch NFC: nfc=${watch.nfc} on=${watch.nfcOn} hce=${watch.cardEmulation} keyCard=${watch.keyCard} " +
              "lastContact=${watch.nfcContactAgeSec}s",
          )
          appendLine("--- Watch NFC key card log ---")
          appendLine(tail(watch.nfcLog, maxLogLines).ifBlank { "(empty)" })
        }
      }
    }
  }

  fun share(context: Context, report: String) {
    val send = Intent(Intent.ACTION_SEND)
      .setType("text/plain")
      .putExtra(Intent.EXTRA_SUBJECT, "ATA debug report ${BuildConfig.VERSION_NAME}")
      .putExtra(Intent.EXTRA_TEXT, report)
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }

  /** Opens a pre-filled GitHub issue. Logs are shortened to their latest lines so the link stays valid. */
  suspend fun openIssue(context: Context, watch: WatchState?, title: String, description: String) {
    var lines = 120
    var url: String
    do {
      val body = buildString {
        if (description.isNotBlank()) appendLine(description).appendLine()
        appendLine("```")
        append(build(context, watch, lines))
        appendLine("```")
      }
      url = "$ISSUES_URL?title=${enc(title)}&labels=bug&body=${enc(body)}"
      lines -= 15
    } while (url.length > MAX_URL && lines > 0)
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }

  private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

  private fun tail(text: String, maxLines: Int): String {
    val lines = text.lines()
    return if (lines.size <= maxLines) text else "(… ${lines.size - maxLines} earlier lines)\n" + lines.takeLast(maxLines).joinToString("\n")
  }

  /** "5YJ3E7EB0KF123456" → "5YJ…3456": enough to compare, not the full VIN. */
  fun maskVin(vin: String): String = if (vin.length < 8) (if (vin.isBlank()) "-" else "…") else vin.take(3) + "…" + vin.takeLast(4)
}
