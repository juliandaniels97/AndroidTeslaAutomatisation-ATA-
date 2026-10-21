package org.tfv.ata.ui.theme

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.tfv.ata.data.AtaStore

/** The user's chosen theme (Settings → Appearance) for screens outside MainActivity. */
@Composable
fun AtaAppTheme(content: @Composable () -> Unit) {
  val context = LocalContext.current
  val settings by remember { AtaStore.get(context).settings }.collectAsState(initial = null)
  val s = settings ?: return // first frame: wait for the stored theme instead of flashing the wrong one
  AtaTheme(themeMode = s.theme, dynamicColor = s.dynamicColor) {
    SystemBarsForTheme()
    content()
  }
}

/** Status / navigation bar icons that match the app theme (not the system theme). */
@Composable
fun SystemBarsForTheme() {
  val activity = LocalContext.current as? ComponentActivity ?: return
  val dark = AtaColors.current.isDark
  DisposableEffect(dark) {
    val style = if (dark) {
      SystemBarStyle.dark(AndroidColor.TRANSPARENT)
    } else {
      SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
    }
    activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    onDispose { }
  }
}
