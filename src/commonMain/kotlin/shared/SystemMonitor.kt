package shared

interface SystemMonitor {
    val interval: Long
    fun cpuUsage(): Double
    fun memoryUsage(): Double
    fun networkRecv(): Double
    fun networkSent(): Double
    fun getDeviceType(): String
    fun run(): SystemInfoData
}