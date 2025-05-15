package shared

interface SystemMonitor {
    val interval: Long
    fun cpuUsage(): Double
    fun memoryUsage(): Double
    fun networkRecv(): Long
    fun networkSent(): Long
    fun getDeviceType(): String
    fun run(): SystemInfoData
}