package com.machadothi.templateapp.ui.screen.address

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machadothi.templateapp.repository.heliostat.HeliostatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Point the app at an address by hand: a heliostat already on WiFi, or the
 * laptop mock server (tools/mock_server.py, e.g. 192.168.50.132:8080).
 */
@Composable
fun AddressScreen(onDone: () -> Unit, viewModel: AddressViewModel = hiltViewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Heliostat address", style = MaterialTheme.typography.headlineSmall)
        Text(
            "The IP shown when it joined WiFi, or heliostat.local. For the laptop mock " +
                "server, use the laptop's IP and port, e.g. 192.168.50.132:8080.",
        )
        OutlinedTextField(
            value = viewModel.address,
            onValueChange = { viewModel.address = it },
            label = { Text("IP or IP:port") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.save(onDone) },
            enabled = viewModel.address.isNotBlank(),
        ) { Text("Use this address") }
    }
}

@HiltViewModel
class AddressViewModel @Inject constructor(
    private val repository: HeliostatRepository,
) : ViewModel() {

    var address by mutableStateOf("")

    init {
        viewModelScope.launch { address = repository.host().orEmpty() }
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.useHost(address.trim().removePrefix("http://").trimEnd('/'))
            onDone()
        }
    }
}
