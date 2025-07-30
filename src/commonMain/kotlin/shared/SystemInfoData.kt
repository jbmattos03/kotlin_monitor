package shared

interface SystemInfoData {
    val cpuUsage: Double
    val memoryUsage: Double
    val networkRecv: Double
    val networkSent: Double
}