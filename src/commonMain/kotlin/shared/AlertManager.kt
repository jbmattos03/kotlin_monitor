package shared

interface AlertManager {
    val host: String?
    val alerts: MutableList<Alert>
    fun addAlert(alert: Alert)
    fun removeAlert(alert: Alert)
    fun getAlertsByMetric(metric: String): List<Alert>
    fun checkAlerts(alert: Alert, value: Double, host: String)
    fun sendAlerts(alerts: List<Alert>)
}