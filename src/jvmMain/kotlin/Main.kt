import OTelHandler.OTelConfig
import shared.JVMSystemMonitor
import shared.JVMController

fun main() {
    val systemMonitor = JVMSystemMonitor()
    val otel = OTelConfig(systemMonitor)
    val controller = JVMController(otel)
    controller.start()
}