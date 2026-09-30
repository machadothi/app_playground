package com.machadothi.templateapp.ui.screen.provision

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

@Composable
fun ProvisionScreen(
    onDone: () -> Unit,
    viewModel: ProvisionViewModel = hiltViewModel(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Connect to WiFi", style = MaterialTheme.typography.headlineSmall)

        when (val state = viewModel.uiState) {
            ProvisionUiState.Connecting -> Busy("Connecting to my_heliostat…")

            is ProvisionUiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                if (state.canRetry) Button(onClick = viewModel::connect) { Text("Try again") }
            }

            is ProvisionUiState.Joining -> Busy(state.step)

            is ProvisionUiState.Joined -> {
                Text("✓ Joined ${state.ssid}", style = MaterialTheme.typography.titleMedium)
                Text("The heliostat is at ${state.ip}. Bluetooth turns off in a few seconds.")
                LaunchedEffect(state) {
                    delay(1_500)
                    onDone()
                }
            }

            is ProvisionUiState.Ready -> ReadyForm(state, viewModel)
        }
    }
}

@Composable
private fun ReadyForm(state: ProvisionUiState.Ready, viewModel: ProvisionViewModel) {
    val phone = state.phone
    when {
        phone == null -> Text(
            "Couldn't read which WiFi network your phone is on. Type the network name " +
                "below, or pick one the heliostat can hear.",
        )

        phone.is24GHz -> Text("Your phone is on ${phone.ssid}. The heliostat will join the same network.")

        else -> Warning(
            "Your phone is on ${phone.ssid}, which is a 5 GHz network. The heliostat only has " +
                "2.4 GHz WiFi and can't join it." +
                (state.suggestion?.let {
                    "\n\n${it.ssid} looks like the same router's 2.4 GHz network, so it's " +
                        "selected below. Routers usually use the same password for both."
                } ?: "\n\nPick a 2.4 GHz network the heliostat can hear, below."),
        )
    }

    state.error?.let { Warning(it) }

    OutlinedTextField(
        value = viewModel.ssid,
        onValueChange = { viewModel.ssid = it },
        label = { Text("Network name (SSID)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    var showPassword by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = viewModel.password,
        onValueChange = { viewModel.password = it },
        label = { Text("WiFi password") },
        singleLine = true,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "Hide" else "Show") }
        },
        modifier = Modifier.fillMaxWidth(),
    )

    Button(
        onClick = viewModel::join,
        enabled = viewModel.ssid.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Connect heliostat to ${viewModel.ssid.ifBlank { "WiFi" }}") }

    Text(
        if (state.locationSent) "Time and location sent to the heliostat."
        else "Time sent. Location unavailable; set it later from the dashboard.",
        style = MaterialTheme.typography.bodySmall,
    )

    if (state.visible.isNotEmpty()) {
        Text("Networks the heliostat can hear", style = MaterialTheme.typography.titleSmall)
        state.visible.take(10).forEach { network ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.pick(network) }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(network.ssid)
                Text("${network.rssi} dBm · ch ${network.channel}")
            }
        }
    }
}

@Composable
private fun Busy(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator()
        Text(text)
    }
}

@Composable
private fun Warning(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(text, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}
