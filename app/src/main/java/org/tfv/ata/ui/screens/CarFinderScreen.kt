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
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.tfv.ata.R
import org.tfv.ata.geo.CarFinder
import org.tfv.ata.geo.Geo
import org.tfv.ata.geo.rememberCompass
import org.tfv.ata.geo.shortestTurn
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
  val compass by rememberCompass(enabled = true, location = me)
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
          if (distance < 15f) {
            Text(stringResource(R.string.finder_here), style = MaterialTheme.typography.headlineSmall, color = palette.success)
          } else {
            CompassDial(heading = compass.degrees, bearing = bearing, available = compass.available)
          }
          Text(CarFinder.distanceText(distance), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
          Text(
            stringResource(R.string.finder_direction, CarFinder.cardinal(context, bearing), ((distance / 1.3f) / 60f).toInt().coerceAtLeast(1)),
            style = MaterialTheme.typography.titleMedium,
          )
          if (!compass.available) {
            Text(stringResource(R.string.finder_no_compass), style = MaterialTheme.typography.bodySmall, color = palette.warning, textAlign = TextAlign.Center)
          } else if (!compass.reliable) {
            Text(stringResource(R.string.finder_calibrate), style = MaterialTheme.typography.bodySmall, color = palette.warning, textAlign = TextAlign.Center)
          }
          Text(
            if (me.accuracy > distance) stringResource(R.string.finder_gps_rough, me.accuracy.toInt())
            else stringResource(R.string.finder_gps_accuracy, me.accuracy.toInt()),
            style = MaterialTheme.typography.labelSmall,
            color = if (me.accuracy > distance) palette.warning else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
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

/**
 * Compass rose that turns with the phone (N points north), the car marked on the rim and a big arrow pointing
 * at it. Turns the shortest way round; a short vibration when the phone faces the car.
 */
@Composable
private fun CompassDial(heading: Float, bearing: Float, available: Boolean) {
  val palette = AtaColors.current
  val haptics = LocalHapticFeedback.current
  val scheme = MaterialTheme.colorScheme
  val textMeasurer = rememberTextMeasurer()
  // Continuous angles (no jump at 0°/360°), so the animation always takes the short way.
  val angles = remember { floatArrayOf(-heading, bearing - heading) }
  angles[0] += shortestTurn(angles[0], -heading)
  angles[1] += shortestTurn(angles[1], bearing - heading)
  val dialAnim by animateFloatAsState(angles[0], label = "dial")
  val arrowAnim by animateFloatAsState(angles[1], label = "arrow")
  val onTarget = available && kotlin.math.abs(shortestTurn(0f, bearing - heading)) < 8f
  LaunchedEffect(onTarget) { if (onTarget) haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
  val cardinals = listOf("N", "O", "S", "W").let { if (java.util.Locale.getDefault().language == "de") it else listOf("N", "E", "S", "W") }

  Canvas(Modifier.size(280.dp)) {
    val c = center
    val r = size.minDimension / 2f - 8f
    drawCircle(scheme.surfaceVariant.copy(alpha = .5f), r, c)
    drawCircle(scheme.outlineVariant, r, c, style = Stroke(2f))
    rotate(dialAnim, c) {
      for (i in 0 until 72) {
        val major = i % 9 == 0
        val len = if (major) 18f else if (i % 3 == 0) 11f else 6f
        val a = Math.toRadians(i * 5.0)
        val from = Offset(c.x + (r - len) * kotlin.math.sin(a).toFloat(), c.y - (r - len) * kotlin.math.cos(a).toFloat())
        val to = Offset(c.x + r * kotlin.math.sin(a).toFloat(), c.y - r * kotlin.math.cos(a).toFloat())
        drawLine(if (i == 0) palette.critical else scheme.onSurfaceVariant.copy(alpha = if (major) .9f else .45f), from, to, if (major) 4f else 2f, StrokeCap.Round)
      }
      cardinals.forEachIndexed { k, label ->
        val a = Math.toRadians(k * 90.0)
        val layout = textMeasurer.measure(label, TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (k == 0) palette.critical else scheme.onSurface))
        val pos = Offset(c.x + (r - 40f) * kotlin.math.sin(a).toFloat(), c.y - (r - 40f) * kotlin.math.cos(a).toFloat())
        rotate(-dialAnim, pos) {
          drawText(layout, topLeft = Offset(pos.x - layout.size.width / 2f, pos.y - layout.size.height / 2f))
        }
      }
    }
    // Car on the rim and the arrow towards it.
    rotate(arrowAnim, c) {
      drawCircle(if (onTarget) palette.success else palette.accent, 14f, Offset(c.x, c.y - r + 2f))
      val tip = Offset(c.x, c.y - r * .62f)
      val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(c.x + r * .2f, c.y + r * .22f)
        lineTo(c.x, c.y + r * .1f)
        lineTo(c.x - r * .2f, c.y + r * .22f)
        close()
      }
      drawPath(path, if (onTarget) palette.success else palette.accent)
    }
  }
}

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
