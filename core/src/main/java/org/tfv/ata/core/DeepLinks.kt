package org.tfv.ata.core

import android.content.Context
import android.content.Intent
import android.net.Uri

enum class NavigationApp { TESLA, GOOGLE_MAPS, WAZE }

object DeepLinks {
  const val TESLA_PACKAGE = "com.teslamotors.tesla"
  private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
  private const val WAZE_PACKAGE = "com.waze"

  fun launchTeslaApp(context: Context): Intent? =
    context.packageManager.getLaunchIntentForPackage(TESLA_PACKAGE)
      ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      ?: storeFallback(context)

  fun teslaSection(context: Context, vararg paths: String): Intent {
    for (path in paths) {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tesla://$path")).apply {
        setPackage(TESLA_PACKAGE)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      if (intent.resolveActivity(context.packageManager) != null) return intent
    }
    return launchTeslaApp(context) ?: storeFallback(context)
  }

  fun navigate(context: Context, app: NavigationApp, query: String): Intent {
    val encoded = Uri.encode(query)
    val preferred = when (app) {
      NavigationApp.GOOGLE_MAPS ->
        Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$encoded")).setPackage(GOOGLE_MAPS_PACKAGE)
      NavigationApp.WAZE ->
        Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?q=$encoded&navigate=yes")).setPackage(WAZE_PACKAGE)
      NavigationApp.TESLA -> return teslaShare(context, query)
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (preferred.resolveActivity(context.packageManager) != null) return preferred
    // Chosen navigation app not installed: let any maps app handle it.
    return Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }

  /** Share the destination to the Tesla app (send-to-car), like the Android share sheet. */
  private fun teslaShare(context: Context, query: String): Intent {
    // Classic Google Maps share URL — the format the Tesla app parses for send-to-car.
    val mapsUrl = "https://maps.google.com/?q=${Uri.encode(query)}"
    val send = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, mapsUrl)
      putExtra(Intent.EXTRA_TITLE, query)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val direct = Intent(send).setPackage(TESLA_PACKAGE)
    return if (direct.resolveActivity(context.packageManager) != null) {
      direct
    } else {
      Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  }

  fun shareDestination(query: String): Intent =
    Intent.createChooser(
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, query)
      },
      null,
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

  /** Tesla app not installed: open its Play Store page (or the web page without a store). */
  private fun storeFallback(context: Context): Intent {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$TESLA_PACKAGE")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return if (market.resolveActivity(context.packageManager) != null) market
    else webFallback("https://play.google.com/store/apps/details?id=$TESLA_PACKAGE")
  }

  private fun webFallback(url: String) =
    Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
