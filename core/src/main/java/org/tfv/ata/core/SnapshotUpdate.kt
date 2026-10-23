package org.tfv.ata.core

/**
 * The status the car will report after [action] (with [value]) succeeded, so tiles show the new state at once
 * instead of waiting for the next status read.
 */
fun VehicleSnapshot.after(action: AtaAction, value: Int? = null): VehicleSnapshot = when (action) {
  AtaAction.LOCK -> copy(locked = true)
  AtaAction.UNLOCK -> copy(locked = false)
  AtaAction.CLIMATE_ON -> copy(climateOn = true)
  AtaAction.CLIMATE_OFF -> copy(climateOn = false, defrost = false, climateKeeper = 0)
  AtaAction.SET_TEMPERATURE -> value?.let { copy(driverTempC = it.toFloat(), passengerTempC = it.toFloat()) } ?: this
  AtaAction.SET_TEMPERATURE_DRIVER -> value?.let { copy(driverTempC = it.toFloat()) } ?: this
  AtaAction.SET_TEMPERATURE_PASSENGER -> value?.let { copy(passengerTempC = it.toFloat()) } ?: this
  AtaAction.SEAT_HEATER_FRONT_LEFT -> copy(seatHeatFrontLeft = value ?: seatHeatFrontLeft)
  AtaAction.SEAT_HEATER_FRONT_RIGHT -> copy(seatHeatFrontRight = value ?: seatHeatFrontRight)
  AtaAction.SEAT_HEATER_REAR_LEFT -> copy(seatHeatRearLeft = value ?: seatHeatRearLeft)
  AtaAction.SEAT_HEATER_REAR_CENTER -> copy(seatHeatRearCenter = value ?: seatHeatRearCenter)
  AtaAction.SEAT_HEATER_REAR_RIGHT -> copy(seatHeatRearRight = value ?: seatHeatRearRight)
  AtaAction.SEAT_COOLER_FRONT_LEFT -> copy(seatCoolFrontLeft = value ?: seatCoolFrontLeft)
  AtaAction.SEAT_COOLER_FRONT_RIGHT -> copy(seatCoolFrontRight = value ?: seatCoolFrontRight)
  AtaAction.STEERING_WHEEL_HEATER -> copy(steeringHeater = true)
  AtaAction.STEERING_WHEEL_HEATER_OFF -> copy(steeringHeater = false)
  AtaAction.DEFROST -> copy(defrost = true, climateOn = true)
  AtaAction.DEFROST_OFF -> copy(defrost = false)
  AtaAction.CHARGE_START -> copy(charging = true)
  AtaAction.CHARGE_STOP -> copy(charging = false, chargingPowerKw = 0)
  AtaAction.CHARGE_LIMIT -> copy(chargeLimit = value ?: chargeLimit)
  AtaAction.CHARGING_AMPS -> copy(chargeAmpsRequest = value ?: chargeAmpsRequest)
  AtaAction.CHARGE_PORT_OPEN -> copy(chargePortOpen = true)
  AtaAction.CHARGE_PORT_CLOSE -> copy(chargePortOpen = false)
  AtaAction.SENTRY_ON -> copy(sentry = true)
  AtaAction.SENTRY_OFF -> copy(sentry = false)
  AtaAction.CLIMATE_KEEP -> copy(climateKeeper = 1, climateOn = true)
  AtaAction.DOG_MODE -> copy(climateKeeper = 2, climateOn = true)
  AtaAction.CAMP_MODE -> copy(climateKeeper = 3, climateOn = true)
  AtaAction.CLIMATE_KEEP_OFF -> copy(climateKeeper = 0)
  AtaAction.BIOWEAPON_ON -> copy(bioweapon = true)
  AtaAction.BIOWEAPON_OFF -> copy(bioweapon = false)
  AtaAction.OVERHEAT_PROTECTION_ON -> copy(overheatProtection = 1)
  AtaAction.OVERHEAT_PROTECTION_FAN_ONLY -> copy(overheatProtection = 2)
  AtaAction.OVERHEAT_PROTECTION_OFF -> copy(overheatProtection = 0)
  AtaAction.VENT_WINDOWS -> copy(windowsOpen = true)
  AtaAction.CLOSE_WINDOWS -> copy(windowsOpen = false)
  AtaAction.FRUNK -> copy(frunkOpen = true)
  AtaAction.TRUNK -> copy(trunkOpen = !trunkOpen)
  AtaAction.TRUNK_CLOSE -> copy(trunkOpen = false)
  AtaAction.DOOR_DRIVER_OPEN -> copy(doorDriverOpen = true, locked = false)
  AtaAction.DOOR_PASSENGER_OPEN -> copy(doorPassengerOpen = true, locked = false)
  AtaAction.SET_VOLUME -> value?.let { copy(volume = it / 10f) } ?: this
  AtaAction.VOLUME_UP -> copy(volume = volume?.let { (it + 1f).coerceAtMost(volumeMax) })
  AtaAction.VOLUME_DOWN -> copy(volume = volume?.let { (it - 1f).coerceAtLeast(0f) })
  else -> this
}
