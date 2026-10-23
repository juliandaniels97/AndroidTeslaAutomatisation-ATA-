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
  // Setting a level by hand ends the automatic mode of that seat (as in the car).
  AtaAction.SEAT_HEATER_FRONT_LEFT -> copy(seatHeatFrontLeft = value ?: seatHeatFrontLeft, seatAutoFrontLeft = seatAutoFrontLeft?.let { false })
  AtaAction.SEAT_HEATER_FRONT_RIGHT -> copy(seatHeatFrontRight = value ?: seatHeatFrontRight, seatAutoFrontRight = seatAutoFrontRight?.let { false })
  AtaAction.SEAT_AUTO_FRONT_LEFT -> copy(seatAutoFrontLeft = value == 1)
  AtaAction.SEAT_AUTO_FRONT_RIGHT -> copy(seatAutoFrontRight = value == 1)
  AtaAction.SEAT_HEATER_REAR_LEFT -> copy(seatHeatRearLeft = value ?: seatHeatRearLeft)
  AtaAction.SEAT_HEATER_REAR_CENTER -> copy(seatHeatRearCenter = value ?: seatHeatRearCenter)
  AtaAction.SEAT_HEATER_REAR_RIGHT -> copy(seatHeatRearRight = value ?: seatHeatRearRight)
  AtaAction.SEAT_COOLER_FRONT_LEFT -> copy(seatCoolFrontLeft = value ?: seatCoolFrontLeft, seatAutoFrontLeft = seatAutoFrontLeft?.let { false })
  AtaAction.SEAT_COOLER_FRONT_RIGHT -> copy(seatCoolFrontRight = value ?: seatCoolFrontRight, seatAutoFrontRight = seatAutoFrontRight?.let { false })
  AtaAction.STEERING_WHEEL_HEATER -> copy(steeringHeater = true, steeringAuto = steeringAuto?.let { false })
  AtaAction.STEERING_WHEEL_HEATER_OFF -> copy(steeringHeater = false, steeringLevel = 0, steeringAuto = steeringAuto?.let { false })
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

/**
 * The charging state carried forward to [now] from the last reading: time to full counts down, and battery,
 * range and energy added rise with the reported power (roughly: the rate comes from the reading's level, limit
 * and time to full). Readings without charging, or older than a day, are returned unchanged.
 */
fun VehicleSnapshot.projected(now: Long): VehicleSnapshot {
  if (!charging || updatedAt <= 0 || now <= updatedAt) return this
  val minutes = (now - updatedAt) / 60_000f
  if (minutes < .5f || minutes > 24 * 60) return this
  val limit = (chargeLimit ?: 100).coerceIn(batteryPercent, 100)
  val ratePerMinute = if (minutesToFull > 0) (limit - batteryPercent).toFloat() / minutesToFull else 0f
  val left = (minutesToFull - minutes).coerceAtLeast(0f)
  val battery = (batteryPercent + ratePerMinute * minutes).coerceAtMost(limit.toFloat())
  val range = if (batteryPercent > 0) (rangeKm * battery / batteryPercent).toInt() else rangeKm
  val done = minutesToFull > 0 && left <= 0f
  return copy(
    batteryPercent = battery.toInt(),
    rangeKm = range,
    minutesToFull = left.toInt(),
    chargeEnergyAddedKwh = chargeEnergyAddedKwh + chargingPowerKw * (if (done) minutesToFull.toFloat() else minutes) / 60f,
    charging = !done,
    chargingPowerKw = if (done) 0 else chargingPowerKw,
  )
}

/** Demo mode: a made-up car, connected and charging, used instead of Bluetooth (Settings → Demo mode). */
object DemoCar {
  const val VEHICLE_ID = "demo"
  val vehicle = Vehicle(id = VEHICLE_ID, name = "Demo Model Y", vin = "7SAYGDEE1RA000001")

  /** The state the demo always starts from: 50 %, charging at 22 kW to 80 %, 1 h left, 20 kWh added. */
  fun start(now: Long) = VehicleSnapshot(
    batteryPercent = 50,
    rangeKm = 265,
    charging = true,
    locked = true,
    climateOn = false,
    insideTempC = 19.5f,
    outsideTempC = 12f,
    sentry = false,
    chargingPowerKw = 22,
    chargerVoltage = 400,
    chargerCurrent = 32,
    minutesToFull = 60,
    chargeLimit = 80,
    chargeAmpsRequest = 32,
    pluggedIn = true,
    chargePortOpen = true,
    chargeEnergyAddedKwh = 20f,
    driverTempC = 21f,
    passengerTempC = 21f,
    seatHeatFrontLeft = 0,
    seatHeatFrontRight = 0,
    seatHeatRearLeft = 0,
    seatHeatRearCenter = 0,
    seatHeatRearRight = 0,
    seatCoolFrontLeft = 0,
    seatCoolFrontRight = 0,
    seatAutoFrontLeft = false,
    seatAutoFrontRight = false,
    steeringHeater = false,
    steeringAuto = false,
    bioweapon = false,
    overheatProtection = 1,
    volume = 4f,
    volumeMax = 11f,
    nowPlaying = "ATA Demo Radio",
    updatedAt = now,
  )
}
