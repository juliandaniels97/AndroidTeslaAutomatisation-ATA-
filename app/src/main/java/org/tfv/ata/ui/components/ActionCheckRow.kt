package org.tfv.ata.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.tfv.ata.action.ActionInfo
import org.tfv.ata.core.ActionPolicy
import org.tfv.ata.core.AtaAction
import org.tfv.ata.ui.theme.AtaColors

/** Selectable action row (icon + label + checkbox) used by the menu, widget, watch and automation settings. */
@Composable
fun ActionCheckRow(action: AtaAction, checked: Boolean, enabled: Boolean = true, onToggle: () -> Unit) {
  val palette = AtaColors.current
  val tint = if (ActionPolicy.isCritical(action)) palette.critical else palette.accent
  Row(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
      .padding(horizontal = 4.dp, vertical = 4.dp)
      .alpha(if (enabled || checked) 1f else 0.45f),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.14f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(iconFor(action), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
    Text(
      stringResource(ActionInfo.labelRes(action)),
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.weight(1f).padding(start = 12.dp),
    )
    Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
  }
}
