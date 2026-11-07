package com.uvg.cc3087.eztunez

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CustomPresetEditor(
    draft: CustomPresetDraft,
    isSaving: Boolean,
    problem: PresetProblem?,
    onChange: (CustomPresetDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    BackHandler { if (!isSaving) onCancel() }
    Scaffold(
        topBar = { ScreenHeader(stringResource(
            if (draft.id == null) R.string.create_custom_tuning else R.string.custom_edit_title
        )) }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 600.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = draft.name, onValueChange = { onChange(draft.copy(name = it)) },
                    label = { Text(stringResource(R.string.custom_name)) },
                    singleLine = true, enabled = !isSaving,
                    isError = problem == PresetProblem.NAME_REQUIRED || problem == PresetProblem.DUPLICATE_NAME,
                    modifier = Modifier.fillMaxWidth().testTag("preset-name")
                )
                OutlinedTextField(
                    value = draft.description, onValueChange = { onChange(draft.copy(description = it)) },
                    label = { Text(stringResource(R.string.custom_description)) },
                    enabled = !isSaving, minLines = 2, modifier = Modifier.fillMaxWidth().testTag("preset-description")
                )
                Text(stringResource(R.string.custom_strings_help), style = MaterialTheme.typography.bodyMedium)
                draft.midiNotes.forEachIndexed { index, midi ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(when (index) {
                                0 -> R.string.custom_lowest_string
                                5 -> R.string.custom_highest_string
                                else -> R.string.custom_string
                            }, 6 - index),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(noteNameForMidi(midi), style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NoteMenu(
                                label = stringResource(R.string.custom_note),
                                value = noteNameForMidi(midi, includeOctave = false),
                                choices = (0..11).map { it to noteNameForMidi(60 + it, includeOctave = false) },
                                enabled = !isSaving, modifier = Modifier.weight(1f).testTag("preset-note-$index"),
                                onSelect = { pitchClass ->
                                    val octave = minOf(midi / 12 - 1, if (pitchClass <= 7) 9 else 8)
                                    onChange(draft.copy(midiNotes = draft.midiNotes.toMutableList().apply {
                                        this[index] = (octave + 1) * 12 + pitchClass
                                    }))
                                }
                            )
                            NoteMenu(
                                label = stringResource(R.string.custom_octave), value = (midi / 12 - 1).toString(),
                                choices = (-1..if (midi % 12 <= 7) 9 else 8).map { it to it.toString() },
                                enabled = !isSaving, modifier = Modifier.weight(1f).testTag("preset-octave-$index"),
                                onSelect = { octave ->
                                    onChange(draft.copy(midiNotes = draft.midiNotes.toMutableList().apply {
                                        this[index] = (octave + 1) * 12 + midi % 12
                                    }))
                                }
                            )
                        }
                    }
                }
                if (problem != null) {
                    Text(stringResource(problem.messageRes), color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("preset-error"))
                }
                if (isSaving) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.custom_saving))
                }
                Button(onClick = onSave, enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth().testTag("preset-save")) {
                    Text(stringResource(R.string.custom_save))
                }
                OutlinedButton(onClick = onCancel, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.custom_cancel))
                }
            }
        }
    }
}

@Composable
private fun NoteMenu(
    label: String, value: String, choices: List<Pair<Int, String>>, enabled: Boolean,
    modifier: Modifier, onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.custom_selector_value, label, value))
        }
        DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            choices.forEach { (id, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; onSelect(id) })
            }
        }
    }
}

internal val PresetProblem.messageRes: Int get() = when (this) {
    PresetProblem.NAME_REQUIRED -> R.string.custom_name_required
    PresetProblem.DUPLICATE_NAME -> R.string.custom_name_duplicate
    PresetProblem.INVALID_NOTES -> R.string.custom_invalid_notes
    PresetProblem.READ_ONLY -> R.string.custom_read_only
    PresetProblem.NOT_FOUND -> R.string.custom_not_found
    PresetProblem.STORAGE -> R.string.custom_storage_error
}
