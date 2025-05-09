package shared

import OTelHandler.OTelConfig

class JVMController(private val otelConfig: OTelConfig) : MetricController {
    @Volatile
    private var running: Boolean = true

    override fun start() {
        otelConfig.initialize()
        println("System Monitor started. Press Ctrl+C to stop.")

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
            println("System Monitor is already stopped.")
            return
        }
        println("Stopping System Monitor...")
        otelConfig.shutdown()
        running = false
    }
}