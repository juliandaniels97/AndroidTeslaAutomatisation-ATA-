package org.tfv.ata.wear

import android.os.Bundle
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.produceState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay
import org.tfv.ata.core.VehicleSnapshot
import org.tfv.ata.core.projected
import kotlin.math.cos
import kotlin.math.sin
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PageIndicatorState
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.curvedText
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.tfv.ata.core.WearContract
import kotlin.math.roundToInt

private const val CONFIRM_WINDOW_MS = 5_000L

private val NEON = Color(0xFF4FB4FF)
private val TILE = Color(0xFF10141B)
private val TILE_TOP = Color(0xFF1E2532)
private val TILE_EDGE = Color(0xFF2C3646)
private val CRITICAL = Color(0xFFFF6B5E)
private val CHARGE = Color(0xFF2ECC71)
private val MUTED = Color(0xFF9FB2C6)

class WearMainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { WearApp() }
  }
}

/** Colour of the battery value like the phone's ring: dark blue / blue / yellow / red. */
private fun batteryColor(level: Int, charging: Boolean): Color = when {
  charging -> CHARGE
  level >= 80 -> Color(0xFF3D7BFF)
  level >= 20 -> NEON
  level >= 10 -> Color(0xFFFFD43B)
  else -> Color(0xFFFF4D40)
}

