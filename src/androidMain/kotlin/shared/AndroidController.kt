package shared

import otelHandler.OTelConfig
import android.content.Context

class AndroidController(
    private val otelConfig: OTelConfig,
    private val context: Context
) : MetricController {
    override fun start() {
        otelConfig.init(context.applicationContext)
    }

    override fun stop() {
        otelConfig.shutdown()
    }
}
