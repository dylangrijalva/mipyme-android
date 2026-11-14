package co.mipyme.screen.ask_notification_permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.mipyme.data.notification.NotificationPermissionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AskNotificationPermissionsViewModel(
    private val repository: NotificationPermissionRepository,
) : ViewModel() {
    private val _stateFlow = MutableStateFlow(AskNotificationPermissionsScreenUiState())
    val stateFlow: StateFlow<AskNotificationPermissionsScreenUiState> = _stateFlow.asStateFlow()
    val state: AskNotificationPermissionsScreenUiState
        get() = _stateFlow.value

    private var saveStarted = false

    fun onRequestPermission(): Boolean {
        return beginResponding()
    }

    fun onPermissionResult() {
        markAnswered()
    }

    fun onSkip() {
        if (!beginResponding()) return
        markAnswered()
    }

    private fun beginResponding(): Boolean {
        var started = false
        _stateFlow.update { current ->
            started = !current.isResponding
            current.copy(isResponding = true)
        }
        return started
    }

    private fun markAnswered() {
        if (saveStarted) return
        saveStarted = true
        viewModelScope.launch {
            repository.markAnswered()
        }
    }
}
