package com.rachajusto.feature.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rachajusto.data.network.MockNetworkSimulator
import com.rachajusto.data.network.SimulatedLatencyMode
import com.rachajusto.domain.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DemoModeUiState(
    val isResetting: Boolean = false,
    val resetCompleted: Boolean = false,
    val resetFailed: Boolean = false
)

class DemoModeViewModel(
    private val repository: GroupRepository,
    private val networkSimulator: MockNetworkSimulator
) : ViewModel() {

    val latencyMode: StateFlow<SimulatedLatencyMode> = networkSimulator.latencyMode
    val simulateNextError: StateFlow<Boolean> = networkSimulator.simulateNextError

    private val _uiState = MutableStateFlow(DemoModeUiState())
    val uiState: StateFlow<DemoModeUiState> = _uiState.asStateFlow()

    fun setLatencyMode(mode: SimulatedLatencyMode) {
        networkSimulator.setLatencyMode(mode)
    }

    fun setSimulateNextError(enabled: Boolean) {
        networkSimulator.setSimulateNextError(enabled)
    }

    fun resetDemoData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isResetting = true, resetFailed = false) }
            runCatching { repository.resetDemoData() }
                .onSuccess {
                    _uiState.update { it.copy(isResetting = false, resetCompleted = true) }
                }
                .onFailure {
                    _uiState.update { it.copy(isResetting = false, resetFailed = true) }
                }
        }
    }

    fun consumeResetCompleted() {
        _uiState.update { it.copy(resetCompleted = false) }
    }

    fun consumeResetFailed() {
        _uiState.update { it.copy(resetFailed = false) }
    }

    companion object {
        fun factory(
            repository: GroupRepository,
            networkSimulator: MockNetworkSimulator
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DemoModeViewModel(repository, networkSimulator) as T
            }
        }
    }
}
