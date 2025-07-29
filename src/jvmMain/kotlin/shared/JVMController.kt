package shared

import otelHandler.OTelConfig
import org.slf4j.LoggerFactory

class JVMController(private val otelConfig: OTelConfig) : MetricController {
    @Volatile
    private var running: Boolean = true
    private val logger = LoggerFactory.getLogger(JVMController::class.java)

    override fun start() {
        otelConfig.initialize()

        // Add shutdown hook to handle Ctrl+C
        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
        })

        // Keep the program running
        while (running) {
            try {
                Thread.sleep(5)
            } catch (e: InterruptedException) {
                // Ignore, will exit loop if running is false
            }
        }
    }

    override fun stop() {
        if (!running) {
            logger.warn("System Monitor is already stopped.")
            return
        }
        logger.info("Stopping System Monitor...")
        otelConfig.shutdown()
        running = false
    }
}