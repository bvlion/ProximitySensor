package net.ambitious.android.proximitysensor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.ambitious.android.proximitysensor.services.SensorService
import net.ambitious.android.proximitysensor.util.Util

/**
 * 起動検知BroadcastReceiver
 * @version 1.0
 * @author bvlion
 */
class StartupReceiver : BroadcastReceiver() {
  /** @override BroadcastReceiver.onReceive */
  override fun onReceive(context: Context, intent: Intent) {
    val dataStore = Util.getDataStore(context)
    CoroutineScope(Dispatchers.Main).launch {
      dataStore.isEnableSensor.collect {
        // 起動フラグが立っていればサービスを起動
        if (it && (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, SensorService::class.java))
            } else {
                context.startService(Intent(context, SensorService::class.java))
            }
        }
      }
    }
  }
}
