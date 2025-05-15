package shared

import android.app.ActivityManager
import android.content.Context
import android.net.TrafficStats

data class AndroidSystemInfoData(
    override val cpuUsage: Double,
    override val memoryUsage: Double,
    override val networkRecv: Long,
    override val networkSent: Long,
    override val deviceType: String,
    val temperature : Double
) : SystemInfoData

class AndroidSystemMonitor(private val context: Context) : SystemMonitor {
    override val interval: Long = 5000 // 5 seconds
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    override fun cpuUsage(): Double {
        // TODO: Implement CPU usage monitoring for Android
        return 0.0
    }

    override fun memoryUsage(): Double {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val total = memInfo.totalMem.toDouble()
        val avail = memInfo.availMem.toDouble()
        val usedMemory = total - avail
        val usedMemoryPercentage = (usedMemory / total) * 100

        return usedMemoryPercentage
    }

    fun temperature(): Double {
        // TODO: Implement temperature monitoring for Android
        return 0.0
    }

    override fun networkRecv(): Long {
        var networkRecv = TrafficStats.getTotalRxBytes() 
        return networkRecv
    }

    override fun networkSent(): Long {
        var networkSent = TrafficStats.getTotalTxBytes()
        return networkSent
    }

    override fun getDeviceType(): String {
        val manufacturer = android.os.Build.MANUFACTURER
        val model = android.os.Build.MODEL

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