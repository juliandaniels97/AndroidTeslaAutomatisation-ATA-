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
  SEAT_HEATER("seat_heater", AtaCategory.FUNCTION_A, false, needsValue = true),
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
  BOOMBOX_FART("boombox_fart", AtaCategory.FUNCTION_A, true, beta = true),
  BOOMBOX_LOCATE("boombox_locate", AtaCategory.FUNCTION_A, true, beta = true);

  companion object {
    fun fromId(id: String?): AtaAction? = entries.firstOrNull { it.id == id }
  }
}
