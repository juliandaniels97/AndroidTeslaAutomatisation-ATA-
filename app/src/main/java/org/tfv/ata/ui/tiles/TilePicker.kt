package org.tfv.ata.ui.tiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.tfv.ata.R
import org.tfv.ata.action.ActionInfo
import org.tfv.ata.ui.components.iconFor

fun TileSpec.labelRes(): Int = when (this) {
  is TileSpec.Single -> ActionInfo.labelRes(action)
  is TileSpec.Multi -> tile.labelRes
}

/** Picks a tile to add (single actions and multi-tiles). */
@Composable
fun TilePickerDialog(title: String, tiles: List<TileSpec>, onPick: (TileSpec) -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      LazyColumn(Modifier.heightIn(max = 460.dp)) {
        items(tiles, key = { it.key }) { spec ->
          Row(
            Modifier.fillMaxWidth().clickable { onPick(spec) }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              when (spec) {
                is TileSpec.Single -> iconFor(spec.action)
                is TileSpec.Multi -> spec.tile.icon
              },
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp),
            )
            Text(stringResource(spec.labelRes()), modifier = Modifier.padding(start = 14.dp).weight(1f))
            if (spec.w > 1 || spec.h > 1) {
              Text("${spec.w}×${spec.h}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
  )
}
