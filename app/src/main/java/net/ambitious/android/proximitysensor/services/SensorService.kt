package net.ambitious.android.proximitysensor.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import net.ambitious.android.proximitysensor.util.*

/**
 * 近接センサー検知Service
 * @version 1.0
 * @author bvlion
 */
class SensorService : Service(), SensorEventListener {

  /** SensorManager */
  private lateinit var sensorManager: SensorManager

  /** 初回起動フラグ（sensorManager.registerListenerでonSensorChangedが走ってしまう対策） */
  private var firstActive = false

  /** 対象センサー（TYPE_PROXIMITY） */
  private lateinit var sensor: Sensor

  /** Activityからの切り替えMessenger */
  private var messenger: Messenger? = null

  /** 起動フラグ */
  private var isActive = false

  /** 常駐フラグ */
  private var isNotify = false

  /** ダブルタップでロックフラグ */
  private var isDoubleTapLock = false

  /** 画面オフBroadcastReceiver */
  private val offReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      unregisterReceiver(this)
      sensorManager.registerListener(this@SensorService, sensor, SensorManager.SENSOR_DELAY_UI)
      firstActive = true
    }
  }

  /** @override Service.onCreate */
  override fun onCreate() {
    // SensorManager作成
    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    // Sensor取得
    sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    val pref = getSharedPreferences(packageName, Context.MODE_PRIVATE)

    // 初期作成時はSharedPreferencesより設定
    isActive = pref.getBoolean(Util.ENABLE_SWITCH_PREF, false)
    isNotify = pref.getBoolean(Util.ENABLE_NOTIFY_PREF, false)
    isDoubleTapLock = pref.getBoolean(Util.SLEEP_DOUBLE_TAP_PREF, false)
  }

  /** @override Service.onStartCommand */
  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
    // 画面の状態で設定するReceiver（Listener）を分ける
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    when {
      pm.isInteractive -> registerReceiver(offReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
      else -> sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    // Notification切り替え
    startForegroundNotification()

    // インストール時など自動的に作成されてしまうので、その場合は止める
    when {
      !isActive -> Handler(Looper.getMainLooper()).post { stopSelf() }
    }

    // インスタンスを使いまわして再起動
    return START_REDELIVER_INTENT
  }

  /** @override Service.onDestroy */
  override fun onDestroy() {
    sensorManager.unregisterListener(this)
    try {
      unregisterReceiver(offReceiver)
    } catch (e: IllegalArgumentException) {
      // 初回起動時にエラーが発生する場合がある
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE)
    } else {
      stopForeground(true)
    }
  }

  /** @override SensorEventListener.onSensorChanged */
  @SuppressLint("InvalidWakeLockTag")
  override fun onSensorChanged(event: SensorEvent) {
    // 画面オフのBroadcastReceiverから初回呼び出しされた場合のみ、何もしない
    if (firstActive) {
      firstActive = false
      return
    }

    // タイプがTYPE_PROXIMITYかつ0より大きい場合
    if (event.sensor.type == Sensor.TYPE_PROXIMITY && event.values[0] > 0f) {
      // 画面オン
      val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
      val wl = pm.newWakeLock(Util.WAKE_UP_CODE, packageName)
      wl.acquire(1 * 60 * 1000L /* 1minutes */)
      wl.release()
      // 次回の画面オフを監視
      registerReceiver(offReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
      // 近接センサーオフ
      sensorManager.unregisterListener(this)
    }
  }

  /** @override SensorEventListener.onAccuracyChanged */
  override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

  /** @override Service.onBind */
  override fun onBind(intent: Intent): IBinder = messenger?.binder
      ?: Messenger(ServiceHandler()).also { messenger = it }.binder

  /** Activityからのメッセージ受信 */
  @SuppressLint("HandlerLeak")
  private inner class ServiceHandler : Handler(Looper.getMainLooper()) {
    /** @override Handler.handleMessage */
    override fun handleMessage(msg: Message) {
      when (msg.what) {
          Util.ENABLE_NOTIFY_TYPE, Util.ENABLE_SWITCH_TYPE, Util.SLEEP_DOUBLE_TAP_TYPE -> {
              val bundle = msg.obj as Bundle
              when (msg.what) {
                  Util.ENABLE_NOTIFY_TYPE -> isNotify = bundle.getBoolean(Util.MESSENGER_BUNDLE_KEY)
                  Util.ENABLE_SWITCH_TYPE -> isActive = bundle.getBoolean(Util.MESSENGER_BUNDLE_KEY)
                  Util.SLEEP_DOUBLE_TAP_TYPE -> isDoubleTapLock = bundle.getBoolean(Util.MESSENGER_BUNDLE_KEY)
              }
              startForegroundNotification()
          }
        else -> super.handleMessage(msg)
      }
    }
  }

  /** 条件に応じてNotificationを表示する */
  private fun startForegroundNotification() =
      if (isActive) {
        if (isNotify) {
          if (isDoubleTapLock) {
            startForeground(Util.ONGOING_NOTIFICATION_ID, Notifications.getOngoingShowDoubleTapNotification(applicationContext))
          } else {
            startForeground(Util.ONGOING_NOTIFICATION_ID, Notifications.getOngoingShowNotification(applicationContext))
          }
        } else {
          startForeground(Util.ONGOING_NOTIFICATION_ID, Notifications.getOngoingHideNotification(applicationContext))
        }
      } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
          stopForeground(true)
        }
      }
}
