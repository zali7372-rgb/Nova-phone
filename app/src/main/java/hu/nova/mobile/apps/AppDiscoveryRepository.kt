package hu.nova.mobile.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import hu.nova.mobile.domain.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Known aliases for common apps, in Hungarian and English, on top of whatever the
 * PackageManager-reported label already is. Matching against these (see
 * [AppMatcher]) is what lets "Nyisd meg a YouTube-ot" or "böngésző" resolve correctly
 * even though the installed label is just "YouTube" / "Chrome".
 */
private val KNOWN_ALIASES: Map<String, List<String>> = mapOf(
    "com.google.android.youtube" to listOf("youtube", "yt", "videók", "videok"),
    "com.android.chrome" to listOf("chrome", "böngésző", "bongeszo", "internet"),
    "com.android.settings" to listOf("beállítások", "beallitasok", "settings"),
    "com.google.android.dialer" to listOf("telefon", "hívás", "hivas", "phone", "dialer"),
    "com.google.android.apps.messaging" to listOf("üzenetek", "uzenetek", "messages", "sms"),
    "com.google.android.gm" to listOf("gmail", "email", "e-mail", "levelezés", "levelezes"),
    "com.google.android.apps.maps" to listOf("térkép", "terkep", "maps", "navigáció", "navigacio"),
    "com.spotify.music" to listOf("spotify", "zene", "music"),
    "com.whatsapp" to listOf("whatsapp"),
    "com.instagram.android" to listOf("instagram", "insta"),
    "com.facebook.katana" to listOf("facebook", "fészbuk", "feszbuk"),
    "com.google.android.apps.photos" to listOf("fotók", "fotok", "photos", "galéria", "galeria"),
    "com.android.camera2" to listOf("kamera", "camera")
)

/**
 * Discovers installed applications dynamically via [PackageManager] - NOVA never ships
 * a hard-coded app list as its source of truth; [KNOWN_ALIASES] only *augments* real,
 * queried packages with extra Hungarian/English names to match against.
 */
class AppDiscoveryRepository(private val context: Context) {

    suspend fun getLaunchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

        resolveInfos.map { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(packageManager).toString()
            AppInfo(
                packageName = packageName,
                label = label,
                aliases = KNOWN_ALIASES[packageName].orEmpty()
            )
        }.distinctBy { it.packageName }
    }

    /** Returns true and launches the app, or false if it isn't found - never pretends to succeed. */
    suspend fun launchApp(appInfo: AppInfo): Boolean = withContext(Dispatchers.Main) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName) ?: return@withContext false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        true
    }
}
