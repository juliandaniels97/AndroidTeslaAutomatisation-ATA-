package org.tfv.ata.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.tfv.ata.R
import org.tfv.ata.geo.CarFinder
import org.tfv.ata.geo.Geo
import org.tfv.ata.geo.rememberDeviceHeading
import androidx.glance.appwidget.updateAll
import org.tfv.ata.data.AtaStore
import org.tfv.ata.widget.CarFinderWidget
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.theme.AtaColors

/** Live compass to the parked car: arrow, distance and direction from the phone's GPS and heading. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarFinderScreen(state: AtaUiState, onBack: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var permissionTick by remember { mutableIntStateOf(0) }
  val hasPermission = remember(permissionTick) { Geo.hasLocationPermission(context) }
  val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
    permissionTick++
  }
  val me = rememberLiveLocation(context, hasPermission)
  val heading by rememberDeviceHeading(enabled = true)
  val car = CarFinder.carLocation(state.settings)
  val palette = AtaColors.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.finder_title)) },
        navigationIcon = {
          IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        },
      )
    },
  ) { padding ->
    Column(
      Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      when {
        !hasPermission -> {
          Text(stringResource(R.string.finder_need_location), textAlign = TextAlign.Center)
          Button(onClick = {
            permissionLauncher.launch(
              arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
            )
          }) { Text(stringResource(R.string.pair_action_allow)) }
        }
        car == null -> Text(stringResource(R.string.finder_no_car_location), textAlign = TextAlign.Center)
        me == null -> Text(stringResource(R.string.finder_waiting_gps), textAlign = TextAlign.Center)
        else -> {
          val distance = me.distanceTo(car)
          val bearing = me.bearingTo(car)
          val rotation by animateFloatAsState(targetValue = normalize(bearing - heading), label = "arrow")
          if (distance < 15f) {
            Text(stringResource(R.string.finder_here), style = MaterialTheme.typography.headlineSmall, color = palette.success)
          } else {
            Icon(
              Icons.Filled.Navigation,
              contentDescription = null,
              tint = palette.accent,
              modifier = Modifier.size(220.dp).rotate(rotation),
            )
          }
          Text(CarFinder.distanceText(distance), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
          Text(CarFinder.cardinal(context, bearing), style = MaterialTheme.typography.titleMedium)
          Text(
            stringResource(R.string.finder_gps_accuracy, me.accuracy.toInt()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (car != null) {
        Text(
          stringResource(R.string.finder_saved, CarFinder.ageText(context, state.settings.carLocAt)),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = {
          val uri = Uri.parse("geo:${car.latitude},${car.longitude}?q=${car.latitude},${car.longitude}(Tesla)")
          runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        }) {
          Icon(Icons.Filled.Map, contentDescription = null)
          Text(stringResource(R.string.finder_open_map), modifier = Modifier.padding(start = 8.dp))
        }
      }
      if (me != null) {
        OutlinedButton(onClick = {
          val here = me
          scope.launch {
            AtaStore.get(context).updateSettings {
              it.copy(carLat = here.latitude, carLng = here.longitude, carLocAt = System.currentTimeMillis())
            }
            runCatching { CarFinderWidget().updateAll(context) }
          }
        }) {
          Icon(Icons.Filled.MyLocation, contentDescription = null)
          Text(stringResource(R.string.finder_parked_here), modifier = Modifier.padding(start = 8.dp))
        }
      }
      Text(
        stringResource(R.string.finder_how),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

private fun normalize(degrees: Float): Float = ((degrees % 360) + 360) % 360

/** The phone's position, updated every second while the screen is shown. */
@SuppressLint("MissingPermission") // checked by the caller (hasPermission)
@Composable
private fun rememberLiveLocation(context: Context, hasPermission: Boolean): Location? {
  var location by remember { mutableStateOf(if (hasPermission) Geo.lastLocation(context) else null) }
  DisposableEffect(hasPermission) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    val listener = LocationListener { location = it }
    if (hasPermission && lm != null) {
      location = Geo.lastLocation(context)
      listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        .forEach { provider ->
          runCatching { lm.requestLocationUpdates(provider, 1_000L, 1f, listener, Looper.getMainLooper()) }
        }
    }
    onDispose { runCatching { lm?.removeUpdates(listener) } }
  }
  return location
}
