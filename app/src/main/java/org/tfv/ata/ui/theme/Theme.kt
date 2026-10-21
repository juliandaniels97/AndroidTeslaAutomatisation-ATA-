package org.tfv.ata.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.tfv.ata.core.ThemeMode

// Logo-inspired palette: electric blue + neon red on deep dark.
val NeonBlue = Color(0xFF4FB4FF)
val NeonRed = Color(0xFFFF4D40)

private val LightColors = lightColorScheme(
  primary = Color(0xFF0B63BD),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFD5E6FF),
  onPrimaryContainer = Color(0xFF001C3B),
  secondary = Color(0xFF4F5D6C),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFDDE5EF),
  onSecondaryContainer = Color(0xFF1A2530),
  tertiary = Color(0xFFC62828),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFFFE3DF),
  onTertiaryContainer = Color(0xFF410002),
  error = Color(0xFFB3261E),
  background = Color(0xFFF4F6F9),
  onBackground = Color(0xFF161B22),
  surface = Color(0xFFF4F6F9),
  onSurface = Color(0xFF161B22),
  surfaceVariant = Color(0xFFE3E8EF),
  onSurfaceVariant = Color(0xFF45505C),
  surfaceContainerLowest = Color.White,
  surfaceContainerLow = Color(0xFFFAFBFD),
  surfaceContainer = Color(0xFFEEF2F6),
  surfaceContainerHigh = Color(0xFFE8ECF2),
  surfaceContainerHighest = Color(0xFFE1E6ED),
  outline = Color(0xFF8592A0),
  outlineVariant = Color(0xFFC8D1DC),
)

private val DarkColors = darkColorScheme(
  primary = NeonBlue,
  onPrimary = Color(0xFF001320),
  primaryContainer = Color(0xFF123247),
  onPrimaryContainer = Color(0xFFCDE7FF),
  secondary = Color(0xFF9FB2C6),
  onSecondary = Color(0xFF0E1A26),
  secondaryContainer = Color(0xFF263241),
  onSecondaryContainer = Color(0xFFD5E2F0),
  tertiary = NeonRed,
  onTertiary = Color(0xFF2A0603),
  tertiaryContainer = Color(0xFF3A1512),
  onTertiaryContainer = Color(0xFFFFD7D2),
  background = Color(0xFF0A0C10),
  onBackground = Color(0xFFE6ECF5),
  surface = Color(0xFF0E1116),
  onSurface = Color(0xFFE6ECF5),
  surfaceVariant = Color(0xFF1B1F27),
  onSurfaceVariant = Color(0xFFABB6C4),
  surfaceContainerLowest = Color(0xFF080A0D),
  surfaceContainerLow = Color(0xFF111419),
  surfaceContainer = Color(0xFF15191F),
  surfaceContainerHigh = Color(0xFF1B1F27),
  surfaceContainerHighest = Color(0xFF232833),
  outline = Color(0xFF59636F),
  outlineVariant = Color(0xFF2E3540),
)

/** ATA-specific colours (action tiles, accents, badges) that follow the light / dark theme. */
@Immutable
data class AtaPalette(
  val isDark: Boolean,
  /** Normal actions: icon, glow and tile edge. */
  val accent: Color,
  /** Critical actions that open the car. */
  val critical: Color,
  val tileTop: Color,
  val tileBottom: Color,
  val tileEdge: Color,
  val tileText: Color,
  val badgeBackground: Color,
  val badgeIcon: Color,
  val success: Color,
  val warning: Color,
  val danger: Color,
  val carBody: Color,
  val carGlass: Color,
  val muted: Color,
)

private val DarkPalette = AtaPalette(
  isDark = true,
  accent = NeonBlue,
  critical = NeonRed,
  tileTop = Color(0xFF1C2230),
  tileBottom = Color(0xFF0C0F15),
  tileEdge = Color(0xFF2A3446),
  tileText = Color(0xFFE6ECF5),
  badgeBackground = Color(0xCC0B0F16),
  badgeIcon = Color(0xFF9FB2C6),
  success = Color(0xFF2ECC71),
  warning = Color(0xFFF1C40F),
  danger = Color(0xFFE74C3C),
  carBody = Color(0xFF53606D),
  carGlass = Color(0xFF2B333B),
  muted = Color(0xFF8A929E),
)

private val LightPalette = AtaPalette(
  isDark = false,
  accent = Color(0xFF0B63BD),
  critical = Color(0xFFC62828),
  tileTop = Color.White,
  tileBottom = Color(0xFFEDF1F6),
  tileEdge = Color(0xFFC8D1DC),
  tileText = Color(0xFF161B22),
  badgeBackground = Color(0xE6FFFFFF),
  badgeIcon = Color(0xFF5B6B7C),
  success = Color(0xFF1E8E4E),
  warning = Color(0xFFB7791F),
  danger = Color(0xFFC62828),
  carBody = Color(0xFF8B97A4),
  carGlass = Color(0xFF3E4752),
  muted = Color(0xFF7A8592),
)

val LocalAtaPalette = staticCompositionLocalOf { DarkPalette }

/** Shortcut: `AtaColors.current.accent` etc. */
object AtaColors {
  val current: AtaPalette
    @Composable get() = LocalAtaPalette.current
}

fun isDarkTheme(themeMode: ThemeMode, systemDark: Boolean): Boolean = when (themeMode) {
  ThemeMode.SYSTEM -> systemDark
  ThemeMode.LIGHT -> false
  ThemeMode.DARK -> true
}

@Composable
fun AtaTheme(
  themeMode: ThemeMode,
  dynamicColor: Boolean,
  content: @Composable () -> Unit,
) {
  val dark = isDarkTheme(themeMode, isSystemInDarkTheme())
  val context = LocalContext.current
  val colors: ColorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
      if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    dark -> DarkColors
    else -> LightColors
  }
  val base = if (dark) DarkPalette else LightPalette
  // With Material You, accents follow the wallpaper colours; critical stays red.
  val palette = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    base.copy(accent = colors.primary)
  } else {
    base
  }
  CompositionLocalProvider(LocalAtaPalette provides palette) {
    MaterialTheme(colorScheme = colors, content = content)
  }
}
