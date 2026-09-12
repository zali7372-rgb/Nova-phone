package hu.nova.mobile.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import hu.nova.mobile.MainActivity
import hu.nova.mobile.NovaApplication
import hu.nova.mobile.R

/**
 * Foreground service that owns the active voice session (see WakeWordManager.kt for
 * why this must be a visible foreground service rather than a silent background
 * listener). It is started only when the user explicitly begins a voice session
 * (tapping the mic, or opting into "Continuous conversation" mode) and is always
 * stopped after [hu.nova.mobile.voice.WakeWordManager.SESSION_TIMEOUT_MS] of inactivity
 * or when the user ends the session, so it never drains the battery unattended.
 *
 * The actual recognition/TTS/command work happens in the ViewModel layer
 * (ChatViewModel/HomeViewModel) via SpeechRecognitionManager, TextToSpeechManager and
 * WakeWordManager; this service's only job is to keep that work alive with a proper
 * foreground notification while the screen may be off or the app backgrounded.
 */
class NovaVoiceService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
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

        return NotificationCompat.Builder(this, NovaApplication.VOICE_SESSION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.wake_word_active_notification_title))
            .setContentText(getString(R.string.wake_word_active_notification_text))
            .setContentIntent(openAppIntent)
            .addAction(0, getString(android.R.string.cancel), stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
        const val ACTION_STOP = "hu.nova.mobile.service.ACTION_STOP"

        fun handleAction(intent: Intent?): Boolean = intent?.action == ACTION_STOP
    }
}
