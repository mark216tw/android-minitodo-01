package com.vibecode.minitodo.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vibecode.minitodo.data.backup.BackupManager
import com.vibecode.minitodo.data.prefs.AppSettings
import com.vibecode.minitodo.data.prefs.SettingsRepository
import com.vibecode.minitodo.domain.SortMode
import com.vibecode.minitodo.domain.ThemeMode
import com.vibecode.minitodo.domain.ThemePalette
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsEvent {
    data class ExportSuccess(val count: Int) : SettingsEvent
    data object ExportFailed : SettingsEvent
    data class ImportSuccess(val count: Int) : SettingsEvent
    data object ImportFailed : SettingsEvent
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
) : ViewModel() {

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    val settings = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AppSettings(),
    )

    fun setSortMode(mode: SortMode) {
        viewModelScope.launch { settingsRepository.setSortMode(mode) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setThemePalette(palette: ThemePalette) {
        viewModelScope.launch { settingsRepository.setThemePalette(palette) }
    }

    fun export(uri: Uri) {
        viewModelScope.launch {
            backupManager.export(uri)
                .onSuccess { count -> _events.send(SettingsEvent.ExportSuccess(count)) }
                .onFailure { _events.send(SettingsEvent.ExportFailed) }
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            backupManager.import(uri)
                .onSuccess { count -> _events.send(SettingsEvent.ImportSuccess(count)) }
                .onFailure { _events.send(SettingsEvent.ImportFailed) }
        }
    }

    companion object {
        fun factory(
            settingsRepository: SettingsRepository,
            backupManager: BackupManager,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(settingsRepository, backupManager) as T
            }
    }
}
