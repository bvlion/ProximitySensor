package net.ambitious.android.proximitysensor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import net.ambitious.android.proximitysensor.util.Util

class MainViewModel(app: Application) : AndroidViewModel(app) {

  private val dataStore = Util.getDataStore(getApplication<Application>().applicationContext)

  private val job = SupervisorJob()
  private val scope = CoroutineScope(Dispatchers.IO + job)

  private var firstAccess = true

  val setting: LiveData<Util.SettingModel> = combine(
    dataStore.isEnableSensor,
    dataStore.isEnableNotifyLock,
    dataStore.isEnableSleepDoubleTapLock,
    ::Triple
  ).map {
    Util.SettingModel(it.first, it.second, it.third, firstAccess)
  }.asLiveData(scope.coroutineContext)

  fun saveEnableSensor(enable: Boolean) {
    scope.launch {
      dataStore.setEnableSensor(enable)
    }
  }

  fun saveEnableNotifyLock(enable: Boolean) {
    scope.launch {
      dataStore.setEnableNotifyLock(enable)
    }
  }
    
  fun saveEnableSleepDoubleTapLock(enable: Boolean) {
    scope.launch {
      dataStore.setEnableSleepDoubleTapLock(enable)
    }
  }

  fun firstAccessed() {
    firstAccess = false
  }

  override fun onCleared() {
    super.onCleared()
    job.cancel()
  }
}