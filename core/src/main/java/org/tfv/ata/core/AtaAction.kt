package org.tfv.ata.core

enum class AtaCategory { FUNCTION_B, FUNCTION_A }

enum class AtaAction(
  val id: String,
  val category: AtaCategory,
  val enabled: Boolean,
  val needsValue: Boolean = false,
  val beta: Boolean = false,
) {
  OPEN_TESLA_APP("open_tesla_app", AtaCategory.FUNCTION_B, true),
  OPEN_CHARGING("open_charging", AtaCategory.FUNCTION_B, true, beta = true),
  OPEN_CLIMATE("open_climate", AtaCategory.FUNCTION_B, true, beta = true),
  OPEN_CONTROLS("open_controls", AtaCategory.FUNCTION_B, true, beta = true),
  OPEN_LOCATION("open_location", AtaCategory.FUNCTION_B, true, beta = true),
  NAVIGATE_HOME("navigate_home", AtaCategory.FUNCTION_B, true),
  NAVIGATE_WORK("navigate_work", AtaCategory.FUNCTION_B, true),
  NAVIGATE_ADDRESS("navigate_address", AtaCategory.FUNCTION_B, true, needsValue = true),
  SHARE_LOCATION("share_location", AtaCategory.FUNCTION_B, true, needsValue = true),

  LOCK("lock", AtaCategory.FUNCTION_A, true, beta = true),
  UNLOCK("unlock", AtaCategory.FUNCTION_A, true, beta = true),
  WAKE("wake", AtaCategory.FUNCTION_A, true, beta = true),
  CLIMATE_ON("climate_on", AtaCategory.FUNCTION_A, true, beta = true),
  CLIMATE_OFF("climate_off", AtaCategory.FUNCTION_A, true, beta = true),
  SET_TEMPERATURE("set_temperature", AtaCategory.FUNCTION_A, true, needsValue = true, beta = true),
  STEERING_WHEEL_HEATER("steering_wheel_heater", AtaCategory.FUNCTION_A, true, beta = true),
  DEFROST("defrost", AtaCategory.FUNCTION_A, true, beta = true),
  CHARGE_START("charge_start", AtaCategory.FUNCTION_A, true, beta = true),
  CHARGE_STOP("charge_stop", AtaCategory.FUNCTION_A, true, beta = true),
  CHARGE_LIMIT("charge_limit", AtaCategory.FUNCTION_A, true, needsValue = true, beta = true),
  CHARGING_AMPS("charging_amps", AtaCategory.FUNCTION_A, true, needsValue = true, beta = true),
  CHARGE_PORT_OPEN("charge_port_open", AtaCategory.FUNCTION_A, true, beta = true),
  CHARGE_PORT_CLOSE("charge_port_close", AtaCategory.FUNCTION_A, true, beta = true),
  TRUNK("trunk", AtaCategory.FUNCTION_A, true, beta = true),
  FRUNK("frunk", AtaCategory.FUNCTION_A, true, beta = true),
  VENT_WINDOWS("vent_windows", AtaCategory.FUNCTION_A, true, beta = true),
  CLOSE_WINDOWS("close_windows", AtaCategory.FUNCTION_A, true, beta = true),
  HONK("honk", AtaCategory.FUNCTION_A, true, beta = true),
  FLASH_LIGHTS("flash_lights", AtaCategory.FUNCTION_A, true, beta = true),
  SENTRY_ON("sentry_on", AtaCategory.FUNCTION_A, true, beta = true),
  SENTRY_OFF("sentry_off", AtaCategory.FUNCTION_A, true, beta = true),
  CLIMATE_KEEP("climate_keep", AtaCategory.FUNCTION_A, true, beta = true),
  DOG_MODE("dog_mode", AtaCategory.FUNCTION_A, true, beta = true),
  CAMP_MODE("camp_mode", AtaCategory.FUNCTION_A, true, beta = true),
  CLIMATE_KEEP_OFF("climate_keep_off", AtaCategory.FUNCTION_A, true, beta = true),
  DEFROST_OFF("defrost_off", AtaCategory.FUNCTION_A, true, beta = true),
  STEERING_WHEEL_HEATER_OFF("steering_wheel_heater_off", AtaCategory.FUNCTION_A, true, beta = true),
  BIOWEAPON_ON("bioweapon_on", AtaCategory.FUNCTION_A, true, beta = true),
  BIOWEAPON_OFF("bioweapon_off", AtaCategory.FUNCTION_A, true, beta = true),
  OVERHEAT_PROTECTION_ON("overheat_protection_on", AtaCategory.FUNCTION_A, true, beta = true),
  OVERHEAT_PROTECTION_OFF("overheat_protection_off", AtaCategory.FUNCTION_A, true, beta = true),
  DOOR_DRIVER_OPEN("door_driver_open", AtaCategory.FUNCTION_A, true, beta = true),
  DOOR_PASSENGER_OPEN("door_passenger_open", AtaCategory.FUNCTION_A, true, beta = true),
  TRUNK_CLOSE("trunk_close", AtaCategory.FUNCTION_A, true, beta = true),
  REMOTE_DRIVE("remote_drive", AtaCategory.FUNCTION_A, true, beta = true),
  MEDIA_TOGGLE("media_toggle", AtaCategory.FUNCTION_A, true, beta = true),
  MEDIA_NEXT("media_next", AtaCategory.FUNCTION_A, true, beta = true),
  MEDIA_PREVIOUS("media_previous", AtaCategory.FUNCTION_A, true, beta = true),
  // Seat heaters, each seat on its own: value 0 = off, 1 … 3 = level.
  SEAT_HEATER_FRONT_LEFT("seat_heater_fl", AtaCategory.FUNCTION_A, true, needsValue = true),
  SEAT_HEATER_FRONT_RIGHT("seat_heater_fr", AtaCategory.FUNCTION_A, true, needsValue = true),
  SEAT_HEATER_REAR_LEFT("seat_heater_rl", AtaCategory.FUNCTION_A, true, needsValue = true),
  SEAT_HEATER_REAR_CENTER("seat_heater_rc", AtaCategory.FUNCTION_A, true, needsValue = true),
  SEAT_HEATER_REAR_RIGHT("seat_heater_rr", AtaCategory.FUNCTION_A, true, needsValue = true),
  // Ventilated front seats: value 0 = off, 1 … 3 = level.
  SEAT_COOLER_FRONT_LEFT("seat_cooler_fl", AtaCategory.FUNCTION_A, true, needsValue = true),
  SEAT_COOLER_FRONT_RIGHT("seat_cooler_fr", AtaCategory.FUNCTION_A, true, needsValue = true),
  // Volume: value in tenths (0 … 100 = 0 … 10, the car's scale).
  SET_VOLUME("set_volume", AtaCategory.FUNCTION_A, true, needsValue = true),
  VOLUME_UP("volume_up", AtaCategory.FUNCTION_A, true),
  VOLUME_DOWN("volume_down", AtaCategory.FUNCTION_A, true),
  // Temperature for one side only (°C); the other side keeps its setting.
  SET_TEMPERATURE_DRIVER("set_temperature_driver", AtaCategory.FUNCTION_A, true, needsValue = true),
  SET_TEMPERATURE_PASSENGER("set_temperature_passenger", AtaCategory.FUNCTION_A, true, needsValue = true),
  OVERHEAT_PROTECTION_FAN_ONLY("overheat_protection_fan_only", AtaCategory.FUNCTION_A, true),
  // Automatic seat climate (heating / ventilation chosen by the car) for a front seat: value 1 = on, 0 = off.
  SEAT_AUTO_FRONT_LEFT("seat_auto_fl", AtaCategory.FUNCTION_A, true, needsValue = true),
  SEAT_AUTO_FRONT_RIGHT("seat_auto_fr", AtaCategory.FUNCTION_A, true, needsValue = true);

  companion object {
    fun fromId(id: String?): AtaAction? = entries.firstOrNull { it.id == id }
  }
}
