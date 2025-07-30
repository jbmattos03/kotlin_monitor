package shared

interface SystemMonitor {
    val interval: Long
    fun cpuUsage(): Double
    fun memoryUsage(): Double
    fun networkRecv(): Double
    fun networkSent(): Double
    fun run(): SystemInfoData
}