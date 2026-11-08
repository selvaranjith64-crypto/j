package com.jarvis.assistant

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Speaks replies. Uses "transient may duck" audio focus so music (Spotify) just gets quieter
 * while Jarvis talks and returns to full volume afterwards instead of being stopped.
 */
class Speaker(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var ready = false
    private lateinit var tts: TextToSpeech

    @Volatile
    var isSpeaking = false
        private set

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attrs)
        .build()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
                tts.setAudioAttributes(attrs)
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                    }

                    override fun onDone(utteranceId: String?) {
                        finish()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        finish()
                    }
                })
                ready = true
            }
        }
    }

    private fun finish() {
        isSpeaking = false
        audio.abandonAudioFocusRequest(focusRequest)
    }

    fun say(text: String) {
        Bus.log("Jarvis: $text")
        if (!ready) return
        audio.requestAudioFocus(focusRequest)
        isSpeaking = true
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "jarvis-${System.nanoTime()}")
    }

    fun shutdown() {
        if (ready) {
            tts.stop()
            tts.shutdown()
        }
        audio.abandonAudioFocusRequest(focusRequest)
    }
}
