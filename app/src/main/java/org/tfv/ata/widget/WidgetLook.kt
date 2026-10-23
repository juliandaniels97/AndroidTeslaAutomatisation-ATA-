package org.tfv.ata.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import org.tfv.ata.core.AtaSettings
import org.tfv.ata.core.ThemeMode
import org.tfv.ata.core.UiDesign
import org.tfv.ata.core.WidgetDesign
import androidx.glance.color.ColorProvider as DayNight

/** Colours of a widget, matching the app's tiles: neon (glowing edge) or Material (tonal), light / dark. */
data class WidgetLook(
  val neon: Boolean,
  val background: ColorProvider,
  val card: ColorProvider,
  val edge: ColorProvider,
  val text: ColorProvider,
  val muted: ColorProvider,
  val accent: ColorProvider,
  val iconBackground: ColorProvider,
  val onIconBackground: ColorProvider,
  val charge: ColorProvider,
  val warning: ColorProvider,
  val critical: ColorProvider,
  val track: ColorProvider,
)

/** The look chosen under Settings → Design → Widgets (by default the app's design and theme). */
fun widgetLook(settings: AtaSettings): WidgetLook {
  val neon = when (settings.widgetDesign) {
    WidgetDesign.APP -> settings.design == UiDesign.NEON
    WidgetDesign.NEON -> true
    WidgetDesign.MATERIAL -> false
  }
  fun c(day: Long, night: Long, see: Boolean = false): ColorProvider {
    val a = if (see && settings.widgetTranslucent) .72f else 1f
    val d = Color(day).let { it.copy(alpha = it.alpha * a) }
    val n = Color(night).let { it.copy(alpha = it.alpha * a) }
    return when (settings.theme) {
      ThemeMode.LIGHT -> ColorProvider(d)
      ThemeMode.DARK -> ColorProvider(n)
      ThemeMode.SYSTEM -> DayNight(day = d, night = n)
    }
  }
  return if (neon) {
    WidgetLook(
      neon = true,
      background = c(0xFFF4F6F9, 0xFF0B0D11, see = true),
      card = c(0xFFFFFFFF, 0xFF1A202C, see = true),
      edge = c(0xFFC8D1DC, 0x8C4FB4FF),
      text = c(0xFF161B22, 0xFFE6ECF5),
      muted = c(0xFF5B6B7C, 0xFF9FB2C6),
      accent = c(0xFF0B63BD, 0xFF4FB4FF),
      iconBackground = c(0x1A0B63BD, 0x294FB4FF),
      onIconBackground = c(0xFF0B63BD, 0xFF4FB4FF),
      charge = c(0xFF1E8E4E, 0xFF39FF88),
      warning = c(0xFFB7791F, 0xFFF1C40F),
      critical = c(0xFFC62828, 0xFFFF4D5E),
      track = c(0xFFE3E8EF, 0xFF2A3446),
    )
  } else {
    WidgetLook(
      neon = false,
      background = c(0xFFF7F9FF, 0xFF111318, see = true),
      card = c(0xFFE3E8F2, 0xFF262B33, see = true),
      edge = c(0xFFE3E8F2, 0xFF262B33, see = true),
      text = c(0xFF191C20, 0xFFE2E2E9),
      muted = c(0xFF5A6170, 0xFFB8BFCC),
      accent = c(0xFF3B5F99, 0xFFA8C8FF),
      iconBackground = c(0xFFD6E3FF, 0xFF284777),
      onIconBackground = c(0xFF001B3E, 0xFFD6E3FF),
      charge = c(0xFF2E7D4F, 0xFF7FD99A),
      warning = c(0xFF8C5D00, 0xFFF2BE5B),
      critical = c(0xFFBA1A1A, 0xFFFFB4AB),
      track = c(0xFFC9D2E0, 0xFF3A404B),
    )
  }
}

/** A card like the app's tiles: neon with a thin glowing edge, Material as a calm tonal surface. */
@Composable
fun WidgetCard(
  look: WidgetLook,
  modifier: GlanceModifier = GlanceModifier,
  radius: Dp = 16.dp,
  alignment: Alignment = Alignment.Center,
  content: @Composable () -> Unit,
) {
  if (look.neon) {
    Box(modifier.background(look.edge).cornerRadius(radius).padding(1.dp)) {
      Box(GlanceModifier.fillMaxSize().background(look.card).cornerRadius(radius), contentAlignment = alignment) { content() }
    }
  } else {
    Box(modifier.background(look.card).cornerRadius(radius), contentAlignment = alignment) { content() }
  }
}

/** A Material icon, tinted (widgets have no vector icons of their own). */
@Composable
fun WidgetIcon(icon: ImageVector, tint: ColorProvider, size: Dp, modifier: GlanceModifier = GlanceModifier) {
  Image(
    provider = ImageProvider(maskOf(icon)),
    contentDescription = null,
    modifier = modifier.size(size),
    colorFilter = ColorFilter.tint(tint),
  )
}

/** Icon in a round tonal badge (Material) or plain with the accent colour (neon), like the app's tiles. */
@Composable
fun WidgetBadgeIcon(look: WidgetLook, icon: ImageVector, size: Dp, tint: ColorProvider = look.onIconBackground) {
  Box(
    GlanceModifier.size(size).background(look.iconBackground).cornerRadius(size / 2),
    contentAlignment = Alignment.Center,
  ) {
    WidgetIcon(icon, tint, size * .58f)
  }
}

private val masks = HashMap<ImageVector, Bitmap>()

/** White mask of [icon] (tinted by the widget), cached. */
private fun maskOf(icon: ImageVector): Bitmap = synchronized(masks) {
  masks.getOrPut(icon) {
    val sizePx = 72
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.scale(sizePx / icon.viewportWidth, sizePx / icon.viewportHeight)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; style = Paint.Style.FILL }
    fun draw(group: VectorGroup) {
      for (node in group) {
        when (node) {
          is VectorGroup -> draw(node)
          is VectorPath -> {
            val path = node.pathData.toPath().also { it.fillType = node.pathFillType }.asAndroidPath()
            paint.alpha = (255 * node.fillAlpha).toInt()
            canvas.drawPath(path, paint)
          }
        }
      }
    }
    draw(icon.root)
    bitmap
  }
}