@Composable
private fun WearApp() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val config by remember { WearStore.config(context) }.collectAsState()
  val status by remember { WearStore.status(context) }.collectAsState()
  var vin by remember { mutableStateOf(WearPrefs.vin(context)) }
  var directMode by remember { mutableStateOf(WearPrefs.directMode(context)) }
  var keyCard by remember { mutableStateOf(WearPrefs.keyCard(context)) }
  val pairedVins by remember { org.tfv.ata.core.ble.PairedKeys.vins(context) }.collectAsState()
  val paired = vin.isNotBlank() && org.tfv.ata.core.Vin.normalize(vin) in pairedVins.orEmpty()
  // Critical actions (unlock, trunk …) need a second tap within a few seconds.
  var armed by remember { mutableStateOf<String?>(null) }
  var armedAt by remember { mutableStateOf(0L) }
  var running by remember { mutableStateOf<String?>(null) }

  var pendingAfterPerm by remember { mutableStateOf<(() -> Unit)?>(null) }
  val blePermLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions(),
  ) { _ ->
    val run = pendingAfterPerm
    pendingAfterPerm = null
    if (WearControl.hasBlePermission(context)) run?.invoke()
  }
  fun withBle(block: () -> Unit) {
    if (WearControl.hasBlePermission(context)) {
      block()
    } else {
      pendingAfterPerm = block
      blePermLauncher.launch(org.tfv.ata.core.ble.TeslaController.BLE_PERMISSIONS)
    }
  }

  // Ask for Bluetooth right away once a car is synced: the phone's watch-pairing assistant needs it.
  LaunchedEffect(vin) {
    if (vin.isNotBlank() && WearControl.directSupported &&
      (!WearControl.hasBlePermission(context) || !org.tfv.ata.core.ble.BleTransport.hasLocationPermission(context))
    ) {
      blePermLauncher.launch(org.tfv.ata.core.ble.TeslaController.BLE_PERMISSIONS)
    }
  }

  DisposableEffect(Unit) {
    val client = Wearable.getDataClient(context)
    fun apply(item: com.google.android.gms.wearable.DataItem) {
      WearStore.apply(context, item)
      if (item.uri.path == WearContract.PATH_CONFIG) {
        val map = DataMapItem.fromDataItem(item).dataMap
        WearPrefs.setVehicle(context, map.getString(WearContract.KEY_VIN).orEmpty(), map.getString(WearContract.KEY_VEHICLE).orEmpty())
        vin = WearPrefs.vin(context)
      }
    }
    val listener = com.google.android.gms.wearable.DataClient.OnDataChangedListener { events: DataEventBuffer ->
      for (event in events) if (event.type == DataEvent.TYPE_CHANGED) apply(event.dataItem)
    }
    client.addListener(listener)
    val messages = Wearable.getMessageClient(context)
    val resultListener = MessageClient.OnMessageReceivedListener { event ->
      if (event.path == WearContract.MSG_RESULT) {
        val text = String(event.data, Charsets.UTF_8)
        val ok = text.startsWith("1|")
        Toast.makeText(context, (if (ok) "✓ " else "✗ ") + text.substringAfter('|'), Toast.LENGTH_LONG).show()
      }
    }
    messages.addListener(resultListener)
    scope.launch {
      runCatching {
        val buffer = client.dataItems.await()
        for (item in buffer) apply(item)
        buffer.release()
      }
    }
    onDispose {
      client.removeListener(listener)
      messages.removeListener(resultListener)
    }
  }

  fun runAction(id: String) {
    val now = System.currentTimeMillis()
    if (WearControl.isCritical(id) && (armed != id || now - armedAt > CONFIRM_WINDOW_MS)) {
      armed = id
      armedAt = now
      Toast.makeText(context, R.string.wear_tap_again, Toast.LENGTH_SHORT).show()
      return
    }
    armed = null
    running = id
    scope.launch {
      val msg = when (WearControl.run(context, id)) {
        WearResult.DIRECT -> R.string.wear_sent_car
        WearResult.RELAYED -> R.string.sent_to_phone
        WearResult.FAILED -> R.string.wear_failed
        WearResult.WATCH_LOCKED -> R.string.wear_locked
      }
      running = null
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
  }

  // The car's state, carried forward to now while it charges (refreshed every 30 s).
  val now by produceState(System.currentTimeMillis(), status) {
    while (true) {
      value = System.currentTimeMillis()
      delay(30_000)
    }
  }
  val car: VehicleSnapshot? = remember(status, now) { status?.takeIf { it.known }?.let { (it.snapshot ?: it.asSnapshot()).projected(now) } }

  val cfg = config ?: WatchConfig()
  val round = LocalConfiguration.current.isScreenRound
  val gridSize = if (cfg.grid == WearContract.GRID_LARGE) (if (round) 7 else 9) else 4
  val gridActions = cfg.actions.take(gridSize)
  val moreActions = cfg.actions.drop(gridSize)
  val pager = rememberPagerState(pageCount = { 3 })

  MaterialTheme {
    Scaffold(
      timeText = { if (pager.currentPage == 0) StatusTimeText(car, round) else TimeText() },
      pageIndicator = { HorizontalPageIndicator(pageIndicatorState = remember(pager) { PagerIndicator(pager) }) },
    ) {
      HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
        if (page == 0) {
          Box(Modifier.fillMaxSize()) {
            if (gridActions.isEmpty()) {
              Text(
                stringResource(R.string.wear_configure_on_phone),
                style = MaterialTheme.typography.caption1,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(28.dp),
              )
            } else {
              ActionGrid(gridActions, round, armed, running, car) { runAction(it) }
            }
            StatusRim(car, round)
          }
        } else if (page == 1) {
          StatusCard(car, status?.vehicle.orEmpty(), round)
        } else {
          MoreList(
            actions = moreActions,
            armed = armed,
            onRun = { runAction(it) },
            directMode = directMode,
            onToggleDirect = {
              if (directMode) {
                WearPrefs.setDirectMode(context, false)
                directMode = false
              } else if (!WearControl.directSupported) {
                Toast.makeText(context, R.string.wear_direct_unsupported, Toast.LENGTH_LONG).show()
              } else {
                withBle {
                  WearPrefs.setDirectMode(context, true)
                  directMode = true
                }
              }
            },
            keyCard = keyCard,
            onToggleKeyCard = {
              keyCard = !keyCard
              WearPrefs.setKeyCard(context, keyCard)
            },
            vin = vin,
            paired = paired,
            onPair = {
              scope.launch {
                val ok = WearSync.openPairingOnPhone(context)
                Toast.makeText(context, if (ok) R.string.wear_continue_on_phone else R.string.wear_phone_unreachable, Toast.LENGTH_LONG).show()
              }
            },
          )
        }
      }
    }
  }
}

