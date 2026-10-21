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
    AtaAction.SEAT_HEATER -> R.string.a_seat_heater
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
  }
}
