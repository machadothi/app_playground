package com.machadothi.templateapp.ui.screen.devicescan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machadothi.templateapp.ble.DiscoveredDevice
import com.machadothi.templateapp.repository.heliostat.ProvisioningRepository
import com.machadothi.templateapp.ui.permission.PermissionGate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Finds heliostats in Bluetooth range. The board only advertises while it needs
 * setting up, so an empty list usually means it is already on WiFi -- which the
 * screen says, along with how to force Bluetooth on (hold BOOT for 3 s).
 */
@Composable
fun DeviceScanScreen(
    onDeviceSelected: (DiscoveredDevice) -> Unit,
    onUseAddress: () -> Unit,
    viewModel: DeviceScanViewModel = hiltViewModel(),
) {
    PermissionGate {
        DisposableEffect(Unit) {
            viewModel.start()
            onDispose { viewModel.stop() }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Find your heliostat", style = MaterialTheme.typography.headlineSmall)
            LinearProgressIndicator(Modifier.fillMaxWidth())

            when (val state = viewModel.uiState) {
                is DeviceScanUiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                is DeviceScanUiState.Scanning -> {
                    if (state.devices.isEmpty()) {
                        Text(
                            "Looking for my_heliostat…\n\n" +
                                "The heliostat only advertises while it is not on WiFi. If it " +
                                "already is, or you want to move it to another network, hold " +
                                "its BOOT button for 3 seconds: Bluetooth comes on for 5 minutes " +
                                "(its LED blinks three quick pulses).",
                        )
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.devices, key = { it.address }) { device ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.stop()
                                        onDeviceSelected(device)
                                    },
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(device.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${device.address}  ·  ${device.rssi} dBm")
                                }
                            }
                        }
                    }
                }
            }

            TextButton(onClick = onUseAddress) {
                Text("Already on WiFi? Enter its address instead")
            }
        }
    }
}

@HiltViewModel
class DeviceScanViewModel @Inject constructor(
    private val repository: ProvisioningRepository,
) : ViewModel() {

    var uiState by mutableStateOf<DeviceScanUiState>(DeviceScanUiState.Scanning(emptyList()))
        private set

    private var scan: Job? = null

    fun start() {
        if (scan?.isActive == true) return
        scan = viewModelScope.launch {
            repository.discover()
                .catch { uiState = DeviceScanUiState.Error(it.message ?: "Bluetooth scan failed") }
                .collect { uiState = DeviceScanUiState.Scanning(it) }
        }
    }

    fun stop() {
        scan?.cancel()
        scan = null
    }
}

sealed class DeviceScanUiState {
    data class Scanning(val devices: List<DiscoveredDevice>) : DeviceScanUiState()
    data class Error(val message: String) : DeviceScanUiState()
}
