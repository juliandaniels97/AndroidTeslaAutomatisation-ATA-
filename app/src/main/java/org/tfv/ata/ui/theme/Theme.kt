package org.tfv.ata.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.tfv.ata.core.ThemeMode

// Logo-inspired palette: electric blue + neon red on deep dark.
val NeonBlue = Color(0xFF4FB4FF)
val NeonRed = Color(0xFFFF4D40)

private val LightColors = lightColorScheme(
  primary = Color(0xFF0E66C2),
  secondary = Color(0xFF52606E),
  tertiary = Color(0xFFD62A1A),
)

private val DarkColors = darkColorScheme(
  primary = NeonBlue,
  onPrimary = Color(0xFF001320),
  primaryContainer = Color(0xFF123247),
  onPrimaryContainer = Color(0xFFCDE7FF),
  secondary = Color(0xFF9FB2C6),
  tertiary = NeonRed,
  onTertiary = Color(0xFF2A0603),
  tertiaryContainer = Color(0xFF3A1512),
  onTertiaryContainer = Color(0xFFFFD7D2),
  background = Color(0xFF0A0C10),
  onBackground = Color(0xFFE6ECF5),
  surface = Color(0xFF13161C),
  onSurface = Color(0xFFE6ECF5),
  surfaceVariant = Color(0xFF1B1F27),
  onSurfaceVariant = Color(0xFFABB6C4),
  outline = Color(0xFF3A414C),
)

@Composable
fun AtaTheme(
  themeMode: ThemeMode,
  dynamicColor: Boolean,
  content: @Composable () -> Unit,
) {
  val dark = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
  }
  val context = LocalContext.current
  val colors = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
      if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    dark -> DarkColors
    else -> LightColors
  }
  MaterialTheme(colorScheme = colors, content = content)
}
