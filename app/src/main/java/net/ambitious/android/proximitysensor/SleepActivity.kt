package net.ambitious.android.proximitysensor

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Bundle

/**
 * 画面ロックActivity
 * @version 1.0
 * @author bvlion
 */
class SleepActivity : Activity() {
  /** @override Activity.onCreate */
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // ロック状態を取得
    val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    // ロックして終了
    dpm.lockNow()
    finish()
  }
}