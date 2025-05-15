package com.example.kotlinmonitor

import shared.AndroidController
import otelHandler.OTelConfig
import shared.AndroidSystemMonitor
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class MetricsService : Service() {
    private val CHANNEL_ID = "MetricsServiceChannel"

    private lateinit var systemMonitor: AndroidSystemMonitor
    private lateinit var otel: OTelConfig
    private lateinit var controller: AndroidController

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        try {
            super.onCreate()
            createNotificationChannel()

            systemMonitor = AndroidSystemMonitor(applicationContext)
            otel = OTelConfig(systemMonitor)
            otel.init(applicationContext)
            controller = AndroidController(otel, applicationContext)

            log("Service created.")
        } catch (error: Exception) {
            log("Error in onCreate: ${error.message}")
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Monitoring Metrics")
                .setContentText("Collecting CPU, RAM, and network data...")
                .setSmallIcon(R.drawable.ic_notification_icon)
                .build()

            startForeground(1, notification)

            controller.start()
        } catch (error: Exception) {
            log("Error in onStartCommand: ${error.message}")
        }

        return START_STICKY // If the service is killed, it will be automatically restarted
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::controller.isInitialized) {
            controller.stop()
        }

        log("Service destroyed.")
    }

    private fun log(string: String) {
        Log.d("MetricsService", "message: $string")
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Metrics Collection Service",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}