private class PagerIndicator(private val pager: PagerState) : PageIndicatorState {
  override val pageOffset: Float get() = pager.currentPageOffsetFraction
  override val selectedPage: Int get() = pager.currentPage
  override val pageCount: Int get() = pager.pageCount
}

/** Time at the top, battery left and range right of it (round: along the edge). */
@Composable
private fun StatusTimeText(s: VehicleSnapshot?, round: Boolean) {
  val battery = s?.let { "${it.batteryPercent}%${if (it.charging) " ⚡" else ""}" } ?: "--"
  val range = s?.let { "${it.rangeKm} km" } ?: "--"
  val color = s?.let { batteryColor(it.batteryPercent, it.charging) } ?: MUTED
  if (round) {
    TimeText(
      startCurvedContent = { curvedText(battery, color = color) },
      endCurvedContent = { curvedText(range, color = MUTED) },
    )
  } else {
    TimeText(
      startLinearContent = { Text(battery, color = color, style = MaterialTheme.typography.caption1) },
      endLinearContent = { Text(range, color = MUTED, style = MaterialTheme.typography.caption1) },
    )
  }
}

/** Temperatures and lock state at the bottom edge (round: curved along the rim). */
@Composable
private fun StatusRim(s: VehicleSnapshot?, round: Boolean) {
  val lock = when (s?.locked) { true -> "🔒"; false -> "🔓"; null -> "" }
  val text = when {
    s == null -> stringResource(R.string.wear_status_unknown)
    // While charging: power and time to full instead of the temperatures.
    s.charging -> chargeLine(s)
    else -> stringResource(R.string.wear_status_temps, s.insideTempC.roundToInt(), s.outsideTempC.roundToInt()) + if (lock.isNotEmpty()) "  $lock" else ""
  }
  if (round) {
    // A little inside the edge, so the page dots stay free.
    CurvedLayout(modifier = Modifier.padding(9.dp), anchor = 90f, angularDirection = CurvedDirection.Angular.Reversed) {
      curvedText(text, style = CurvedTextStyle(fontSize = 13.sp, color = MUTED))
    }
  } else {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
      Text(text, color = MUTED, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(bottom = 12.dp))
    }
  }
}

/** Where each tile sits: centre offset from the screen centre and the tile size. */
private data class TileSpot(val x: Dp, val y: Dp)

/**
 * Big tiles filling the screen without wasted space. Round: hexagonal rows 1/2/1 or 2/3/2 that touch the edge
 * ring left for the status; square: 2×2 or 3×3.
 */
