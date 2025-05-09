package com.example.kotlinmonitor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import OTelHandler.OTelConfig
import shared.AndroidSystemMonitor
import shared.AndroidController
import shared.MetricController

class MainActivity : AppCompatActivity() {
    private lateinit var controller: MetricController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val systemMonitor = AndroidSystemMonitor(this)
        val otel = OTelConfig(systemMonitor)
        controller = AndroidController(otel)

        controller.start()
    }

    override fun onDestroy() {
        controller.stop()
        super.onDestroy()
    }
}
