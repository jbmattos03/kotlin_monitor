package shared

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AndroidAlertManager(
    private val context: Context,
    private val deviceType: DeviceTypes,
    override val host: String,
    private val thresholdConfig: ThresholdConfig
): AlertManager {
    override val alerts = mutableListOf<Alert>()
    private val hostIdentifier: DeviceCategory = deviceType.getCategory(host)

    override fun addAlert(alert: Alert) {
        if (alert in alerts) {
            log("Alert already exists")
        }
        else {
            alerts.add(alert)
        }
    }

    fun initializeAlerts(thresholdConfig: ThresholdConfig) {
        alerts.clear()

        val supportedMetrics = listOf("cpu_usage", "memory_usage", "temperature", "network_recv", "network_sent")

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

            addAlert(alert)
        }
}

    override fun removeAlert(alert: Alert) {
        if (alert in alerts) {
            alerts.remove(alert)
        }
        else {
            log("Alert does not exist")
        }
    }

    override fun checkAlerts(alert: Alert, value: Double, host: String) {
        val managedAlert = alerts.find { it.metric == alert.metric && it.host == host }

        if (managedAlert != null) {
            if (value > managedAlert.threshold) {
                log("Alert ${managedAlert.metric} for host $host TRIGGERED: value ($value) > threshold (${managedAlert.threshold})")

                managedAlert.value = value
                managedAlert.setTimestamp()
                sendAlerts(listOf(managedAlert))
            } else {
                log("Alert ${managedAlert.metric} for host $host NOT TRIGGERED: value ($value) <= threshold (${managedAlert.threshold})")
            }
        }
    }

    override fun sendAlerts(alertsToSend: List<Alert>) {
        if (alertsToSend.isEmpty()) {
            log("No alerts to send.")
            return
        }

        log("Attempting to append ${alertsToSend.size} alert(s) to alerts.json: $alertsToSend")

        Thread {
            try {
                val json = Json {
                    prettyPrint = true
                    encodeDefaults = true
                }
                // Each call to sendAlerts will serialize the passed list (often a single alert)
                // as a complete JSON array.
                val jsonString = json.encodeToString(alertsToSend)

                val file = File(context.filesDir, "alerts.json")

                // Using FileOutputStream in append mode (true)
                FileOutputStream(file, true).use { fos ->
                    fos.write(jsonString.toByteArray())
                    // Add a newline after each JSON entry (which is a JSON array in this case)
                    // This makes the file more manageable if you're reading it line by line later.
                    fos.write("\n".toByteArray())
                    log("Alert(s) appended successfully to ${file.absolutePath}")
                }
            } catch (e: IOException) {
                e.printStackTrace()
                log("IOException while appending alerts: ${e.message}")
            } catch (e: kotlinx.serialization.SerializationException) {
                e.printStackTrace()
                log("SerializationException while appending alerts: ${e.message}")
            } catch (e: Exception) {
                e.printStackTrace()
                log("Unexpected error while appending alerts: ${e.message}")
            }
        }.start()
    }

    override fun getAlertsByMetric(metric: String): List<Alert> {
        return alerts.filter { it.metric == metric }
    }

    fun setThreshold(metric: String, threshold: Double) {
        val existingAlert = alerts.find { it.metric == metric }
        if (existingAlert != null) {
            existingAlert.threshold = threshold
        }
        else {
            val newAlert = Alert(metric, threshold)
            addAlert(newAlert)
        }
    }

    private fun log(string: String) {
        Log.d("AndroidAlertManager", "message: $string")
    }
}