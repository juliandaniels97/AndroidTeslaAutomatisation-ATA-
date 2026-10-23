package org.tfv.ata.ui.tiles

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Masks
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.tfv.ata.R
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.VehicleSnapshot
import org.tfv.ata.ui.ActionRun
import org.tfv.ata.ui.components.SteeringWheelIcon
import org.tfv.ata.ui.theme.AtaColors

/** Draws [tile] with its buttons; [interactive] is false in edit mode (the board catches the touches then). */
@Composable
fun MultiTileView(
  tile: MultiTile,
  snapshot: VehicleSnapshot?,
  enabled: Boolean,
  interactive: Boolean,
  compact: Boolean,
  haptics: Boolean,
  run: ActionRun,
  modifier: Modifier = Modifier,
) {
  val on = enabled && interactive
  val s = snapshot
  val palette = AtaColors.current
  val active = when (tile) {
    MultiTile.CLIMATE -> s?.climateOn == true
    MultiTile.CHARGING -> s?.charging == true
    MultiTile.SENTRY -> s?.sentry == true
    MultiTile.STEERING -> s?.steeringHeater == true
    MultiTile.BIOWEAPON -> s?.bioweapon == true
    MultiTile.SEATS -> listOfNotNull(
      s?.seatHeatFrontLeft, s?.seatHeatFrontRight, s?.seatHeatRearLeft, s?.seatHeatRearCenter, s?.seatHeatRearRight, s?.seatCoolFrontLeft, s?.seatCoolFrontRight,
    ).any { it > 0 } || s?.seatAutoFrontLeft == true || s?.seatAutoFrontRight == true
    MultiTile.KEEPER -> (s?.climateKeeper ?: 0) > 0
    else -> false
  }
  TileFrame(modifier.fillMaxSize(), enabled = enabled, active = active) {
    Column(Modifier.fillMaxSize().padding(if (compact) 5.dp else 8.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 6.dp)) {
      val label = stringResource(tile.labelRes)
      when (tile) {
        MultiTile.LOCK -> {
          val locked = s?.locked
          TileHeader(
            tile.icon, label,
            status = when (locked) {
              true -> stringResource(R.string.st_locked)
              false -> stringResource(R.string.st_unlocked)
              null -> null
            },
            statusColor = if (locked == false) palette.warning else palette.success,
            compact = compact,
          )
          ButtonRow {
            Btn(Icons.Filled.Lock, stringResource(R.string.btn_lock), locked == true, on, haptics) { run(AtaAction.LOCK) }
            Btn(Icons.Filled.LockOpen, stringResource(R.string.btn_unlock), locked == false, on, haptics, critical = true) { run(AtaAction.UNLOCK) }
          }
        }
        MultiTile.CLIMATE -> {
          TileHeader(tile.icon, label, s?.takeIf { it.updatedAt > 0 }?.let { "${it.insideTempC.roundToInt()}°" }, compact = compact)
          Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              val climateOn = s?.climateOn == true
              Btn(Icons.Filled.PowerSettingsNew, stringResource(if (climateOn) R.string.st_on else R.string.st_off), climateOn, on, haptics, Modifier.weight(1f).fillMaxWidth()) {
                run(if (climateOn) AtaAction.CLIMATE_OFF else AtaAction.CLIMATE_ON)
              }
              val defrost = s?.defrost == true
              Btn(Icons.Filled.AcUnit, stringResource(R.string.btn_defrost), defrost, on, haptics, Modifier.weight(1f).fillMaxWidth()) {
                run(if (defrost) AtaAction.DEFROST_OFF else AtaAction.DEFROST)
              }
            }
            TempFader(s?.driverTempC, stringResource(R.string.seat_fl), on, haptics, Modifier.weight(1f).fillMaxHeight()) {
              run.withValue(AtaAction.SET_TEMPERATURE_DRIVER, it)
            }
            TempFader(s?.passengerTempC ?: s?.driverTempC, stringResource(R.string.seat_fr), on, haptics, Modifier.weight(1f).fillMaxHeight()) {
              run.withValue(AtaAction.SET_TEMPERATURE_PASSENGER, it)
            }
          }
        }
        MultiTile.SEATS -> {
          val heating = listOfNotNull(s?.seatHeatFrontLeft, s?.seatHeatFrontRight, s?.seatHeatRearLeft, s?.seatHeatRearCenter, s?.seatHeatRearRight).any { it > 0 }
          val cooling = listOfNotNull(s?.seatCoolFrontLeft, s?.seatCoolFrontRight).any { it > 0 }
          // Heating or ventilation, switched in the tile; starts with ventilation only when that is what runs.
          var cool by rememberSaveable { mutableStateOf(cooling && !heating) }
          Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { TileHeader(if (cool) Icons.Filled.Air else tile.icon, label, compact = compact) }
            ModeSwitch(cool, interactive, haptics, compact) { cool = it }
          }
          // A car that reports its seats leaves out the ones it doesn't have.
          val known = s?.seatHeatFrontLeft != null
          val last = remember { mutableStateMapOf<AtaAction, Int>() }
          val set: (AtaAction, Int) -> Unit = { action, lv ->
            if (lv > 0) last[action] = lv
            run.withValue(action, lv)
          }
          val ctx = SeatCtx(known, on, haptics, compact, last, set, cool)
          val autoL = s?.seatAutoFrontLeft
          val autoR = s?.seatAutoFrontRight
          Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (cool) {
              Seat(ctx, AtaAction.SEAT_COOLER_FRONT_LEFT, R.string.seat_fl, s?.seatCoolFrontLeft, autoL) { run.withValue(AtaAction.SEAT_AUTO_FRONT_LEFT, if (autoL == true) 0 else 1) }
              Seat(ctx, AtaAction.SEAT_COOLER_FRONT_RIGHT, R.string.seat_fr, s?.seatCoolFrontRight, autoR) { run.withValue(AtaAction.SEAT_AUTO_FRONT_RIGHT, if (autoR == true) 0 else 1) }
            } else {
              Seat(ctx, AtaAction.SEAT_HEATER_FRONT_LEFT, R.string.seat_fl, s?.seatHeatFrontLeft, autoL) { run.withValue(AtaAction.SEAT_AUTO_FRONT_LEFT, if (autoL == true) 0 else 1) }
              Seat(ctx, AtaAction.SEAT_HEATER_FRONT_RIGHT, R.string.seat_fr, s?.seatHeatFrontRight, autoR) { run.withValue(AtaAction.SEAT_AUTO_FRONT_RIGHT, if (autoR == true) 0 else 1) }
            }
          }
          if (!cool && (!known || listOf(s?.seatHeatRearLeft, s?.seatHeatRearCenter, s?.seatHeatRearRight).any { it != null })) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
              Seat(ctx, AtaAction.SEAT_HEATER_REAR_LEFT, R.string.seat_rl, s?.seatHeatRearLeft)
              Seat(ctx, AtaAction.SEAT_HEATER_REAR_CENTER, R.string.seat_rc, s?.seatHeatRearCenter)
              Seat(ctx, AtaAction.SEAT_HEATER_REAR_RIGHT, R.string.seat_rr, s?.seatHeatRearRight)
            }
          }
        }
        MultiTile.CHARGING -> ChargingBody(tile, label, s, on, haptics, compact, run)
        MultiTile.LIDS -> {
          val frunk = s?.frunkOpen == true
          val trunk = s?.trunkOpen == true
          TileHeader(tile.icon, label, if (frunk || trunk) stringResource(R.string.st_open) else null, palette.warning, compact)
          ButtonRow {
            Btn(Icons.Filled.Inventory2, stringResource(R.string.a_frunk), frunk, on, haptics, critical = true) { run(AtaAction.FRUNK) }
            Btn(Icons.Filled.Luggage, stringResource(R.string.btn_trunk), trunk, on, haptics, critical = true) {
              run(if (trunk) AtaAction.TRUNK_CLOSE else AtaAction.TRUNK)
            }
          }
        }
        MultiTile.DOORS -> {
          TileHeader(tile.icon, label, compact = compact)
          ButtonRow {
            Btn(Icons.Filled.DoorFront, stringResource(R.string.seat_fl), s?.doorDriverOpen == true, on, haptics, critical = true) { run(AtaAction.DOOR_DRIVER_OPEN) }
            Btn(Icons.Filled.DoorBack, stringResource(R.string.seat_fr), s?.doorPassengerOpen == true, on, haptics, critical = true) { run(AtaAction.DOOR_PASSENGER_OPEN) }
          }
        }
        MultiTile.WINDOWS -> {
          val open = s?.windowsOpen == true
          TileHeader(tile.icon, label, if (open) stringResource(R.string.st_open) else null, palette.warning, compact)
          ButtonRow {
            Btn(Icons.Filled.Window, stringResource(R.string.btn_vent), open, on, haptics) { run(AtaAction.VENT_WINDOWS) }
            Btn(Icons.Filled.Window, stringResource(R.string.btn_close), s != null && !open, on, haptics) { run(AtaAction.CLOSE_WINDOWS) }
          }
        }
        MultiTile.CHARGE_PORT -> {
          val open = s?.chargePortOpen
          TileHeader(tile.icon, label, if (open == true) stringResource(R.string.st_open) else null, palette.accent, compact)
          ButtonRow {
            Btn(Icons.Filled.EvStation, stringResource(R.string.btn_open), open == true, on, haptics) { run(AtaAction.CHARGE_PORT_OPEN) }
            Btn(Icons.Filled.EvStation, stringResource(R.string.btn_close), open == false, on, haptics) { run(AtaAction.CHARGE_PORT_CLOSE) }
          }
        }
        MultiTile.VOLUME -> {
          val max = s?.volumeMax ?: 10f
          val volume = s?.volume
          TileHeader(tile.icon, label, compact = compact)
          Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Btn(Icons.Filled.VolumeDown, null, false, on, haptics, Modifier.weight(.8f).fillMaxHeight()) { run(AtaAction.VOLUME_DOWN) }
            DragFader(
              value = volume ?: max / 2,
              range = 0f..max,
              step = .5f,
              format = { if (volume == null && it == max / 2) "–" else formatVolume(it) },
              onCommit = { run.withValue(AtaAction.SET_VOLUME, (it * 10).roundToInt().coerceIn(0, 100)) },
              enabled = on,
              modifier = Modifier.weight(1.4f).fillMaxHeight(),
              haptics = haptics,
            )
            Btn(Icons.Filled.VolumeUp, null, false, on, haptics, Modifier.weight(.8f).fillMaxHeight()) { run(AtaAction.VOLUME_UP) }
          }
        }
        MultiTile.MEDIA -> {
          TileHeader(tile.icon, s?.nowPlaying ?: label, compact = compact)
          ButtonRow {
            Btn(Icons.Filled.SkipPrevious, null, false, on, haptics) { run(AtaAction.MEDIA_PREVIOUS) }
            Btn(Icons.Filled.PlayArrow, null, false, on, haptics) { run(AtaAction.MEDIA_TOGGLE) }
            Btn(Icons.Filled.SkipNext, null, false, on, haptics) { run(AtaAction.MEDIA_NEXT) }
          }
        }
        MultiTile.SENTRY -> Toggle(tile, label, Icons.Filled.Security, s?.sentry == true, on, haptics, compact) {
          run(if (s?.sentry == true) AtaAction.SENTRY_OFF else AtaAction.SENTRY_ON)
        }
        MultiTile.STEERING -> Toggle(
          tile, label, SteeringWheelIcon, s?.steeringHeater == true, on, haptics, compact,
          // The car's automatic mode can only be shown (Bluetooth has no command for it); the level while on.
          stateText = when {
            s?.steeringAuto == true -> stringResource(R.string.st_auto) + if (s.steeringHeater == true) " · " + stringResource(R.string.st_on) else ""
            s?.steeringHeater == true && s.steeringLevel == 1 -> stringResource(R.string.st_low)
            s?.steeringHeater == true && s.steeringLevel == 2 -> stringResource(R.string.st_high)
            else -> null
          },
        ) {
          run(if (s?.steeringHeater == true) AtaAction.STEERING_WHEEL_HEATER_OFF else AtaAction.STEERING_WHEEL_HEATER)
        }
        MultiTile.BIOWEAPON -> Toggle(tile, label, Icons.Filled.Masks, s?.bioweapon == true, on, haptics, compact) {
          run(if (s?.bioweapon == true) AtaAction.BIOWEAPON_OFF else AtaAction.BIOWEAPON_ON)
        }
        MultiTile.KEEPER -> {
          val mode = s?.climateKeeper ?: 0
          TileHeader(tile.icon, label, compact = compact)
          ButtonRow {
            Btn(Icons.Filled.PowerSettingsNew, stringResource(R.string.st_off), s != null && mode == 0, on, haptics) { run(AtaAction.CLIMATE_KEEP_OFF) }
            Btn(Icons.Filled.Thermostat, stringResource(R.string.kp_keep), mode == 1, on, haptics) { run(AtaAction.CLIMATE_KEEP) }
            Btn(Icons.Filled.Pets, stringResource(R.string.kp_dog), mode == 2, on, haptics) { run(AtaAction.DOG_MODE) }
            Btn(Icons.Filled.Nightlight, stringResource(R.string.kp_camp), mode == 3, on, haptics) { run(AtaAction.CAMP_MODE) }
          }
        }
        MultiTile.OVERHEAT -> {
          val mode = s?.overheatProtection
          TileHeader(tile.icon, label, compact = compact)
          ButtonRow {
            Btn(Icons.Filled.PowerSettingsNew, stringResource(R.string.st_off), mode == 0, on, haptics) { run(AtaAction.OVERHEAT_PROTECTION_OFF) }
            Btn(Icons.Filled.AcUnit, stringResource(R.string.oh_ac), mode == 1, on, haptics) { run(AtaAction.OVERHEAT_PROTECTION_ON) }
            Btn(Icons.Filled.Air, stringResource(R.string.oh_fan), mode == 2, on, haptics) { run(AtaAction.OVERHEAT_PROTECTION_FAN_ONLY) }
          }
        }
      }
    }
  }
}

