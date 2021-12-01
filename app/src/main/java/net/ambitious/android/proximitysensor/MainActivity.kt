package net.ambitious.android.proximitysensor

import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.*
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import net.ambitious.android.proximitysensor.receiver.LockDeviceAdminReceiver
import net.ambitious.android.proximitysensor.services.SensorService
import net.ambitious.android.proximitysensor.util.*
import net.ambitious.android.proximitysensor.databinding.ActivityMainBinding

/**
 * メイン画面Activity
 * @version 1.0
 * @author bvlion
 */
class MainActivity : AppCompatActivity() {

  /** Serviceへの状態変更送信Messenger */
  private var messenger: Messenger? = null

  /** ServiceConnection */
  private val serviceConnection = object : ServiceConnection {
    override fun onServiceDisconnected(componentName: ComponentName?) {
      messenger = null
    }

    override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
      messenger = Messenger(binder)
    }
  }

  /** layout */
  private lateinit var binding: ActivityMainBinding

  /** 画面OFF パーミッション取得 result */
  private val startActivityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    when (it.resultCode) {
      // OKの場合は通知からのオフ機能を有効にする　
      Activity.RESULT_OK -> {
        changeNotification(true)
        binding.content.uninstallButton.visibility = View.VISIBLE
      }
      // キャンセルの場合、ダイアログを表示して次回の設定を促す
      else -> {
        binding.content.notifySwitch.isChecked = false
        AlertDialog.Builder(this)
          .setTitle(R.string.sleep_activity_error_title)
          .setMessage(R.string.sleep_activity_error_message)
          .setPositiveButton(R.string.sleep_activity_error_ok) { _, _ -> finish() }
          .show()
      }
    }
  }

  /** @override Activity.onCreate */
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)

    val pref = getSharedPreferences(packageName, Activity.MODE_PRIVATE)

    // ロック状態を取得
    val cn = ComponentName(this, LockDeviceAdminReceiver::class.java)
    val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    // 機能オンオフスイッチ
    binding.content.enableSwitch.setOnCheckedChangeListener { _, b ->
      // 変更されたら保存
      val edit = pref.edit()
      edit.putBoolean(Util.ENABLE_SWITCH_PREF, b)
      edit.apply()
      val bundle = Bundle()

      // 変更処理
      when {
        b -> {
          // 常駐スイッチ解放
          binding.content.notifySwitch.isEnabled = true
          binding.content.sleepDoubleTapSwitch.isEnabled = binding.content.notifySwitch.isChecked
        }
        else -> {
          // 常駐スイッチロック
          binding.content.notifySwitch.isEnabled = false
          binding.content.sleepDoubleTapSwitch.isEnabled = false
        }
      }

      // サービスへの変更通知
      bundle.putBoolean(Util.MESSENGER_BUNDLE_KEY, b)
      messenger?.send(Message.obtain(null, Util.ENABLE_SWITCH_TYPE, bundle))
    }
    // 初期値
    binding.content.enableSwitch.isChecked = pref.getBoolean(Util.ENABLE_SWITCH_PREF, true)
    // 連動（オフの場合はロック機能を設定させない）
    when {
      !binding.content.enableSwitch.isChecked -> {
        binding.content.notifySwitch.isEnabled = false
        binding.content.sleepDoubleTapSwitch.isEnabled = false
      }
    }

    // 通知エリアからのロックスイッチ
    binding.content.notifySwitch.setOnCheckedChangeListener { _, b ->
      when {
        // 権限がある or 機能オフの場合は保存
        dpm.isAdminActive(cn) or !b -> changeNotification(b)

        // 機能ON and 権限がない場合は権限取得画面に遷移
        else -> {
          val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
          intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cn)
          startActivityForResult.launch(intent)
        }
      }
    }
    // 初期値
    binding.content.notifySwitch.isChecked = pref.getBoolean(Util.ENABLE_NOTIFY_PREF, false)

    // ダブルタップで通知エリアオフスイッチ
    binding.content.sleepDoubleTapSwitch.setOnCheckedChangeListener { _, b ->
      val edit = pref.edit()
      edit.putBoolean(Util.SLEEP_DOUBLE_TAP_PREF, b)
      edit.apply()
      val bundle = Bundle()

      // サービスへの変更通知
      bundle.putBoolean(Util.MESSENGER_BUNDLE_KEY, b)
      messenger?.send(Message.obtain(null, Util.SLEEP_DOUBLE_TAP_TYPE, bundle))
    }

    binding.content.sleepDoubleTapSwitch.isChecked = pref.getBoolean(Util.SLEEP_DOUBLE_TAP_PREF, false)

    // アンインストールボタン
    binding.content.uninstallButton.setOnClickListener {
      Util.removeActiveAdmin(this)
      binding.content.uninstallButton.visibility = View.INVISIBLE
      binding.content.notifySwitch.isChecked = false
      binding.content.sleepDoubleTapSwitch.isChecked = false
      startActivity(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null)))
    }

    // ロック権限がなければアンインストールボタンは不要なので非表示にする
    when {
      !dpm.isAdminActive(cn) -> binding.content.uninstallButton.visibility = View.INVISIBLE
    }

    val intent = Intent(this, SensorService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      startForegroundService(intent)
    } else {
      startService(intent)
    }
  }

  /** @override Activity.onStart */
  override fun onStart() {
    super.onStart()

    // 近接センサーが使用可能かのチェック
    val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    // 使用できない場合は…
    if (sensor == null) {
      binding.content.enableSwitch.isEnabled = false
      binding.content.notifySwitch.isEnabled = false
      binding.content.sleepDoubleTapSwitch.isEnabled = false
      // 切ないけど、アンインストールしていただく(´･ω･`)
      AlertDialog.Builder(this)
          .setTitle(R.string.activation_impossible_title)
          .setMessage(R.string.activation_impossible_message)
          .setPositiveButton(R.string.activation_impossible_button
          ) { _, _ ->
            startActivity(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null)))
            finish()
          }
          .setCancelable(false)
          .show()
      stopService(Intent(applicationContext, SensorService::class.java))
    }
  }

  /** @override Activity.onResume */
  override fun onResume() {
    bindService(Intent(this, SensorService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    super.onResume()
  }

  /** @override Activity.onPause */
  override fun onPause() {
    unbindService(serviceConnection)
    super.onPause()
  }

  /**
   * 通知エリアからのロックスイッチ切り替えを通知する
   * @param isNotify 変更結果
   */
  private fun changeNotification(isNotify: Boolean) {
    // 変更されたら保存
    getSharedPreferences(packageName, Activity.MODE_PRIVATE)
        .edit().apply {
          putBoolean(Util.ENABLE_NOTIFY_PREF, isNotify)
          apply()
        }

    // サービスへの変更通知
    messenger?.send(Message.obtain(null, Util.ENABLE_NOTIFY_TYPE, Bundle().apply {
        putBoolean(Util.MESSENGER_BUNDLE_KEY, isNotify)
    }))

    binding.content.sleepDoubleTapSwitch.isEnabled = isNotify
  }
}