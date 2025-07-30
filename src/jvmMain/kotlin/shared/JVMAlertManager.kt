package shared

import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import org.slf4j.LoggerFactory

class JVMAlertManager(
    private val deviceType: DeviceTypes,
    override val host: String,
    private val thresholdConfig: ThresholdConfig
): AlertManager {
    override val alerts = mutableListOf<Alert>()
    private val hostIdentifier: DeviceCategory = deviceType.getCategory(host)
    private val logger = LoggerFactory.getLogger(JVMAlertManager::class.java)

    init {
        logger.debug("Device type: $hostIdentifier")
        logger.debug("Host: $host")
    }

    override fun addAlert(alert: Alert) {
        if (alert in alerts) {
            logger.warn("Alert already exists")
        }
        else {
            alerts.add(alert)
        }
    }

    fun initializeAlerts(thresholdConfig: ThresholdConfig) {
        alerts.clear()

        val supportedMetrics = listOf("cpu_usage", "memory_usage", "disk_usage", "disk_read", "disk_write", "network_recv", "network_sent")

        supportedMetrics.forEach { metricName ->
            val thresholdValue = thresholdConfig.getThresholdOrDefault(
                deviceCategory = hostIdentifier,
                metric = metricName,
                fallbackValue = ABSOLUTE_DEFAULT_THRESHOLD
            )
            val alert = Alert(
                metric = metricName,
                threshold = thresholdValue
            )
            alert.host = host
            logger.debug("Inserting alert with name: ${alert.metric}, threshold: ${alert.threshold}, host: ${alert.host}")
            addAlert(alert)
        }
}

    override fun removeAlert(alert: Alert) {
        if (alert in alerts) {
            alerts.remove(alert)
        }
        else {
            logger.warn("Alert does not exist")
        }
    }

    override fun checkAlerts(alert: Alert, value: Double, host: String) {
        val managedAlert = alerts.find { it.metric == alert.metric && it.host == host }

        if (managedAlert != null) {
            if (value > managedAlert.threshold) {
                logger.info("Alert ${managedAlert.metric} for host $host TRIGGERED: value ($value) > threshold (${managedAlert.threshold})")

                managedAlert.value = value
                managedAlert.setTimestamp()
                sendAlerts(listOf(managedAlert))
            } else {
                logger.info("Alert ${managedAlert.metric} for host $host NOT TRIGGERED: value ($value) <= threshold (${managedAlert.threshold})")
            }
        }
    }

    override fun sendAlerts(alerts: List<Alert>) {
        if (alerts.isEmpty()) {
            logger.warn("No alerts to send.")
            return
        }

        logger.info("Attempting to append ${alerts.size} alert(s) to alerts.json")

        Thread {
            try {
                val json = Json {
                    prettyPrint = true
                    encodeDefaults = true
                }
                // Each call to sendAlerts will serialize the passed list (often a single alert)
                // as a complete JSON array.
                val jsonString = json.encodeToString(alerts)

                val appDir = File(System.getProperty("user.home"), ".kotlin_monitor")
                appDir.mkdirs() // Ensure the directory exists
                val file = File(appDir, "alerts.json")

                // Using FileOutputStream in append mode (true)
                FileOutputStream(file, true).use { fos ->
                    fos.write(jsonString.toByteArray())
                    // Add a newline after each JSON entry (which is a JSON array in this case)
                    // This makes the file more manageable if you're reading it line by line later.
                    fos.write("\n".toByteArray())
                    logger.info("Alert(s) appended successfully to ${file.absolutePath}")
                }
            } catch (e: IOException) {
                e.printStackTrace()
                logger.error("IOException while appending alerts: ${e.message}")
            } catch (e: kotlinx.serialization.SerializationException) {
                e.printStackTrace()
                logger.error("SerializationException while appending alerts: ${e.message}")
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("Unexpected error while appending alerts: ${e.message}")
            }
        }.start()
    }

    override fun getAlertsByMetric(metric: String): List<Alert> {
        return alerts.filter { it.metric == metric }
    }

    fun setThreshold(metric: String, threshold: Double) {
        logger.debug("Setting threshold for metric: $metric to $threshold")
        val existingAlert = alerts.find { it.metric == metric }
        if (existingAlert != null) {
            existingAlert.threshold = threshold
        } else {
            val newAlert = Alert(metric, threshold)
            addAlert(newAlert)
        }
    }
}