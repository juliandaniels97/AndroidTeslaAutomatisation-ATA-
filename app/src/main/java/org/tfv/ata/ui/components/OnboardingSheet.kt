package org.tfv.ata.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.tfv.ata.R

@Composable
fun OnboardingSheet(onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Image(
        painter = painterResource(R.drawable.ata_logo),
        contentDescription = null,
        modifier = Modifier.size(72.dp),
      )
    },
    title = {
      Text(
        stringResource(R.string.onboarding_title),
        style = MaterialTheme.typography.headlineSmall,
      )
    },
    text = {
      Column(
        Modifier
          .fillMaxWidth()
          .heightIn(max = 420.dp)
          .verticalScroll(rememberScrollState()),
      ) {
        Text(stringResource(R.string.onboarding_body), style = MaterialTheme.typography.bodyMedium)
      }
    },
    confirmButton = {
      Button(onClick = onDismiss, modifier = Modifier.padding(top = 4.dp)) {
        Text(stringResource(R.string.onboarding_cta))
      }
    },
  )
}