@Composable
private fun ActionGrid(actions: List<WatchAction>, round: Boolean, armed: String?, running: String?, car: VehicleSnapshot?, onRun: (String) -> Unit) {
  BoxWithConstraints(Modifier.fillMaxSize()) {
    val w = maxWidth
    val h = maxHeight
    val d = if (w < h) w else h
    val gap = d * .03f
    val spots: List<TileSpot>
    val size: Dp
    if (round) {
      // Ring left at the edge for time and status.
      val r = d / 2 - d * .125f
      if (actions.size <= 4) {
        // 1/2/1 as a diamond: the four tiles touch each other diagonally and the status ring.
        size = (r - gap * .707f) / 1.207f
        val step = (size + gap) * .707f
        spots = listOf(TileSpot(0.dp, -step), TileSpot(-step, 0.dp), TileSpot(step, 0.dp), TileSpot(0.dp, step))
      } else {
        // 2/3/2.
        size = (r - gap) / 1.5f
        val step = size + gap
        val rows = listOf(listOf(-.5f, .5f) to -.866f, listOf(-1f, 0f, 1f) to 0f, listOf(-.5f, .5f) to .866f)
        spots = rows.flatMap { (xs, y) -> xs.map { TileSpot(step * it, step * y) } }
      }
    } else {
      val cols = if (actions.size <= 4) 2 else 3
      val top = h * .13f
      val bottom = h * .14f
      val side = w * .04f
      val availW = w - side * 2
      val availH = h - top - bottom
      size = minOf((availW - gap * (cols - 1)) / cols, (availH - gap * (cols - 1)) / cols)
      val step = size + gap
      val centerY = (top - bottom) / 2
      spots = (0 until cols * cols).map { i ->
        TileSpot(step * ((i % cols) - (cols - 1) / 2f), centerY + step * ((i / cols) - (cols - 1) / 2f))
      }
    }
    val shape: Shape = if (round) CircleShape else RoundedCornerShape(size * .24f)
    val showLabel = actions.size <= 4
    actions.zip(spots).forEach { (action, spot) ->
      Tile(
        action = action,
        size = size,
        shape = shape,
        showLabel = showLabel,
        armed = armed == action.id,
        running = running == action.id,
        active = car?.let { isActive(action.id, it) } == true,
        modifier = Modifier.align(Alignment.Center).offset(spot.x, spot.y),
        onClick = { onRun(action.id) },
      )
    }
  }
}

@Composable
private fun Tile(
  action: WatchAction,
  size: Dp,
  shape: Shape,
  showLabel: Boolean,
  armed: Boolean,
  running: Boolean,
  active: Boolean,
  modifier: Modifier,
  onClick: () -> Unit,
) {
  val critical = WearControl.isCritical(action.id)
  val tint = if (critical) CRITICAL else NEON
  // Card look like the phone's tiles: dark gradient, glowing edge; brighter when the car is in that state.
  val background = when {
    armed -> Brush.verticalGradient(listOf(CRITICAL.copy(alpha = .9f), CRITICAL.copy(alpha = .7f)))
    active -> Brush.verticalGradient(listOf(tint.copy(alpha = .32f), TILE))
    else -> Brush.verticalGradient(listOf(TILE_TOP, TILE))
  }
  val edge = when {
    armed -> CRITICAL
    active -> tint
    else -> tint.copy(alpha = .45f)
  }
  Box(
    modifier.size(size).clip(shape)
      .background(background)
      .border(if (active) 2.dp else 1.dp, edge, shape)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    if (running) CircularProgressIndicator(Modifier.fillMaxSize(), indicatorColor = tint, strokeWidth = 3.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
      Icon(
        iconForId(action.id),
        contentDescription = action.label,
        tint = if (armed) Color.White else tint,
        modifier = Modifier.size(size * (if (showLabel) .36f else .44f)),
      )
      if (showLabel) {
        Text(
          if (armed) stringResource(R.string.wear_tap_again_short) else action.label,
          fontSize = 10.sp,
          lineHeight = 11.sp,
          color = Color.White,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = size * .14f).padding(top = 2.dp),
        )
      }
    }
    if (active && !armed) {
      // Small dot: this is the car's current state (e.g. locked).
      Box(Modifier.align(Alignment.TopCenter).padding(top = size * .1f).size(6.dp).clip(CircleShape).background(tint))
    }
  }
}

/** Whether the car is in the state an action leads to (shown as an active tile). */
private fun isActive(id: String, s: VehicleSnapshot): Boolean = when (id) {
  "lock" -> s.locked == true
  "unlock" -> s.locked == false
  "climate_on" -> s.climateOn
  "climate_off" -> !s.climateOn
  "sentry_on" -> s.sentry
  "sentry_off" -> !s.sentry
  "charge_start" -> s.charging
  "charge_stop" -> !s.charging && s.pluggedIn
  "frunk" -> s.frunkOpen
  "trunk" -> s.trunkOpen
  "vent_windows" -> s.windowsOpen
  "close_windows" -> !s.windowsOpen
  "defrost" -> s.defrost
  "steering_wheel_heater" -> s.steeringHeater == true
  "bioweapon_on" -> s.bioweapon == true
  "charge_port_open" -> s.chargePortOpen == true
  "climate_keep" -> s.climateKeeper == 1
  "dog_mode" -> s.climateKeeper == 2
  "camp_mode" -> s.climateKeeper == 3
  else -> false
}