private fun formatVolume(v: Float): String = if (v % 1f == 0f) v.roundToInt().toString() else String.format(java.util.Locale.ROOT, "%.1f", v)

@Composable
private fun ColumnScope.ButtonRow(content: @Composable RowScope.() -> Unit) {
  Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), content = content)
}

@Composable
private fun RowScope.Btn(
  icon: ImageVector,
  label: String?,
  active: Boolean,
  enabled: Boolean,
  haptics: Boolean,
  modifier: Modifier = Modifier.weight(1f).fillMaxHeight(),
  critical: Boolean = false,
  onClick: () -> Unit,
) {
  TileButton(icon, label, active, enabled, onClick, modifier, critical = critical, haptics = haptics)
}

@Composable
private fun ColumnScope.Btn(
  icon: ImageVector,
  label: String?,
  active: Boolean,
  enabled: Boolean,
  haptics: Boolean,
  modifier: Modifier,
  onClick: () -> Unit,
) {
  TileButton(icon, label, active, enabled, onClick, modifier, haptics = haptics)
}

/** 1 × 1 on/off tile: name on top, one big button showing the state. */
@Composable
private fun ColumnScope.Toggle(
  tile: MultiTile,
  label: String,
  icon: ImageVector,
  active: Boolean,
  enabled: Boolean,
  haptics: Boolean,
  compact: Boolean,
  stateText: String? = null,
  onClick: () -> Unit,
) {
  Text(
    label,
    fontSize = if (compact) 9.sp else 11.sp,
    lineHeight = if (compact) 10.sp else 12.sp,
    fontWeight = FontWeight.SemiBold,
    color = if (AtaColors.current.material) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else AtaColors.current.tileText,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
  )
  TileButton(
    icon,
    stateText ?: stringResource(if (active) R.string.st_on else R.string.st_off),
    active,
    enabled,
    onClick,
    Modifier.weight(1f).fillMaxWidth(),
    haptics = haptics,
  )
}

