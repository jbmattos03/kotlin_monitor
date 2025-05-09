package shared

import OTelHandler.OTelConfig

class AndroidController(private val otelConfig: OTelConfig) : MetricController {
    override fun start() {
        otelConfig.initialize()
    }

    override fun stop() {
        otelConfig.shutdown()
    }
}
