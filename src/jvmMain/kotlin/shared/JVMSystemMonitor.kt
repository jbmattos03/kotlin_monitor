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
    override var networkRecv: Long,
    override var networkSent: Long,
    override var deviceType: String,
    var diskUsage: Double,
    var diskWrite: Long,
    var diskRead: Long
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

    init {
        val systemInfo = SystemInfo()

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

        prevTicks = currTicks // Update the previous ticks for the next calculation

        return cpuLoad
    }

    override fun memoryUsage(): Double {
        val totalMemory = memory.total
        val availableMemory = memory.available
        val usedMemory = totalMemory - availableMemory
        val usedMemoryPercentage = (usedMemory / totalMemory.toDouble()) * 100

        return usedMemoryPercentage
    }

    fun diskUsage(): Double {
        disk.updateAttributes() // Update disk attributes
        val diskCurrTime = disk.transferTime
        val diskBusyTime = ((diskCurrTime - diskPrevTime).toDouble() / interval) * 100
        
        diskPrevTime = diskCurrTime // Update the previous time for the next calculation

        return diskBusyTime
    }

    fun diskWrite(): Long {
        disk.updateAttributes() // Update disk attributes
        val diskWrites = disk.writes

        return diskWrites
    }

    fun diskRead(): Long {
        disk.updateAttributes() // Update disk attributes
        val diskReads = disk.reads

        return diskReads
    }

    override fun networkRecv(): Long {
        networkInterface.updateAttributes() // Update network attributes
        val bytesRecv = networkInterface.bytesRecv

        return bytesRecv
    }

    override fun networkSent(): Long {
        networkInterface.updateAttributes() // Update network attributes
        val bytesSent = networkInterface.bytesSent

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