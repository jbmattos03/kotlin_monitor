package otelHandler

import shared.AndroidSystemMonitor
import shared.ThresholdConfig
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
import android.util.Log
import android.content.Context

class OTelConfig(private val systemMonitor: AndroidSystemMonitor) {
    private val interval: Long = systemMonitor.interval
    private val device: String = systemMonitor.getDeviceType()
    private lateinit var provider: SdkMeterProvider
    private lateinit var exporter: OtlpHttpMetricExporter

    // init function
    fun init(context: Context) {
        val dotenv = try {
            dotenv {
                directory = "/res/raw"
                filename = "dotenv"
            }
        } catch(error: Exception) {
            error.printStackTrace()
            throw RuntimeException("Error loading environment variables")
        }

        val ip = dotenv["IP_ADDR"] ?: ""
        val serviceName = "${device}-system-monitor"
        val otlpEndpoint = "http://${ip}:4318/v1/metrics"

        val resource = createResource(serviceName)
        exporter = createExporter(otlpEndpoint)
        val reader = createMeterReader(exporter)
        provider = createProvider(resource, reader)
        val meter = createMeter(provider, serviceName)
        createMetrics(meter)

        log("System monitoring started.")
        log("$device's System Monitor")

    }
    // Create a resource
    private fun createResource(serviceName: String): Resource {
        return Resource.getDefault().toBuilder()
        .put(AttributeKey.stringKey("service.name"), serviceName)
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
    private fun createMeter(provider: SdkMeterProvider, serviceName: String): Meter {
        return provider.get(serviceName)
    }

    // Create an exporter
    private fun createExporter(endpoint: String): OtlpHttpMetricExporter {
        return OtlpHttpMetricExporter.builder()
            .setEndpoint(endpoint)
            .setTimeout(Duration.ofSeconds(interval / 1000)) // Convert milliseconds to seconds
            .build()
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

        meter.gaugeBuilder("network_recv")
            .buildWithCallback { measurement: ObservableDoubleMeasurement ->
                measurement.record(systemMonitor.networkRecv())
            }

        meter.gaugeBuilder("network_sent")
            .buildWithCallback { measurement: ObservableDoubleMeasurement ->
                measurement.record(systemMonitor.networkSent())
            }
        
        meter.gaugeBuilder("temperature")
            .buildWithCallback { measurement: ObservableDoubleMeasurement ->
                measurement.record(systemMonitor.temperature())
            }
    }

    fun shutdown() {
        provider?.shutdown()
        exporter?.close()
        log("System monitoring stopped.")
    }

    fun log(string: String) {
        Log.d("OTelConfig", "message: $string")
    }
}