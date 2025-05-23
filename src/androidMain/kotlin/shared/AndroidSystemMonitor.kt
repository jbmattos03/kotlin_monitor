package shared

import android.app.ActivityManager
import android.content.Context
import android.net.TrafficStats
import android.os.Build
import java.io.BufferedReader
import kotlin.text.toDoubleOrNull
import kotlin.text.trim
import java.io.File
import java.io.FileReader
import java.io.IOException
import android.util.Log

data class AndroidSystemInfoData(
    override val cpuUsage: Double,
    override val memoryUsage: Double,
    override val networkRecv: Double,
    override val networkSent: Double,
    override val deviceType: String,
    val temperature: Double
) : SystemInfoData

class AndroidSystemMonitor(private val context: Context) : SystemMonitor {
    override val interval: Long = 5000 // 5 seconds
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val alertManager = AndroidAlertManager(
        deviceType = DeviceTypes(),
        context = context,
        host = getDeviceType(),
        thresholdConfig = ThresholdConfig()
    )

    init {
        alertManager.initializeAlerts(thresholdConfig = ThresholdConfig())
    }

    override fun cpuUsage(): Double {
        var currentCpuUsage = 0.0 // Default value, returned in case of early exit or error
        try {
            val reader = BufferedReader(FileReader("/proc/stat"))
            val line = reader.readLine()
            // It's good practice to close the reader as soon as you're done with it.
            reader.close()

            val parts = line.split("\\s+".toRegex())
            if (parts.isNotEmpty() && parts[0] == "cpu") {
                // drop(1) to remove the "cpu" label itself
                val values = parts.drop(1).mapNotNull { it.toLongOrNull() }

                // Ensure we have enough values (user, nice, system, idle at minimum)
                if (values.size >= 4) {
                    val idleTime = values[3] // idle is the 4th numeric value (index 3)
                    val totalTime = values.sum()
                    if (totalTime > 0) { // Avoid division by zero
                        currentCpuUsage = 100.0 * (1.0 - (idleTime.toDouble() / totalTime))
                    } else {
                        log("CPU total time is zero, cannot calculate usage.")
                    }
                } else {
                    log("Failed to parse enough CPU values from /proc/stat. Values found: ${values.size}")
                }
            } else {
                log("Unexpected format in /proc/stat line. Line: $line")
            }

            val alertToCheck = alertManager.alerts.find { it.metric == "cpu_usage" }
            if (alertToCheck != null) {
                alertManager.checkAlerts(alertToCheck, currentCpuUsage, getDeviceType())
            }
            return currentCpuUsage // Explicit return for the successful path

        } catch (e: IOException) {
            log("Failed to read CPU usage: ${e.message}")
            return 0.0 // REQUIRED: Return a Double in the catch block
        }
    }

    override fun memoryUsage(): Double {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val total = memInfo.totalMem.toDouble()
        val avail = memInfo.availMem.toDouble()
        val usedMemory = total - avail
        val usedMemoryPercentage = (usedMemory / total) * 100

        val alertToCheck = alertManager.alerts.find { it.metric == "memory_usage" }
        if (alertToCheck != null) {
            alertManager.checkAlerts(alertToCheck, usedMemoryPercentage, getDeviceType())
        }

        return usedMemoryPercentage
    }

    fun temperature(): Double {
        var currentTemperature = 0.0 // Default value, or consider Double.NaN for "not available"
        try {
            val tempFile = File("/sys/class/thermal/thermal_zone0/temp")
            if (tempFile.exists() && tempFile.canRead()) {
                val tempStr = tempFile.readText().trim()
                currentTemperature = tempStr.toDoubleOrNull()?.let {
                    if (it > 1000) it / 1000.0 else it // Some devices report in millidegrees Celsius
                } ?: 0.0 // Default to 0.0 if parsing fails (toDoubleOrNull returns null)

                val alertToCheck = alertManager.alerts.find { it.metric == "temperature" }
                if (alertToCheck != null) {
                    alertManager.checkAlerts(alertToCheck, currentTemperature, getDeviceType())
                }

                return currentTemperature
            } else {
                log("Temperature file not accessible or does not exist: /sys/class/thermal/thermal_zone0/temp")
                return currentTemperature
            }
        } catch (e: IOException) {
            log("Failed to read temperature: ${e.message}")
            return currentTemperature
        }
    }

    override fun networkRecv(): Double {
        var networkRecv = TrafficStats.getTotalRxBytes().toDouble()

        val alertToCheck = alertManager.alerts.find { it.metric == "network_recv" }
        if (alertToCheck != null) {
            alertManager.checkAlerts(alertToCheck, networkRecv, getDeviceType())
        }

        return networkRecv
    }

    override fun networkSent(): Double {
        var networkSent = TrafficStats.getTotalTxBytes().toDouble()

        val alertToCheck = alertManager.alerts.find { it.metric == "network_sent" }
        if (alertToCheck != null) {
            alertManager.checkAlerts(alertToCheck, networkSent, getDeviceType())
        }

        return networkSent
    }

    override fun getDeviceType(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL

        return when(model.startsWith(manufacturer)) {
            true -> capitalize(model)
            false -> capitalize(manufacturer) + " " + model
        }
    }

    private fun capitalize(str: String): String {
        if (str.isEmpty()) {
            return str
        }

        return when(val first = str[0]) {
            in 'a'..'z' -> first.uppercaseChar() + str.substring(1)
            else -> str
        }
    }

    private fun log(message: String) {
        Log.d("AndroidSystemMonitor", message)
    }

    override fun run(): AndroidSystemInfoData {
        return AndroidSystemInfoData(
            cpuUsage(),
            memoryUsage(),
            networkRecv(),
            networkSent(),
            getDeviceType(),
            temperature()
        )
    }
}