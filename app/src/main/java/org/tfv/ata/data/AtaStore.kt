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
import org.tfv.ata.core.SettingsBackup
import org.tfv.ata.core.TriggerSource
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
  private val keyNfcToken = stringPreferencesKey("nfc_token")

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

  /** Per-install secret carried by ATA's own shortcuts, widgets and (copied by the user) Tasker tasks. */
  suspend fun triggerToken(): String = tokenFor(keyToken)

  /** Separate secret for NFC tags (anyone holding a tag can read it), so tags can be revoked on their own. */
  suspend fun nfcToken(): String = tokenFor(keyNfcToken)

  suspend fun rotateTriggerToken(): String = newToken().also { t -> context.dataStore.edit { it[keyToken] = t } }

  suspend fun rotateNfcToken(): String = newToken().also { t -> context.dataStore.edit { it[keyNfcToken] = t } }

  /**
   * Decides the trust level of an incoming request. [extraToken] comes from intent extras (shortcuts, widgets,
   * Tasker), [uriToken] from an `ata://run?token=…` URI (NFC tags). Never trust a source named by the caller.
   */
  suspend fun resolveSource(extraToken: String?, uriToken: String?): TriggerSource {
    val install = triggerToken()
    if (matches(extraToken, install)) return TriggerSource.TRUSTED
    if (matches(uriToken, nfcToken()) || matches(uriToken, install)) return TriggerSource.NFC
    return TriggerSource.EXTERNAL
  }

  private fun matches(candidate: String?, secret: String): Boolean =
    !candidate.isNullOrBlank() &&
      java.security.MessageDigest.isEqual(candidate.toByteArray(Charsets.UTF_8), secret.toByteArray(Charsets.UTF_8))

  private suspend fun tokenFor(key: androidx.datastore.preferences.core.Preferences.Key<String>): String {
    val existing = context.dataStore.data.map { it[key] }.first()
    if (!existing.isNullOrBlank()) return existing
    val token = newToken()
    context.dataStore.edit { it[key] = token }
    return token
  }

  private fun newToken(): String {
    val bytes = ByteArray(24).also { java.security.SecureRandom().nextBytes(it) }
    return bytes.joinToString("") { "%02x".format(it) }
  }

  /**
   * One edit for a settings backup: vehicles, the whitelisted settings, then the selection.
   * Tokens and snapshots stay untouched. A null result from [block] writes nothing.
   */
  suspend fun importLayout(
    block: (AtaSettings, List<Vehicle>, String?) -> SettingsBackup.Write?,
  ): Boolean {
    var wrote = false
    context.dataStore.edit { prefs ->
      val current = prefs[keySettings]?.let {
        runCatching { json.decodeFromString<AtaSettings>(it) }.getOrNull()
      } ?: AtaSettings()
      val list = prefs[keyVehicles]?.let {
        runCatching { json.decodeFromString(ListSerializer(Vehicle.serializer()), it) }.getOrNull()
      } ?: emptyList()
      val write = block(current, list, prefs[keySelected]) ?: return@edit
      prefs[keyVehicles] = json.encodeToString(ListSerializer(Vehicle.serializer()), write.vehicles)
      prefs[keySettings] = json.encodeToString(AtaSettings.serializer(), write.settings)
      val selected = write.selectedId
      if (selected == null) prefs.remove(keySelected) else prefs[keySelected] = selected
      wrote = true
    }
    return wrote
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
