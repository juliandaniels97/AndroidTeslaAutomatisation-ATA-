package org.tfv.ata.ui.tiles

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import org.tfv.ata.ui.theme.AtaColors

/** Background of every tile: glowing gradient + edge (Neon) or a calm tonal surface (Material). */
@Composable
fun TileFrame(
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  critical: Boolean = false,
  active: Boolean = false,
  content: @Composable BoxScope.() -> Unit,
) {
  val palette = AtaColors.current
  val accent = if (critical) palette.critical else palette.accent
  val alpha = if (enabled) 1f else .5f
  if (palette.material) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (active) scheme.secondaryContainer else palette.tileTop, label = "tileBg")
    Box(modifier.clip(RoundedCornerShape(24.dp)).background(bg.copy(alpha = if (enabled) 1f else .5f)), content = content)
    return
  }
  val shape = RoundedCornerShape(20.dp)
  val glow by animateFloatAsState(if (active) 1f else 0f, label = "tileGlow")
  Box(
    modifier
      .clip(shape)
      .background(
        Brush.verticalGradient(
          listOf(
            lerp(palette.tileTop, accent, .10f * glow).copy(alpha = alpha),
            palette.tileBottom.copy(alpha = alpha),
          ),
        ),
      )
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
          listOf(
            accent.copy(alpha = ((if (palette.isDark) .55f else .45f) + .35f * glow).coerceAtMost(1f) * alpha),
            palette.tileEdge.copy(alpha = if (palette.isDark) .25f else .9f),
          ),
        ),
        shape = shape,
      ),
    content = content,
  )
}

private fun lerp(a: Color, b: Color, t: Float) = Color(
  a.red + (b.red - a.red) * t,
  a.green + (b.green - a.green) * t,
  a.blue + (b.blue - a.blue) * t,
  a.alpha,
)

/** Icon + name (+ status) at the top of a multi-tile. */
@Composable
fun TileHeader(icon: ImageVector, label: String, status: String? = null, statusColor: Color? = null, compact: Boolean = false) {
  val palette = AtaColors.current
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(if (compact) 14.dp else 18.dp))
    Text(
      label,
      style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      color = palette.tileText,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f).padding(start = 5.dp),
    )
    if (status != null) {
      Text(
        status,
        style = MaterialTheme.typography.labelSmall,
        color = statusColor ?: palette.muted,
        maxLines = 1,
        modifier = Modifier.padding(start = 4.dp),
      )
    }
  }
}

/** A button inside a multi-tile. [active] marks the car's current state (e.g. "locked"). */
@Composable
fun TileButton(
  icon: ImageVector,
  label: String?,
  active: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  critical: Boolean = false,
  haptics: Boolean = true,
) {
  val palette = AtaColors.current
  val view = LocalView.current
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val scale by animateFloatAsState(if (pressed) .92f else 1f, label = "btnPress")
  val accent = if (critical) palette.critical else palette.accent
  val shape = RoundedCornerShape(14.dp)
  val bg: Color
  val fg: Color
  if (palette.material) {
    val scheme = MaterialTheme.colorScheme
    bg = if (active) (if (critical) scheme.errorContainer else scheme.primaryContainer) else scheme.surfaceVariant.copy(alpha = .55f)
    fg = if (active) (if (critical) scheme.onErrorContainer else scheme.onPrimaryContainer) else scheme.onSurfaceVariant
  } else {
    bg = if (active) accent.copy(alpha = if (palette.isDark) .24f else .16f) else palette.tileEdge.copy(alpha = if (palette.isDark) .22f else .35f)
    fg = if (active) accent else palette.tileText.copy(alpha = .8f)
  }
  val animBg by animateColorAsState(bg, label = "btnBg")
  Box(
    modifier
      .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else .45f }
      .clip(shape)
      .background(animBg)
      .then(if (!palette.material && active) Modifier.border(1.dp, accent.copy(alpha = .8f), shape) else Modifier)
      .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button) {
        if (haptics) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        onClick()
      }
      .padding(horizontal = 4.dp, vertical = 4.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
      Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(20.dp))
      if (label != null) {
        Text(
          label,
          fontSize = 10.sp,
          lineHeight = 11.sp,
          color = fg,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 1.dp),
        )
      }
    }
  }
}

/** Three dots for level 1 … 3 (seat heater, ventilation); tapping a dot selects that level. */
@Composable
fun LevelDots(level: Int, enabled: Boolean, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, dot: Dp = 8.dp, haptics: Boolean = true) {
  val palette = AtaColors.current
  val view = LocalView.current
  val on = if (palette.material) MaterialTheme.colorScheme.primary else palette.accent
  val off = palette.muted.copy(alpha = .35f)
  Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
    for (i in 1..3) {
      val filled = level >= i
      val color by animateColorAsState(if (filled) on else off, label = "dot")
      Box(
        Modifier
          .size(dot + 10.dp)
          .clip(CircleShape)
          .clickable(enabled = enabled, role = Role.Button) {
            if (haptics) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onSelect(i)
          },
        contentAlignment = Alignment.Center,
      ) {
        Box(
          Modifier.size(dot).clip(CircleShape).background(color)
            .then(if (!palette.material && filled) Modifier.border(1.dp, on.copy(alpha = .5f), CircleShape) else Modifier),
        )
      }
    }
  }
}

