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
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import org.tfv.ata.R
import org.tfv.ata.action.ActionInfo
import org.tfv.ata.core.ActionPolicy
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.BodyStyle
import org.tfv.ata.ui.ActionHost
import org.tfv.ata.ui.AtaUiState
import org.tfv.ata.ui.AtaViewModel
import org.tfv.ata.ui.components.car.CarCamera
import org.tfv.ata.ui.components.car.CarHotspot
import org.tfv.ata.ui.components.car.CarModel
import org.tfv.ata.ui.components.car.CarStatus
import org.tfv.ata.ui.components.car.drawBatteryRing
import org.tfv.ata.ui.components.car.drawNeonCar
import org.tfv.ata.ui.components.car.drawFlatCar
import org.tfv.ata.ui.components.car.rememberCarLook
import org.tfv.ata.ui.theme.AtaColors
import org.tfv.ata.ui.components.car.rememberFrameClock
import org.tfv.ata.ui.components.iconFor
import kotlin.math.roundToInt

private val SHOWROOM = Color(0xFF04060B)

/** Animation demo: what plays one after another (drawing only, nothing is sent to the car). */
private val DEMO_STEPS: List<List<AtaAction>> = listOf(
  listOf(AtaAction.WAKE),
  listOf(AtaAction.UNLOCK),
  listOf(AtaAction.DOOR_DRIVER_OPEN, AtaAction.DOOR_PASSENGER_OPEN),
  listOf(AtaAction.FRUNK, AtaAction.TRUNK),
  listOf(AtaAction.VENT_WINDOWS),
  listOf(AtaAction.CHARGE_PORT_OPEN, AtaAction.CHARGE_START),
  listOf(AtaAction.CLIMATE_ON, AtaAction.DEFROST, AtaAction.STEERING_WHEEL_HEATER),
  listOf(AtaAction.MEDIA_TOGGLE),
  listOf(AtaAction.FLASH_LIGHTS, AtaAction.HONK),
  listOf(AtaAction.BIOWEAPON_ON),
  listOf(AtaAction.SENTRY_ON),
  listOf(
    AtaAction.CLOSE_WINDOWS, AtaAction.TRUNK_CLOSE, AtaAction.CHARGE_STOP, AtaAction.CHARGE_PORT_CLOSE, AtaAction.CLIMATE_OFF,
    AtaAction.DEFROST_OFF, AtaAction.STEERING_WHEEL_HEATER_OFF, AtaAction.BIOWEAPON_OFF, AtaAction.SENTRY_OFF, AtaAction.REMOTE_DRIVE,
  ),
  listOf(AtaAction.BOOMBOX_FART),
  listOf(AtaAction.LOCK),
)
private const val DEMO_STEP_MS = 2500L

private const val MIN_ZOOM = .7f
private const val MAX_ZOOM = 3f

