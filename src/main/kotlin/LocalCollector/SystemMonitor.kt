package LocalCollector

import io.github.cdimascio.dotenv.dotenv
import oshi.SystemInfo
import oshi.hardware.HWDiskStore
import oshi.hardware.CentralProcessor
import oshi.hardware.GlobalMemory
import oshi.hardware.NetworkIF

// Environment variables
val dotenv = dotenv()
val IP_ADDR = dotenv["IP_ADDR"] ?: ""
val HOST = dotenv["HOST"] ?: "localhost"

// Creating a data class to hold the system information
data class SystemInfoData(
    val cpuUsage: Double,
    val memoryUsage: Double,
    val diskUsage: Double,
    val diskWrite: Long,
    val diskRead: Long,
    val networkRecv: Long,
    val networkSent: Long
)

class SystemMonitor() {
    // Properties
    val processor: CentralProcessor
    var prevTicks: LongArray
    val memory: GlobalMemory
    val disk: HWDiskStore
    var diskPrevTime: Long
    val networkInterface: NetworkIF
    val interval: Long = 5000 // 5 seconds

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

    fun cpuUsage(): Double {
        val currTicks = processor.systemCpuLoadTicks
        val cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100

        prevTicks = currTicks // Update the previous ticks for the next calculation

        return cpuLoad
    }

    fun memoryUsage(): Double {
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
        val diskWrites = disk.getWrites()

        return diskWrites
    }

    fun diskRead(): Long {
        disk.updateAttributes() // Update disk attributes
        val diskReads = disk.getReads()

        return diskReads
    }

    fun networkRecv(): Long {
        networkInterface.updateAttributes() // Update network attributes
        val bytesRecv = networkInterface.getBytesRecv()

        return bytesRecv
    }

    fun networkSent(): Long {
        networkInterface.updateAttributes() // Update network attributes
        val bytesSent = networkInterface.getBytesSent()

        return bytesSent
    }

    fun run(): SystemInfoData {
        val cpuRes = cpuUsage()
        val memoryRes = memoryUsage()
        val diskRes = diskUsage()
        val diskWriteRes = diskWrite()
        val diskReadRes = diskRead()
        val networkRecvRes = networkRecv()
        val networkSentRes = networkSent()

        return SystemInfoData(cpuRes, memoryRes, diskRes, diskWriteRes, diskReadRes, networkRecvRes, networkSentRes)
    }

    fun printResults(stats: SystemInfoData) {
        println("CPU Usage: ${stats.cpuUsage}%")
        println("Memory Usage: ${stats.memoryUsage}%")
        println("Disk Usage: ${stats.diskUsage}%")
        println("Disk Write: ${stats.diskWrite} bytes")
        println("Disk Read: ${stats.diskRead} bytes")
        println("Network Received: ${stats.networkRecv} bytes")
        println("Network Sent: ${stats.networkSent} bytes")
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