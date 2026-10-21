package org.tfv.ata.core

/** Shared Data Layer paths/keys for phone <-> watch sync. */
object WearContract {
  const val PATH_CONFIG = "/ata/config"
  const val PATH_STATUS = "/ata/status"
  const val MSG_RUN = "/ata/run"
  /** Phone → watch: "1|label" on success, "0|label: reason" on failure. */
  const val MSG_RESULT = "/ata/result"
  /** Capability the phone app advertises, so the watch sends commands only to the phone running ATA. */
  const val CAPABILITY_PHONE = "ata_phone"
  /** Capability the watch app advertises, so the phone can tell whether ATA is installed on the watch. */
  const val CAPABILITY_WATCH = "ata_watch"

  /** Phone → watch: one of the WATCH_CMD_* commands. The watch answers with [MSG_WATCH_STATE]. */
  const val MSG_WATCH_COMMAND = "/ata/watch/command"
  /** Watch → phone: an encoded [WatchState]. */
  const val MSG_WATCH_STATE = "/ata/watch/state"
  const val WATCH_CMD_STATE = "state"
  const val WATCH_CMD_PROBE = "probe"
  const val WATCH_CMD_PAIR = "pair"
  const val WATCH_CMD_DIRECT_ON = "direct_on"
  const val WATCH_CMD_DIRECT_OFF = "direct_off"
  const val WATCH_CMD_KEYCARD_ON = "keycard_on"
  const val WATCH_CMD_KEYCARD_OFF = "keycard_off"

  /** Watch → phone: open the watch-pairing assistant (fallback when the watch can't open it directly). */
  const val MSG_OPEN_WEAR_PAIR = "/ata/phone/open_wear_pair"
  /** Deep link the watch opens on the phone (RemoteActivityHelper) to show the watch-pairing assistant. */
  const val DEEP_LINK_WEAR_PAIR = "ata://wear-pair"

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
