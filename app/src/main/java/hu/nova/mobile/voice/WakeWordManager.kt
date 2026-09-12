package hu.nova.mobile.voice

import android.content.Context
import android.speech.SpeechRecognizer
import hu.nova.mobile.domain.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * ============================================================================
 * DOCUMENTED ANDROID LIMITATION - PLEASE READ BEFORE MODIFYING THIS FILE
 * ============================================================================
 *
 * True, system-wide, always-on "Hey Nova"-style wake-word detection - the kind that
 * works with the screen off and the app fully backgrounded/killed, the way "Hey Google"
 * or "Alexa" do - requires either:
 *   1. A dedicated low-power DSP / hotword-detection library (e.g. Porcupine, Snowboy-
 *      successors, or a vendor's proprietary hotword chip access), which is a paid or
 *      licensed third-party SDK and is NOT part of the public Android SDK; or
 *   2. OEM-level privileges that regular, non-system apps are not granted.
 *
 * Android's public [SpeechRecognizer] API - the one this project uses everywhere else -
 * cannot listen continuously in the background: it performs single utterance
 * recognition sessions, is throttled/killed by battery optimization when the app is not
 * foregrounded, and Android 10+ restricts microphone access from background apps
 * entirely (a foreground service with a visible notification is required).
 *
 * Rather than FAKE always-on detection (e.g. by claiming to listen for "Nova" while
 * actually doing nothing, or by silently keeping the mic hot in a way that would rapidly
 * drain the battery and likely violate Play Store background-microphone policy), this
 * class implements the best legitimate architecture available on stock Android:
 *
 *   - "Active session" wake word: while [NovaVoiceService] is running in the
 *     foreground (started explicitly by the user tapping the mic, or by opting into
 *     "Continuous conversation" in Settings), this manager repeatedly restarts short
 *     [SpeechRecognitionManager] listening windows and checks each transcript for the
 *     word "nova" (Hungarian and English homophones included) before treating what
 *     follows as a command. This lets a real conversation flow ("Nova" -> "Igen?" ->
 *     command -> reply -> next command) without re-tapping the mic each turn.
 *   - A visible, ongoing notification and a session timeout (see [SESSION_TIMEOUT_MS])
 *     are always shown/enforced while this is active, so the user always knows the mic
 *     is live and it never runs indefinitely in the background.
 *   - Manual activation (tapping the mic button) is always available and does not
 *     depend on wake-word detection at all.
 *
 * If you need real "phone is asleep, wake word still works" behavior, integrate a
 * dedicated on-device hotword SDK (e.g. Picovoice Porcupine) behind this same
 * [WakeWordManager] interface - the rest of the app (CommandRouter, NovaVoiceService,
 * VoiceState UI) does not need to change.
 * ============================================================================
 */
class WakeWordManager(
    context: Context,
    private val speechRecognitionManager: SpeechRecognitionManager = SpeechRecognitionManager(context)
) {

    companion object {
        val WAKE_WORD_VARIANTS = listOf("nova", "nóva", "nova,", "hé nova", "hey nova")
        const val SESSION_TIMEOUT_MS = 20_000L
    }

    fun isSystemSpeechRecognitionAvailable(): Boolean = speechRecognitionManager.isRecognitionAvailable()

    /**
     * Emits `true` once a wake-word-bearing transcript is detected during an active
     * foreground listening session. Consumers should follow this with a normal
     * [SpeechRecognitionManager.listen] call to capture the actual command, per the
     * "don't require the wake word before every sentence" requirement - see
     * [NovaVoiceService] for how the session loop is driven.
     */
    fun watchForWakeWord(language: AppLanguage): Flow<Boolean> = flow {
        speechRecognitionManager.listen(language, preferOffline = false).collect { event ->
            if (event is SpeechRecognitionEvent.FinalResult) {
                val normalized = event.text.trim().lowercase()
                emit(WAKE_WORD_VARIANTS.any { normalized.contains(it) })
            }
        }
    }

    fun containsWakeWord(transcript: String): Boolean {
        val normalized = transcript.trim().lowercase()
        return WAKE_WORD_VARIANTS.any { normalized.contains(it) }
    }
}
