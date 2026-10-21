package org.tfv.ata.core

object IntentContract {
  const val ACTION_RUN = "org.tfv.ata.action.RUN"
  const val EXTRA_ACTION_ID = "action_id"
  const val EXTRA_VALUE = "value"
  const val EXTRA_VEHICLE_ID = "vehicle_id"
  const val EXTRA_DELAY_MS = "delay_ms"
  const val EXTRA_TOKEN = "token"

  const val ACTION_SET_ACTIVE = "org.tfv.ata.action.SET_ACTIVE"
  const val EXTRA_ACTIVE = "active"

  // Status broadcast (for Tasker / automation / Samsung Routines triggers).
  const val ACTION_STATUS = "org.tfv.ata.action.STATUS"
  const val EXTRA_BATTERY = "battery_percent"
  const val EXTRA_RANGE_KM = "range_km"
  const val EXTRA_CHARGING = "charging"
  const val EXTRA_LOCKED = "locked"
  const val EXTRA_CLIMATE_ON = "climate_on"
  const val EXTRA_INSIDE_C = "inside_temp_c"
  const val EXTRA_OUTSIDE_C = "outside_temp_c"
}
