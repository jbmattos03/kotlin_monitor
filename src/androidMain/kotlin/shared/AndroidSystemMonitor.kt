package shared

import android.app.ActivityManager
import android.content.Context
import android.net.TrafficStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidSystemMonitor(private val context: Context) : SystemMonitor {
    override val interval: Long = 5000 // 5 seconds

    override fun cpuUsage(): Double {
        // TODO: Implement CPU usage monitoring for Android
        return 0.0
    }

    override fun memoryUsage(): Double {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()

        activityManager.getMemoryInfo(memInfo)

        val total = memInfo.totalMem.toDouble()
        val avail = memInfo.availMem.toDouble()
        val usedMemory = total - avail
        val usedMemoryPercentage = (usedMemory / total) * 100

        return usedMemoryPercentage
    }

    // ----------------------------------------------------------
    // Android does not provide a direct way to get disk write/read stats
    // So, for now, we will simply return 0
    override fun diskUsage(): Double {
        // TODO: Implement disk usage monitoring for Android
        return 0.0
    }

    override fun diskWrite(): Long {
        // TODO: Implement disk write monitoring for Android
        return 0L
    }

    override fun diskRead(): Long {
        // TODO: Implement disk read monitoring for Android
        return 0L
    }

    // ----------------------------------------------------------

    override fun networkRecv(): Long {
        var networkRecv = TrafficStats.getTotalRxBytes() 
        return networkRecv
    }

    override fun networkSent(): Long {
        var networkSent = TrafficStats.getTotalTxBytes()
        return networkSent
    }

    override fun getDeviceType(): String {
        return "Android"
    }

    override fun run(): SystemInfoData {
        return SystemInfoData(
            cpuUsage(),
            memoryUsage(),
            diskUsage(),
            diskWrite(),
            diskRead(),
            networkRecv(),
            networkSent(),
            getDeviceType()
        )
    }
}