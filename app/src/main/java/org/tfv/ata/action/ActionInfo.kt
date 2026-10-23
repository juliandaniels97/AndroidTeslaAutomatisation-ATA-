package org.tfv.ata.action

import org.tfv.ata.R
import org.tfv.ata.core.AtaAction

object ActionInfo {
  fun labelRes(action: AtaAction): Int = when (action) {
    AtaAction.OPEN_TESLA_APP -> R.string.a_open_tesla_app
    AtaAction.OPEN_CHARGING -> R.string.a_open_charging
    AtaAction.OPEN_CLIMATE -> R.string.a_open_climate
    AtaAction.OPEN_CONTROLS -> R.string.a_open_controls
    AtaAction.OPEN_LOCATION -> R.string.a_open_location
    AtaAction.NAVIGATE_HOME -> R.string.a_navigate_home
    AtaAction.NAVIGATE_WORK -> R.string.a_navigate_work
    AtaAction.NAVIGATE_ADDRESS -> R.string.a_navigate_address
    AtaAction.SHARE_LOCATION -> R.string.a_share_location
    AtaAction.WAKE -> R.string.a_wake
    AtaAction.LOCK -> R.string.a_lock
    AtaAction.UNLOCK -> R.string.a_unlock
    AtaAction.CLIMATE_ON -> R.string.a_climate_on
    AtaAction.CLIMATE_OFF -> R.string.a_climate_off
    AtaAction.SET_TEMPERATURE -> R.string.a_set_temperature
    AtaAction.STEERING_WHEEL_HEATER -> R.string.a_steering_wheel_heater
    AtaAction.DEFROST -> R.string.a_defrost
    AtaAction.CHARGE_START -> R.string.a_charge_start
    AtaAction.CHARGE_STOP -> R.string.a_charge_stop
    AtaAction.CHARGE_LIMIT -> R.string.a_charge_limit
    AtaAction.CHARGING_AMPS -> R.string.a_charging_amps
    AtaAction.CHARGE_PORT_OPEN -> R.string.a_charge_port_open
    AtaAction.CHARGE_PORT_CLOSE -> R.string.a_charge_port_close
    AtaAction.TRUNK -> R.string.a_trunk
    AtaAction.FRUNK -> R.string.a_frunk
    AtaAction.VENT_WINDOWS -> R.string.a_vent_windows
    AtaAction.CLOSE_WINDOWS -> R.string.a_close_windows
    AtaAction.HONK -> R.string.a_honk
    AtaAction.FLASH_LIGHTS -> R.string.a_flash_lights
    AtaAction.SENTRY_ON -> R.string.a_sentry_on
    AtaAction.SENTRY_OFF -> R.string.a_sentry_off
    AtaAction.CLIMATE_KEEP -> R.string.a_climate_keep
    AtaAction.DOG_MODE -> R.string.a_dog_mode
    AtaAction.CAMP_MODE -> R.string.a_camp_mode
    AtaAction.CLIMATE_KEEP_OFF -> R.string.a_climate_keep_off
    AtaAction.DEFROST_OFF -> R.string.a_defrost_off
    AtaAction.STEERING_WHEEL_HEATER_OFF -> R.string.a_steering_wheel_heater_off
    AtaAction.BIOWEAPON_ON -> R.string.a_bioweapon_on
    AtaAction.BIOWEAPON_OFF -> R.string.a_bioweapon_off
    AtaAction.OVERHEAT_PROTECTION_ON -> R.string.a_overheat_protection_on
    AtaAction.OVERHEAT_PROTECTION_OFF -> R.string.a_overheat_protection_off
    AtaAction.DOOR_DRIVER_OPEN -> R.string.a_door_driver_open
    AtaAction.DOOR_PASSENGER_OPEN -> R.string.a_door_passenger_open
    AtaAction.TRUNK_CLOSE -> R.string.a_trunk_close
    AtaAction.REMOTE_DRIVE -> R.string.a_remote_drive
    AtaAction.MEDIA_TOGGLE -> R.string.a_media_toggle
    AtaAction.MEDIA_NEXT -> R.string.a_media_next
    AtaAction.MEDIA_PREVIOUS -> R.string.a_media_previous
    AtaAction.SEAT_HEATER_FRONT_LEFT -> R.string.a_seat_heater_fl
    AtaAction.SEAT_HEATER_FRONT_RIGHT -> R.string.a_seat_heater_fr
    AtaAction.SEAT_HEATER_REAR_LEFT -> R.string.a_seat_heater_rl
    AtaAction.SEAT_HEATER_REAR_CENTER -> R.string.a_seat_heater_rc
    AtaAction.SEAT_HEATER_REAR_RIGHT -> R.string.a_seat_heater_rr
    AtaAction.SEAT_COOLER_FRONT_LEFT -> R.string.a_seat_cooler_fl
    AtaAction.SEAT_COOLER_FRONT_RIGHT -> R.string.a_seat_cooler_fr
    AtaAction.SET_VOLUME -> R.string.a_set_volume
    AtaAction.VOLUME_UP -> R.string.a_volume_up
    AtaAction.VOLUME_DOWN -> R.string.a_volume_down
    AtaAction.SET_TEMPERATURE_DRIVER -> R.string.a_set_temperature_driver
    AtaAction.SET_TEMPERATURE_PASSENGER -> R.string.a_set_temperature_passenger
    AtaAction.OVERHEAT_PROTECTION_FAN_ONLY -> R.string.a_overheat_protection_fan_only
  }
}
