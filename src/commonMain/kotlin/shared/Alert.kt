package shared

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable

@Serializable
class Alert(
    var metric: String,
    var threshold: Double

) {
    var value: Double? = null
    var timestamp: String? = null
    var host: String? = null

    fun setTimestamp() {
        this.timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }
}