/** What the seat cells of the seat tile share; [cool] = ventilation instead of heating. */
private class SeatCtx(
  val known: Boolean,
  val enabled: Boolean,
  val haptics: Boolean,
  val compact: Boolean,
  val last: Map<AtaAction, Int>,
  val set: (AtaAction, Int) -> Unit,
  val cool: Boolean,
)

/**
 * One seat; skipped when the car reports that this seat has no heater. Front seats also get the automatic
 * mode ([auto], toggled by [onAuto]).
 */
@Composable
private fun RowScope.Seat(ctx: SeatCtx, action: AtaAction, name: Int, level: Int?, auto: Boolean? = null, onAuto: (() -> Unit)? = null) {
  if (ctx.known && level == null && !ctx.cool) return
  val lv = level ?: 0
  SeatCell(
    if (ctx.cool) Icons.Filled.Air else Icons.Filled.AirlineSeatReclineNormal, stringResource(name), lv, ctx.enabled, ctx.haptics, ctx.compact,
    Modifier.weight(1f).fillMaxHeight(),
    // On = the level used last on this seat (3 the first time).
    onToggle = { ctx.set(action, if (lv > 0) 0 else ctx.last[action] ?: 3) },
    onLevel = { ctx.set(action, it) },
    auto = auto,
    onAuto = onAuto,
  )
}

