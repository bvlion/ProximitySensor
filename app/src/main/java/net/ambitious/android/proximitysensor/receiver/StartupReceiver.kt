package net.ambitious.android.proximitysensor.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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
    val pref = context.getSharedPreferences(context.packageName, Activity.MODE_PRIVATE)
    // 起動フラグが立っていればサービスを起動
    when {
      pref.getBoolean(Util.ENABLE_SWITCH_PREF, false) &&
          (intent.action == Intent.ACTION_BOOT_COMPLETED ||
              intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(Intent(context, SensorService::class.java))
        } else {
          context.startService(Intent(context, SensorService::class.java))
        }
    }
  }
}
