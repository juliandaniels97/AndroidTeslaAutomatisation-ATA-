package org.tfv.ata.geo

import android.content.Context
import android.location.Location
import java.util.Locale
import org.tfv.ata.R
import org.tfv.ata.core.AtaSettings

/** Where the car is relative to the phone: from the phone position saved the last time the car was reached. */
object CarFinder {
  fun carLocation(settings: AtaSettings): Location? {
    if (settings.carLat == 0.0 && settings.carLng == 0.0) return null
    return Location("car").apply {
      latitude = settings.carLat
      longitude = settings.carLng
    }
  }

  fun distanceText(meters: Float): String =
    if (meters < 1000) "${meters.toInt()} m" else String.format(Locale.getDefault(), "%.1f km", meters / 1000f)

  /** Compass direction (N, NE …) of a bearing in degrees, 0 = north. */
  fun cardinal(context: Context, bearing: Float): String {
    val names = context.resources.getStringArray(R.array.compass_directions)
    val index = (((bearing % 360) + 360) % 360 / 45f + 0.5f).toInt() % 8
    return names[index]
  }

  /** Arrow pointing in a north-up direction, for places that can't rotate (widgets). */
  fun arrow(bearing: Float): String {
    val arrows = arrayOf("↑", "↗", "→", "↘", "↓", "↙", "←", "↖")
    return arrows[(((bearing % 360) + 360) % 360 / 45f + 0.5f).toInt() % 8]
  }

  fun ageText(context: Context, savedAt: Long): String {
    val minutes = ((System.currentTimeMillis() - savedAt) / 60_000).coerceAtLeast(0)
    return when {
      minutes < 1 -> context.getString(R.string.finder_age_now)
      minutes < 60 -> context.getString(R.string.finder_age_minutes, minutes)
      minutes < 48 * 60 -> context.getString(R.string.finder_age_hours, minutes / 60)
      else -> context.getString(R.string.finder_age_days, minutes / (24 * 60))
    }
  }
}
