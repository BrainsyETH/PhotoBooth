package com.snapcabin.share

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.snapcabin.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that keeps the LAN QR-download server alive while the
 * Share screen is active. Posts a persistent notification (required for
 * foreground services on Android 8+) and stops itself on demand.
 *
 * Lifecycle:
 *   - START intent with a serialized photo path / cached bitmap triggers
 *     LocalPhotoServer.servePhoto(...).
 *   - STOP intent stops the server and tears down the notification.
 *
 * The Service exists so the photo stays reachable even if the activity is
 * briefly backgrounded or the system pages it out — without it, Play 14+
 * may kill the in-process HTTP server.
 */
@AndroidEntryPoint
class PhotoShareService : Service() {

    @Inject lateinit var localPhotoServer: LocalPhotoServer

    companion object {
        const val ACTION_START = "com.snapcabin.share.START_SERVING"
        const val ACTION_STOP = "com.snapcabin.share.STOP_SERVING"

        private const val NOTIFICATION_CHANNEL_ID = "snapcabin_photo_share"
        private const val NOTIFICATION_ID = 4711

        /** Bitmap handoff via a process-global. Service runs in the same process
         *  as the activity, so passing through a static is safe and avoids
         *  serializing a JPEG into the Intent extras (which has a hard 1 MB cap). */
        @Volatile
        var pendingBitmap: Bitmap? = null

        fun start(context: Context, bitmap: Bitmap) {
            pendingBitmap = bitmap
            val intent = Intent(context, PhotoShareService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PhotoShareService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startServing()
            ACTION_STOP -> stopServing()
            else -> stopServing()
        }
        return START_NOT_STICKY
    }

    private fun startServing() {
        val bitmap = pendingBitmap
        ensureChannel()
        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (bitmap != null) {
            localPhotoServer.servePhoto(bitmap)
            pendingBitmap = null
        }
    }

    private fun stopServing() {
        localPhotoServer.stopServing()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        localPhotoServer.stopServing()
        pendingBitmap = null
        super.onDestroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.photo_service_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.photo_service_channel_description)
                    setShowBadge(false)
                }
                mgr.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_photo)
            .setContentTitle(getString(R.string.photo_service_notification_title))
            .setContentText(getString(R.string.photo_service_notification_body))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
}
