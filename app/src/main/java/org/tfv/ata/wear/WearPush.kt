package org.tfv.ata.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import org.tfv.ata.action.ActionInfo
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.VehicleSnapshot
import org.tfv.ata.core.WearContract

object WearPush {
  fun pushConfig(context: Context, actions: List<AtaAction>, vin: String = "", vehicleName: String = "") {
    val ids = ArrayList(actions.map { it.id })
    val labels = ArrayList(actions.map { context.getString(ActionInfo.labelRes(it)) })
    val request = PutDataMapRequest.create(WearContract.PATH_CONFIG).apply {
      dataMap.putStringArrayList(WearContract.KEY_ACTION_IDS, ids)
      dataMap.putStringArrayList(WearContract.KEY_ACTION_LABELS, labels)
      dataMap.putString(WearContract.KEY_VIN, vin)
      dataMap.putString(WearContract.KEY_VEHICLE, vehicleName)
      dataMap.putLong(WearContract.KEY_UPDATED, System.currentTimeMillis())
    }.asPutDataRequest().setUrgent()
    runCatching { Wearable.getDataClient(context).putDataItem(request) }
  }

  fun pushStatus(context: Context, snapshot: VehicleSnapshot, vehicleName: String) {
    val request = PutDataMapRequest.create(WearContract.PATH_STATUS).apply {
      dataMap.putInt(WearContract.KEY_BATTERY, snapshot.batteryPercent)
      dataMap.putInt(WearContract.KEY_RANGE, snapshot.rangeKm)
      dataMap.putBoolean(WearContract.KEY_CHARGING, snapshot.charging)
      snapshot.locked?.let { dataMap.putBoolean(WearContract.KEY_LOCKED, it) } ?: dataMap.remove(WearContract.KEY_LOCKED)
      dataMap.putString(WearContract.KEY_VEHICLE, vehicleName)
      dataMap.putLong(WearContract.KEY_UPDATED, snapshot.updatedAt)
    }.asPutDataRequest().setUrgent()
    runCatching { Wearable.getDataClient(context).putDataItem(request) }
  }
}
