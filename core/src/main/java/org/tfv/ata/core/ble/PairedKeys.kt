package org.tfv.ata.core.ble

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.tfv.ata.core.Vin

/**
 * Remembers for which cars this device's key works (a session was established), so the UI can stop
 * offering pairing once it's done. Cleared when the car reports the key as unknown.
 */
object PairedKeys {
  private const val PREFS = "ata_paired_keys"
  private const val KEY = "vins"
  private val _vins = MutableStateFlow<Set<String>?>(null)

  fun vins(context: Context): StateFlow<Set<String>?> {
    if (_vins.value == null) _vins.value = prefs(context).getStringSet(KEY, emptySet()).orEmpty().toSet()
    return _vins.asStateFlow()
  }

  fun isPaired(context: Context, vin: String): Boolean = Vin.normalize(vin) in vins(context).value.orEmpty()

  fun set(context: Context, vin: String, paired: Boolean) {
    val v = Vin.normalize(vin)
    val current = vins(context).value.orEmpty()
    val next = if (paired) current + v else current - v
    if (next == current) return
    _vins.value = next
    prefs(context).edit().putStringSet(KEY, next).apply()
  }

  private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
