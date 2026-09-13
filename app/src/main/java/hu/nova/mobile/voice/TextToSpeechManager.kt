package hu.nova.mobile.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import hu.nova.mobile.domain.model.AppLanguage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale
import java.util.UUID

sealed class TtsEvent {
    object Started : TtsEvent()
    object Done : TtsEvent()
    data class Error(val message: String) : TtsEvent()
}

/**
 * Wraps Android's built-in [TextToSpeech] engine for both Hungarian and English output.
 * Availability of a natural-sounding Hungarian voice depends on the TTS engine and voice
 * packs installed on the device (Google's TTS engine supports "hu-HU"); if the locale is
 * unavailable this reports [TtsEvent.Error] honestly instead of silently speaking in the
 * wrong language.
 */
class TextToSpeechManager(context: Context) {

    private var tts: TextToSpeech? = null
    private val appContext = context.applicationContext

    fun speak(text: String, language: AppLanguage): Flow<TtsEvent> = callbackFlow {
        val utteranceId = UUID.randomUUID().toString()

        val instance = tts ?: TextToSpeech(appContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                trySend(TtsEvent.Error("TTS engine failed to initialize."))
                close()
            }
        }.also { tts = it }

        val locale = if (language == AppLanguage.HUNGARIAN) Locale("hu", "HU") else Locale.US
        val localeResult = instance.setLanguage(locale)
        if (localeResult == TextToSpeech.LANG_MISSING_DATA || localeResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            trySend(TtsEvent.Error("The selected language voice is not installed on this device."))
            close()
            return@callbackFlow
        }

        instance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                trySend(TtsEvent.Started)
            }

            override fun onDone(id: String?) {
                trySend(TtsEvent.Done)
                close()
            }

            @Deprecated("Deprecated in API")
            override fun onError(id: String?) {
                trySend(TtsEvent.Error("TTS playback error."))
                close()
            }
        })

        instance.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

        awaitClose { /* keep the engine instance alive across calls; released in shutdown() */ }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}
