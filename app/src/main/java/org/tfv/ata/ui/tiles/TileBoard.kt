package org.tfv.ata.ui.tiles

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.tfv.ata.action.ActionInfo
import org.tfv.ata.action.CommandDispatcher
import org.tfv.ata.core.AtaAction
import org.tfv.ata.ui.components.iconFor
import org.tfv.ata.ui.theme.AtaColors

/** Cell positions (column, row) of [sizes] (w × h) packed row by row into [columns]: each tile takes the first gap it fits. */
fun packTiles(sizes: List<Pair<Int, Int>>, columns: Int): Pair<List<IntOffset>, Int> {
  val rows = mutableListOf<BooleanArray>()
  fun free(r: Int, c: Int) = r >= rows.size || !rows[r][c]
  val out = sizes.map { (w0, h) ->
    val w = w0.coerceAtMost(columns)
    var r = 0
    var found: IntOffset? = null
    while (found == null) {
      for (c in 0..columns - w) {
        if ((r until r + h).all { rr -> (c until c + w).all { cc -> free(rr, cc) } }) {
          found = IntOffset(c, r)
          break
        }
      }
      if (found == null) r++
    }
    val pos = found!!
    while (rows.size < pos.y + h) rows += BooleanArray(columns)
    for (rr in pos.y until pos.y + h) for (cc in pos.x until pos.x + w) rows[rr][cc] = true
    pos
  }
  return out to rows.size
}

/** Tiles per row: the setting, or automatic (3 on phones, more on wide screens). */
fun columnsFor(setting: Int, width: Dp): Int = when {
  setting in 2..6 -> setting
  width < 600.dp -> 3
  else -> (width / 150.dp).toInt().coerceIn(4, 6)
}

private const val ADD_KEY = "__add__"

/**
 * The tile grid of the favourites and vehicle tabs. Single tiles run their action on tap; multi-tiles only
 * react to the buttons inside them ([multi] draws those). Long-press starts the edit mode; there, tiles are
 * moved by long-press + drag, removed with ✕ and starred as favourite with ☆.
 */
