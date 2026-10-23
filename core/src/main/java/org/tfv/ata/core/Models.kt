package org.tfv.ata.core

import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
  val id: String,
  val name: String,
  val vin: String,
  val navHome: String = "",
  val navWork: String = "",
  /** [BodyStyle] name, empty = derived from the VIN. */
  val bodyStyle: String = "",
  /** [CarPaint] name for the Material design, empty = the default paint of the body style. */
  val paint: String = "",
  /** [NeonColor] name for the neon design, empty = blue. */
  val neonColor: String = "",
) {
  /** Paint shown in the Material design. */
  val carPaint: CarPaint get() = CarPaint.entries.firstOrNull { it.name == paint } ?: CarPaint.defaultFor(body)
  /** Line colour of the neon car. */
  val carNeon: NeonColor get() = NeonColor.entries.firstOrNull { it.name == neonColor } ?: NeonColor.BLUE
  /** The chosen body style, else the one encoded in the VIN, else Model 3. */
  val body: BodyStyle get() = BodyStyle.entries.firstOrNull { it.name == bodyStyle } ?: BodyStyle.fromVin(vin) ?: BodyStyle.MODEL_3
}

/** Tesla paints (approximate colours, ARGB) for the Material car. */
enum class CarPaint(val argb: Long) {
  PEARL_WHITE(0xFFEDEEEC),
  SOLID_BLACK(0xFF16171A),
  STEALTH_GREY(0xFF41454B),
  MIDNIGHT_SILVER(0xFF5A5F66),
  QUICKSILVER(0xFFA4A8AC),
  DEEP_BLUE(0xFF1E3767),
  ULTRA_RED(0xFFA3161F),
  STAINLESS(0xFFB9BCBF);

  companion object {
    fun defaultFor(style: BodyStyle) = if (style == BodyStyle.CYBERTRUCK) STAINLESS else PEARL_WHITE
  }
}

/** Line colours of the neon car. */
enum class NeonColor(val argb: Long) {
  BLUE(0xFF4FB4FF),
  CYAN(0xFF2EF2E2),
  PURPLE(0xFFB57BFF),
  PINK(0xFFFF5FB1),
  RED(0xFFFF4D40),
  GREEN(0xFF39FF88),
  AMBER(0xFFFFB347),
  WHITE(0xFFE8F6FF),
}

/** Tesla body styles, used for the neon drawing of the car. */
enum class BodyStyle {
  MODEL_3, MODEL_Y, MODEL_S, MODEL_X, CYBERTRUCK, ROADSTER;

  companion object {
    /** The 4th VIN character is the model line (5YJ3…, 7SAY…, LRWY…, 5YJS…, 5YJX…, 7G2C…, 5YJR…). */
    fun fromVin(vin: String): BodyStyle? = when (vin.trim().uppercase().getOrNull(3)) {
      '3' -> MODEL_3
      'Y' -> MODEL_Y
      'S' -> MODEL_S
      'X' -> MODEL_X
      'C' -> CYBERTRUCK
      'R' -> ROADSTER
      else -> null
    }
  }
}