/** What each tappable spot on the car offers; in the side view ([sideView]) the far door is reached from the near one. */
private fun actionsFor(spot: CarHotspot?, sideView: Boolean): List<AtaAction> = when (spot) {
  CarHotspot.DRIVER_DOOR -> listOfNotNull(
    AtaAction.DOOR_DRIVER_OPEN, AtaAction.DOOR_PASSENGER_OPEN.takeIf { sideView }, AtaAction.UNLOCK, AtaAction.LOCK, AtaAction.REMOTE_DRIVE,
  )
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
 * Full-screen car: turn it with a finger (3D) or look at the side view (2D), pinch to zoom, tap the glowing spots on the car (doors, lids, charge port,
 * windows, lights, climate) or "General" to run actions right there. Effects play on the car after each action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Car3DScreen(vm: AtaViewModel, state: AtaUiState, onBack: () -> Unit) {
  val fx by vm.carFx.collectAsStateWithLifecycle()
  val now by rememberFrameClock()
  var yaw by remember { mutableFloatStateOf(-35f) }
  var pitch by remember { mutableFloatStateOf(18f) }
  var zoom by remember { mutableFloatStateOf(1f) }
  var pan by remember { mutableStateOf(Offset.Zero) }
  val threeD = state.settings.car3d
  var demo by remember { mutableStateOf(false) }
  LaunchedEffect(demo) {
    if (!demo) return@LaunchedEffect
    val before = vm.carFx.value
    try {
      for (step in DEMO_STEPS) {
        step.forEach { vm.onCarAction(it) }
        delay(DEMO_STEP_MS)
      }
    } finally {
      vm.restoreCarFx(before)
    }
    demo = false
  }
  val shape = CarModel.shape(state.selected?.body ?: BodyStyle.MODEL_3)
  var sheet by remember { mutableStateOf<Pair<Boolean, CarHotspot?>?>(null) }
  val snapshot = state.snapshot
  val known = snapshot != null && snapshot.updatedAt > 0
  val status = CarStatus(snapshot?.charging == true, snapshot?.climateOn == true, snapshot?.sentry == true, if (known) snapshot?.batteryPercent else null)
  val palette = AtaColors.current
  val material = palette.material
  // Neon: always the dark showroom; Material: the app's surface in light or dark.
  val look = rememberCarLook(state.selected, dark = if (material) palette.isDark else true, lineWidth = 3f)
  val scheme = MaterialTheme.colorScheme
  val bg = if (material) scheme.surface else SHOWROOM
  val fg = if (material) scheme.onSurface else Color.White
  val accent = look.style.line
  val pulse by rememberInfiniteTransition(label = "spots").animateFloat(
    initialValue = .85f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "spotPulse",
  )

  ActionHost(vm, state) { run ->
    BoxWithConstraints(Modifier.fillMaxSize().background(bg)) {
      val density = LocalDensity.current
      val w = constraints.maxWidth.toFloat()
      val h = constraints.maxHeight.toFloat()
      val camera = if (threeD) CarCamera(yaw, pitch, w, h, zoom = 1.35f * shape.fit * zoom, pan = pan)
      else CarCamera.side(w, h, zoom = 1.3f * shape.fit * zoom, pan = pan)

      Canvas(
        Modifier.fillMaxSize()
          .pointerInput(threeD) {
            // One finger turns the car (3D) or moves it (2D); two fingers zoom and move.
            detectTransformGestures { centroid, drag, scale, _ ->
              val newZoom = (zoom * scale).coerceIn(MIN_ZOOM, MAX_ZOOM)
              val fromCenter = centroid - Offset(w / 2f, h / 2f)
              if (scale != 1f) pan = fromCenter - (fromCenter - pan) * (newZoom / zoom)
              zoom = newZoom
              if (threeD && scale == 1f) {
                yaw += drag.x * .35f
                pitch = (pitch - drag.y * .15f).coerceIn(4f, 55f)
              } else {
                pan += drag
              }
            }
          },
      ) {
        if (threeD) drawNeonCar(shape, camera, yaw, fx, status, now, look.style)
        else drawFlatCar(shape, camera, fx, status, now, look.flat)
        if (known) {
          drawBatteryRing(Offset(size.width / 2f, size.height * .86f), size.minDimension * .07f, snapshot?.batteryPercent, status.charging, now, !material, accent, width = 8f, material = material)
        }
      }

      // Tappable spots, only on the side facing the viewer.
      CarHotspot.entries.forEach { spot ->
        val facing = camera.facing(spot.normal)
        if (facing > -.15f) {
          val p = camera.project(shape.anchor(spot)).offset
          val sizePx = with(density) { 44.dp.toPx() }
          Box(
            Modifier
              .offset { IntOffset((p.x - sizePx / 2).roundToInt(), (p.y - sizePx / 2).roundToInt()) }
              .size(44.dp)
              .alpha(if (threeD) ((facing + .15f) / .5f).coerceIn(.25f, 1f) else 1f)
              .scale(pulse)
              .clickable { sheet = true to spot },
            contentAlignment = Alignment.Center,
          ) {
            if (material) {
              Box(Modifier.size(34.dp).background(scheme.primaryContainer, CircleShape).border(2.dp, scheme.surface, CircleShape))
            } else {
              Box(Modifier.size(34.dp).background(accent.copy(alpha = .22f), CircleShape).border(1.5.dp, accent, CircleShape))
            }
            Icon(spotIcon(spot), contentDescription = stringResource(titleFor(spot)), tint = if (material) scheme.onPrimaryContainer else Color.White, modifier = Modifier.size(18.dp))
          }
        }
      }

      // Top bar.
      Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = fg) }
        Column(Modifier.weight(1f)) {
          Text(state.selected?.name ?: stringResource(R.string.tab_vehicle), color = fg, style = MaterialTheme.typography.titleLarge)
          if (known && snapshot != null) {
            Text("${snapshot.batteryPercent}% · ${snapshot.rangeKm} km", color = accent, style = MaterialTheme.typography.labelLarge)
          }
        }
        IconButton(onClick = { yaw = -35f; pitch = 18f; zoom = 1f; pan = Offset.Zero }) {
          Icon(Icons.Filled.CenterFocusStrong, contentDescription = stringResource(R.string.car3d_reset), tint = fg)
        }
      }

      // Bottom: general actions and hints.
      Column(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(
          Modifier.background(accent.copy(alpha = .14f), CircleShape).border(1.dp, accent.copy(alpha = .6f), CircleShape)
            .clickable { sheet = true to null }.padding(horizontal = 18.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
          Text(stringResource(R.string.hs_general), color = fg, fontWeight = FontWeight.Medium)
        }
        Row(
          Modifier.padding(top = 8.dp).background(fg.copy(alpha = .06f), CircleShape).border(1.dp, fg.copy(alpha = .25f), CircleShape)
            .clickable { demo = !demo }.padding(horizontal = 14.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(if (demo) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
          Text(stringResource(if (demo) R.string.car_demo_stop else R.string.car_demo), color = fg, style = MaterialTheme.typography.labelLarge)
        }
        Text(
          stringResource(if (threeD) R.string.car3d_hint else R.string.car2d_hint),
          color = fg.copy(alpha = .6f),
          style = MaterialTheme.typography.labelMedium,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 10.dp),
        )
        Text(
          stringResource(R.string.vehicle_specific_hint),
          color = fg.copy(alpha = .4f),
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
          items(actionsFor(spot, !threeD)) { action ->
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