/**
 * A button you slide up and down to change a value (volume, temperature, charge limit …). The value follows the
 * finger, but the command is only sent once the finger lets go ([onCommit]), so the car gets one command, not
 * dozens. A tap on the upper / lower half steps up / down by [step].
 */
@Composable
fun DragFader(
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  step: Float,
  format: (Float) -> String,
  onCommit: (Float) -> Unit,
  enabled: Boolean,
  modifier: Modifier = Modifier,
  label: String? = null,
  icon: ImageVector? = null,
  haptics: Boolean = true,
) {
  val palette = AtaColors.current
  val view = LocalView.current
  val density = LocalDensity.current
  // Shown value: follows the finger while dragging and keeps the sent value until the car reports a new one.
  var shown by remember { mutableFloatStateOf(value.coerceIn(range.start, range.endInclusive)) }
  var dragging by remember { mutableStateOf(false) }
  val latestCommit by rememberUpdatedState(onCommit)
  val latestValue by rememberUpdatedState(value)
  LaunchedEffect(value) { if (!dragging) shown = value.coerceIn(range.start, range.endInclusive) }
  val fullTravelPx = with(density) { 180.dp.toPx() }
  val span = range.endInclusive - range.start
  val fraction = if (span > 0f) ((shown - range.start) / span).coerceIn(0f, 1f) else 0f
  val animFraction by animateFloatAsState(fraction, label = "fader")
  val fill = if (palette.material) MaterialTheme.colorScheme.primary else palette.accent
  val track = if (palette.material) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f) else palette.tileEdge.copy(alpha = if (palette.isDark) .25f else .4f)
  val text = if (palette.material) MaterialTheme.colorScheme.onSurface else palette.tileText
  val shape = RoundedCornerShape(14.dp)
  fun quantize(v: Float) = (((v - range.start) / step).roundToInt() * step + range.start).coerceIn(range.start, range.endInclusive)

  Box(
    modifier
      .graphicsLayer { alpha = if (enabled) 1f else .45f; scaleX = if (dragging) 1.03f else 1f; scaleY = scaleX }
      .clip(shape)
      .background(track)
      .drawBehind {
        val h = size.height * animFraction
        drawRoundRect(
          brush = Brush.verticalGradient(listOf(fill.copy(alpha = .55f), fill.copy(alpha = .9f)), startY = size.height - h, endY = size.height),
          topLeft = Offset(0f, size.height - h),
          size = Size(size.width, h),
          cornerRadius = CornerRadius(14.dp.toPx()),
        )
      }
      .then(if (!palette.material) Modifier.border(1.dp, fill.copy(alpha = if (dragging) .9f else .4f), shape) else Modifier)
      .pointerInput(enabled, range, step) {
        if (!enabled) return@pointerInput
        var raw = 0f
        detectVerticalDragGestures(
          onDragStart = { raw = shown; dragging = true },
          onDragEnd = {
            dragging = false
            if (abs(shown - latestValue) > step / 2) latestCommit(shown)
          },
          onDragCancel = { dragging = false; shown = latestValue },
          onVerticalDrag = { change, dy ->
            change.consume()
            raw = (raw - dy / fullTravelPx * span).coerceIn(range.start, range.endInclusive)
            val q = quantize(raw)
            if (q != shown) {
              shown = q
              if (haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
          },
        )
      }
      .pointerInput(enabled, range, step) {
        if (!enabled) return@pointerInput
        detectTapGestures { pos ->
          val next = quantize(shown + if (pos.y < size.height / 2) step else -step)
          if (next != shown) {
            shown = next
            if (haptics) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            latestCommit(next)
          }
        }
      },
    contentAlignment = Alignment.Center,
  ) {
    Column(Modifier.fillMaxHeight().padding(vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
      Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = text.copy(alpha = .5f), modifier = Modifier.size(14.dp))
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) Icon(icon, contentDescription = null, tint = text, modifier = Modifier.size(16.dp))
        Text(format(shown), color = text, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
        if (label != null) Text(label, color = text.copy(alpha = .75f), fontSize = 9.sp, lineHeight = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = text.copy(alpha = .5f), modifier = Modifier.size(14.dp))
    }
  }
}

/** Small "status" pill, e.g. "48 kW". */
@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
  Box(
    modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = .16f)).padding(horizontal = 6.dp, vertical = 1.dp),
  ) {
    Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
  }
}

/** Fills the remaining space of a tile below its header. */
@Composable
fun TileBody(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
  Box(modifier.fillMaxSize(), content = content)
}
