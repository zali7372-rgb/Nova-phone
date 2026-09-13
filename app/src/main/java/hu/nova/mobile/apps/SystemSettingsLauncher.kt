package hu.nova.mobile.apps

import android.content.Context
import android.content.Intent
import android.provider.Settings

enum class SystemSettingsTarget(val action: String, val aliases: List<String>) {
    WIFI(Settings.ACTION_WIFI_SETTINGS, listOf("wifi", "wi-fi", "vezeték nélküli")),
    BLUETOOTH(Settings.ACTION_BLUETOOTH_SETTINGS, listOf("bluetooth", "blútúsz")),
    DISPLAY(Settings.ACTION_DISPLAY_SETTINGS, listOf("kijelző", "kijelzo", "display", "fényerő", "fenyero")),
    SOUND(Settings.ACTION_SOUND_SETTINGS, listOf("hang", "sound", "hangerő", "hangero")),
    NOTIFICATIONS(Settings.ACTION_APP_NOTIFICATION_SETTINGS, listOf("értesítések", "ertesitesek", "notifications")),
    BATTERY(Settings.ACTION_BATTERY_SAVER_SETTINGS, listOf("akkumulátor", "akkumulator", "battery")),
    APPS(Settings.ACTION_APPLICATION_SETTINGS, listOf("alkalmazások", "alkalmazasok", "apps")),
    ACCESSIBILITY(Settings.ACTION_ACCESSIBILITY_SETTINGS, listOf("kisegítő lehetőségek", "kisegito lehetosegek", "accessibility")),
    DATE_TIME(Settings.ACTION_DATE_SETTINGS, listOf("dátum", "datum", "idő", "ido", "date", "time"));

    companion object {
        fun matchByAlias(text: String): SystemSettingsTarget? {
            val normalized = text.trim().lowercase()
            return entries.firstOrNull { target -> target.aliases.any { normalized.contains(it) } }
        }
    }
}

/**
 * Opens the requested Android system settings screen. Every action here is a real,
 * documented public Settings intent action - there is no simulated toggling of system
 * state (NOVA does not have platform-signature permissions to flip Wi-Fi/Bluetooth
 * directly on modern Android; opening the relevant settings screen is the correct,
 * legitimate behavior for a normal app).
 */
object SystemSettingsLauncher {
    fun open(context: Context, target: SystemSettingsTarget): Boolean {
        return try {
            val intent = Intent(target.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
