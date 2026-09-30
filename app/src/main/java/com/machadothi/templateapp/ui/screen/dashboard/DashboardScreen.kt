package com.machadothi.templateapp.ui.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.machadothi.templateapp.data.network.TelemetryResponse
import com.machadothi.templateapp.ui.component.SafetyBar

@Composable
fun DashboardScreen(
    onJog: () -> Unit,
    onTarget: () -> Unit,
    onSetupAgain: () -> Unit,
    onChangeAddress: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    DisposableEffect(Unit) {
        viewModel.startPolling()
        onDispose { viewModel.stopPolling() }
    }
    Scaffold(bottomBar = { SafetyBar() }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                viewModel.status?.device?.name ?: "Heliostat",
                style = MaterialTheme.typography.headlineSmall,
            )
            when (val state = viewModel.uiState) {
                DashboardUiState.Loading -> CircularProgressIndicator()

                is DashboardUiState.Unreachable -> Unreachable(state, onSetupAgain, onChangeAddress)

                is DashboardUiState.Live -> Live(state.telemetry, viewModel, onJog, onTarget)
            }
            viewModel.actionMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = viewModel::sendTime) { Text("Send phone time") }
                TextButton(onClick = viewModel::sendLocation) { Text("Send phone location") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onChangeAddress) { Text("Change address") }
                TextButton(onClick = { viewModel.forget(onSetupAgain) }) { Text("Set up again") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class) // FlowRow
@Composable
private fun Live(
    t: TelemetryResponse,
    viewModel: DashboardViewModel,
    onJog: () -> Unit,
    onTarget: () -> Unit,
) {
    t.latched?.let { reason ->
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Latched: $reason", color = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    "The heliostat stopped for safety. Check the machine, then clear it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Button(onClick = viewModel::clearFault) { Text("Clear fault") }
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(onClick = {}, label = { Text("Mode: ${t.mode}") })
        if (t.intent != t.mode) AssistChip(onClick = {}, label = { Text("Wants: ${t.intent}") })
    }
    if (t.trips.isNotEmpty()) {
        Text("Active: ${t.trips.joinToString()}", color = MaterialTheme.colorScheme.error)
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Reading("Sun", t.sun?.let { "az ${it.fmt(0)}°  el ${it.fmt(1)}°" } ?: "no valid time")
            Reading("Mirror", "az ${t.pos.fmt(0)}°  el ${t.pos.fmt(1)}°" + if (t.moving.any { it }) "  (moving)" else "")
            Reading("Aim", t.plan?.let { "az ${it.fmt(0)}°  el ${it.fmt(1)}°" } ?: "-")
            Reading("Beam", t.beam?.let { "az ${it.fmt(0)}°  el ${it.fmt(1)}°" } ?: "-")
            Reading("Efficiency", t.efficiency?.let { "${(it * 100).toInt()} %" } ?: "-")
            Reading("Servos", "${t.volts.joinToString(" / ") { v -> v?.let { "%.1f V".format(it) } ?: "-" }}   " +
                t.temp_c.joinToString(" / ") { c -> c?.let { "$it °C" } ?: "-" })
        }
    }

    val allowed = viewModel.status?.allowed_modes.orEmpty()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("track", "idle", "manual").forEach { mode ->
            FilledTonalButton(
                onClick = { viewModel.setMode(mode) },
                enabled = allowed.isEmpty() || mode in allowed || mode == t.mode,
            ) { Text(mode.replaceFirstChar { it.uppercase() }) }
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onJog) { Text("Jog") }
        OutlinedButton(onClick = onTarget) { Text("Target") }
    }
}

@Composable
private fun Unreachable(state: DashboardUiState.Unreachable, onSetupAgain: () -> Unit, onChangeAddress: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Can't reach the heliostat at ${state.host}", color = MaterialTheme.colorScheme.onErrorContainer)
            Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(
                "Is the phone on the same WiFi? If the heliostat moved network, it turns " +
                    "Bluetooth back on after 5 minutes offline, or hold its BOOT button for 3 s.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSetupAgain) { Text("Set up over Bluetooth") }
                OutlinedButton(onClick = onChangeAddress) { Text("Enter address") }
            }
        }
    }
}

@Composable
private fun Reading(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun List<Double?>.fmt(index: Int) = getOrNull(index)?.let { "%.1f".format(it) } ?: "-"