/** "⚡ 22 kW · 1 h · +20.0 kWh" while charging. */
@Composable
private fun chargeLine(s: VehicleSnapshot): String = listOfNotNull(
  "⚡ ${s.chargingPowerKw} kW".takeIf { s.chargingPowerKw > 0 } ?: "⚡",
  formatMinutes(s.minutesToFull).takeIf { s.minutesToFull > 0 },
  ("+" + String.format(java.util.Locale.getDefault(), "%.1f kWh", s.chargeEnergyAddedKwh)).takeIf { s.chargeEnergyAddedKwh > 0f },
).joinToString(" · ")

private fun formatMinutes(m: Int): String = if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"

/**
 * Second page: the car as a card, like the phone's vehicle view. Battery ring (green dots flow into the charged
 * part while charging, a tick marks the charge limit), battery and range in the middle, then charging details
 * or temperatures and lock state.
 */
@Composable
private fun StatusCard(s: VehicleSnapshot?, name: String, round: Boolean) {
  val transition = rememberInfiniteTransition(label = "ring")
  val t by transition.animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t")
  Box(Modifier.fillMaxSize().padding(if (round) 14.dp else 8.dp), contentAlignment = Alignment.Center) {
    Canvas(Modifier.fillMaxSize()) {
      val stroke = size.minDimension * .045f
      val radius = size.minDimension / 2 - stroke
      val c = center
      val topLeft = Offset(c.x - radius, c.y - radius)
      val box = Size(radius * 2, radius * 2)
      if (s == null) {
        drawCircle(MUTED.copy(alpha = .4f), radius, c, style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f))))
        return@Canvas
      }
      val color = batteryColor(s.batteryPercent, false)
      val sweep = 360f * s.batteryPercent.coerceIn(0, 100) / 100f
      drawCircle(color.copy(alpha = .14f), radius, c, style = Stroke(stroke))
      drawArc(color, -90f, sweep, false, topLeft, box, style = Stroke(stroke, cap = StrokeCap.Round))
      if (s.charging) {
        val empty = 360f - sweep
        val spacing = 24f
        val count = (empty / spacing).toInt() + 1
        val speed = (12f + s.chargingPowerKw * .1f).coerceAtMost(30f)
        for (k in 0 until count) {
          val d = (k * spacing + t * speed) % (count * spacing)
          if (d > empty) continue
          val f = d / empty
          val fade = minOf(1f, f * 5f, (1f - f) * 4f).coerceIn(0f, 1f)
          val rad = Math.toRadians((-90f + sweep + empty - d).toDouble())
          val p = Offset(c.x + radius * cos(rad).toFloat(), c.y + radius * sin(rad).toFloat())
          drawCircle(CHARGE.copy(alpha = .95f * fade), stroke * (.25f + .1f * fade), p)
        }
      }
      s.chargeLimit?.takeIf { it in 1..99 }?.let { limit ->
        val rad = Math.toRadians((-90f + 360f * limit / 100f).toDouble())
        val dir = Offset(cos(rad).toFloat(), sin(rad).toFloat())
        drawLine(Color.White.copy(alpha = .85f), c + dir * (radius - stroke * 1.2f), c + dir * (radius + stroke * 1.2f), strokeWidth = stroke * .35f, cap = StrokeCap.Round)
      }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      if (name.isNotBlank()) Text(name, fontSize = 11.sp, color = MUTED, maxLines = 1)
      if (s == null) {
        Text(stringResource(R.string.wear_status_unknown), color = MUTED, fontSize = 13.sp, textAlign = TextAlign.Center)
      } else {
        Text("${s.batteryPercent}%", fontSize = 30.sp, color = batteryColor(s.batteryPercent, s.charging))
        Text("${s.rangeKm} km", fontSize = 13.sp, color = Color.White)
        if (s.charging) {
          Text(listOfNotNull("⚡ ${s.chargingPowerKw} kW".takeIf { s.chargingPowerKw > 0 } ?: "⚡", formatMinutes(s.minutesToFull).takeIf { s.minutesToFull > 0 }).joinToString(" · "), fontSize = 12.sp, color = CHARGE)
          if (s.chargeEnergyAddedKwh > 0f) {
            Text("+" + String.format(java.util.Locale.getDefault(), "%.1f kWh", s.chargeEnergyAddedKwh), fontSize = 11.sp, color = CHARGE)
          }
        } else {
          val lock = when (s.locked) { true -> "🔒"; false -> "🔓"; null -> "" }
          Text(stringResource(R.string.wear_status_temps, s.insideTempC.roundToInt(), s.outsideTempC.roundToInt()) + if (lock.isNotEmpty()) "  $lock" else "", fontSize = 11.sp, color = MUTED)
        }
      }
    }
  }
}

