package org.tfv.ata.core

import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
  val id: String,
  val name: String,
  val vin: String,
  val navHome: String = "",
  val navWork: String = "",
)

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
  /** True when only VCSEC answered (car asleep): battery/range/temperatures are from an earlier reading. */
  val asleep: Boolean = false,
  val updatedAt: Long = 0,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class AtaSettings(
  val theme: ThemeMode = ThemeMode.DARK,
  val dynamicColor: Boolean = false,
  val confirmCritical: Boolean = true,
  val haptics: Boolean = true,
  val biometricLock: Boolean = false,
  val experimentalBle: Boolean = false,
  val pollIntervalMinutes: Int = 0,
  val navigationApp: NavigationApp = NavigationApp.TESLA,
  val homeAddress: String = "",
  val workAddress: String = "",
  val active: Boolean = true,
  val onboarded: Boolean = false,
  val directionFx: Boolean = false,
  val nfcKeyCard: Boolean = false,
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
  val proximityUnlock: Boolean = false,
  val proximityMeters: Int = 30,
  val homeActionIds: List<String> = emptyList(),
  val vehicleActionIds: List<String> = emptyList(),
  val wearActionIds: List<String> = emptyList(),
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
