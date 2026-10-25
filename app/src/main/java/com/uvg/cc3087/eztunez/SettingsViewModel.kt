package com.uvg.cc3087.eztunez

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {
    // Null means the first disk read is still pending, rather than temporary default settings.
    // stateIn retains its last value while the Activity's collector is recreated.
    val settings: StateFlow<TunerSettings?> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun updateMicSensitivity(value: Float) = update {
        repository.setMicSensitivity(value)
    }

    fun updateReferencePitch(value: Int) = update {
        repository.setReferencePitch(value)
    }

    fun updateShowRealTimeHz(value: Boolean) = update {
        repository.setShowRealTimeHz(value)
    }

    fun updateNotationSystem(value: NotationSystem) = update {
        repository.setNotationSystem(value)
    }

    fun updateDarkTheme(value: Boolean) = update {
        repository.setDarkTheme(value)
    }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
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