@Serializable
data class VehicleSnapshot(
  val batteryPercent: Int = 0,
  val rangeKm: Int = 0,
  val charging: Boolean = false,
  /** null = unknown (not reported by the car). */
  val locked: Boolean? = null,
  val climateOn: Boolean = false,
  val insideTempC: Float = 0f,
  val outsideTempC: Float = 0f,
  val sentry: Boolean = false,
  /** Charging power in kW while [charging] (0 otherwise). */
  val chargingPowerKw: Int = 0,
  val chargerVoltage: Int = 0,
  val chargerCurrent: Int = 0,
  val minutesToFull: Int = 0,
  val chargeLimit: Int? = null,
  val chargeAmpsRequest: Int? = null,
  val pluggedIn: Boolean = false,
  val chargePortOpen: Boolean? = null,
  val chargeEnergyAddedKwh: Float = 0f,
  /** Temperature settings (°C) of the driver and passenger side. */
  val driverTempC: Float? = null,
  val passengerTempC: Float? = null,
  /** Seat heater levels 0 … 3, null = seat has no heater (or unknown). */
  val seatHeatFrontLeft: Int? = null,
  val seatHeatFrontRight: Int? = null,
  val seatHeatRearLeft: Int? = null,
  val seatHeatRearCenter: Int? = null,
  val seatHeatRearRight: Int? = null,
  /** Ventilated seat levels 0 … 3, null = no ventilation. */
  val seatCoolFrontLeft: Int? = null,
  val seatCoolFrontRight: Int? = null,
  val steeringHeater: Boolean? = null,
  /** Steering wheel heater level while on: 1 low, 2 high (0 = off / unknown). */
  val steeringLevel: Int = 0,
  /** The car switches the steering wheel heater by itself. */
  val steeringAuto: Boolean? = null,
  /** Automatic seat climate of the front seats; null = not reported. */
  val seatAutoFrontLeft: Boolean? = null,
  val seatAutoFrontRight: Boolean? = null,
  val defrost: Boolean = false,
  val bioweapon: Boolean? = null,
  /** Cabin overheat protection: 0 off, 1 on, 2 fan only; null = unknown. */
  val overheatProtection: Int? = null,
  /** Climate keeper: 0 off, 1 keep, 2 dog, 3 camp. */
  val climateKeeper: Int = 0,
  val doorDriverOpen: Boolean = false,
  val doorPassengerOpen: Boolean = false,
  val doorRearLeftOpen: Boolean = false,
  val doorRearRightOpen: Boolean = false,
  val frunkOpen: Boolean = false,
  val trunkOpen: Boolean = false,
  val windowsOpen: Boolean = false,
  /** Media volume on the car's scale (0 … [volumeMax]), null = unknown. */
  val volume: Float? = null,
  val volumeMax: Float = 10f,
  val nowPlaying: String? = null,
  /** True when only VCSEC answered (car asleep): battery/range/temperatures are from an earlier reading. */
  val asleep: Boolean = false,
  val updatedAt: Long = 0,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * When ATA reads the car's status by itself:
 * - [BATTERY_SAVER]: never, only when you refresh (pull down or ↻).
 * - [SMART]: at app start, then every [AtaSettings.smartMinutes] while the car is plugged in and the phone is in use.
 * - [INTERVAL]: every [AtaSettings.intervalMinutes], also in the background.
 * Background reads never wake a sleeping car.
 */
enum class StatusMode { BATTERY_SAVER, SMART, INTERVAL }

/** Look of the app: glowing neon lines, or calm Material 3 surfaces. Both come in light and dark. */
enum class UiDesign { NEON, MATERIAL }

/** Look of the home-screen widgets: like the app ([APP]), or always neon / Material. */
enum class WidgetDesign { APP, NEON, MATERIAL }

@Serializable
data class AtaSettings(
  val theme: ThemeMode = ThemeMode.DARK,
  val design: UiDesign = UiDesign.NEON,
  val dynamicColor: Boolean = false,
  val confirmCritical: Boolean = true,
  val haptics: Boolean = true,
  val biometricLock: Boolean = false,
  val experimentalBle: Boolean = false,
  /** Old interval setting (before 1.9.4), read once to pick [statusMode]. */
  val pollIntervalMinutes: Int = 0,
  val statusMode: StatusMode = StatusMode.BATTERY_SAVER,
  /** [StatusMode.SMART]: minutes between reads while plugged in (5 … 180). */
  val smartMinutes: Int = 30,
  /** [StatusMode.INTERVAL]: minutes between reads (5 … 180). */
  val intervalMinutes: Int = 30,
  /** Status settings moved to [statusMode] (1.9.4). */
  val statusModeMigrated: Boolean = false,
  /** Demo mode: a made-up car instead of Bluetooth (see DemoCar). */
  val demoMode: Boolean = false,
  val navigationApp: NavigationApp = NavigationApp.TESLA,
  val homeAddress: String = "",
  val workAddress: String = "",
  val active: Boolean = true,
  val onboarded: Boolean = false,
  val directionFx: Boolean = false,
  val nfcKeyCard: Boolean = false,
  /** Turning 3D car; off = static side view. */
  val car3d: Boolean = true,
  // Automation (Samsung Routines, Tasker, shortcuts, widgets, NFC tags, watch). See ActionPolicy.
  /** Run non-critical BLE actions from ATA shortcuts/widgets/watch while the phone is locked. */
  val automationWhenLocked: Boolean = true,
  /** Critical actions that ATA shortcuts/widgets may run without a confirmation tap (phone unlocked). */
  val criticalDirectIds: List<String> = emptyList(),
  /** Also run [criticalDirectIds] while the phone is locked. */
  val criticalDirectWhenLocked: Boolean = false,
  /** Non-critical BLE actions that other apps may trigger without ATA's token. */
  val externalActionIds: List<String> = emptyList(),
  /** Send the org.tfv.ata.action.STATUS broadcast (readable by every app) after each status poll. */
  val statusBroadcast: Boolean = false,
  val carLat: Double = 0.0,
  val carLng: Double = 0.0,
  val carLocAt: Long = 0,
  /** Unlock when walking up to the car (Bluetooth signal strength, see ProximityDetector). */
  val proximityUnlock: Boolean = false,
  /** Distance at which the car unlocks, 2 … 20 m (approximate). */
  val proximityMeters: Int = 5,
  /** Also unlock while the phone is locked (otherwise ATA asks with a notification). */
  val proximityWhenLocked: Boolean = false,
  /** Favourite tiles (action ids and multi-tile ids, see the app's TileCatalog). */
  val homeActionIds: List<String> = emptyList(),
  /** Tiles of the vehicle tab in order (action ids and multi-tile ids). */
  val vehicleActionIds: List<String> = emptyList(),
  /** Several actions of one kind in one tile (lock/unlock, seats, volume …); off = one tile per action. */
  val multiTiles: Boolean = true,
  /** Tiles per row, 2 … 6; 0 = automatic (3 on phones, more on tablets). */
  val tileColumns: Int = 0,
  val widgetDesign: WidgetDesign = WidgetDesign.APP,
  /** Widgets with a see-through background. */
  val widgetTranslucent: Boolean = false,
  /** Tab shown at the start: the one open when the app was closed. */
  val lastTab: String = "vehicle",
  /** The vehicle tab got the former actions tab's tiles appended (1.9.2). */
  val vehicleTilesMerged: Boolean = false,
  val wearActionIds: List<String> = emptyList(),
  /** Tiles on the watch's first screen: 4 (round 1/2/1, square 2×2) or 7 (round 2/3/2, square 3×3 = 9). */
  val wearGrid: Int = 4,
  /** Actions offered by the watch-face complications "ATA action 1 … 4". */
  val wearComplicationIds: List<String> = WearContract.DEFAULT_COMPLICATION_IDS,
  /**
   * Actions with a fixed value ("set_temperature|21"), published as app shortcuts so Samsung Routines and the
   * icon menu can run them without asking for the value.
   */
  val presets: List<String> = emptyList(),
  val launcherMenuActionIds: List<String> = listOf(
    AtaAction.OPEN_TESLA_APP.id,
    AtaAction.NAVIGATE_HOME.id,
    AtaAction.OPEN_CHARGING.id,
    AtaAction.OPEN_CLIMATE.id,
  ),
  /**
   * Extra actions published as app shortcuts (after the 4 icon-menu ones) so Samsung Routines can pick them.
   * Launchers show only the first 4 by rank in the icon menu.
   */
  val routineActionIds: List<String> = emptyList(),
  val widgetActionIds: List<String> = listOf(
    AtaAction.OPEN_TESLA_APP.id,
    AtaAction.NAVIGATE_HOME.id,
  ),
)

/** 120 slider steps from 5 min to 3 h: 1-minute steps up to an hour, coarser after that. */
object StatusIntervals {
  val steps: List<Int> = buildList {
    for (m in 5..60) add(m) // 56 steps of 1 min
    for (m in 62..120 step 2) add(m) // 30 steps of 2 min
    for (i in 1..34) add(120 + Math.round(i * 60f / 34)) // 34 steps up to 3 h
  }

  fun indexOf(minutes: Int): Int = steps.indices.minByOrNull { kotlin.math.abs(steps[it] - minutes) } ?: 0
}
