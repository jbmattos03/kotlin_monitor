package com.example.kotlinmonitor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import androidx.core.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout

class MainActivity : AppCompatActivity(),  View.OnClickListener {
   override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val linearLayout = findViewById<LinearLayout>(R.id.linearLayout)

        // Start button
        val startButton = Button(this)
        startButton.apply {
            id = R.id.buttonStart
            text = "Start Service"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        startButton.setOnClickListener(this)
        linearLayout.addView(startButton)

        // Stop button
        val stopButton = Button(this)
        stopButton.apply {
            id = R.id.buttonStop
            text = "Stop Service"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        stopButton.setOnClickListener(this)
        linearLayout.addView(stopButton)

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.buttonStart -> {
                val serviceIntent = Intent(this, MetricsService::class.java)
                ContextCompat.startForegroundService(this, serviceIntent)
            }
            R.id.buttonStop -> {
                val serviceIntent = Intent(this, MetricsService::class.java)
                stopService(serviceIntent)
            }
            else -> {
                // Do nothing
            }
        }
    }
}
