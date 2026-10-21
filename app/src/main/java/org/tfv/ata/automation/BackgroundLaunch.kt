package org.tfv.ata.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Android only lets a background app open other apps when the user granted "Display over other apps".
 * ATA needs that for watch taps and automations that open the Tesla app or navigation on the phone.
 */
object BackgroundLaunch {
  fun allowed(context: Context): Boolean = Settings.canDrawOverlays(context)

  fun settingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
