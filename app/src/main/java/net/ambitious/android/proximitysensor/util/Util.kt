package net.ambitious.android.proximitysensor.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import net.ambitious.android.proximitysensor.receiver.LockDeviceAdminReceiver

object Util {
  /** 通知エリア常駐ID */
  const val ONGOING_NOTIFICATION_ID = 32671

  /** 画面ロック解除コード（固定値） */
  const val WAKE_UP_CODE = 268435462

  /** 起動SharedPreferences識別子 */
  const val ENABLE_SWITCH_PREF = "enableSwitchView"
  const val ENABLE_SWITCH_TYPE = 1

  /** 常駐SharedPreferences識別子 */
  const val ENABLE_NOTIFY_PREF = "enableNotifySwitch"
  const val ENABLE_NOTIFY_TYPE = 2

  /** ダブルタップSharedPreferences識別子 */
  const val SLEEP_DOUBLE_TAP_PREF = "sleepDoubleTapSwitch"
  const val SLEEP_DOUBLE_TAP_TYPE = 3
  const val SLEEP_DOUBLE_TAP_COUNT_PREF = "sleepDoubleTapCountSwitch"

  /** Messengerで使うBundle用のキー */
  const val MESSENGER_BUNDLE_KEY = "proximitysensor"

  /** チャンネルID */
  const val NOTIFICATION_CHANNEL_ID = "notification_channel_id"

  fun removeActiveAdmin(context: Context) {
    val cn = ComponentName(context, LockDeviceAdminReceiver::class.java)
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    dpm.removeActiveAdmin(cn)
  }
}