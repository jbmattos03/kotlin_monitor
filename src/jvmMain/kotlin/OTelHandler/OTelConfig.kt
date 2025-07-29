package otelHandler

import shared.JVMSystemMonitor
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement
import io.opentelemetry.api.metrics.ObservableLongMeasurement
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import java.time.Duration
import io.github.cdimascio.dotenv.dotenv
import org.slf4j.LoggerFactory

// Environment variables
val dotenv = dotenv()
var IP_ADDR = dotenv["IP_ADDR"] ?: ""
var HOST = dotenv["HOST"] ?: "localhost"
var SERVICE_NAME = "$HOST-system-monitor"
var OTLP_ENDPOINT = "http://$IP_ADDR:4318/v1/metrics"

class OTelConfig(private val systemMonitor: JVMSystemMonitor) {
    private val interval: Long = systemMonitor.interval
    private lateinit var provider: SdkMeterProvider
    private lateinit var exporter: OtlpHttpMetricExporter
    private val logger = LoggerFactory.getLogger(OTelConfig::class.java)

    init {
        logger.debug("Host: $HOST")
    }

    // Create a resource
    private fun createResource(): Resource {
        return Resource.getDefault().toBuilder()
        .put(AttributeKey.stringKey("service.name"), SERVICE_NAME)
        .build()
    }

     // Create a meter reader
    private fun createMeterReader(exporter: OtlpHttpMetricExporter): PeriodicMetricReader {
        return PeriodicMetricReader.builder(exporter)
            .setInterval(Duration.ofSeconds(5)) // 5 seconds
            .build()
    }

    // Create a meter provider
    private fun createProvider(resource: Resource, reader: PeriodicMetricReader): SdkMeterProvider {
        return SdkMeterProvider.builder()
            .setResource(resource)
            .registerMetricReader(reader)
            .build()
    }

    // Create a meter
    private fun createMeter(provider: SdkMeterProvider): Meter {
        return provider.get(SERVICE_NAME)
    }

    // Create an exporter
    private fun createExporter(): OtlpHttpMetricExporter {
        return OtlpHttpMetricExporter.builder()
            .setEndpoint(OTLP_ENDPOINT)
            .setTimeout(Duration.ofSeconds(interval / 1000)) // Convert milliseconds to seconds
            .build();
    }

    // Create metrics
    fun createMetrics(meter: Meter) {
        meter.gaugeBuilder("cpu_usage")
            .buildWithCallback { measurement: ObservableDoubleMeasurement ->
                measurement.record(systemMonitor.cpuUsage())
            }

        meter.gaugeBuilder("memory_usage")
            .buildWithCallback { measurement: ObservableDoubleMeasurement ->
                measurement.record(systemMonitor.memoryUsage())
            }

        meter.gaugeBuilder("disk_usage")
            .buildWithCallback { measurement: ObservableDoubleMeasurement ->
                measurement.record(systemMonitor.diskUsage())
            }

        meter.gaugeBuilder("disk_write")
            .buildWithCallback { measurement: ObservableDoubleMeasurement ->
                measurement.record(systemMonitor.diskWrite())
            }

        meter.gaugeBuilder("disk_read")
            .buildWithCallback { measurement: ObservableDoubleMeasurement ->
                measurement.record(systemMonitor.diskRead())
            }

        meter.gaugeBuilder("network_recv")
            .buildWithCallback { measurement: ObservableDoubleMeasurement ->
                measurement.record(systemMonitor.networkRecv())
            }

        meter.gaugeBuilder("network_sent")
            .buildWithCallback { measurement: ObservableDoubleMeasurement ->
                measurement.record(systemMonitor.networkSent())
            }
    }

    // Main function
    fun initialize() {
        val resource = createResource()
        exporter = createExporter()
        val reader = createMeterReader(exporter)
        provider = createProvider(resource, reader)
        val meter = createMeter(provider)
        createMetrics(meter)

        logger.info("System Monitor started. Press Ctrl+C to stop.")
        logger.info("$HOST's System Monitor")
        logger.debug("Device type: ${systemMonitor.getDeviceType()}")
    }

    fun shutdown() {
        provider?.shutdown()
        exporter?.close()
        logger.info("System monitoring stopped.")
    }
}