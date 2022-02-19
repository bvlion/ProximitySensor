package net.ambitious.android.proximitysensor.receiver

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import net.ambitious.android.proximitysensor.util.Util

/**
 * ダブルタップでのロックBroadcastReceiver
 * @version 1.0
 * @author bvlion
 */
class DoubleTapBroadCastReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    // ダブルタップと認識したらロックする
    Util.tapCount++
    if (Util.tapCount >= 2) {
      val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
      dpm.lockNow()
    } else {
      CoroutineScope(Dispatchers.Main).launch {
        flow {
          delay(1000)
          emit(0)
        }.collect {
          Util.tapCount = it
        }
      }
    }
  }
}