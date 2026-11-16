package com.zameerahma.snakeapp.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

enum class Sfx { START, EAT, BONUS_APPEAR, BONUS, GAME_OVER, WIN }

/**
 * Retro sound effects. The sounds are generated in code (simple musical tones),
 * so the app needs no audio files. They are written once as small WAV files in
 * the cache folder and played with SoundPool for low latency.
 */
class SoundPlayer(context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = ConcurrentHashMap<Sfx, Int>()

    init {
        val cacheDir = File(context.cacheDir, "sfx").apply { mkdirs() }
        thread(name = "sfx-generator", isDaemon = true) {
            Sfx.entries.forEach { sfx ->
                val file = File(cacheDir, "${sfx.name.lowercase()}_v1.wav")
                if (!file.exists()) file.writeBytes(wav(notesFor(sfx)))
                soundIds[sfx] = soundPool.load(file.absolutePath, 1)
            }
        }
    }

    fun play(sfx: Sfx) {
        val id = soundIds[sfx] ?: return
        soundPool.play(id, 0.8f, 0.8f, 1, 0, 1f)
    }

    /** A note is a frequency in Hz (0 = silence) and a length in milliseconds. */
    private data class Note(val hz: Double, val ms: Int)

    private fun notesFor(sfx: Sfx): List<Note> = when (sfx) {
        Sfx.START -> listOf(Note(523.3, 70), Note(784.0, 110))
        Sfx.EAT -> listOf(Note(660.0, 45), Note(990.0, 70))
        Sfx.BONUS_APPEAR -> listOf(Note(1318.5, 40), Note(0.0, 20), Note(1318.5, 60))
        Sfx.BONUS -> listOf(Note(523.3, 60), Note(659.3, 60), Note(784.0, 60), Note(1046.5, 140))
        Sfx.GAME_OVER -> listOf(Note(392.0, 150), Note(330.0, 150), Note(262.0, 150), Note(196.0, 320))
        Sfx.WIN -> listOf(Note(523.3, 90), Note(659.3, 90), Note(784.0, 90), Note(1046.5, 90), Note(1318.5, 300))
    }

    private fun wav(notes: List<Note>): ByteArray {
        val sampleRate = 22050
        val samples = ArrayList<Short>()
        for (note in notes) {
            val count = sampleRate * note.ms / 1000
            val fade = min(count / 4, sampleRate * 8 / 1000) // short fade in/out avoids clicks
            for (i in 0 until count) {
                val envelope = when {
                    i < fade -> i / fade.toDouble()
                    i > count - fade -> (count - i) / fade.toDouble()
                    else -> 1.0
                }
                val t = i / sampleRate.toDouble()
                // Sine plus a little 3rd harmonic gives a soft "retro" tone.
                val wave = if (note.hz == 0.0) 0.0 else
                    sin(2 * PI * note.hz * t) * 0.8 + sin(2 * PI * note.hz * 3 * t) * 0.2
                samples += (wave * envelope * 0.5 * Short.MAX_VALUE).toInt().toShort()
            }
        }
        val dataSize = samples.size * 2
        return ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + dataSize); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(sampleRate); putInt(sampleRate * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(dataSize)
            samples.forEach { putShort(it) }
        }.array()
    }
}
