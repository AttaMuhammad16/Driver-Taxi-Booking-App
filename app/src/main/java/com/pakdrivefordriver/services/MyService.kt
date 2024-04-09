package com.pakdrivefordriver.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.pakdrivefordriver.MyConstants.LATLANG_UPDATE_DELAY
import com.pakdrivefordriver.MyConstants.broadCastAction
import com.pakdrivefordriver.R
import com.pakdrivefordriver.data.driver.DriverRepo
import com.pakdrivefordriver.services.broadcastreciver.StopServiceReceiver
import com.pakdrivefordriver.ui.activities.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MyService : Service() {
    @Inject
    lateinit var driverRepo: DriverRepo
    private var lastUpdateTime = 0L
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var wakeLock: PowerManager.WakeLock? = null


    @OptIn(DelicateCoroutinesApi::class)
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            GlobalScope.launch {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTime >= LATLANG_UPDATE_DELAY) { // 5 seconds
                    lastUpdateTime = currentTime
                    driverRepo.updateDriverLocationOnDataBase(locationResult.lastLocation)
                }
            }
        }
    }

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            stopSelf()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationUpdates()
        val filter = IntentFilter(broadCastAction)
        registerReceiver(stopReceiver, filter)

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
             newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocationService::lock").apply {
                 acquire(10*1000L)  // 10 seconds
             }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = getNotification()
        startForeground(1, notification)
        return START_STICKY
    }

    private fun requestLocationUpdates() {
        try {
            fusedLocationProviderClient.requestLocationUpdates(
                LocationRequest.create().apply {
                    interval = 1000
                    fastestInterval = 1000
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                },
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel("location update", "Foreground Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun getNotification(): Notification {

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopServiceIntent = Intent(this, StopServiceReceiver::class.java)
        val stopServicePendingIntent = PendingIntent.getBroadcast(this, 2, stopServiceIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, "location update")
            .setContentTitle("Pak Drive location updates")
            .setContentText("If you turn off the location then you will become offline.").setSmallIcon(R.drawable.app_ic)
            .addAction(R.drawable.baseline_clear_24, "Turn off", stopServicePendingIntent) // Add the action here
            .setSmallIcon(R.drawable.outline_local_taxi_24)
            .setContentIntent(pendingIntent).build()
    }

}






