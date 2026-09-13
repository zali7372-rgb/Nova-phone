package hu.nova.mobile.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import hu.nova.mobile.MainActivity
import hu.nova.mobile.NovaApplication
import hu.nova.mobile.R
import hu.nova.mobile.voice.BackgroundListeningEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Foreground service that keeps NOVA listening for the wake word after the user leaves
 * the app (Home button, switching apps, screen off) - see [BackgroundListeningEngine] and
 * [hu.nova.mobile.voice.WakeWordManager] for exactly what this can and cannot do.
 *
 * It is only ever running because the user explicitly turned on "Wake word" in Settings
 * (or tapped a manual "start listening" action); it always shows an ongoing notification
 * while active, and it stops the moment the user taps "Stop" on that notification or turns
 * the setting back off - it never listens silently or indefinitely without the user's
 * knowledge, and it never survives the app being force-stopped or fully killed by the OS.
 */
class NovaVoiceService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var engine: BackgroundListeningEngine? = null

    @Volatile private var isHandlingCommand = false

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        val app = application as NovaApplication
        engine = BackgroundListeningEngine(
            context = this,
            chatRepository = app.chatRepository,
            memoryRepository = app.memoryRepository,
            settingsRepository = app.settingsRepository,
            commandRouter = app.commandRouter,
            aiProviderFactory = app.aiProviderFactory,
            scope = serviceScope,
            onActivityChanged = { active ->
                isHandlingCommand = active
                updateNotification()
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            engine?.stop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        engine?.start()
        // START_STICKY: if the system kills this process under memory pressure, Android
        // will try to recreate and restart it. It will resume listening for the wake word,
        // but any in-progress command is lost - this is disclosed in the README, it is not
        // a guarantee of uninterrupted background listening.
        return START_STICKY
    }

    override fun onDestroy() {
        engine?.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, NovaVoiceService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isHandlingCommand) {
            getString(R.string.home_status_thinking)
        } else {
            getString(R.string.wake_word_active_notification_text)
        }

        return NotificationCompat.Builder(this, NovaApplication.VOICE_SESSION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.wake_word_active_notification_title))
            .setContentText(statusText)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(android.R.string.cancel), stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
        const val ACTION_STOP = "hu.nova.mobile.service.ACTION_STOP"

        fun start(context: Context) {
            val intent = Intent(context, NovaVoiceService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, NovaVoiceService::class.java).setAction(ACTION_STOP))
        }
    }
}
