package com.machadothi.templateapp.ui.screen.jog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machadothi.templateapp.data.network.TelemetryResponse
import com.machadothi.templateapp.repository.heliostat.HeliostatRepository
import com.machadothi.templateapp.ui.component.SafetyBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Move each axis by hand. Needs MANUAL mode, which suspends automatic tracking.
 * The heliostat refuses a jog that would put the beam into a keep-out zone.
 */
@Composable
fun JogScreen(onDone: () -> Unit, viewModel: JogViewModel = hiltViewModel()) {
    DisposableEffect(Unit) {
        viewModel.startPolling()
        onDispose { viewModel.stopPolling() }
    }
    Scaffold(bottomBar = { SafetyBar() }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Jog", style = MaterialTheme.typography.headlineSmall)
            val t = viewModel.telemetry
            val manual = t?.mode == "manual"
            if (!manual) {
                Text("Mode is ${t?.mode ?: "unknown"}. Jogging needs manual mode, which pauses tracking.")
                Button(onClick = viewModel::enterManual) { Text("Enter manual mode") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.1, 1.0, 5.0, 10.0).forEach { step ->
                    FilterChip(
                        selected = viewModel.step == step,
                        onClick = { viewModel.step = step },
                        label = { Text("$step°") },
                    )
                }
            }

            AxisPad("Azimuth", t?.pos?.getOrNull(0), manual, { viewModel.jog(0, -1) }, { viewModel.jog(0, +1) })
            AxisPad("Elevation", t?.pos?.getOrNull(1), manual, { viewModel.jog(1, -1) }, { viewModel.jog(1, +1) })

            viewModel.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(onClick = { viewModel.done(onDone) }, modifier = Modifier.fillMaxWidth()) {
                Text("Done: back to idle")
            }
        }
    }
}

@Composable
private fun AxisPad(name: String, position: Double?, enabled: Boolean, minus: () -> Unit, plus: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(onClick = minus, enabled = enabled) { Text("−") }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(name)
                Text(
                    position?.let { "%.1f°".format(it) } ?: "-",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            FilledTonalButton(onClick = plus, enabled = enabled) { Text("+") }
        }
    }
}

@HiltViewModel
class JogViewModel @Inject constructor(
    private val repository: HeliostatRepository,
) : ViewModel() {

    var telemetry by mutableStateOf<TelemetryResponse?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var step by mutableDoubleStateOf(1.0)

    private var polling: Job? = null

    fun startPolling() {
        if (polling?.isActive == true) return
        polling = viewModelScope.launch {
            repository.telemetry(periodMs = 300).collect { result ->
                result.onSuccess { telemetry = it }
            }
        }
    }

    fun stopPolling() {
        polling?.cancel()
        polling = null
    }

    /**
     * The heliostat only allows MANUAL from IDLE: an operator stops tracking
     * deliberately before taking the axes by hand. So from any other mode, go
     * through IDLE first.
     */
    fun enterManual() {
        viewModelScope.launch {
            if (telemetry?.mode != "idle") {
                repository.setMode("idle").onFailure {
                    message = it.message
                    return@launch
                }
            }
            message = repository.setMode("manual").exceptionOrNull()?.message
        }
    }

    fun jog(axis: Int, direction: Int) {
        viewModelScope.launch {
            message = repository.jog(axis, direction * step).exceptionOrNull()?.message
        }
    }

    fun done(onDone: () -> Unit) {
        viewModelScope.launch {
            if (telemetry?.mode == "manual") repository.setMode("idle")
            onDone()
        }
    }
}
