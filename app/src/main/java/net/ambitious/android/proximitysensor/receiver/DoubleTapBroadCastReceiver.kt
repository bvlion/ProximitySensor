package net.ambitious.android.proximitysensor.receiver

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import net.ambitious.android.proximitysensor.util.Util
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * ダブルタップでのロックBroadcastReceiver
 * @version 1.0
 * @author bvlion
 */
class DoubleTapBroadCastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // ダブルタップと認識したらロックする
        if (context.getSharedPreferences(context.packageName, Activity.MODE_PRIVATE).getBoolean(Util.SLEEP_DOUBLE_TAP_COUNT_PREF, false)) {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.lockNow()
        } else {
            Observable.create<Unit> {
                Thread.sleep(1000)
                it.onNext(Unit)
            }
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    saveDoubleTapPref(context, false)
                }
        }
        saveDoubleTapPref(context, true)
    }

    private fun saveDoubleTapPref(context: Context, isDoubleTap: Boolean) =
        context.getSharedPreferences(context.packageName, Activity.MODE_PRIVATE)
            .edit().apply {
                putBoolean(Util.SLEEP_DOUBLE_TAP_COUNT_PREF, isDoubleTap)
                apply()
            }
}