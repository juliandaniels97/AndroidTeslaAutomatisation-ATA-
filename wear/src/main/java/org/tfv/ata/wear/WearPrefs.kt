package org.tfv.ata.wear

import android.content.Context

/** Small persistent store for the watch: the VIN synced from the phone and the direct-control preference. */
object WearPrefs {
  private const val NAME = "ata_wear"
  private const val KEY_VIN = "vin"
  private const val KEY_VEHICLE = "vehicle"
  private const val KEY_DIRECT = "direct"
  private const val KEY_KEYCARD = "keycard"

  private fun prefs(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

  fun vin(context: Context): String = prefs(context).getString(KEY_VIN, "").orEmpty()

  fun vehicleName(context: Context): String = prefs(context).getString(KEY_VEHICLE, "").orEmpty()

  fun setVehicle(context: Context, vin: String, name: String) {
    prefs(context).edit().putString(KEY_VIN, vin).putString(KEY_VEHICLE, name).apply()
  }

  fun directMode(context: Context): Boolean = prefs(context).getBoolean(KEY_DIRECT, false)

  fun setDirectMode(context: Context, value: Boolean) {
    prefs(context).edit().putBoolean(KEY_DIRECT, value).apply()
  }

  /** The watch answers the car's card reader as an NFC key card. */
  fun keyCard(context: Context): Boolean = prefs(context).getBoolean(KEY_KEYCARD, false)

  fun setKeyCard(context: Context, value: Boolean) {
    prefs(context).edit().putBoolean(KEY_KEYCARD, value).apply()
    WatchKeyCardService.apply(context)
  }
}
