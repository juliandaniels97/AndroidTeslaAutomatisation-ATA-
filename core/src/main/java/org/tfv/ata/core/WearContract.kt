package org.tfv.ata.core

/** Shared Data Layer paths/keys for phone <-> watch sync. */
object WearContract {
  const val PATH_CONFIG = "/ata/config"
  const val PATH_STATUS = "/ata/status"
  const val MSG_RUN = "/ata/run"

  const val KEY_ACTION_IDS = "action_ids"
  const val KEY_ACTION_LABELS = "action_labels"
  const val KEY_UPDATED = "updated"

  /** VIN + name of the vehicle selected on the phone, so the watch can act without VIN entry. */
  const val KEY_VIN = "vin"

  const val KEY_BATTERY = "battery"
  const val KEY_RANGE = "range"
  const val KEY_CHARGING = "charging"
  const val KEY_LOCKED = "locked"
  const val KEY_VEHICLE = "vehicle"
}
