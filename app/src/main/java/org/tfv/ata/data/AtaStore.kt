package org.tfv.ata.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.tfv.ata.core.AtaSettings
import org.tfv.ata.core.Vehicle
import org.tfv.ata.core.VehicleSnapshot

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("ata")

class AtaStore(private val context: Context) {
  private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

  private val keySettings = stringPreferencesKey("settings")
  private val keyVehicles = stringPreferencesKey("vehicles")
  private val keySelected = stringPreferencesKey("selected_vehicle")
  private val keySnapshotPrefix = "snapshot_"
  private val keyToken = stringPreferencesKey("trigger_token")

  val settings: Flow<AtaSettings> = context.dataStore.data.map { prefs ->
    prefs[keySettings]?.let { runCatching { json.decodeFromString<AtaSettings>(it) }.getOrNull() }
      ?: AtaSettings()
  }

  val vehicles: Flow<List<Vehicle>> = context.dataStore.data.map { prefs ->
    prefs[keyVehicles]?.let {
      runCatching { json.decodeFromString(ListSerializer(Vehicle.serializer()), it) }.getOrNull()
    } ?: emptyList()
  }

  val selectedVehicleId: Flow<String?> = context.dataStore.data.map { it[keySelected] }

  suspend fun updateSettings(transform: (AtaSettings) -> AtaSettings) {
    context.dataStore.edit { prefs ->
      val current = prefs[keySettings]?.let {
        runCatching { json.decodeFromString<AtaSettings>(it) }.getOrNull()
      } ?: AtaSettings()
      prefs[keySettings] = json.encodeToString(AtaSettings.serializer(), transform(current))
    }
  }

  suspend fun saveVehicles(list: List<Vehicle>) {
    context.dataStore.edit {
      it[keyVehicles] = json.encodeToString(ListSerializer(Vehicle.serializer()), list)
    }
  }

  suspend fun selectVehicle(id: String?) {
    context.dataStore.edit {
      if (id == null) it.remove(keySelected) else it[keySelected] = id
    }
  }

  fun snapshot(vehicleId: String): Flow<VehicleSnapshot?> =
    context.dataStore.data.map { prefs ->
      prefs[stringPreferencesKey(keySnapshotPrefix + vehicleId)]?.let {
        runCatching { json.decodeFromString<VehicleSnapshot>(it) }.getOrNull()
      }
    }

  suspend fun triggerToken(): String {
    val existing = context.dataStore.data.map { it[keyToken] }.first()
    if (!existing.isNullOrBlank()) return existing
    val token = java.util.UUID.randomUUID().toString().replace("-", "")
    context.dataStore.edit { it[keyToken] = token }
    return token
  }

  suspend fun saveSnapshot(vehicleId: String, snapshot: VehicleSnapshot) {
    context.dataStore.edit {
      it[stringPreferencesKey(keySnapshotPrefix + vehicleId)] =
        json.encodeToString(VehicleSnapshot.serializer(), snapshot)
    }
  }

  companion object {
    @Volatile private var instance: AtaStore? = null
    fun get(context: Context): AtaStore =
      instance ?: synchronized(this) {
        instance ?: AtaStore(context.applicationContext).also { instance = it }
      }
  }
}
