import otelHandler.OTelConfig
import shared.JVMSystemMonitor
import shared.JVMController
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Main")

fun main() {
    logger.info("Starting JVM monitoring...")
    
    logger.debug("Initializing JVM System Monitor and OTel configuration...")
    val systemMonitor = JVMSystemMonitor()
    val otel = OTelConfig(systemMonitor)
    logger.debug("JVM System Monitor and OTel configuration initialized successfully.")

    val controller = JVMController(otel)
    controller.start()
}