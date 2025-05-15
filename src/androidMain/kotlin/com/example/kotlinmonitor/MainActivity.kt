package com.example.kotlinmonitor

import shared.MetricController
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(this, MetricsService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

    }

    override fun onDestroy() {
        super.onDestroy()

        val serviceIntent = Intent(this, MetricsService::class.java)
        stopService(serviceIntent)
    }
}
