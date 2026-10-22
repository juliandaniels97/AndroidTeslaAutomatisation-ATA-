package org.tfv.ata.core.ble

import org.tfv.ata.core.AtaAction

internal data class Command(val domain: Int, val payload: ByteArray)

internal object Commands {

  fun forAction(action: AtaAction, value: Int?): Command? = when (action) {
    AtaAction.WAKE -> vcsecRke(30)
    AtaAction.LOCK -> vcsecRke(1)
    AtaAction.UNLOCK -> vcsecRke(0)
    AtaAction.TRUNK -> vcsecClosure(field = 5, move = 1)
    AtaAction.FRUNK -> vcsecClosure(field = 6, move = 1)

    AtaAction.CLIMATE_ON -> infotainment(10, buildProto { bool(1, true) })
    AtaAction.CLIMATE_OFF -> infotainment(10, buildProto { bool(1, false) })
    AtaAction.STEERING_WHEEL_HEATER -> infotainment(13, buildProto { bool(1, true) })
    AtaAction.CHARGE_START -> infotainment(6, buildProto { message(2, ByteArray(0)) })
    AtaAction.CHARGE_STOP -> infotainment(6, buildProto { message(5, ByteArray(0)) })
    AtaAction.CHARGE_LIMIT -> value?.let { infotainment(5, buildProto { varint(1, it.toLong()) }) }
    AtaAction.CHARGING_AMPS -> value?.let { infotainment(43, buildProto { varint(1, it.toLong()) }) }
    AtaAction.SET_TEMPERATURE -> value?.let {
      infotainment(14, buildProto { float(6, it.toFloat()); float(7, it.toFloat()) })
    }
    AtaAction.DEFROST -> infotainment(12, buildProto { bool(1, true) })
    AtaAction.CHARGE_PORT_OPEN -> infotainment(62, ByteArray(0))
    AtaAction.CHARGE_PORT_CLOSE -> infotainment(61, ByteArray(0))
    AtaAction.VENT_WINDOWS -> infotainment(34, buildProto { message(3, ByteArray(0)) })
    AtaAction.CLOSE_WINDOWS -> infotainment(34, buildProto { message(4, ByteArray(0)) })
    AtaAction.HONK -> infotainment(27, ByteArray(0))
    AtaAction.FLASH_LIGHTS -> infotainment(26, ByteArray(0))
    AtaAction.SENTRY_ON -> infotainment(30, buildProto { bool(1, true) })
    AtaAction.SENTRY_OFF -> infotainment(30, buildProto { bool(1, false) })
    AtaAction.CLIMATE_KEEP -> infotainment(44, buildProto { enum(1, 1) })
    AtaAction.DOG_MODE -> infotainment(44, buildProto { enum(1, 2) })
    AtaAction.CAMP_MODE -> infotainment(44, buildProto { enum(1, 3) })
    AtaAction.CLIMATE_KEEP_OFF -> infotainment(44, buildProto { enum(1, 0) })
    AtaAction.DEFROST_OFF -> infotainment(12, buildProto { bool(1, false) })
    AtaAction.STEERING_WHEEL_HEATER_OFF -> infotainment(13, buildProto { bool(1, false) })
    AtaAction.BIOWEAPON_ON -> infotainment(35, buildProto { bool(1, true) })
    AtaAction.BIOWEAPON_OFF -> infotainment(35, buildProto { bool(1, false) })
    AtaAction.OVERHEAT_PROTECTION_ON -> infotainment(50, buildProto { bool(1, true) })
    AtaAction.OVERHEAT_PROTECTION_OFF -> infotainment(50, buildProto { bool(1, false) })
    // ClosureMoveRequest: 1 front driver door, 2 front passenger door, 5 rear trunk; move type 3 OPEN, 4 CLOSE.
    // Opening a door releases its latch; not every model supports it.
    AtaAction.DOOR_DRIVER_OPEN -> vcsecClosure(field = 1, move = 3)
    AtaAction.DOOR_PASSENGER_OPEN -> vcsecClosure(field = 2, move = 3)
    AtaAction.TRUNK_CLOSE -> vcsecClosure(field = 5, move = 4)
    // RKE_ACTION_REMOTE_DRIVE: keyless driving for about two minutes.
    AtaAction.REMOTE_DRIVE -> vcsecRke(20)
    AtaAction.MEDIA_TOGGLE -> infotainment(15, ByteArray(0))
    AtaAction.MEDIA_NEXT -> infotainment(19, ByteArray(0))
    AtaAction.MEDIA_PREVIOUS -> infotainment(20, ByteArray(0))
    // BoomboxAction (external pedestrian speaker): sound 0 = fart, 2000 = locate ping.
    AtaAction.BOOMBOX_FART -> infotainment(65, buildProto { varint(1, 0) })
    AtaAction.BOOMBOX_LOCATE -> infotainment(65, buildProto { varint(1, 2000) })
    else -> null
  }

  private fun vcsecRke(action: Int) =
    Command(Domain.VEHICLE_SECURITY, buildProto { enum(2, action) })

  private fun vcsecClosure(field: Int, move: Int) =
    Command(
      Domain.VEHICLE_SECURITY,
      buildProto { message(4, buildProto { enum(field, move) }) },
    )

  private fun infotainment(vehicleActionField: Int, submessage: ByteArray) =
    Command(
      Domain.INFOTAINMENT,
      buildProto { message(2, buildProto { message(vehicleActionField, submessage) }) },
    )

  fun statusRequest(): Command =
    Command(Domain.VEHICLE_SECURITY, buildProto { message(1, buildProto { enum(1, 0) }) })

  /** GetVehicleData categories (car_server.proto): charge, climate, closures. */
  const val DATA_CHARGE = 2
  const val DATA_CLIMATE = 3
  const val DATA_CLOSURES = 8
  val STATUS_CATEGORIES = listOf(DATA_CHARGE, DATA_CLIMATE, DATA_CLOSURES)

  /**
   * Vehicle data for one category. Over BLE the whole answer must fit in one message: asking for several
   * categories at once fails with fault 25 (response size exceeds MTU), so callers ask one by one.
   */
  fun vehicleData(category: Int): Command =
    Command(
      Domain.INFOTAINMENT,
      buildProto {
        message(2, buildProto {
          message(1, buildProto { message(category, ByteArray(0)) })
        })
      },
    )
}
