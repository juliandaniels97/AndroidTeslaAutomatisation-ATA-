package org.tfv.ata.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import android.view.HapticFeedbackConstants
import org.tfv.ata.core.AtaAction
import org.tfv.ata.ui.theme.NeonBlue

private const val ADD_KEY = "__add__"

@Composable
fun EditableActionGrid(
  actions: List<AtaAction>,
  editMode: Boolean,
  showAddTile: Boolean,
  canDelete: Boolean,
  onRun: (AtaAction) -> Unit,
  onEnterEdit: () -> Unit,
  onDelete: (AtaAction) -> Unit,
  onMove: (Int, Int) -> Unit,
  onAdd: () -> Unit,
  modifier: Modifier = Modifier,
  tilesEnabled: Boolean = true,
  haptics: Boolean = false,
  favorite: ((AtaAction) -> Boolean)? = null,
  onToggleFavorite: ((AtaAction) -> Unit)? = null,
  header: (@Composable () -> Unit)? = null,
) {
  val view = LocalView.current
  fun tapHaptic() { if (haptics) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
  fun longHaptic() { if (haptics) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
  val gridState = rememberLazyGridState()
  var draggingKey by remember { mutableStateOf<String?>(null) }
  var pressedKey by remember { mutableStateOf<String?>(null) }
  var dragOffset by remember { mutableStateOf(Offset.Zero) }
  val wiggle = rememberInfiniteTransition(label = "wiggle")
  val angle by wiggle.animateFloat(
    initialValue = -1.4f,
    targetValue = 1.4f,
    animationSpec = infiniteRepeatable(tween(160), RepeatMode.Reverse),
    label = "angle",
  )

  LazyVerticalGrid(
    state = gridState,
    columns = GridCells.Adaptive(110.dp),
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    contentPadding = PaddingValues(12.dp),
  ) {
    if (header != null) {
      item(span = { GridItemSpan(maxLineSpan) }, key = "__header__") { header() }
    }
    items(actions, key = { it.id }) { action ->
      val dragging = draggingKey == action.id
      val pressed = pressedKey == action.id
      val pressScale by animateFloatAsState(if (pressed && !dragging) 0.9f else 1f, label = "press")
      Box(
        modifier = Modifier
          .zIndex(if (dragging) 1f else 0f)
          .graphicsLayer {
            if (dragging) {
              translationX = dragOffset.x
              translationY = dragOffset.y
              scaleX = 1.08f
              scaleY = 1.08f
            } else {
              scaleX = pressScale
              scaleY = pressScale
            }
            rotationZ = if (editMode && !dragging) angle else 0f
          }
          .then(
            if (editMode) {
              Modifier.pointerInput(action.id, actions.size) {
                detectDragGesturesAfterLongPress(
                  onDragStart = { longHaptic(); draggingKey = action.id; dragOffset = Offset.Zero },
                  onDragEnd = { draggingKey = null; dragOffset = Offset.Zero },
                  onDragCancel = { draggingKey = null; dragOffset = Offset.Zero },
                  onDrag = { change, delta ->
                    change.consume()
                    dragOffset += delta
                    val key = draggingKey
                    val info = gridState.layoutInfo.visibleItemsInfo
                    val fromInfo = key?.let { k -> info.firstOrNull { it.key == k } }
                    val from = actions.indexOfFirst { it.id == key }
                    if (key != null && fromInfo != null && from >= 0) {
                      val cx = fromInfo.offset.x + fromInfo.size.width / 2f + dragOffset.x
                      val cy = fromInfo.offset.y + fromInfo.size.height / 2f + dragOffset.y
                      val target = info.firstOrNull { t ->
                        t.key != key && t.key != ADD_KEY &&
                          cx >= t.offset.x && cx <= t.offset.x + t.size.width &&
                          cy >= t.offset.y && cy <= t.offset.y + t.size.height
                      }
                      if (target != null) {
                        val to = actions.indexOfFirst { it.id == target.key }
                        if (to >= 0) {
                          onMove(from, to)
                          dragOffset = Offset(
                            dragOffset.x + (fromInfo.offset.x - target.offset.x),
                            dragOffset.y + (fromInfo.offset.y - target.offset.y),
                          )
                        }
                      }
                    }
                  },
                )
              }
            } else {
              Modifier.pointerInput(action.id) {
                detectTapGestures(
                  onPress = {
                    pressedKey = action.id
                    tryAwaitRelease()
                    pressedKey = null
                  },
                  onLongPress = { longHaptic(); onEnterEdit() },
                  onTap = { tapHaptic(); onRun(action) },
                )
              }
            },
          ),
      ) {
        ActionTile(
          action = action,
          onClick = {},
          interactive = false,
          enabled = tilesEnabled,
        )
        if (!editMode && onToggleFavorite != null) {
          val fav = favorite?.invoke(action) == true
          Surface(
            onClick = { onToggleFavorite(action) },
            shape = CircleShape,
            color = Color(0xAA0B0F16),
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(26.dp),
          ) {
            Icon(
              if (fav) Icons.Filled.Star else Icons.Filled.StarBorder,
              contentDescription = null,
              tint = if (fav) NeonBlue else Color(0xFF9FB2C6),
              modifier = Modifier.padding(4.dp),
            )
          }
        }
        if (editMode && canDelete) {
          Surface(
            onClick = { onDelete(action) },
            shape = CircleShape,
            color = Color(0xFFFF4D40),
            modifier = Modifier.align(Alignment.TopStart).padding(2.dp).size(24.dp),
          ) {
            Icon(
              Icons.Filled.Close,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.padding(4.dp),
            )
          }
        }
      }
    }
    if (editMode && showAddTile) {
      item(key = ADD_KEY) { AddTile(onClick = onAdd) }
    }
  }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
  val pulse = rememberInfiniteTransition(label = "add")
  val scale by pulse.animateFloat(
    initialValue = 0.94f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
    label = "scale",
  )
  val shape = RoundedCornerShape(20.dp)
  Box(
    modifier = Modifier
      .aspectRatio(1f)
      .graphicsLayer { scaleX = scale; scaleY = scale }
      .clip(shape)
      .background(Brush.verticalGradient(listOf(Color(0xFF122133), Color(0xFF0B0F16))))
      .border(1.dp, NeonBlue.copy(alpha = 0.6f), shape)
      .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      Icons.Filled.Add,
      contentDescription = null,
      tint = NeonBlue,
      modifier = Modifier.size(40.dp),
    )
  }
}
