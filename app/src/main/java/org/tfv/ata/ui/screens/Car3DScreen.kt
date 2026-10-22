package org.tfv.ata.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.tfv.ata.R
import org.tfv.ata.action.ActionInfo
import org.tfv.ata.core.ActionPolicy
import org.tfv.ata.core.AtaAction
import org.tfv.ata.ui.ActionHost
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.AtaViewModel
import org.tfv.ata.ui.components.car.CarCamera
import org.tfv.ata.ui.components.car.CarHotspot
import org.tfv.ata.ui.components.car.CarStatus
import org.tfv.ata.ui.components.car.NeonStyle
import org.tfv.ata.ui.components.car.drawBatteryRing
import org.tfv.ata.ui.components.car.drawNeonCar
import org.tfv.ata.ui.components.car.rememberFrameClock
import org.tfv.ata.ui.components.iconFor
import kotlin.math.roundToInt

private val SHOWROOM = Color(0xFF04060B)
private val NEON = Color(0xFF4FB4FF)

/** What each tappable spot on the car offers. */
private fun actionsFor(spot: CarHotspot?): List<AtaAction> = when (spot) {
  CarHotspot.DRIVER_DOOR -> listOf(AtaAction.DOOR_DRIVER_OPEN, AtaAction.UNLOCK, AtaAction.LOCK, AtaAction.REMOTE_DRIVE)
  CarHotspot.PASSENGER_DOOR -> listOf(AtaAction.DOOR_PASSENGER_OPEN, AtaAction.UNLOCK, AtaAction.LOCK)
  CarHotspot.FRUNK -> listOf(AtaAction.FRUNK)
  CarHotspot.TRUNK -> listOf(AtaAction.TRUNK, AtaAction.TRUNK_CLOSE)
  CarHotspot.CHARGE_PORT -> listOf(
    AtaAction.CHARGE_PORT_OPEN, AtaAction.CHARGE_PORT_CLOSE, AtaAction.CHARGE_START, AtaAction.CHARGE_STOP,
    AtaAction.CHARGE_LIMIT, AtaAction.CHARGING_AMPS,
  )
  CarHotspot.WINDOWS -> listOf(AtaAction.VENT_WINDOWS, AtaAction.CLOSE_WINDOWS)
  CarHotspot.LIGHTS -> listOf(AtaAction.FLASH_LIGHTS, AtaAction.HONK, AtaAction.BOOMBOX_FART, AtaAction.BOOMBOX_LOCATE)
  CarHotspot.CLIMATE -> listOf(
    AtaAction.CLIMATE_ON, AtaAction.CLIMATE_OFF, AtaAction.SET_TEMPERATURE, AtaAction.DEFROST, AtaAction.DEFROST_OFF,
    AtaAction.STEERING_WHEEL_HEATER, AtaAction.STEERING_WHEEL_HEATER_OFF, AtaAction.CLIMATE_KEEP, AtaAction.DOG_MODE,
    AtaAction.CAMP_MODE, AtaAction.CLIMATE_KEEP_OFF, AtaAction.BIOWEAPON_ON, AtaAction.BIOWEAPON_OFF,
    AtaAction.OVERHEAT_PROTECTION_ON, AtaAction.OVERHEAT_PROTECTION_OFF,
  )
  null -> listOf(
    AtaAction.LOCK, AtaAction.UNLOCK, AtaAction.WAKE, AtaAction.SENTRY_ON, AtaAction.SENTRY_OFF,
    AtaAction.MEDIA_TOGGLE, AtaAction.MEDIA_PREVIOUS, AtaAction.MEDIA_NEXT, AtaAction.REMOTE_DRIVE,
  )
}.filter { it.enabled }

private fun spotIcon(spot: CarHotspot): ImageVector = when (spot) {
  CarHotspot.DRIVER_DOOR, CarHotspot.PASSENGER_DOOR -> Icons.Filled.DoorFront
  CarHotspot.FRUNK, CarHotspot.TRUNK -> Icons.Filled.Luggage
  CarHotspot.CHARGE_PORT -> Icons.Filled.EvStation
  CarHotspot.WINDOWS -> Icons.Filled.Window
  CarHotspot.LIGHTS -> Icons.Filled.FlashOn
  CarHotspot.CLIMATE -> Icons.Filled.Thermostat
}

private fun titleFor(spot: CarHotspot?): Int = when (spot) {
  CarHotspot.DRIVER_DOOR -> R.string.hs_driver_door
  CarHotspot.PASSENGER_DOOR -> R.string.hs_passenger_door
  CarHotspot.FRUNK -> R.string.hs_frunk
  CarHotspot.TRUNK -> R.string.hs_trunk
  CarHotspot.CHARGE_PORT -> R.string.hs_charge_port
  CarHotspot.WINDOWS -> R.string.hs_windows
  CarHotspot.LIGHTS -> R.string.hs_lights
  CarHotspot.CLIMATE -> R.string.hs_climate
  null -> R.string.hs_general
}

