package com.sunflower.shortcut.automation.triggers

/**
 * Every value a scenario can pass to on(...), e.g. on("charging", ...).
 * Kept as a flat enum rather than free-form strings so TriggerRegistry can
 * exhaustively `when` over the ones that map to a real Android listener.
 */
enum class TriggerType(val key: String, val label: String) {
    CHARGING_CONNECTED("charging", "Подключена зарядка"),
    CHARGING_DISCONNECTED("charging_disconnected", "Отключена зарядка"),
    BATTERY_LOW("battery_low", "Низкий заряд батареи"),
    WIFI_CONNECTED("wifi_connected", "Подключение к Wi-Fi"),
    WIFI_DISCONNECTED("wifi_disconnected", "Отключение от Wi-Fi"),
    BLUETOOTH_CONNECTED("bluetooth_connected", "Подключение Bluetooth-устройства"),
    BLUETOOTH_DISCONNECTED("bluetooth_disconnected", "Отключение Bluetooth-устройства"),
    BOOT_COMPLETED("boot_completed", "Включение телефона"),
    MANUAL("manual", "Ручной запуск");

    companion object {
        fun fromKey(key: String): TriggerType? = entries.firstOrNull { it.key == key }
    }
}
