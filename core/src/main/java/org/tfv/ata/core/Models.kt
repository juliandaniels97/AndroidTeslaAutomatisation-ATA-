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
  val locked: Boolean = true,
  val climateOn: Boolean = false,
  val insideTempC: Float = 0f,
  val outsideTempC: Float = 0f,
  val sentry: Boolean = false,
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
  val nfcKeyCardRequireUnlock: Boolean = true,
  val carLat: Double = 0.0,
  val carLng: Double = 0.0,
  val carLocAt: Long = 0,
  val proximityUnlock: Boolean = false,
  val proximityMeters: Int = 30,
  val homeActionIds: List<String> = emptyList(),
  val vehicleActionIds: List<String> = emptyList(),
  val wearActionIds: List<String> = emptyList(),
  val launcherMenuActionIds: List<String> = listOf(
    AtaAction.OPEN_TESLA_APP.id,
    AtaAction.NAVIGATE_HOME.id,
    AtaAction.OPEN_CHARGING.id,
    AtaAction.OPEN_CLIMATE.id,
  ),
  val widgetActionIds: List<String> = listOf(
    AtaAction.OPEN_TESLA_APP.id,
    AtaAction.NAVIGATE_HOME.id,
  ),
)
