package com.uvg.cc3087.eztunez

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {
    private val mutableSettings = MutableStateFlow<TunerSettings?>(null)
    val settings = mutableSettings.asStateFlow()
    private val mutableSaveError = MutableStateFlow(false)
    val saveError = mutableSaveError.asStateFlow()

    private var savedSettings: TunerSettings? = null
    private var requestedSettings: TunerSettings? = null
    private val saveRequests = Channel<Unit>(Channel.CONFLATED)

    init {
        viewModelScope.launch {
            // This ViewModel owns edits for the Activity's lifetime. Read once so
            // delayed disk acknowledgements cannot overwrite newer local choices.
            val initial = repository.settings.first()
            savedSettings = initial
            requestedSettings = initial
            mutableSettings.value = initial
            for (request in saveRequests) {
                val target = requestedSettings ?: continue
                if (target == savedSettings) {
                    mutableSaveError.value = false
                    continue
                }
                try {
                    repository.saveSettings(target)
                    savedSettings = target
                    mutableSaveError.value = false
                } catch (_: IOException) {
                    // Keep choices active for explicit retry. Cancellation and
                    // programming errors are deliberately not swallowed.
                    mutableSaveError.value = true
                }
            }
        }
    }

    fun updateMicSensitivity(value: Float) {
        mutableSettings.value = mutableSettings.value?.copy(micSensitivity = normalizeSensitivity(value))
    }

    fun commitMicSensitivity() {
        val value = mutableSettings.value?.micSensitivity ?: return
        requestSave { it.copy(micSensitivity = value) }
    }

    fun adjustReferencePitch(delta: Int) = requestSave {
        it.copy(referencePitch = normalizeReferencePitch(it.referencePitch + delta))
    }

    fun updateShowRealTimeHz(value: Boolean) = requestSave { it.copy(showRealTimeHz = value) }
    fun updateNotationSystem(value: NotationSystem) = requestSave { it.copy(notationSystem = value) }
    fun updateDarkTheme(value: Boolean) = requestSave { it.copy(darkTheme = value) }

    fun retrySave() {
        commitMicSensitivity()
        saveRequests.trySend(Unit)
    }

    private fun requestSave(transform: (TunerSettings) -> TunerSettings) {
        val previous = requestedSettings ?: return
        val next = transform(previous)
        if (next == previous) return
        requestedSettings = next
        // Preserve a sensitivity gesture that has not finished yet.
        mutableSettings.value = next.copy(micSensitivity = mutableSettings.value!!.micSensitivity)
        saveRequests.trySend(Unit)
    }

    class Factory(
        private val repository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
            return SettingsViewModel(repository) as T
        }
    }
}
