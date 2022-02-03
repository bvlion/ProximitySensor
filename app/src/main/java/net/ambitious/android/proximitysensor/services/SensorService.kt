package net.ambitious.android.proximitysensor.services

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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import net.ambitious.android.proximitysensor.util.*

/**
 * 近接センサー検知Service
 * @version 1.0
 * @author bvlion
 */
class SensorService : Service(), SensorEventListener {

  /** SensorManager */
  private val sensorManager: SensorManager by lazy {
    getSystemService(Context.SENSOR_SERVICE) as SensorManager
  }

  /** 初回起動フラグ（sensorManager.registerListenerでonSensorChangedが走ってしまう対策） */
  private var firstActive = false

  /** Activityからの切り替えMessenger */
  private var messenger: Messenger? = null

  /** 対象センサー（TYPE_PROXIMITY） */
  private val sensor: Sensor by lazy {
    sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
  }

  private val dataStore by lazy { Util.getDataStore(this) }

  private val job = SupervisorJob()
  private val scope = CoroutineScope(Dispatchers.Main + job)

  /** 画面オフBroadcastReceiver */
  private val offReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      unregisterReceiver(this)
      sensorManager.registerListener(this@SensorService, sensor, SensorManager.SENSOR_DELAY_UI)
      firstActive = true
    }
  }

  /** @override Service.onStartCommand */
  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
    // 画面の状態で設定するReceiver（Listener）を分ける
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    when {
      pm.isInteractive -> registerReceiver(offReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
      else -> sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    scope.launch {
      // インストール時など自動的に作成されてしまうので、その場合は止める
      dataStore.isEnableSensor.collect {
        if (!it) {
          Handler(Looper.getMainLooper()).post { stopSelf() }
        }
      }
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
    stopForeground()
  }

  /** @override SensorEventListener.onSensorChanged */
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
  override fun onBind(intent: Intent): IBinder = messenger?.binder ?: Messenger(
    ServiceHandler {
      scope.launch {
        startForegroundNotification(
          it.isEnableSensor,
          it.isEnableNotifyLock,
          it.isEnableSleepDoubleTapLock
        )
      }
    }
  ).also { messenger = it }.binder

  /** 条件に応じてNotificationを表示する */
  private fun startForegroundNotification(
    isActive: Boolean, isNotifyLock: Boolean, isDoubleTapLock: Boolean
  ) {
    if (isActive) {
      if (isNotifyLock) {
        if (isDoubleTapLock) {
          startForeground(
            Util.ONGOING_NOTIFICATION_ID,
            Notifications.getOngoingShowDoubleTapNotification(applicationContext)
          )
        } else {
          startForeground(
            Util.ONGOING_NOTIFICATION_ID,
            Notifications.getOngoingShowNotification(applicationContext)
          )
        }
      } else {
        startForeground(
          Util.ONGOING_NOTIFICATION_ID,
          Notifications.getOngoingHideNotification(applicationContext)
        )
      }
    } else {
      stopForeground()
    }
  }

  private fun stopForeground() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE)
    } else {
      stopForeground(true)
    }
    stopSelf()
  }

  /** Activityからのメッセージ受信 */
  private class ServiceHandler(
    private val function: (setting : Util.SettingModel) -> Unit
  ) : Handler(Looper.getMainLooper()) {
    override fun handleMessage(msg: Message) {
      when (msg.what) {
        Util.MESSENGER_BUNDLE_WHAT ->
          function(
            (msg.obj as Bundle).getSerializable(Util.MESSENGER_BUNDLE_KEY)
                    as? Util.SettingModel ?: return
          )
        else -> super.handleMessage(msg)
      }
    }
  }
}
