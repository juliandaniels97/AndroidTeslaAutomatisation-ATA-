package org.tfv.ata.core.ble

import org.tfv.ata.core.VehicleSnapshot

internal object StatusParser {
  fun parse(responseBytes: ByteArray): VehicleSnapshot? {
    val response = ProtoReader(responseBytes)
    val vehicleData = response.message(2) ?: return null
    val charge = vehicleData.message(3)
    val climate = vehicleData.message(4)
    val closures = vehicleData.message(9)

    val battery = charge?.varint(114)?.toInt() ?: 0
    val rangeMiles = charge?.fixed32(113)?.let { Float.fromBits(it) }
      ?: charge?.fixed32(112)?.let { Float.fromBits(it) } ?: 0f
    val charging = charge?.message(1)?.bytes(5) != null ||
      charge?.message(1)?.varint(5) != null
    val inside = climate?.fixed32(101)?.let { Float.fromBits(it) } ?: 0f
    val outside = climate?.fixed32(102)?.let { Float.fromBits(it) } ?: 0f
    val climateOn = (climate?.varint(110) ?: 0L) != 0L
    val locked = (closures?.varint(113) ?: 1L) != 0L

    return VehicleSnapshot(
      batteryPercent = battery,
      rangeKm = (rangeMiles * 1.609f).toInt(),
      charging = charging,
      locked = locked,
      climateOn = climateOn,
      insideTempC = inside,
      outsideTempC = outside,
      sentry = false,
      updatedAt = System.currentTimeMillis(),
    )
  }
}
