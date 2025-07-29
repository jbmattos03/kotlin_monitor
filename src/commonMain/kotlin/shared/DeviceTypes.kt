package shared

enum class DeviceCategory {
    MOBILE, DESKTOP, UNKNOWN
}

class DeviceTypes {
    fun getCategory(device: String): DeviceCategory {
        return when (device.lowercase()) {
            // Add more mobile phones with which the app is compatible
            "google sdk_gphone64_x86_64"  -> DeviceCategory.MOBILE
            // Add more operating systems with which the app is compatible
            "windows", "mac", "x86_64-conda-linux-gnu" , "linux" -> DeviceCategory.DESKTOP
            else -> DeviceCategory.UNKNOWN
        }
    }
}