package tech.sadique.qayam.audio

import tech.sadique.qayam.data.model.AdhanSoundType
import javax.inject.Inject
import javax.inject.Singleton

data class Note(
    val freq: Double,
    val durationMs: Int,
    val attackMs: Int = 100,
    val decayMs: Int = 150,
    val vibrato: Boolean = true
)

@Singleton
class MelodyRepository @Inject constructor() {

    fun getMelodySequence(soundType: AdhanSoundType): List<Note> {
        return when (soundType) {
            AdhanSoundType.TAKBEER_ONLY -> listOf(
                // Al-laa-hu Ak-bar (Takbeer 1)
                Note(220.0, 450, 100, 100),
                Note(261.63, 600, 120, 150),
                Note(293.66, 750, 150, 200),
                Note(261.63, 500, 100, 150),
                Note(220.0, 900, 150, 350),
                Note(0.0, 400),
                // Al-laa-hu Ak-bar (Takbeer 2)
                Note(220.0, 450, 100, 100),
                Note(261.63, 600, 120, 150),
                Note(329.63, 850, 180, 250),
                Note(293.66, 500, 100, 150),
                Note(261.63, 1100, 200, 400)
            )

            AdhanSoundType.GENTLE_CHIME -> listOf(
                Note(523.25, 600, 30, 500, vibrato = false), // C5
                Note(659.25, 600, 30, 500, vibrato = false), // E5
                Note(783.99, 800, 30, 700, vibrato = false), // G5
                Note(1046.50, 1200, 30, 1000, vibrato = false) // C6
            )

            AdhanSoundType.MAKKAH -> listOf(
                // Allahu Akbar (Bayati / Rast Maqam style)
                Note(220.00, 500, 100, 120),  // A3
                Note(246.94, 550, 100, 150),  // B3
                Note(293.66, 900, 180, 300),  // D4
                Note(261.63, 650, 120, 200),  // C4
                Note(220.00, 1200, 200, 450), // A3
                Note(0.0, 350),

                // Allahu Akbar (Rising Call)
                Note(220.00, 450, 100, 100),  // A3
                Note(293.66, 650, 140, 200),  // D4
                Note(349.23, 1100, 220, 400), // F4
                Note(329.63, 700, 150, 250),  // E4
                Note(293.66, 1300, 220, 500), // D4
                Note(0.0, 400),

                // Ash-hadu alla ilaha illallah
                Note(293.66, 600, 120, 150),  // D4
                Note(349.23, 750, 150, 200),  // F4
                Note(392.00, 1200, 250, 450), // G4
                Note(349.23, 600, 120, 200),  // F4
                Note(293.66, 1400, 250, 550), // D4
                Note(0.0, 400),

                // Hayya 'alas-Salah
                Note(329.63, 600, 120, 180),  // E4
                Note(392.00, 900, 180, 300),  // G4
                Note(440.00, 1300, 250, 500), // A4
                Note(392.00, 600, 120, 200),  // G4
                Note(349.23, 1500, 250, 600)  // F4
            )

            AdhanSoundType.MADINAH -> listOf(
                // Hijaz/Saba Maqam tone
                Note(196.00, 600, 120, 180),  // G3
                Note(233.08, 700, 140, 220),  // Bb3
                Note(293.66, 1100, 200, 400), // D4
                Note(246.94, 600, 120, 200),  // B3
                Note(196.00, 1400, 250, 550), // G3
                Note(0.0, 400),

                // Allahu Akbar Part 2
                Note(233.08, 550, 120, 180),  // Bb3
                Note(293.66, 800, 160, 250),  // D4
                Note(369.99, 1300, 250, 450), // F#4
                Note(293.66, 700, 140, 250),  // D4
                Note(233.08, 1500, 250, 600)  // Bb3
            )

            AdhanSoundType.AL_AQSA -> listOf(
                // Majestic resonant melody
                Note(220.00, 600, 120, 200),  // A3
                Note(277.18, 750, 150, 250),  // C#4
                Note(329.63, 1100, 220, 400), // E4
                Note(277.18, 650, 140, 220),  // C#4
                Note(220.00, 1350, 250, 500), // A3
                Note(0.0, 350),

                Note(277.18, 550, 120, 180),  // C#4
                Note(329.63, 750, 150, 250),  // E4
                Note(415.30, 1250, 240, 450), // G#4
                Note(329.63, 700, 140, 220),  // E4
                Note(277.18, 1450, 250, 600)  // C#4
            )

            else -> listOf(
                Note(440.0, 500),
                Note(554.37, 500),
                Note(659.25, 800)
            )
        }
    }
}
