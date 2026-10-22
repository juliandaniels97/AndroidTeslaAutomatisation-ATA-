package org.tfv.ata.ui.components.car

import org.tfv.ata.core.AtaAction

/** What the car drawing shows besides its status: doors and lids that were opened, lights, heating … */
enum class CarEffect {
  DOOR_DRIVER, DOOR_PASSENGER, TRUNK, FRUNK, CHARGE_PORT, WINDOWS, DEFROST, WHEEL_HEAT, BIOWEAPON, CLIMATE,
  // One-shot effects:
  LIGHTS, HONK, WAKE, LOCK, UNLOCK, PUFF, LOCATE,
}

/** A part that is open / on: since when, and until when it is shown (the car doesn't report every part back). */
data class FxToggle(val on: Boolean, val since: Long, val until: Long = Long.MAX_VALUE)

data class CarFxState(
  val toggles: Map<CarEffect, FxToggle> = emptyMap(),
  val pulses: Map<CarEffect, Long> = emptyMap(),
) {
  /** 0 … 1 how far a part is open, animated over [durationMs]. */
  fun openness(effect: CarEffect, now: Long, durationMs: Long = 700): Float {
    val t = toggles[effect] ?: return 0f
    val on = t.on && now < t.until
    val changedAt = if (t.on && now >= t.until) t.until else t.since
    val p = ((now - changedAt).toFloat() / durationMs).coerceIn(0f, 1f)
    val eased = p * p * (3 - 2 * p)
    return if (on) eased else if (t.on || t.since > 0) 1f - eased else 0f
  }

  /** Progress 0 … 1 of a one-shot effect, or null when it isn't running. */
  fun pulse(effect: CarEffect, now: Long, durationMs: Long): Float? {
    val start = pulses[effect] ?: return null
    val p = (now - start).toFloat() / durationMs
    return if (p in 0f..1f) p else null
  }

  fun with(action: AtaAction, now: Long): CarFxState {
    fun on(e: CarEffect, holdMs: Long = Long.MAX_VALUE) =
      copy(toggles = toggles + (e to FxToggle(true, now, if (holdMs == Long.MAX_VALUE) Long.MAX_VALUE else now + holdMs)))
    fun off(e: CarEffect): CarFxState {
      val current = toggles[e] ?: return this
      if (!current.on || now >= current.until) return this
      return copy(toggles = toggles + (e to FxToggle(false, now)))
    }
    fun pulse(e: CarEffect) = copy(pulses = pulses + (e to now))
    return when (action) {
      AtaAction.DOOR_DRIVER_OPEN -> on(CarEffect.DOOR_DRIVER, 12_000)
      AtaAction.DOOR_PASSENGER_OPEN -> on(CarEffect.DOOR_PASSENGER, 12_000)
      AtaAction.TRUNK -> on(CarEffect.TRUNK, 20_000)
      AtaAction.TRUNK_CLOSE -> off(CarEffect.TRUNK)
      AtaAction.FRUNK -> on(CarEffect.FRUNK, 20_000)
      AtaAction.CHARGE_PORT_OPEN -> on(CarEffect.CHARGE_PORT, 120_000)
      AtaAction.CHARGE_PORT_CLOSE -> off(CarEffect.CHARGE_PORT)
      AtaAction.VENT_WINDOWS -> on(CarEffect.WINDOWS)
      AtaAction.CLOSE_WINDOWS -> off(CarEffect.WINDOWS)
      AtaAction.DEFROST -> on(CarEffect.DEFROST, 15 * 60_000)
      AtaAction.DEFROST_OFF -> off(CarEffect.DEFROST)
      AtaAction.STEERING_WHEEL_HEATER -> on(CarEffect.WHEEL_HEAT, 15 * 60_000)
      AtaAction.STEERING_WHEEL_HEATER_OFF -> off(CarEffect.WHEEL_HEAT)
      AtaAction.BIOWEAPON_ON -> on(CarEffect.BIOWEAPON)
      AtaAction.BIOWEAPON_OFF -> off(CarEffect.BIOWEAPON)
      AtaAction.CLIMATE_ON, AtaAction.CLIMATE_KEEP, AtaAction.DOG_MODE, AtaAction.CAMP_MODE -> on(CarEffect.CLIMATE)
      AtaAction.CLIMATE_OFF -> off(CarEffect.CLIMATE)
      AtaAction.FLASH_LIGHTS -> pulse(CarEffect.LIGHTS)
      AtaAction.HONK -> pulse(CarEffect.HONK)
      AtaAction.WAKE -> pulse(CarEffect.WAKE)
      AtaAction.LOCK -> pulse(CarEffect.LOCK)
      AtaAction.UNLOCK -> pulse(CarEffect.UNLOCK)
      AtaAction.BOOMBOX_FART -> pulse(CarEffect.PUFF)
      AtaAction.BOOMBOX_LOCATE -> pulse(CarEffect.LOCATE)
      else -> this
    }
  }
}