/**
 * Full-screen 3D car: turn it with a finger, tap the glowing spots on the car (doors, lids, charge port,
 * windows, lights, climate) or "General" to run actions right there. Effects play on the car after each action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Car3DScreen(vm: AtaViewModel, state: AtaUiState, onBack: () -> Unit) {
  val fx by vm.carFx.collectAsStateWithLifecycle()
  val now by rememberFrameClock()
  var yaw by remember { mutableFloatStateOf(-35f) }
  var pitch by remember { mutableFloatStateOf(18f) }
  var sheet by remember { mutableStateOf<Pair<Boolean, CarHotspot?>?>(null) }
  val snapshot = state.snapshot
  val known = snapshot != null && snapshot.updatedAt > 0
  val status = CarStatus(snapshot?.charging == true, snapshot?.climateOn == true, snapshot?.sentry == true)
  val style = NeonStyle(line = NEON, glow = true, lineWidth = 3f)
  val pulse by rememberInfiniteTransition(label = "spots").animateFloat(
    initialValue = .85f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "spotPulse",
  )

  ActionHost(vm, state) { run ->
    BoxWithConstraints(Modifier.fillMaxSize().background(SHOWROOM)) {
      val density = LocalDensity.current
      val w = constraints.maxWidth.toFloat()
      val h = constraints.maxHeight.toFloat()
      val camera = CarCamera(yaw, pitch, w, h, zoom = 1.35f)

      Canvas(
        Modifier.fillMaxSize()
          .pointerInput(Unit) {
            detectDragGestures { change, drag ->
              change.consume()
              yaw += drag.x * .35f
              pitch = (pitch - drag.y * .15f).coerceIn(4f, 55f)
            }
          },
      ) {
        drawNeonCar(camera, yaw, fx, status, now, style)
        if (known) {
          drawBatteryRing(Offset(size.width / 2f, size.height * .86f), size.minDimension * .07f, snapshot?.batteryPercent, status.charging, now, true, NEON, width = 8f)
        }
      }

      // Tappable spots, only on the side facing the viewer.
      CarHotspot.entries.forEach { spot ->
        val facing = camera.facing(spot.normal)
        if (facing > -.15f) {
          val p = camera.project(spot.anchor).offset
          val sizePx = with(density) { 44.dp.toPx() }
          Box(
            Modifier
              .offset { IntOffset((p.x - sizePx / 2).roundToInt(), (p.y - sizePx / 2).roundToInt()) }
              .size(44.dp)
              .alpha(((facing + .15f) / .5f).coerceIn(.25f, 1f))
              .scale(pulse)
              .clickable { sheet = true to spot },
            contentAlignment = Alignment.Center,
          ) {
            Box(Modifier.size(34.dp).background(NEON.copy(alpha = .22f), CircleShape).border(1.5.dp, NEON, CircleShape))
            Icon(spotIcon(spot), contentDescription = stringResource(titleFor(spot)), tint = Color.White, modifier = Modifier.size(18.dp))
          }
        }
      }

      // Top bar.
      Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White) }
        Column(Modifier.weight(1f)) {
          Text(state.selected?.name ?: stringResource(R.string.tab_vehicle), color = Color.White, style = MaterialTheme.typography.titleLarge)
          if (known && snapshot != null) {
            Text("${snapshot.batteryPercent}% · ${snapshot.rangeKm} km", color = NEON, style = MaterialTheme.typography.labelLarge)
          }
        }
        IconButton(onClick = { yaw = -35f; pitch = 18f }) {
          Icon(Icons.Filled.CenterFocusStrong, contentDescription = stringResource(R.string.car3d_reset), tint = Color.White)
        }
      }

      // Bottom: general actions and hints.
      Column(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(
          Modifier.background(NEON.copy(alpha = .14f), CircleShape).border(1.dp, NEON.copy(alpha = .6f), CircleShape)
            .clickable { sheet = true to null }.padding(horizontal = 18.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
          Text(stringResource(R.string.hs_general), color = Color.White, fontWeight = FontWeight.Medium)
        }
        Text(
          stringResource(R.string.car3d_hint),
          color = Color.White.copy(alpha = .6f),
          style = MaterialTheme.typography.labelMedium,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 10.dp),
        )
        Text(
          stringResource(R.string.vehicle_specific_hint),
          color = Color.White.copy(alpha = .4f),
          style = MaterialTheme.typography.labelSmall,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 4.dp),
        )
      }
    }

    sheet?.let { (_, spot) ->
      ModalBottomSheet(onDismissRequest = { sheet = null }) {
        Text(
          stringResource(titleFor(spot)),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(Modifier.padding(bottom = 24.dp)) {
          items(actionsFor(spot)) { action ->
            val critical = ActionPolicy.isCritical(action)
            ListItem(
              headlineContent = { Text(stringResource(ActionInfo.labelRes(action))) },
              leadingContent = {
                Icon(
                  iconFor(action),
                  contentDescription = null,
                  tint = if (critical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
              },
              colors = ListItemDefaults.colors(containerColor = Color.Transparent),
              modifier = Modifier.clickable {
                sheet = null
                run(action)
              },
            )
          }
        }
      }
    }
  }
}
