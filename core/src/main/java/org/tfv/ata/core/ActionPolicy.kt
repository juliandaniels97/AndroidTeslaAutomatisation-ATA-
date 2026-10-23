package org.tfv.ata.core

/** Where a command request came from. Decided by ATA itself, never taken from an intent extra. */
enum class TriggerSource {
  /** A tap inside the ATA app (the UI already ran its confirmation / biometric flow). */
  IN_APP,

  /** The user confirmed the command on ATA's confirmation screen. */
  CONFIRMED,

  /** ATA's own shortcuts, widgets, Samsung Routines (via those shortcuts) or Tasker with the token. */
  TRUSTED,

  /** ATA's own watch app (same signing key); critical actions are confirmed on the watch. */
  WATCH,

  /** An NFC tag written by ATA (its token is readable by anyone holding the tag). */
  NFC,

  /** Proximity unlock: ATA itself noticed the phone walking up to the car (only ever unlocks). */
  PROXIMITY,

  /** Anything else: other apps, links, foreign tags. */
  EXTERNAL,
}

sealed interface PolicyDecision {
  data object Allow : PolicyDecision

  /** Ask the user first (notification → confirmation screen). */
  data object Confirm : PolicyDecision

  data class Deny(val reason: DenyReason) : PolicyDecision
}

enum class DenyReason { INACTIVE, BLE_DISABLED, PHONE_LOCKED, NOT_ALLOWED_EXTERNAL }

/**
 * Who may run what. BLE ("Function A") actions are gated by source, lock state and the automation
 * settings; Basic ("Function B") actions only open apps/links and are always allowed while ATA is active.
 */
object ActionPolicy {

  /** Actions that physically open the car (or switch its guard off). */
  val critical: Set<AtaAction> = setOf(
    AtaAction.UNLOCK,
    AtaAction.CHARGE_PORT_OPEN,
    AtaAction.TRUNK,
    AtaAction.FRUNK,
    AtaAction.VENT_WINDOWS,
    AtaAction.SENTRY_OFF,
    AtaAction.DOOR_DRIVER_OPEN,
    AtaAction.DOOR_PASSENGER_OPEN,
    AtaAction.REMOTE_DRIVE,
  )

  fun isCritical(action: AtaAction): Boolean = action in critical

  fun decide(
    action: AtaAction,
    source: TriggerSource,
    phoneLocked: Boolean,
    settings: AtaSettings,
  ): PolicyDecision {
    if (!settings.active && source != TriggerSource.IN_APP && source != TriggerSource.CONFIRMED) {
      return PolicyDecision.Deny(DenyReason.INACTIVE)
    }
    if (action.category == AtaCategory.FUNCTION_B) return PolicyDecision.Allow
    if (!settings.experimentalBle) return PolicyDecision.Deny(DenyReason.BLE_DISABLED)

    val critical = isCritical(action)
    return when (source) {
      TriggerSource.IN_APP, TriggerSource.CONFIRMED -> PolicyDecision.Allow

      TriggerSource.WATCH ->
        if (critical || !phoneLocked || settings.automationWhenLocked) PolicyDecision.Allow
        else PolicyDecision.Deny(DenyReason.PHONE_LOCKED)

      TriggerSource.TRUSTED ->
        if (!critical) {
          if (!phoneLocked || settings.automationWhenLocked) PolicyDecision.Allow
          else PolicyDecision.Deny(DenyReason.PHONE_LOCKED)
        } else if (action.id in settings.criticalDirectIds && (!phoneLocked || settings.criticalDirectWhenLocked)) {
          PolicyDecision.Allow
        } else {
          PolicyDecision.Confirm
        }

      // Walking up to the car: unlock straight away while the phone is unlocked (or when allowed while locked),
      // otherwise ask. Nothing but unlocking is ever triggered this way.
      TriggerSource.PROXIMITY ->
        if (action != AtaAction.UNLOCK || !settings.proximityUnlock) PolicyDecision.Deny(DenyReason.NOT_ALLOWED_EXTERNAL)
        else if (!phoneLocked || settings.proximityWhenLocked) PolicyDecision.Allow
        else PolicyDecision.Confirm

      // A tag can be read and copied by anyone: never open the car straight from a tag.
      TriggerSource.NFC ->
        if (critical) PolicyDecision.Confirm
        else if (!phoneLocked || settings.automationWhenLocked) PolicyDecision.Allow
        else PolicyDecision.Deny(DenyReason.PHONE_LOCKED)

      TriggerSource.EXTERNAL ->
        if (critical || action.id !in settings.externalActionIds) PolicyDecision.Deny(DenyReason.NOT_ALLOWED_EXTERNAL)
        else if (phoneLocked) PolicyDecision.Deny(DenyReason.PHONE_LOCKED)
        else PolicyDecision.Allow
    }
  }

  /** Allowed value range for actions that take a number, or null if the action takes free text / nothing. */
  fun valueRange(action: AtaAction): IntRange? = when (action) {
    AtaAction.CHARGE_LIMIT -> 50..100
    AtaAction.CHARGING_AMPS -> 1..48
    AtaAction.SET_TEMPERATURE, AtaAction.SET_TEMPERATURE_DRIVER, AtaAction.SET_TEMPERATURE_PASSENGER -> 15..28
    AtaAction.SEAT_HEATER_FRONT_LEFT, AtaAction.SEAT_HEATER_FRONT_RIGHT, AtaAction.SEAT_HEATER_REAR_LEFT,
    AtaAction.SEAT_HEATER_REAR_CENTER, AtaAction.SEAT_HEATER_REAR_RIGHT,
    AtaAction.SEAT_COOLER_FRONT_LEFT, AtaAction.SEAT_COOLER_FRONT_RIGHT -> 0..3
    AtaAction.SET_VOLUME -> 0..100
    AtaAction.SEAT_AUTO_FRONT_LEFT, AtaAction.SEAT_AUTO_FRONT_RIGHT -> 0..1
    else -> null
  }

  /** Parses and range-checks a numeric value; null if the action needs a number and [value] isn't a valid one. */
  fun parseValue(action: AtaAction, value: String?): Int? {
    val range = valueRange(action) ?: return null
    val number = value?.trim()?.replace(',', '.')?.toDoubleOrNull()?.let { Math.round(it).toInt() } ?: return null
    return number.takeIf { it in range }
  }
}
