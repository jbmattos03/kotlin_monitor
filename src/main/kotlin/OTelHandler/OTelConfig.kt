package OTelHandler

import LocalCollector.SystemMonitor
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

class OTelConfig {
    // Environment variables
    val dotenv = dotenv()
    val IP_ADDR = dotenv["IP_ADDR"] ?: ""
    val HOST = dotenv["HOST"] ?: "localhost"
    val SERVICE_NAME = "$HOST-system-monitor"
    val OTLP_ENDPOINT = "http://$IP_ADDR:4318/v1/metrics"

    val systemMonitor: SystemMonitor = SystemMonitor()
    val interval: Long = systemMonitor.interval

    // Create a resource
    fun createResource(): Resource {
        return Resource.getDefault().toBuilder()
        .put(AttributeKey.stringKey("service.name"), SERVICE_NAME)
        .build()
    }

     // Create a meter reader
    fun createMeterReader(exporter: OtlpHttpMetricExporter): PeriodicMetricReader {
        return PeriodicMetricReader.builder(exporter)
            .setInterval(Duration.ofSeconds(5)) // 5 seconds
            .build()
    }

    // Create a meter provider
    fun createProvider(resource: Resource, reader: PeriodicMetricReader): SdkMeterProvider {
        return SdkMeterProvider.builder()
            .setResource(resource)
            .registerMetricReader(reader)
            .build()
    }

    // Create a meter
    fun createMeter(provider: SdkMeterProvider): Meter {
        return provider.get(SERVICE_NAME)
    }

    // Create an exporter
    fun createExporter(): OtlpHttpMetricExporter {
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
            .ofLongs()
            .buildWithCallback { measurement: ObservableLongMeasurement ->
                measurement.record(systemMonitor.diskWrite())
            }

        meter.gaugeBuilder("disk_read")
            .ofLongs()
            .buildWithCallback { measurement: ObservableLongMeasurement ->
                measurement.record(systemMonitor.diskRead())
            }

        meter.gaugeBuilder("network_recv")
            .ofLongs()
            .buildWithCallback { measurement: ObservableLongMeasurement ->
                measurement.record(systemMonitor.networkRecv())
            }

        meter.gaugeBuilder("network_sent")
            .ofLongs()
            .buildWithCallback { measurement: ObservableLongMeasurement ->
                measurement.record(systemMonitor.networkSent())
            }
    }

    // Main function
    fun main() {
        val resource = createResource()
        val exporter = createExporter()
        val reader = createMeterReader(exporter)
        val provider = createProvider(resource, reader)
        val meter = createMeter(provider)
        createMetrics(meter)

        // Add a graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            provider.shutdown()
            exporter.close()
        })

        // Keep the application running to allow metrics to be collected
        println("System Monitor started. Press Ctrl+C to stop.")
        println("$HOST's System Monitor")
        while (true) {
            Thread.sleep(interval)
        }
    }
}