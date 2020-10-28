package com.example.android.treasureHunt.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import com.example.android.treasureHunt.HuntMainActivity
import com.example.android.treasureHunt.MyLocationManager
import com.example.android.treasureHunt.R
import com.example.android.treasureHunt.createChannel

class LocationForegroundService: Service(), LifecycleObserver {

    val myLocationManager = MyLocationManager.getInstance(this)
    private val mBinder: IBinder = LocalBinder(this)


    private val NOTIFICATION_ID_STICKY = 12345678
    private val NOTIFICATION_STICKY_CHANNEL  = "channel_01"


    override fun onCreate() {
        super.onCreate()
        createChannel(this, NOTIFICATION_STICKY_CHANNEL)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    fun startLocationUpdates() {
        startService(Intent(this, LocationForegroundService::class.java))
        myLocationManager.stopLocationUpdates()
        myLocationManager.startLocationUpdates()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onMoveToForeground(source: LifecycleOwner) {
        stopForeground(true)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onMoveToBackground(source: LifecycleOwner) {
        startForeground(NOTIFICATION_ID_STICKY, getNotification())
    }

    override fun onUnbind(intent: Intent?): Boolean {
        startForeground(NOTIFICATION_ID_STICKY, getNotification())
        return true // Ensures onRebind() is called when a client re-binds.
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    override fun onDestroy() {
        super.onDestroy()
        myLocationManager.stopLocationUpdates()
    }

    /**
     * Returns the [NotificationCompat] used as part of the foreground service.
     */
    private fun getNotification(): Notification? {

        // The PendingIntent to launch activity.
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, HuntMainActivity::class.java), 0
        )
        val builder = NotificationCompat.Builder(this)
            .addAction(
                R.drawable.map_small, "Clock out",
                activityPendingIntent
            )
            .setContentText("You are Clocked in at: LocationName")
            .setContentTitle("Hourly")
            .setOngoing(true)
            .setContentIntent(activityPendingIntent)
            .setPriority(Notification.PRIORITY_DEFAULT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("ticker")
            .setWhen(System.currentTimeMillis())

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(NOTIFICATION_STICKY_CHANNEL) // Channel ID
        }
        return builder.build()
    }

    class LocalBinder(val service: LocationForegroundService): Binder()


}