/**
 * One seat: the button switches on/off, the three dots pick level 1 … 3 (tapping the current level = off), "A"
 * switches the car's automatic seat climate (front seats).
 */
@Composable
private fun SeatCell(
  icon: ImageVector,
  name: String,
  level: Int,
  enabled: Boolean,
  haptics: Boolean,
  compact: Boolean,
  modifier: Modifier,
  onToggle: () -> Unit,
  onLevel: (Int) -> Unit,
  auto: Boolean? = null,
  onAuto: (() -> Unit)? = null,
) {
  val isAuto = auto == true
  Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    TileButton(
      icon,
      if (isAuto) "$name · ${stringResource(R.string.st_auto)}" else name,
      level > 0 || isAuto,
      enabled,
      onToggle,
      Modifier.weight(1f).fillMaxWidth(),
      haptics = haptics,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
      LevelDots(level, enabled, onSelect = { onLevel(if (it == level) 0 else it) }, dot = if (compact) 5.dp else 7.dp, haptics = haptics)
      if (onAuto != null) AutoChip(isAuto, enabled, compact, haptics, onAuto)
    }
  }
}

/** Small "A" pill: the car's automatic mode on (filled) or off. */
@Composable
private fun AutoChip(on: Boolean, enabled: Boolean, compact: Boolean, haptics: Boolean, onClick: () -> Unit) {
  val palette = AtaColors.current
  val view = androidx.compose.ui.platform.LocalView.current
  val accent = if (palette.material) androidx.compose.material3.MaterialTheme.colorScheme.primary else palette.accent
  val shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
  Box(
    Modifier
      .padding(start = 2.dp)
      .clip(shape)
      .background(if (on) accent.copy(alpha = if (palette.material) 1f else .25f) else palette.muted.copy(alpha = .15f))
      .clickable(enabled = enabled) {
        if (haptics) view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
        onClick()
      }
      .padding(horizontal = if (compact) 4.dp else 6.dp, vertical = 1.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      "A",
      fontSize = if (compact) 8.sp else 10.sp,
      fontWeight = FontWeight.Bold,
      color = when {
        on && palette.material -> androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
        on -> accent
        else -> palette.muted
      },
    )
  }
}

