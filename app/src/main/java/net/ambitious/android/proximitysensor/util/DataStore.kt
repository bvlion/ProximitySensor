package net.ambitious.android.proximitysensor.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStore(context: Context) {
  private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")
  private val settingsDataStore = context.dataStore

  val isEnableSensor: Flow<Boolean> = get(ENABLE_SENSOR_KEY)
  suspend fun setEnableSensor(enableSensor: Boolean) = save(ENABLE_SENSOR_KEY, enableSensor)

  val isEnableNotifyLock: Flow<Boolean> = get(ENABLE_NOTIFY_LOCK_KEY)
  suspend fun setEnableNotifyLock(enableNotifyLock: Boolean) =
    save(ENABLE_NOTIFY_LOCK_KEY, enableNotifyLock)

  val isEnableSleepDoubleTapLock: Flow<Boolean> = get(SLEEP_DOUBLE_TAP_LOCK_KEY)
  suspend fun setEnableSleepDoubleTapLock(enableSleepDoubleTapLock: Boolean) =
    save(SLEEP_DOUBLE_TAP_LOCK_KEY, enableSleepDoubleTapLock)

  private suspend fun save(key: Preferences.Key<Boolean>, value: Boolean) {
    settingsDataStore.edit {
      it[key] = value
    }
  }

  private fun get(key: Preferences.Key<Boolean>) = settingsDataStore.data.map {
    it[key] ?: false
  }

  companion object {
    private val ENABLE_SENSOR_KEY = booleanPreferencesKey("enable_sensor")
    private val ENABLE_NOTIFY_LOCK_KEY = booleanPreferencesKey("enable_notify_lock")
    private val SLEEP_DOUBLE_TAP_LOCK_KEY = booleanPreferencesKey("sleep_double_tap_lock")
  }
}