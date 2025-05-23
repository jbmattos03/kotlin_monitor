package shared

import io.github.cdimascio.dotenv.dotenv
import oshi.SystemInfo
import oshi.hardware.HWDiskStore
import oshi.hardware.CentralProcessor
import oshi.hardware.GlobalMemory
import oshi.hardware.NetworkIF

data class JVMSystemInfoData(
    override var cpuUsage: Double,
    override var memoryUsage: Double,
    override var networkRecv: Double,
    override var networkSent: Double,
    override var deviceType: String,
    var diskUsage: Double,
    var diskWrite: Double,
    var diskRead: Double
) : SystemInfoData

// Environment variables
val dotenv = dotenv()
val IP_ADDR = dotenv["IP_ADDR"] ?: ""
val HOST = dotenv["HOST"] ?: "localhost"

class JVMSystemMonitor : SystemMonitor {
    // Properties
    private val processor: CentralProcessor
    private var prevTicks: LongArray
    private val memory: GlobalMemory
    private val disk: HWDiskStore
    private var diskPrevTime: Long
    private val networkInterface: NetworkIF
    override val interval: Long = 5000 // 5 seconds
    private val alertManager = JVMAlertManager(
            deviceType = DeviceTypes(),
            host = HOST,
            thresholdConfig = ThresholdConfig()
        )

    init {
        // Initialize SystemInfo and AlertManager
        val systemInfo = SystemInfo()
        alertManager.initializeAlerts(thresholdConfig = ThresholdConfig())

        // Initialize hardware components
        processor = systemInfo.hardware.processor
        prevTicks = systemInfo.hardware.processor.systemCpuLoadTicks
        memory = systemInfo.hardware.memory
        disk = systemInfo.hardware.diskStores.first()
        diskPrevTime = disk.transferTime
        networkInterface = systemInfo.hardware.networkIFs.firstOrNull { it.name != "lo" }
            ?: throw IllegalStateException("No suitable network interface found") // Exclude loopback interface
    }

    override fun cpuUsage(): Double {
        val currTicks = processor.systemCpuLoadTicks
        val cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100

        val alertToCheck = alertManager.alerts.find { it.metric == "cpu_usage" }
            ?: throw IllegalStateException("No alert found for CPU usage")
        alertManager.checkAlerts(
            alert = alertToCheck,
            value = cpuLoad,
            host = getDeviceType()
        )

        prevTicks = currTicks // Update the previous ticks for the next calculation

        return cpuLoad
    }

    override fun memoryUsage(): Double {
        val totalMemory = memory.total
        val availableMemory = memory.available
        val usedMemory = totalMemory - availableMemory
        val usedMemoryPercentage = (usedMemory / totalMemory.toDouble()) * 100

        val alertToCheck = alertManager.alerts.find { it.metric == "memory_usage" }
            ?: throw IllegalStateException("No alert found for Memory usage")
        alertManager.checkAlerts(
            alert = alertToCheck,
            value = usedMemoryPercentage,
            host = getDeviceType()
        )

        return usedMemoryPercentage
    }

    fun diskUsage(): Double {
        disk.updateAttributes() // Update disk attributes
        val diskCurrTime = disk.transferTime
        val diskBusyTime = ((diskCurrTime - diskPrevTime).toDouble() / interval) * 100

        val alertToCheck = alertManager.alerts.find { it.metric == "disk_usage" }
            ?: throw IllegalStateException("No alert found for Disk usage")
        alertManager.checkAlerts(
            alert = alertToCheck,
            value = diskBusyTime,
            host = getDeviceType()
        )
        
        diskPrevTime = diskCurrTime // Update the previous time for the next calculation

        return diskBusyTime
    }

    fun diskWrite(): Double {
        disk.updateAttributes() // Update disk attributes
        val diskWrites = disk.writes.toDouble()

        val alertToCheck = alertManager.alerts.find { it.metric == "disk_write" }
            ?: throw IllegalStateException("No alert found for Disk writes")
        alertManager.checkAlerts(
            alert = alertToCheck,
            value = diskWrites,
            host = getDeviceType()
        )

        return diskWrites
    }

    fun diskRead(): Double {
        disk.updateAttributes() // Update disk attributes
        val diskReads = disk.reads.toDouble()

        val alertToCheck = alertManager.alerts.find { it.metric == "disk_read" }
            ?: throw IllegalStateException("No alert found for Disk reads")
        alertManager.checkAlerts(
            alert = alertToCheck,
            value = diskReads,
            host = getDeviceType()
        )

        return diskReads
    }

    override fun networkRecv(): Double {
        networkInterface.updateAttributes() // Update network attributes
        val bytesRecv = networkInterface.bytesRecv.toDouble()

        val alertToCheck = alertManager.alerts.find { it.metric == "network_recv" }
            ?: throw IllegalStateException("No alert found for Network received")
        alertManager.checkAlerts(
            alert = alertToCheck,
            value = bytesRecv,
            host = getDeviceType()
        )

        return bytesRecv
    }

    override fun networkSent(): Double {
        networkInterface.updateAttributes() // Update network attributes
        val bytesSent = networkInterface.bytesSent.toDouble()

        val alertToCheck = alertManager.alerts.find { it.metric == "network_sent" }
            ?: throw IllegalStateException("No alert found for Network sent")
        alertManager.checkAlerts(
            alert = alertToCheck,
            value = bytesSent,
            host = getDeviceType()
        )

        return bytesSent
    }

    override fun getDeviceType(): String {
        return System.getProperty("os.name").replace(Regex("\\s.*"), "")
    }

    override fun run(): JVMSystemInfoData {
        val cpuRes = cpuUsage()
        val memoryRes = memoryUsage()
        val networkRecvRes = networkRecv()
        val networkSentRes = networkSent()
        val deviceTypeRes = getDeviceType()
        val diskRes = diskUsage()
        val diskWriteRes = diskWrite()
        val diskReadRes = diskRead()

        return JVMSystemInfoData(cpuRes, memoryRes, networkRecvRes, networkSentRes, deviceTypeRes, diskRes, diskWriteRes, diskReadRes)
    }

    private fun printResults(stats: JVMSystemInfoData) {
        println("CPU Usage: ${stats.cpuUsage}%")
        println("Memory Usage: ${stats.memoryUsage}%")
        println("Disk Usage: ${stats.diskUsage}%")
        println("Disk Write: ${stats.diskWrite} bytes")
        println("Disk Read: ${stats.diskRead} bytes")
        println("Network Received: ${stats.networkRecv} bytes")
        println("Network Sent: ${stats.networkSent} bytes")
        println("Device Type: ${stats.deviceType}")
        println("--------------------------------------------------")
    }

    fun main() {
        try {
            println("Monitoring system resources... (Press Ctrl+C to stop)")
            println("$HOST's System Monitor")
            println("--------------------------------------------------")

            while (true) {
                val systemStats = run()
                printResults(systemStats)

                Thread.sleep(interval)
            }
        } catch (e: InterruptedException) {
            println("Monitoring interrupted.")
        }
    }
}