package shared

import io.github.cdimascio.dotenv.dotenv
import oshi.SystemInfo
import oshi.hardware.HWDiskStore
import oshi.hardware.CentralProcessor
import oshi.hardware.GlobalMemory
import oshi.hardware.NetworkIF
import org.slf4j.LoggerFactory

data class JVMSystemInfoData(
    override var cpuUsage: Double,
    override var memoryUsage: Double,
    override var networkRecv: Double,
    override var networkSent: Double,
    var deviceOS: String,
    var diskUsage: Double,
    var diskWrite: Double,
    var diskRead: Double
) : SystemInfoData

// Environment variables
val dotenv = dotenv()
val IP_ADDR = dotenv["IP_ADDR"] ?: ""
val HOST = dotenv["HOST"] ?: "localhost"
private val logger = LoggerFactory.getLogger(JVMSystemMonitor::class.java)

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
            host = getDeviceOS(),
            thresholdConfig = ThresholdConfig()
        )

    init {
        logger.debug("Host: $HOST")
        // Initialize SystemInfo and AlertManager
        alertManager.initializeAlerts(thresholdConfig = ThresholdConfig())
        val systemInfo = SystemInfo()

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
        logger.info("Calculating CPU usage...")
        val currTicks = processor.systemCpuLoadTicks
        val cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100

        val alertToCheck = alertManager.alerts.find { it.metric == "cpu_usage" }
        if (alertToCheck != null) {
            alertManager.checkAlerts(
                alertToCheck,
                cpuLoad,
                getDeviceOS()
            )
        }

        prevTicks = currTicks // Update the previous ticks for the next calculation

        return cpuLoad
    }

    override fun memoryUsage(): Double {
        logger.info("Calculating Memory usage...")
        val totalMemory = memory.total
        val availableMemory = memory.available
        val usedMemory = totalMemory - availableMemory
        val usedMemoryPercentage = (usedMemory / totalMemory.toDouble()) * 100

        val alertToCheck = alertManager.alerts.find { it.metric == "memory_usage" }
        logger.info("Value for memory usage: ${usedMemoryPercentage}%")
        if (alertToCheck != null) {
            alertManager.checkAlerts(
                alertToCheck,
                usedMemoryPercentage,
                getDeviceOS()
            )
        }

        return usedMemoryPercentage
    }

    fun diskUsage(): Double {
        logger.info("Calculating Disk usage...")
        disk.updateAttributes() // Update disk attributes
        val diskCurrTime = disk.transferTime
        val diskBusyTime = ((diskCurrTime - diskPrevTime).toDouble() / interval) * 100

        val alertToCheck = alertManager.alerts.find { it.metric == "disk_usage" }
        logger.info("Value for disk usage: ${diskBusyTime}%")
        if (alertToCheck != null) {
            alertManager.checkAlerts(
                alertToCheck,
                diskBusyTime,
                getDeviceOS()
            )
        } else {
            logger.warn("No alert found for Disk usage")
        }

        diskPrevTime = diskCurrTime // Update the previous time for the next calculation

        return diskBusyTime
    }

    fun diskWrite(): Double {
        logger.info("Calculating Disk write...")
        disk.updateAttributes() // Update disk attributes
        val diskWrites = disk.writes.toDouble()

        val alertToCheck = alertManager.alerts.find { it.metric == "disk_write" }
        logger.info("Value for disk write: ${diskWrites} bytes")
        if (alertToCheck != null) {
            alertManager.checkAlerts(
                alertToCheck,
                diskWrites,
                getDeviceOS()
            )
        }

        return diskWrites
    }

    fun diskRead(): Double {
        logger.info("Calculating Disk read...")
        disk.updateAttributes() // Update disk attributes
        val diskReads = disk.reads.toDouble()

        val alertToCheck = alertManager.alerts.find { it.metric == "disk_read" }
        logger.info("Value for disk read: ${diskReads} bytes")
        if (alertToCheck != null) {
            alertManager.checkAlerts(
                alertToCheck,
                diskReads,
                getDeviceOS()
            )
        }

        return diskReads
    }

    override fun networkRecv(): Double {
        logger.info("Calculating Network received...")
        networkInterface.updateAttributes() // Update network attributes
        val bytesRecv = networkInterface.bytesRecv.toDouble()

        val alertToCheck = alertManager.alerts.find { it.metric == "network_recv" }
        logger.info("Value for network received: ${bytesRecv} bytes")
        if (alertToCheck != null) {
            alertManager.checkAlerts(
                alertToCheck,
                bytesRecv,
                getDeviceOS()
            )
        }


        return bytesRecv
    }

    override fun networkSent(): Double {
        logger.info("Calculating Network sent...")
        networkInterface.updateAttributes() // Update network attributes
        val bytesSent = networkInterface.bytesSent.toDouble()

        val alertToCheck = alertManager.alerts.find { it.metric == "network_sent" }
        logger.info("Value for network sent: ${bytesSent} bytes")
        if (alertToCheck != null) {
            alertManager.checkAlerts(
                alert = alertToCheck,
                value = bytesSent,
                host = getDeviceOS()
            )
        }

        return bytesSent
    }

    fun getDeviceOS(): String {
        logger.info("Getting device OS...")
        val device = System.getProperty("os.name").replace(Regex("\\s.*"), "")
        logger.debug("Detected device OS: $device")

        return device
    }

    override fun run(): JVMSystemInfoData {
        val cpuRes = cpuUsage()
        val memoryRes = memoryUsage()
        val networkRecvRes = networkRecv()
        val networkSentRes = networkSent()
        val deviceOSRes = getDeviceOS()
        val diskRes = diskUsage()
        val diskWriteRes = diskWrite()
        val diskReadRes = diskRead()

        return JVMSystemInfoData(cpuRes, memoryRes, networkRecvRes, networkSentRes, deviceOSRes, diskRes, diskWriteRes, diskReadRes)
    }

    private fun printResults(stats: JVMSystemInfoData) {
        println("CPU Usage: ${stats.cpuUsage}%")
        println("Memory Usage: ${stats.memoryUsage}%")
        println("Disk Usage: ${stats.diskUsage}%")
        println("Disk Write: ${stats.diskWrite} bytes")
        println("Disk Read: ${stats.diskRead} bytes")
        println("Network Received: ${stats.networkRecv} bytes")
        println("Network Sent: ${stats.networkSent} bytes")
        println("Device OS: ${stats.deviceOS}")
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