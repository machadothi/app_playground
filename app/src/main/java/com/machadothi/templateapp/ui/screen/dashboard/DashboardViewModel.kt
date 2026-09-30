package com.machadothi.templateapp.ui.screen.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machadothi.templateapp.data.network.StatusResponse
import com.machadothi.templateapp.data.network.TelemetryResponse
import com.machadothi.templateapp.repository.heliostat.HeliostatRepository
import com.machadothi.templateapp.wifi.PhoneLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: HeliostatRepository,
    private val phoneLocation: PhoneLocation,
) : ViewModel() {

    var uiState by mutableStateOf<DashboardUiState>(DashboardUiState.Loading)
        private set

    /** Result of the last button press, shown under the controls. */
    var actionMessage by mutableStateOf<String?>(null)
        private set

    var status by mutableStateOf<StatusResponse?>(null)
        private set

    private var polling: Job? = null

    /** Poll at 2 Hz while the screen is visible. */
    fun startPolling() {
        if (polling?.isActive == true) return
        polling = viewModelScope.launch {
            refreshStatus()
            repository.telemetry(periodMs = 500).collect { result ->
                val host = repository.host().orEmpty()
                uiState = result.fold(
                    onSuccess = { DashboardUiState.Live(it, host) },
                    onFailure = { DashboardUiState.Unreachable(it.message ?: "unreachable", host) },
                )
            }
        }
    }

    fun stopPolling() {
        polling?.cancel()
        polling = null
    }

    fun setMode(mode: String) = act("Mode: $mode") {
        repository.setMode(mode).also { refreshStatus() }
    }

    fun clearFault() = act("Fault cleared") { repository.clearFault() }

    fun sendTime() = act("Time sent") { repository.sendTime() }

    fun sendLocation() = act("Location sent") {
        val location = phoneLocation.current()
            ?: return@act Result.failure<Unit>(IllegalStateException("Phone location unavailable"))
        repository.sendLocation(location.latitude, location.longitude, location.altitude)
    }

    fun forget(onForgotten: () -> Unit) {
        viewModelScope.launch {
            repository.forget()
            onForgotten()
        }
    }

    private suspend fun refreshStatus() {
        repository.status().onSuccess { status = it }
    }

    private fun act(success: String, block: suspend () -> Result<*>) {
        viewModelScope.launch {
            actionMessage = block().fold({ success }, { it.message ?: "failed" })
        }
    }
}

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Live(val telemetry: TelemetryResponse, val host: String) : DashboardUiState()
    data class Unreachable(val message: String, val host: String) : DashboardUiState()
}
