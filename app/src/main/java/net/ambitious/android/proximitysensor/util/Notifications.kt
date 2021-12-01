package net.ambitious.android.proximitysensor.util

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.res.ResourcesCompat
import net.ambitious.android.proximitysensor.MainActivity
import net.ambitious.android.proximitysensor.R
import net.ambitious.android.proximitysensor.SleepActivity
import net.ambitious.android.proximitysensor.receiver.DoubleTapBroadCastReceiver

object Notifications {
  /**
   * 表示用常駐Notification(ロックはダブルタップ)を生成する
   * @param context Context
   * @return Notification
   */
  @SuppressLint("LaunchActivityFromNotification")
  fun getOngoingShowDoubleTapNotification(context: Context): Notification {
    createChannel(context, NotificationManagerCompat.IMPORTANCE_MAX, R.string.sleep_double_tap)

    val contextIntent =
      PendingIntent.getBroadcast(
        context,
        432,
        Intent(context, DoubleTapBroadCastReceiver::class.java),
        getPendingIntentFlag()
      )

    val largeIconDrawable = ResourcesCompat.getDrawable(
      context.resources,
      R.mipmap.ic_launcher,
      null
    ) as BitmapDrawable
    val largeIconBitmap = largeIconDrawable.bitmap

    return NotificationCompat.Builder(context, Util.NOTIFICATION_CHANNEL_ID)
      .setContentTitle(context.getString(R.string.notification_double_tap)) // タイトル文言
      .setSmallIcon(R.drawable.notification_icon) // アイコン
      .setContentText(context.getString(R.string.app_name)) // メイン文言
      .setShowWhen(false)
      .setLargeIcon(largeIconBitmap) // 表示アイコン
      .setContentIntent(contextIntent)
      .addAction(R.drawable.notification_icon_24, "ここをダブルタップで画面オフ", contextIntent)
      .also {
        it.priority = NotificationCompat.PRIORITY_HIGH
      }.build()
  }

  /**
   * 表示用常駐Notificationを生成する
   * @param context Context
   * @return Notification
   */
  fun getOngoingShowNotification(context: Context): Notification {
    createChannel(context, NotificationManagerCompat.IMPORTANCE_HIGH, R.string.sleep_notify)

    val contextIntent =
      PendingIntent.getActivity(context, 0, Intent(context, SleepActivity::class.java), getPendingIntentFlag())
    val largeIconDrawable = ResourcesCompat.getDrawable(
      context.resources,
      R.mipmap.ic_launcher,
      null
    ) as BitmapDrawable
    val largeIconBitmap = largeIconDrawable.bitmap

    return NotificationCompat.Builder(context, Util.NOTIFICATION_CHANNEL_ID)
      .setContentTitle(context.getString(R.string.notification_main)) // タイトル文言
      .setSmallIcon(R.drawable.notification_icon) // アイコン
      .setContentText(context.getString(R.string.app_name)) // メイン文言
      .setShowWhen(false)
      .setLargeIcon(largeIconBitmap) // 表示アイコン
      .setContentIntent(contextIntent) // タップ時起動Activity
      .build()
  }

  /**
   * 非表示用常駐Notificationを生成する
   * @param context Context
   * @return Notification
   */
  fun getOngoingHideNotification(context: Context): Notification {
    createChannel(context, NotificationManagerCompat.IMPORTANCE_NONE, null)

    val contextIntent =
      PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), getPendingIntentFlag())
    val largeIconDrawable = ResourcesCompat.getDrawable(
      context.resources,
      R.mipmap.ic_launcher,
      null
    ) as BitmapDrawable
    val largeIconBitmap = largeIconDrawable.bitmap

    return NotificationCompat.Builder(context, Util.NOTIFICATION_CHANNEL_ID)
      .setContentTitle(context.getString(R.string.notification_sub)) // メイン文言
      .setSmallIcon(R.drawable.notification_icon) // 透明アイコン
      .setLargeIcon(largeIconBitmap) // 表示アイコン
      .setShowWhen(false)
      .setContentIntent(contextIntent) // タップ時起動Activity
      .also {
        it.priority = NotificationCompat.PRIORITY_MIN
      }.build()
  }

  /**
   * 表示のためのチャンネルを作成する（Oreo対応）
   * @param context Context
   * @param importance 表示優先度
   * @param channelDescriptionId 詳細のstringsのID
   */
  private fun createChannel(context: Context, importance: Int, channelDescriptionId: Int?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.deleteNotificationChannel(Util.NOTIFICATION_CHANNEL_ID)
      val channel = NotificationChannel(
        Util.NOTIFICATION_CHANNEL_ID,
        context.getString(R.string.channel_title),
        importance
      )
      if (channelDescriptionId != null) {
        channel.description =
          context.getString(channelDescriptionId) + context.getString(R.string.channel_description)
      }
      notificationManager.createNotificationChannel(channel)
    }
  }

  private fun getPendingIntentFlag() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
}