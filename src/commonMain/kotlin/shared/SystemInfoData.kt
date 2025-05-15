package shared

interface SystemInfoData {
    val cpuUsage: Double
    val memoryUsage: Double
    val networkRecv: Long
    val networkSent: Long
    val deviceType: String
}