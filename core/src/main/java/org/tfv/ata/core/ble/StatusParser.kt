package org.tfv.ata.core.ble

import org.tfv.ata.core.VehicleSnapshot

internal object StatusParser {
  /**
   * Merges the answers of one-category requests into one Response: the VehicleData of each answer holds a
   * different field (charge 3, climate 4, closures 9), so their contents can simply be concatenated.
   */
  fun combine(responses: List<ByteArray>): ByteArray? {
    val parts = responses.mapNotNull { ProtoReader(it).bytes(2) }
    if (parts.isEmpty()) return null
    val vehicleData = parts.fold(ByteArray(0)) { acc, part -> acc + part }
    return buildProto { message(2, vehicleData) }
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

    return VehicleSnapshot(
      batteryPercent = battery,
      rangeKm = (rangeMiles * 1.609344f).toInt(),
      charging = charging,
      locked = locked,
      climateOn = climateOn,
      insideTempC = inside,
      outsideTempC = outside,
      sentry = sentry,
      updatedAt = System.currentTimeMillis(),
    )
  }
}
