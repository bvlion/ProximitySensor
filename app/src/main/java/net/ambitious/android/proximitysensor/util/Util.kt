package net.ambitious.android.proximitysensor.util

import android.content.Context
import java.io.Serializable

object Util {
  /** 通知エリア常駐ID */
  const val ONGOING_NOTIFICATION_ID = 32671

  /** 画面ロック解除コード（固定値） */
  const val WAKE_UP_CODE = 268435462

  /** Messengerで使うBundle用のキー */
  const val MESSENGER_BUNDLE_WHAT = 234565334
  /** Messengerで使うBundle用のキー */
  const val MESSENGER_BUNDLE_KEY = "proximitysensor"

  /** チャンネルID */
  const val NOTIFICATION_CHANNEL_ID = "notification_channel_id"

  /** DataStore Singleton */
  private var dataStore: DataStore? = null
  fun getDataStore(context: Context) = dataStore ?: DataStore(context).also { dataStore = it }

  /** ダブルタップ画面オフカウンター */
  var tapCount = 0

  /** 設定のModel */
  data class SettingModel(
    val isEnableSensor: Boolean = false,
    val isEnableNotifyLock: Boolean = false,
    val isEnableSleepDoubleTapLock: Boolean = false,
    val isFirstAccess: Boolean = false
  ) : Serializable
}