/** Second page (swipe left): the other actions as a list, then direct control, key card and pairing. */
@Composable
private fun MoreList(
  actions: List<WatchAction>,
  armed: String?,
  onRun: (String) -> Unit,
  directMode: Boolean,
  onToggleDirect: () -> Unit,
  keyCard: Boolean,
  onToggleKeyCard: () -> Unit,
  vin: String,
  paired: Boolean,
  onPair: () -> Unit,
) {
  val context = LocalContext.current
  ScalingLazyColumn(state = rememberScalingLazyListState(), modifier = Modifier.fillMaxWidth()) {
    if (actions.isNotEmpty()) {
      item { ListHeader { Text(stringResource(R.string.wear_more_actions)) } }
      items(actions) { action ->
        Chip(
          modifier = Modifier.fillMaxWidth(),
          label = { Text(if (armed == action.id) stringResource(R.string.wear_confirm_label, action.label) else action.label) },
          icon = { Icon(iconForId(action.id), contentDescription = null, tint = if (WearControl.isCritical(action.id)) CRITICAL else NEON) },
          colors = ChipDefaults.secondaryChipColors(),
          onClick = { onRun(action.id) },
        )
      }
    }
    item { ListHeader { Text(stringResource(R.string.wear_direct_control)) } }
    item {
      Chip(
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(if (directMode) R.string.wear_direct_on else R.string.wear_direct_off)) },
        colors = if (directMode) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
        onClick = onToggleDirect,
      )
    }
    if (org.tfv.ata.core.nfc.NfcInfo.hasCardEmulation(context)) {
      item {
        Chip(
          modifier = Modifier.fillMaxWidth(),
          label = { Text(stringResource(if (keyCard) R.string.wear_keycard_on else R.string.wear_keycard_off)) },
          secondaryLabel = { Text(stringResource(R.string.wear_keycard_setup_on_phone)) },
          colors = if (keyCard) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
          onClick = onToggleKeyCard,
        )
      }
    }
    if (vin.isNotBlank()) {
      item {
        // Pairing runs from the phone's assistant (it guides through the steps and shows the log).
        Chip(
          modifier = Modifier.fillMaxWidth(),
          label = { Text(stringResource(if (paired) R.string.wear_paired_state else R.string.wear_pair)) },
          secondaryLabel = { Text(stringResource(if (paired) R.string.wear_paired_state_hint else R.string.wear_pair_on_phone)) },
          colors = if (paired) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
          onClick = onPair,
        )
      }
    } else {
      item {
        Text(
          stringResource(R.string.wear_pair_hint),
          style = MaterialTheme.typography.caption2,
          modifier = Modifier.fillMaxWidth().padding(8.dp),
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}
