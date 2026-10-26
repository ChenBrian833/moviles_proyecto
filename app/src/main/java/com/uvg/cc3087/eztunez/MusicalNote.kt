package com.uvg.cc3087.eztunez

private val sharpNoteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
private val flatNoteNames = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

internal fun noteNameForMidi(
    midi: Int,
    preferFlats: Boolean = false,
    includeOctave: Boolean = true
): String {
    val names = if (preferFlats) flatNoteNames else sharpNoteNames
    val name = names[((midi % 12) + 12) % 12]
    return if (includeOctave) "$name${midi / 12 - 1}" else name
}
