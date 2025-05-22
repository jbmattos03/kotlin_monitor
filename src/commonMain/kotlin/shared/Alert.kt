package shared

interface Alert {
    val host: String
    val metric: String
    val value: Double
    val threshold: Double
    val timestamp: String
}