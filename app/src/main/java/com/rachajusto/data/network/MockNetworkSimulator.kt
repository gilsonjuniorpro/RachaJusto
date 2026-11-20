package com.rachajusto.data.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import kotlin.random.Random

enum class SimulatedLatencyMode(val labelMs: Long) {
    NORMAL(300L),
    SLOW(1500L),
    INSTANT(0L)
}

class SimulatedNetworkException(
    message: String = "Simulated network failure"
) : IOException(message)

class MockNetworkSimulator(
    initialLatencyMode: SimulatedLatencyMode = SimulatedLatencyMode.NORMAL
) {
    private val _latencyMode = MutableStateFlow(initialLatencyMode)
    val latencyMode: StateFlow<SimulatedLatencyMode> = _latencyMode.asStateFlow()

    private val _simulateNextError = MutableStateFlow(false)
    val simulateNextError: StateFlow<Boolean> = _simulateNextError.asStateFlow()

    fun setLatencyMode(mode: SimulatedLatencyMode) {
        _latencyMode.value = mode
    }

    fun setSimulateNextError(enabled: Boolean) {
        _simulateNextError.value = enabled
    }

    suspend fun <T> execute(block: suspend () -> T): T {
        val delayMs = when (_latencyMode.value) {
            SimulatedLatencyMode.NORMAL -> Random.nextLong(250L, 451L)
            SimulatedLatencyMode.SLOW -> 1500L
            SimulatedLatencyMode.INSTANT -> 0L
        }
        if (delayMs > 0L) {
            delay(delayMs)
        }
        if (_simulateNextError.value) {
            _simulateNextError.value = false
            throw SimulatedNetworkException()
        }
        return block()
    }
}
