package com.machadothi.templateapp.ui.screen.target

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machadothi.templateapp.repository.heliostat.HeliostatRepository
import com.machadothi.templateapp.ui.component.SafetyBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Where the reflected beam should land.
 *
 * Two ways to set it: type the direction (azimuth from true north, elevation
 * above the horizon), or -- far easier in practice -- jog the mirror until the
 * spot sits where you want it and tap "Capture current aim". The heliostat works
 * the target out from the sun's position and the mirror's current angle.
 */
@Composable
fun TargetScreen(onJog: () -> Unit, viewModel: TargetViewModel = hiltViewModel()) {
    Scaffold(bottomBar = { SafetyBar() }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Target", style = MaterialTheme.typography.headlineSmall)
            viewModel.current?.let { Text("Current: $it") }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Capture current aim", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "1. Jog the mirror until the spot lands where you want it.\n" +
                            "2. Tap Capture. Needs the sun to be up and on the mirror.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onJog) { Text("Open jog") }
                        Button(onClick = viewModel::capture) { Text("Capture") }
                    }
                }
            }

            HorizontalDivider()
            Text("Or enter it", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = viewModel.azimuth,
                onValueChange = { viewModel.azimuth = it },
                label = { Text("Azimuth ° (from north, clockwise)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.elevation,
                onValueChange = { viewModel.elevation = it },
                label = { Text("Elevation ° (above horizon)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = viewModel::save) { Text("Save target") }

            viewModel.message?.let { Text(it) }
        }
    }
}

@HiltViewModel
class TargetViewModel @Inject constructor(
    private val repository: HeliostatRepository,
) : ViewModel() {

    var azimuth by mutableStateOf("")
    var elevation by mutableStateOf("")
    var current by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            repository.status().onSuccess { status ->
                status.target?.let {
                    azimuth = "%.1f".format(it.az)
                    elevation = "%.1f".format(it.el)
                    current = "az %.1f°, el %.1f°".format(it.az, it.el)
                }
            }
        }
    }

    fun save() {
        val az = azimuth.replace(',', '.').toDoubleOrNull()
        val el = elevation.replace(',', '.').toDoubleOrNull()
        if (az == null || el == null) {
            message = "Enter numbers for both angles"
            return
        }
        viewModelScope.launch {
            message = repository.setTarget(az, el).fold(
                { "Saved: az %.1f°, el %.1f°".format(it.az, it.el).also { s -> current = s } },
                { it.message },
            )
        }
    }

    fun capture() {
        viewModelScope.launch {
            message = repository.captureTarget().fold(
                {
                    azimuth = "%.1f".format(it.az)
                    elevation = "%.1f".format(it.el)
                    "Captured: az %.1f°, el %.1f°".format(it.az, it.el).also { s -> current = s }
                },
                { it.message },
            )
        }
    }
}
