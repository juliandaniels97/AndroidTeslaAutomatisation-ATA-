package org.tfv.ata.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import org.tfv.ata.action.ActionInfo
import org.tfv.ata.action.CommandDispatcher
import org.tfv.ata.core.AtaAction
import org.tfv.ata.ui.theme.AtaColors

@Composable
fun ActionTile(
  action: AtaAction,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = action.enabled,
  interactive: Boolean = true,
) {
  val shape = RoundedCornerShape(20.dp)
  val palette = AtaColors.current
  val accent = if (action in CommandDispatcher.criticalActions) palette.critical else palette.accent
  val alpha = if (enabled) 1f else 0.45f
  val tile = modifier
    .aspectRatio(1f)
    .clip(shape)
    .background(
      Brush.verticalGradient(
        listOf(palette.tileTop.copy(alpha = alpha), palette.tileBottom.copy(alpha = alpha)),
      ),
    )
    .border(
      width = 1.dp,
      brush = Brush.verticalGradient(
        listOf(accent.copy(alpha = (if (palette.isDark) 0.55f else 0.45f) * alpha), palette.tileEdge.copy(alpha = if (palette.isDark) 0.25f else 0.9f)),
      ),
      shape = shape,
    )
    .then(if (interactive && enabled) Modifier.clickable(onClick = onClick) else Modifier)
  Box(tile) {
    ActionTileContent(action, accent, enabled)
  }
}

@Composable
private fun ActionTileContent(action: AtaAction, accent: Color, enabled: Boolean) {
  Box(Modifier.fillMaxSize().padding(10.dp)) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Box(
          Modifier
            .size(46.dp)
            .background(Brush.radialGradient(listOf(accent.copy(alpha = if (enabled) 0.22f else 0.06f), Color.Transparent))),
        )
        Icon(
          imageVector = iconFor(action),
          contentDescription = null,
          modifier = Modifier.size(32.dp),
          tint = if (enabled) accent else accent.copy(alpha = 0.4f),
        )
      }
      Text(
        text = stringResource(ActionInfo.labelRes(action)),
        style = MaterialTheme.typography.labelMedium,
        color = AtaColors.current.tileText.copy(alpha = if (enabled) 1f else 0.55f),
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
      )
    }
  }
}
