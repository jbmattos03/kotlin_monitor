package shared

interface AlertManager {
    val host: String
    fun addAlert(alert: Alert)
    fun removeAlert(alert: Alert)
    fun getAlerts(): List<Alert>
    fun getAlertsByMetric(metric: String): List<Alert>
    fun setThreshold(alert: Alert, threshold: Double)
    fun sendAlerts(alerts: List<Alert>)
}