/** Heating / ventilation switch in the seat tile's header. */
@Composable
private fun ModeSwitch(cool: Boolean, enabled: Boolean, haptics: Boolean, compact: Boolean, onChange: (Boolean) -> Unit) {
  val palette = AtaColors.current
  val view = androidx.compose.ui.platform.LocalView.current
  val accent = if (palette.material) androidx.compose.material3.MaterialTheme.colorScheme.primary else palette.accent
  val shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
  Row(Modifier.clip(shape).background(palette.muted.copy(alpha = .15f)).padding(2.dp)) {
    listOf(false to stringResource(R.string.seat_mode_heat), true to stringResource(R.string.seat_mode_cool)).forEach { (mode, text) ->
      val selected = mode == cool
      Row(
        Modifier.clip(shape)
          .background(if (selected) accent.copy(alpha = if (palette.material) 1f else .28f) else androidx.compose.ui.graphics.Color.Transparent)
          .clickable(enabled = enabled) {
            if (haptics) view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            onChange(mode)
          }
          .padding(horizontal = if (compact) 4.dp else 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val tint = when {
          selected && palette.material -> androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
          selected -> accent
          else -> palette.muted
        }
        Icon(if (mode) Icons.Filled.Air else Icons.Filled.LocalFireDepartment, contentDescription = text, tint = tint, modifier = Modifier.size(if (compact) 12.dp else 14.dp))
        if (!compact) Text(text, fontSize = 10.sp, color = tint, modifier = Modifier.padding(start = 2.dp), maxLines = 1)
      }
    }
  }
}

@Composable
private fun TempFader(value: Float?, label: String, enabled: Boolean, haptics: Boolean, modifier: Modifier, onCommit: (Int) -> Unit) {
  DragFader(
    value = value ?: 21f,
    range = 15f..28f,
    step = 1f,
    format = { if (value == null) "–" else "${it.roundToInt()}°" },
    onCommit = { onCommit(it.roundToInt()) },
    enabled = enabled,
    modifier = modifier,
    label = label,
    haptics = haptics,
  )
}

@Composable
private fun ColumnScope.ChargingBody(
  tile: MultiTile,
  label: String,
  s: VehicleSnapshot?,
  on: Boolean,
  haptics: Boolean,
  compact: Boolean,
  run: ActionRun,
) {
  val palette = AtaColors.current
  val charging = s?.charging == true
  val status = when {
    charging && (s?.chargingPowerKw ?: 0) > 0 -> stringResource(R.string.st_charging_kw, s!!.chargingPowerKw)
    charging -> stringResource(R.string.st_charging)
    s?.pluggedIn == true -> stringResource(R.string.st_plugged)
    else -> null
  }
  TileHeader(tile.icon, label, status, if (charging) palette.success else palette.muted, compact)
  Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Column(Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      val pulse = rememberInfiniteTransition(label = "chargePulse")
      val glow by pulse.animateFloat(.55f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "glow")
      Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Filled.Bolt, contentDescription = null,
            tint = if (charging) palette.success else palette.muted,
            modifier = Modifier.padding(end = 2.dp).graphicsLayer { alpha = if (charging) glow else 1f },
          )
          Text(
            s?.takeIf { it.updatedAt > 0 }?.let { "${it.batteryPercent}%" } ?: "--",
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 16.sp else 22.sp,
            color = if (palette.material) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else palette.tileText,
          )
        }
        val sub = when {
          charging && (s?.minutesToFull ?: 0) > 0 -> stringResource(R.string.st_full_in, formatMinutes(s!!.minutesToFull))
          s?.chargeLimit != null -> stringResource(R.string.st_limit, s.chargeLimit!!)
          else -> null
        }
        if (sub != null) Text(sub, fontSize = 10.sp, color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      TileButton(
        Icons.Filled.Bolt,
        stringResource(if (charging) R.string.btn_stop else R.string.btn_start),
        charging,
        on,
        onClick = { run(if (charging) AtaAction.CHARGE_STOP else AtaAction.CHARGE_START) },
        modifier = Modifier.weight(1f).fillMaxWidth(),
        haptics = haptics,
      )
    }
    DragFader(
      value = (s?.chargeLimit ?: 80).toFloat(),
      range = 50f..100f,
      step = 1f,
      format = { "${it.roundToInt()}%" },
      onCommit = { run.withValue(AtaAction.CHARGE_LIMIT, it.roundToInt()) },
      enabled = on,
      modifier = Modifier.weight(1f).fillMaxHeight(),
      label = stringResource(R.string.f_limit),
      haptics = haptics,
    )
    DragFader(
      value = (s?.chargeAmpsRequest ?: 16).toFloat(),
      range = 1f..48f,
      step = 1f,
      format = { "${it.roundToInt()} A" },
      onCommit = { run.withValue(AtaAction.CHARGING_AMPS, it.roundToInt()) },
      enabled = on,
      modifier = Modifier.weight(1f).fillMaxHeight(),
      label = stringResource(R.string.f_amps),
      haptics = haptics,
    )
  }
}

/** "1 h 20 min" / "35 min". */
internal fun formatMinutes(m: Int): String = if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"
