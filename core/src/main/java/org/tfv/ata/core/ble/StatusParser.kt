package org.tfv.ata.core.ble

import org.tfv.ata.core.VehicleSnapshot

internal object StatusParser {
  /**
   * Merges the answers of one-category requests into one Response: the VehicleData of each answer holds a
   * different field (charge 3, climate 4, closures 9, media 20), so their contents can simply be concatenated.
   */
  fun combine(responses: List<ByteArray>): ByteArray? {
    val parts = responses.mapNotNull { ProtoReader(it).bytes(2) }
    if (parts.isEmpty()) return null
    val vehicleData = parts.fold(ByteArray(0)) { acc, part -> acc + part }
    return buildProto { message(2, vehicleData) }
  }

  /** Driver and passenger temperature setting from a climate answer, or null when missing. */
  fun temperatureSettings(responseBytes: ByteArray): Pair<Float, Float>? {
    val climate = ProtoReader(responseBytes).message(2)?.message(4) ?: return null
    val driver = climate.fixed32(103)?.let { Float.fromBits(it) } ?: return null
    val passenger = climate.fixed32(104)?.let { Float.fromBits(it) } ?: driver
    return driver to passenger
  }

  /** Parses a CarServer.Response carrying VehicleData (field numbers from vehicle.proto). */
  fun parse(responseBytes: ByteArray): VehicleSnapshot? {
    val response = ProtoReader(responseBytes)
    val vehicleData = response.message(2) ?: return null
    val charge = vehicleData.message(3)
    val climate = vehicleData.message(4)
    val closures = vehicleData.message(9)

    val battery = charge?.varint(114)?.toInt() ?: 0
    // battery_range (111) is what the car and the Tesla app show; est (112) / ideal (113) as fallbacks.
    val rangeMiles = (charge?.fixed32(111) ?: charge?.fixed32(112) ?: charge?.fixed32(113))
      ?.let { Float.fromBits(it) } ?: 0f
    // ChargingState is a oneof of Void messages; field 5 = Charging.
    val charging = charge?.message(1)?.has(5) == true
    val inside = climate?.fixed32(101)?.let { Float.fromBits(it) } ?: 0f
    val outside = climate?.fixed32(102)?.let { Float.fromBits(it) } ?: 0f
    val climateOn = (climate?.varint(110) ?: 0L) != 0L
    val locked = closures?.varint(113)?.let { it != 0L }
    // SentryModeState oneof: 1 Off, 2 Idle, 3 Armed, 4 Aware, 5 Panic, 6 Quiet.
    val sentryState = closures?.message(19)
    val sentry = sentryState != null && (3..6).any { sentryState.has(it) }
    val media = vehicleData.message(20)

    fun ProtoReader?.float(field: Int) = this?.fixed32(field)?.let { Float.fromBits(it) }
    fun ProtoReader?.int(field: Int) = this?.varint(field)?.toInt()
    fun ProtoReader?.flag(field: Int) = this?.varint(field)?.let { it != 0L }
    fun level(v: Int?) = v?.coerceIn(0, 3)

    val chargingState = charge?.message(1)
    // ChargingState oneof: 2 Disconnected, 3 NoPower, 4 Starting, 5 Charging, 6 Complete, 7 Stopped.
    val pluggedIn = chargingState != null && !chargingState.has(2) && (3..8).any { chargingState.has(it) }
    val voltage = charge.int(119) ?: 0
    val current = charge.int(121) ?: 0
    // charger_power is whole kW; at low AC power it reads 0, so fall back to volts × amps (× phases).
    val phases = (charge.int(134) ?: 1).coerceAtLeast(1)
    val powerKw = (charge.int(122) ?: 0).takeIf { it > 0 }
      ?: if (charging && voltage > 0 && current > 0) ((voltage * current * phases + 500) / 1000) else 0
    // ClimateKeeperMode oneof: 2 Off, 3 On, 4 Dog, 5 Party (camp).
    val keeper = climate?.message(30)?.let { k -> listOf(3, 4, 5).indexOfFirst { k.has(it) } + 1 } ?: 0
    val defrost = climate.flag(107) == true || climate?.message(35)?.has(3) == true
    // allow_cabin_overheat_protection (141) + cabin_overheat_protection (143: 0 off, 1 on, 2 fan only).
    val overheat = when {
      climate.flag(141) == false -> 0
      climate?.has(143) == true -> climate.int(143)
      climate.flag(141) == true -> 1
      else -> null
    }

    return VehicleSnapshot(
      batteryPercent = battery,
      rangeKm = (rangeMiles * 1.609344f).toInt(),
      charging = charging,
      locked = locked,
      climateOn = climateOn,
      insideTempC = inside,
      outsideTempC = outside,
      sentry = sentry,
      chargingPowerKw = if (charging) powerKw else 0,
      chargerVoltage = voltage,
      chargerCurrent = current,
      minutesToFull = charge.int(123) ?: 0,
      chargeLimit = charge.int(104),
      chargeAmpsRequest = charge.int(137),
      pluggedIn = pluggedIn || charging,
      chargePortOpen = charge.flag(127),
      chargeEnergyAddedKwh = charge.float(116) ?: 0f,
      driverTempC = climate.float(103),
      passengerTempC = climate.float(104),
      seatHeatFrontLeft = level(climate.int(113)),
      seatHeatFrontRight = level(climate.int(114)),
      seatHeatRearLeft = level(climate.int(115)),
      seatHeatRearCenter = level(climate.int(117)),
      seatHeatRearRight = level(climate.int(116)),
      seatCoolFrontLeft = level(climate.int(139)),
      seatCoolFrontRight = level(climate.int(140)),
      steeringHeater = climate.flag(125),
      defrost = defrost,
      bioweapon = climate.flag(134),
      overheatProtection = overheat,
      climateKeeper = keeper,
      doorDriverOpen = closures.flag(101) == true,
      doorRearLeftOpen = closures.flag(102) == true,
      doorPassengerOpen = closures.flag(103) == true,
      doorRearRightOpen = closures.flag(104) == true,
      frunkOpen = closures.flag(105) == true,
      trunkOpen = closures.flag(106) == true,
      windowsOpen = (107..110).any { closures.flag(it) == true },
      volume = media.float(5),
      volumeMax = media.float(7)?.takeIf { it > 0f } ?: 10f,
      nowPlaying = media?.bytes(4)?.toString(Charsets.UTF_8)?.takeIf { it.isNotBlank() },
      updatedAt = System.currentTimeMillis(),
    )
  }
}
