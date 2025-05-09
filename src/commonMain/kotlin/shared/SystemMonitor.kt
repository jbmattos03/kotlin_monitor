package shared

// Creating a data class to hold the system information
data class SystemInfoData (
    val cpuUsage: Double,
    val memoryUsage: Double,
    val diskUsage: Double,
    val diskWrite: Long,
    val diskRead: Long,
    val networkRecv: Long,
    val networkSent: Long,
    val deviceType: String
)

interface SystemMonitor {
    val interval: Long
    fun cpuUsage(): Double
    fun memoryUsage(): Double
    fun diskUsage(): Double
    fun diskWrite(): Long
    fun diskRead(): Long
    fun networkRecv(): Long
    fun networkSent(): Long
    fun getDeviceType(): String
    fun run(): SystemInfoData
}