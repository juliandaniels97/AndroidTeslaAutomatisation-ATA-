package org.tfv.ata.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.tfv.ata.R
import org.tfv.ata.ui.components.PayPalIcon
import org.tfv.ata.ui.theme.AtaColors

const val DONATE_URL = "https://www.paypal.com/donate/?hosted_button_id=YHEH2ZKAV4E9Y"
private val PAYPAL_BLUE = Color(0xFF0070BA)

/** Thanks, and a PayPal button for those who want to support ATA. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScreen(onBack: () -> Unit) {
  val context = LocalContext.current
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.donate_title)) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
      )
    },
  ) { padding ->
    Column(
      Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Icon(Icons.Filled.Favorite, contentDescription = null, tint = AtaColors.current.critical, modifier = Modifier.size(64.dp).padding(top = 8.dp))
      Text(stringResource(R.string.donate_headline), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
      Text(stringResource(R.string.donate_text), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
      Button(
        onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DONATE_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
        colors = ButtonDefaults.buttonColors(containerColor = PAYPAL_BLUE, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth().height(56.dp),
      ) {
        Icon(PayPalIcon, contentDescription = null, modifier = Modifier.size(24.dp))
        Text(stringResource(R.string.donate_button), fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
      }
      Text(
        stringResource(R.string.donate_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}
