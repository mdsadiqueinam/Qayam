package tech.sadique.qayam.audio

import tech.sadique.qayam.data.model.AdhanSoundType
import javax.inject.Inject
import javax.inject.Singleton

data class Note(
    val freq: Double,
    val durationMs: Int,
    val attackMs: Int = 100,
    val decayMs: Int = 150,
    val vibrato: Boolean = true,
)

@Singleton
class MelodyRepository @Inject constructor() {

    fun getMelodySequence(soundType: AdhanSoundType): List<Note> = when (soundType) {
        AdhanSoundType.TAKBEER_ONLY -> takbeerMelody()
        AdhanSoundType.GENTLE_CHIME -> chimeMelody()
        AdhanSoundType.MAKKAH -> makkahMelody()
        AdhanSoundType.MADINAH -> madinahMelody()
        AdhanSoundType.AL_AQSA -> alAqsaMelody()
        else -> defaultMelody()
    }

    private fun takbeerMelody(): List<Note> = listOf(
        // Al-laa-hu Ak-bar (Takbeer 1)
        Note(freq = 220.0, durationMs = 450, attackMs = 100, decayMs = 100),
        Note(freq = 261.63, durationMs = 600, attackMs = 120, decayMs = 150),
        Note(freq = 293.66, durationMs = 750, attackMs = 150, decayMs = 200),
        Note(freq = 261.63, durationMs = 500, attackMs = 100, decayMs = 150),
        Note(freq = 220.0, durationMs = 900, attackMs = 150, decayMs = 350),
        Note(freq = 0.0, durationMs = 400),
        // Al-laa-hu Ak-bar (Takbeer 2)
        Note(freq = 220.0, durationMs = 450, attackMs = 100, decayMs = 100),
        Note(freq = 261.63, durationMs = 600, attackMs = 120, decayMs = 150),
        Note(freq = 329.63, durationMs = 850, attackMs = 180, decayMs = 250),
        Note(freq = 293.66, durationMs = 500, attackMs = 100, decayMs = 150),
        Note(freq = 261.63, durationMs = 1100, attackMs = 200, decayMs = 400),
    )

    private fun chimeMelody(): List<Note> = listOf(
        Note(freq = 523.25, durationMs = 600, attackMs = 30, decayMs = 500, vibrato = false), // C5
        Note(freq = 659.25, durationMs = 600, attackMs = 30, decayMs = 500, vibrato = false), // E5
        Note(freq = 783.99, durationMs = 800, attackMs = 30, decayMs = 700, vibrato = false), // G5
        Note(freq = 1046.50, durationMs = 1200, attackMs = 30, decayMs = 1000, vibrato = false), // C6
    )

    private fun makkahMelody(): List<Note> = listOf(
        Note(freq = 220.00, durationMs = 500, attackMs = 100, decayMs = 120), // A3
        Note(freq = 246.94, durationMs = 550, attackMs = 100, decayMs = 150), // B3
        Note(freq = 293.66, durationMs = 900, attackMs = 180, decayMs = 300), // D4
        Note(freq = 261.63, durationMs = 650, attackMs = 120, decayMs = 200), // C4
        Note(freq = 220.00, durationMs = 1200, attackMs = 200, decayMs = 450), // A3
        Note(freq = 0.0, durationMs = 350),
        Note(freq = 220.00, durationMs = 450, attackMs = 100, decayMs = 100), // A3
        Note(freq = 293.66, durationMs = 650, attackMs = 140, decayMs = 200), // D4
        Note(freq = 349.23, durationMs = 1100, attackMs = 220, decayMs = 400), // F4
        Note(freq = 329.63, durationMs = 700, attackMs = 150, decayMs = 250), // E4
        Note(freq = 293.66, durationMs = 1300, attackMs = 220, decayMs = 500), // D4
        Note(freq = 0.0, durationMs = 400),
        Note(freq = 293.66, durationMs = 600, attackMs = 120, decayMs = 150), // D4
        Note(freq = 349.23, durationMs = 750, attackMs = 150, decayMs = 200), // F4
        Note(freq = 392.00, durationMs = 1200, attackMs = 250, decayMs = 450), // G4
        Note(freq = 349.23, durationMs = 600, attackMs = 120, decayMs = 200), // F4
        Note(freq = 293.66, durationMs = 1400, attackMs = 250, decayMs = 550), // D4
        Note(freq = 0.0, durationMs = 400),
        Note(freq = 329.63, durationMs = 600, attackMs = 120, decayMs = 180), // E4
        Note(freq = 392.00, durationMs = 900, attackMs = 180, decayMs = 300), // G4
        Note(freq = 440.00, durationMs = 1300, attackMs = 250, decayMs = 500), // A4
        Note(freq = 392.00, durationMs = 600, attackMs = 120, decayMs = 200), // G4
        Note(freq = 349.23, durationMs = 1500, attackMs = 250, decayMs = 600), // F4
    )

    private fun madinahMelody(): List<Note> = listOf(
        Note(freq = 196.00, durationMs = 600, attackMs = 120, decayMs = 180), // G3
        Note(freq = 233.08, durationMs = 700, attackMs = 140, decayMs = 220), // Bb3
        Note(freq = 293.66, durationMs = 1100, attackMs = 200, decayMs = 400), // D4
        Note(freq = 246.94, durationMs = 600, attackMs = 120, decayMs = 200), // B3
        Note(freq = 196.00, durationMs = 1400, attackMs = 250, decayMs = 550), // G3
        Note(freq = 0.0, durationMs = 400),
        Note(freq = 233.08, durationMs = 550, attackMs = 120, decayMs = 180), // Bb3
        Note(freq = 293.66, durationMs = 800, attackMs = 160, decayMs = 250), // D4
        Note(freq = 369.99, durationMs = 1300, attackMs = 250, decayMs = 450), // F#4
        Note(freq = 293.66, durationMs = 700, attackMs = 140, decayMs = 250), // D4
        Note(freq = 233.08, durationMs = 1500, attackMs = 250, decayMs = 600), // Bb3
    )

    private fun alAqsaMelody(): List<Note> = listOf(
        Note(freq = 220.00, durationMs = 600, attackMs = 120, decayMs = 200), // A3
        Note(freq = 277.18, durationMs = 750, attackMs = 150, decayMs = 250), // C#4
        Note(freq = 329.63, durationMs = 1100, attackMs = 220, decayMs = 400), // E4
        Note(freq = 277.18, durationMs = 650, attackMs = 140, decayMs = 220), // C#4
        Note(freq = 220.00, durationMs = 1350, attackMs = 250, decayMs = 500), // A3
        Note(freq = 0.0, durationMs = 350),
        Note(freq = 277.18, durationMs = 550, attackMs = 120, decayMs = 180), // C#4
        Note(freq = 329.63, durationMs = 750, attackMs = 150, decayMs = 250), // E4
        Note(freq = 415.30, durationMs = 1250, attackMs = 240, decayMs = 450), // G#4
        Note(freq = 329.63, durationMs = 700, attackMs = 140, decayMs = 220), // E4
        Note(freq = 277.18, durationMs = 1450, attackMs = 250, decayMs = 600), // C#4
    )

    private fun defaultMelody(): List<Note> = listOf(
        Note(freq = 440.0, durationMs = 500),
        Note(freq = 554.37, durationMs = 500),
        Note(freq = 659.25, durationMs = 800),
    )
}