@Composable
fun TileBoard(
  tiles: List<TileSpec>,
  columnSetting: Int,
  editMode: Boolean,
  enabled: (TileSpec) -> Boolean,
  onRun: (AtaAction) -> Unit,
  onEnterEdit: () -> Unit,
  onMove: (Int, Int) -> Unit,
  onDelete: (TileSpec) -> Unit,
  onAdd: () -> Unit,
  /** Draws a multi-tile: (tile, enabled, interactive, compact). */
  multi: @Composable (MultiTile, Boolean, Boolean, Boolean) -> Unit,
  modifier: Modifier = Modifier,
  canDelete: Boolean = true,
  showAddTile: Boolean = false,
  haptics: Boolean = true,
  favorite: ((TileSpec) -> Boolean)? = null,
  onToggleFavorite: ((TileSpec) -> Unit)? = null,
) {
  val view = LocalView.current
  val density = LocalDensity.current
  fun longHaptic() { if (haptics) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
  fun tapHaptic() { if (haptics) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
  val wiggle = rememberInfiniteTransition(label = "wiggle")
  val angle by wiggle.animateFloat(-0.9f, 0.9f, infiniteRepeatable(tween(170), RepeatMode.Reverse), label = "angle")

  BoxWithConstraints(modifier.fillMaxWidth()) {
    val columns = columnsFor(columnSetting, maxWidth)
    val gap = if (columns <= 3) 10.dp else 8.dp
    val cellW = (maxWidth - gap * (columns - 1)) / columns
    val cellH = cellW.coerceIn(92.dp, 150.dp)
    val compact = cellW < 76.dp
    val withAdd = editMode && showAddTile
    val sizes = tiles.map { it.w.coerceAtMost(columns) to it.h } + if (withAdd) listOf(1 to 1) else emptyList()
    val (cells, rowCount) = packTiles(sizes, columns)
    val stepX = with(density) { (cellW + gap).roundToPx() }
    val stepY = with(density) { (cellH + gap).roundToPx() }
    fun pixelPos(cell: IntOffset) = IntOffset(cell.x * stepX, cell.y * stepY)

    val currentTiles by rememberUpdatedState(tiles)
    val currentColumns by rememberUpdatedState(columns)
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    // The order the drag works with: updated at once, before the parent recomposes with the new order.
    var dragOrder by remember { mutableStateOf<List<TileSpec>?>(null) }

    fun moveDragged(key: String) {
      val order = dragOrder ?: currentTiles
      val from = order.indexOfFirst { it.key == key }
      if (from < 0) return
      val cols = currentColumns
      val before = packTiles(order.map { it.w.coerceAtMost(cols) to it.h }, cols).first
      val origin = pixelPos(before[from])
      val w = order[from].w.coerceAtMost(cols)
      val h = order[from].h
      val cx = origin.x + dragOffset.x + (w * stepX) / 2f
      val cy = origin.y + dragOffset.y + (h * stepY) / 2f
      val to = before.indices.firstOrNull { i ->
        if (i == from) return@firstOrNull false
        val p = pixelPos(before[i])
        val tw = order[i].w.coerceAtMost(cols) * stepX
        val th = order[i].h * stepY
        cx >= p.x && cx < p.x + tw && cy >= p.y && cy < p.y + th
      } ?: return
      val next = order.toMutableList().apply { add(to, removeAt(from)) }
      val after = packTiles(next.map { it.w.coerceAtMost(cols) to it.h }, cols).first
      val moved = pixelPos(after[to])
      dragOffset += Offset((origin.x - moved.x).toFloat(), (origin.y - moved.y).toFloat())
      dragOrder = next
      onMove(from, to)
    }

    Box(Modifier.fillMaxWidth().height(cellH * rowCount + gap * (rowCount - 1).coerceAtLeast(0))) {
      tiles.forEachIndexed { index, spec ->
        key(spec.key) {
          val target = pixelPos(cells[index])
          val dragging = draggingKey == spec.key
          val animated by animateIntOffsetAsState(target, spring(stiffness = 500f), label = "tilePos")
          val base = if (dragging) target else animated
          val w = spec.w.coerceAtMost(columns)
          val tileEnabled = enabled(spec)
          var pressed by remember { mutableStateOf(false) }
          val pressScale by animateFloatAsState(if (pressed && !dragging) .93f else 1f, label = "press")
          Box(
            Modifier
              .offset { base }
              .size(cellW * w + gap * (w - 1), cellH * spec.h + gap * (spec.h - 1))
              .zIndex(if (dragging) 2f else 0f)
              .graphicsLayer {
                if (dragging) {
                  translationX = dragOffset.x
                  translationY = dragOffset.y
                  scaleX = 1.05f
                  scaleY = 1.05f
                  shadowElevation = 16f
                } else {
                  scaleX = pressScale
                  scaleY = pressScale
                }
                rotationZ = if (editMode && !dragging) angle / spec.w else 0f
              },
          ) {
            when (spec) {
              is TileSpec.Single -> SingleTile(
                spec.action,
                tileEnabled,
                compact,
                Modifier.fillMaxSize().then(
                  if (!editMode) {
                    Modifier.pointerInput(spec.key, tileEnabled) {
                      detectTapGestures(
                        onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                        onLongPress = { longHaptic(); onEnterEdit() },
                        onTap = { if (tileEnabled) { tapHaptic(); onRun(spec.action) } },
                      )
                    }
                  } else {
                    Modifier
                  },
                ),
              )
              is TileSpec.Multi -> Box(
                Modifier.fillMaxSize().then(
                  if (!editMode) Modifier.pointerInput(spec.key) { detectTapGestures(onLongPress = { longHaptic(); onEnterEdit() }) } else Modifier,
                ),
              ) {
                multi(spec.tile, tileEnabled, !editMode, compact)
              }
            }
            if (editMode) {
              // Catches every touch in edit mode, so buttons inside multi-tiles don't fire while arranging.
              Box(
                Modifier.fillMaxSize().pointerInput(spec.key) {
                  detectDragGesturesAfterLongPress(
                    onDragStart = { longHaptic(); draggingKey = spec.key; dragOffset = Offset.Zero; dragOrder = currentTiles },
                    onDragEnd = { draggingKey = null; dragOffset = Offset.Zero; dragOrder = null },
                    onDragCancel = { draggingKey = null; dragOffset = Offset.Zero; dragOrder = null },
                    onDrag = { change, delta ->
                      change.consume()
                      dragOffset += delta
                      moveDragged(spec.key)
                    },
                  )
                },
              )
              if (canDelete) {
                Surface(
                  onClick = { onDelete(spec) },
                  shape = CircleShape,
                  color = AtaColors.current.critical,
                  modifier = Modifier.align(Alignment.TopStart).padding(2.dp).size(24.dp),
                ) {
                  Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.padding(4.dp))
                }
              }
              if (onToggleFavorite != null) {
                val fav = favorite?.invoke(spec) == true
                val palette = AtaColors.current
                Surface(
                  onClick = { onToggleFavorite(spec) },
                  shape = CircleShape,
                  color = palette.badgeBackground,
                  shadowElevation = if (palette.isDark) 0.dp else 1.dp,
                  modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(26.dp),
                ) {
                  Icon(
                    if (fav) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = null,
                    tint = if (fav) palette.accent else palette.badgeIcon,
                    modifier = Modifier.padding(4.dp),
                  )
                }
              }
            }
          }
        }
      }
      if (withAdd) {
        val pos = pixelPos(cells.last())
        AddTile(onAdd, Modifier.offset { pos }.size(cellW, cellH))
      }
    }
  }
}

/** One action: icon and name (always shown, also with six tiles per row). */
@Composable
fun SingleTile(action: AtaAction, enabled: Boolean, compact: Boolean, modifier: Modifier = Modifier) {
  val palette = AtaColors.current
  val critical = action in CommandDispatcher.criticalActions
  val accent = if (critical) palette.critical else palette.accent
  TileFrame(modifier, enabled = enabled, critical = critical) {
    Column(
      Modifier.fillMaxSize().padding(horizontal = if (compact) 3.dp else 8.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      if (palette.material) {
        val scheme = MaterialTheme.colorScheme
        Box(
          Modifier.size(if (compact) 32.dp else 46.dp).clip(CircleShape)
            .background(if (critical) scheme.errorContainer else scheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            iconFor(action), contentDescription = null,
            tint = if (critical) scheme.onErrorContainer else scheme.onPrimaryContainer,
            modifier = Modifier.size(if (compact) 18.dp else 24.dp),
          )
        }
      } else {
        Box(contentAlignment = Alignment.Center) {
          Box(Modifier.size(if (compact) 32.dp else 46.dp).background(Brush.radialGradient(listOf(accent.copy(alpha = if (enabled) .22f else .06f), Color.Transparent))))
          Icon(iconFor(action), contentDescription = null, tint = accent, modifier = Modifier.size(if (compact) 22.dp else 30.dp))
        }
      }
      Text(
        stringResource(ActionInfo.labelRes(action)),
        style = MaterialTheme.typography.labelMedium,
        fontSize = if (compact) 9.sp else 12.sp,
        lineHeight = if (compact) 10.sp else 14.sp,
        color = if (palette.material) MaterialTheme.colorScheme.onSurface else palette.tileText,
        textAlign = TextAlign.Center,
        maxLines = if (compact) 3 else 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(top = if (compact) 3.dp else 6.dp),
      )
    }
  }
}

@Composable
private fun AddTile(onClick: () -> Unit, modifier: Modifier) {
  val pulse = rememberInfiniteTransition(label = "add")
  val scale by pulse.animateFloat(.94f, 1.04f, infiniteRepeatable(tween(750), RepeatMode.Reverse), label = "scale")
  val shape = RoundedCornerShape(20.dp)
  val palette = AtaColors.current
  Box(
    modifier
      .graphicsLayer { scaleX = scale; scaleY = scale }
      .clip(shape)
      .background(Brush.verticalGradient(listOf(palette.accent.copy(alpha = .10f), palette.tileBottom)))
      .border(1.dp, palette.accent.copy(alpha = .6f), shape)
      .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
    contentAlignment = Alignment.Center,
  ) {
    Icon(Icons.Filled.Add, contentDescription = null, tint = palette.accent, modifier = Modifier.size(36.dp))
  }
}
