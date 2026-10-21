package org.tfv.ata.geo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import org.tfv.ata.core.AtaSettings

object Geo {

  fun hasLocationPermission(context: Context): Boolean =
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
      ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

  fun lastLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    return try {
      val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
      )
      providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
    } catch (e: SecurityException) {
      null
    }
  }

  /** Screen-relative bearing (degrees, 0 = device top) toward the last-known car location, or null. */
  fun screenBearingToCar(context: Context, settings: AtaSettings, headingDegrees: Float): Float? {
    if (!settings.directionFx) return null
    if (settings.carLat == 0.0 && settings.carLng == 0.0) return null
    val me = lastLocation(context) ?: return null
    val car = Location("car").apply {
      latitude = settings.carLat
      longitude = settings.carLng
    }
    if (me.distanceTo(car) < 25f) return null
    val geoBearing = me.bearingTo(car)
    return geoBearing - headingDegrees
  }
}
