package org.tfv.ata.nfc

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.tfv.ata.core.DeepLinks
import org.tfv.ata.data.AtaStore

/**
 * Keeps ATA's key-card emulation from competing with the Tesla app at the car's card reader.
 *
 * The reader selects the same AID the Tesla app registers, and with two registered services Android asks
 * which app to use on every tap. So the service is only registered when the user turned the key card on, and
 * — if the Tesla app is installed — only while ATA is open (where it is also the preferred service).
 * Everywhere else the Tesla app answers alone, without a chooser.
 */
object KeyCardRegistration {
  @Volatile private var keyCardOn = false
  @Volatile private var foreground = false
  @Volatile private var resumedActivity: WeakReference<Activity>? = null
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val handler = Handler(Looper.getMainLooper())
  private val preferenceRetriesMs = longArrayOf(500, 2_000)

  fun start(context: Context) {
    val app = context.applicationContext
    apply(app)
    scope.launch {
      AtaStore.get(app).settings.map { it.nfcKeyCard }.distinctUntilChanged().collect {
        keyCardOn = it
        apply(app)
        resumedActivity?.get()?.let { activity -> launch(Dispatchers.Main) { updatePreferred(activity) } }
      }
    }
  }

  /** Called from MainActivity's onResume / onPause. */
  fun setForeground(activity: Activity, resumed: Boolean) {
    foreground = resumed
    resumedActivity = if (resumed) WeakReference(activity) else null
    apply(activity.applicationContext)
    updatePreferred(activity)
    if (resumed) {
      // The NFC service picks up a just-enabled component a moment later: set the preference again then.
      preferenceRetriesMs.forEach { delay ->
        handler.postDelayed({ if (resumedActivity?.get() === activity) updatePreferred(activity) }, delay)
      }
    }
  }

  private fun updatePreferred(activity: Activity) {
    val emulation = cardEmulation(activity) ?: return
    runCatching {
      if (foreground && keyCardOn) emulation.setPreferredService(activity, component(activity))
      else emulation.unsetPreferredService(activity)
    }
  }

  fun teslaAppInstalled(context: Context): Boolean =
    runCatching { context.packageManager.getPackageInfo(DeepLinks.TESLA_PACKAGE, 0); true }.getOrDefault(false)

  /** True if the car's reader can currently reach ATA's key card. */
  fun active(context: Context): Boolean = keyCardOn && (foreground || !teslaAppInstalled(context))

  @Synchronized
  private fun apply(context: Context) {
    val pm = context.packageManager
    val wanted = if (active(context)) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    val component = component(context)
    if (runCatching { pm.getComponentEnabledSetting(component) }.getOrNull() == wanted) return
    runCatching { pm.setComponentEnabledSetting(component, wanted, PackageManager.DONT_KILL_APP) }
  }

  private fun component(context: Context) = ComponentName(context, TeslaKeyCardService::class.java)

  private fun cardEmulation(context: Context): CardEmulation? {
    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) return null
    val adapter = NfcAdapter.getDefaultAdapter(context) ?: return null
    return runCatching { CardEmulation.getInstance(adapter) }.getOrNull()
  }
}
