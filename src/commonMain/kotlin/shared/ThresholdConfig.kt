package shared

const val ABSOLUTE_DEFAULT_THRESHOLD = 80.0

class ThresholdConfig {
    private val defaultThresholds = mapOf(
        DeviceCategory.MOBILE to mapOf(
            "cpu_usage" to 0.0,
            "memory_usage" to 60.0,
            "temperature" to 0.0,
            "network_recv" to 40000.0,
            "network_sent" to 40000.0
        ),
        DeviceCategory.DESKTOP to mapOf(
            "cpu_usage" to 50.0,
            "memory_usage" to 50.0,
            "disk_usage" to 50.0,
            "disk_read" to 10000.0,
            "disk_write" to 10000.0,
            "network_recv" to 100000.0,
            "network_sent" to 100000.0
        ),
        DeviceCategory.UNKNOWN to mapOf(
            "cpu_usage" to ABSOLUTE_DEFAULT_THRESHOLD,
            "memory_usage" to ABSOLUTE_DEFAULT_THRESHOLD,
            "network_recv" to ABSOLUTE_DEFAULT_THRESHOLD,
            "network_sent" to ABSOLUTE_DEFAULT_THRESHOLD
        )
    )

    fun getThresholdOrDefault(deviceCategory: DeviceCategory, metric: String, fallbackValue: Double): Double {
        return defaultThresholds[deviceCategory]?.get(metric) ?: fallbackValue
    }
}

