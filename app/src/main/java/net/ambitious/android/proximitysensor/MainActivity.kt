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
import androidx.lifecycle.ViewModelProvider
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

  /** ViewModel */
  private val viewModel: MainViewModel by lazy {
    ViewModelProvider.AndroidViewModelFactory(application).create(MainViewModel::class.java)
  }

  /** 画面OFF パーミッション取得 result */
  private val startActivityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    when (it.resultCode) {
      // OKの場合は通知からのオフ機能を有効にする　
      Activity.RESULT_OK -> {
        viewModel.saveEnableNotifyLock(true)
        binding.content.uninstallButton.visibility = View.VISIBLE
      }
      // キャンセルの場合、ダイアログを表示して次回の設定を促す
      else -> {
        binding.content.notifySwitch.isChecked = false
        AlertDialog.Builder(this)
          .setTitle(R.string.sleep_activity_error_title)
          .setMessage(R.string.sleep_activity_error_message)
          .setPositiveButton(R.string.sleep_activity_error_ok, null)
          .show()
      }
    }
    updateSwitchEnable()
  }

  /** DevicePolicyManager */
  private val dpm by lazy {
    getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
  }

  /** ComponentName */
  private val cn by lazy { ComponentName(this, LockDeviceAdminReceiver::class.java) }

  /** @override Activity.onCreate */
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)

    // 機能オンオフスイッチ
    binding.content.enableSwitch.setOnCheckedChangeListener { _, b ->
      viewModel.saveEnableSensor(b)
      if (b) {
        startService()
      }
      updateSwitchEnable()
    }

    // 通知エリアからのロックスイッチ
    binding.content.notifySwitch.setOnCheckedChangeListener { _, b ->
      // 権限がある or 機能オフの場合は保存
      if (dpm.isAdminActive(cn) || !b) {
        viewModel.saveEnableNotifyLock(b)
        updateSwitchEnable()
        return@setOnCheckedChangeListener
      }

      // 機能ON and 権限がない場合は権限取得画面に遷移
      val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
      intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cn)
      startActivityForResult.launch(intent)

      updateSwitchEnable()
    }

    // ダブルタップで通知エリアオフスイッチ
    binding.content.sleepDoubleTapSwitch.setOnCheckedChangeListener { _, b ->
      viewModel.saveEnableSleepDoubleTapLock(b)
    }

    // 管理者権限削除ボタン
    binding.content.uninstallButton.setOnClickListener {
      dpm.removeActiveAdmin(cn)
      val cn = ComponentName(this, LockDeviceAdminReceiver::class.java)
      val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

      dpm.removeActiveAdmin(cn)
      binding.content.uninstallButton.visibility = View.INVISIBLE
      binding.content.sleepDoubleTapSwitch.isChecked = false
      binding.content.notifySwitch.isChecked = false
      updateSwitchEnable()
    }

    // ロック権限がなければアンインストールボタンは不要なので非表示にする
    when {
      !dpm.isAdminActive(cn) -> binding.content.uninstallButton.visibility = View.INVISIBLE
    }

    viewModel.setting.observeForever {
      if (it.isFirstAccess) {
        binding.content.enableSwitch.isChecked = it.isEnableSensor
        binding.content.notifySwitch.isChecked = it.isEnableNotifyLock
        binding.content.sleepDoubleTapSwitch.isChecked = it.isEnableSleepDoubleTapLock
        viewModel.firstAccessed()
        updateSwitchEnable()
      }
      // サービスへの変更通知
      messenger?.send(Message.obtain(null, Util.MESSENGER_BUNDLE_WHAT, Bundle().apply {
        putSerializable (Util.MESSENGER_BUNDLE_KEY, it)
      }))
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
      return
    }

    startService()
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

  private fun startService() {
    val intent = Intent(this, SensorService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      startForegroundService(intent)
    } else {
      startService(intent)
    }
  }

  private fun updateSwitchEnable() {
    binding.content.notifySwitch.isEnabled = binding.content.enableSwitch.isChecked
    if (!binding.content.enableSwitch.isChecked) {
      binding.content.notifySwitch.isChecked = false
      binding.content.sleepDoubleTapSwitch.isChecked = false
      binding.content.sleepDoubleTapSwitch.isEnabled = false
    }

    binding.content.sleepDoubleTapSwitch.isEnabled = binding.content.notifySwitch.isChecked
    if (!binding.content.notifySwitch.isChecked) {
      binding.content.sleepDoubleTapSwitch.isChecked = false
    }
  }
}