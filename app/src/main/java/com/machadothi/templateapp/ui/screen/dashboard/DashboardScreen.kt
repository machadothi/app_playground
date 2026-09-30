package com.machadothi.templateapp.ui.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.machadothi.templateapp.data.network.StatusResponse
import com.machadothi.templateapp.data.network.TelemetryResponse
import com.machadothi.templateapp.ui.component.Banner
import com.machadothi.templateapp.ui.component.BannerKind
import com.machadothi.templateapp.ui.component.HeliostatTopBar
import com.machadothi.templateapp.ui.component.MetricTile
import com.machadothi.templateapp.ui.component.ModeBadge
import com.machadothi.templateapp.ui.component.SafetyBar
import com.machadothi.templateapp.ui.component.SectionCard
import com.machadothi.templateapp.ui.component.SkyDial
import com.machadothi.templateapp.ui.component.SkyPoint

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
    DashboardContent(
        state = viewModel.uiState,
        status = viewModel.status,
        actionMessage = viewModel.actionMessage,
        onMode = viewModel::setMode,
        onClearFault = viewModel::clearFault,
        onJog = onJog,
        onTarget = onTarget,
        onSendTime = viewModel::sendTime,
        onSendLocation = viewModel::sendLocation,
        onChangeAddress = onChangeAddress,
        onSetupAgain = { viewModel.forget(onSetupAgain) },
    )
}

@Composable
fun DashboardContent(
    state: DashboardUiState,
    status: StatusResponse?,
    actionMessage: String?,
    onMode: (String) -> Unit,
    onClearFault: () -> Unit,
    onJog: () -> Unit,
    onTarget: () -> Unit,
    onSendTime: () -> Unit,
    onSendLocation: () -> Unit,
    onChangeAddress: () -> Unit,
    onSetupAgain: () -> Unit,
    bottomBar: @Composable () -> Unit = { SafetyBar() },
) {
    val host = (state as? DashboardUiState.Live)?.host ?: (state as? DashboardUiState.Unreachable)?.host
    Scaffold(
        topBar = {
            HeliostatTopBar(
                title = status?.device?.name ?: "Heliostat",
                subtitle = listOfNotNull(host, status?.device?.fw?.let { "fw $it" }).joinToString("  ·  ")
                    .ifEmpty { null },
                actions = { OverflowMenu(onSendTime, onSendLocation, onChangeAddress, onSetupAgain) },
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (state) {
                DashboardUiState.Loading -> Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    horizontalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }

                is DashboardUiState.Unreachable -> Banner(
                    title = "Can't reach the heliostat at ${state.host}",
                    text = "${state.message}\n\nIs the phone on the same WiFi? If the heliostat changed " +
                        "network, it turns Bluetooth back on after 5 minutes offline, or hold its BOOT " +
                        "button for 3 seconds.",
                    kind = BannerKind.Error,
                    actions = {
                        Button(onClick = onSetupAgain) { Text("Set up again") }
                        OutlinedButton(onClick = onChangeAddress) { Text("Enter address") }
                    },
                )

                is DashboardUiState.Live -> Live(state.telemetry, status, onMode, onClearFault, onJog, onTarget)
            }
            actionMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Live(
    t: TelemetryResponse,
    status: StatusResponse?,
    onMode: (String) -> Unit,
    onClearFault: () -> Unit,
    onJog: () -> Unit,
    onTarget: () -> Unit,
) {
    t.latched?.let { reason ->
        Banner(
            title = "Stopped for safety",
            text = "$reason\n\nCheck the machine, then clear the fault.",
            kind = BannerKind.Error,
            actions = { Button(onClick = onClearFault) { Text("Clear fault") } },
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ModeBadge(t.mode)
        if (t.intent != t.mode) {
            Text(
                "waiting to ${t.intent}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    val trips = t.trips.filter { it != "comms" }
    if (trips.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            trips.forEach { AssistChip(onClick = {}, label = { Text(it.replace('_', ' ')) }) }
        }
    }

    SectionCard(title = null) {
        SkyDial(
            sun = t.sun?.toSkyPoint(),
            mirror = t.pos.takeIf { it.size == 2 && it.all { v -> v != null } }
                ?.let { SkyPoint(it[0]!!, it[1]!!) },
            beam = t.beam?.toSkyPoint(),
            target = status?.target?.let { SkyPoint(it.az, it.el) },
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile("Sun elevation", t.sun?.getOrNull(1)?.fmt() ?: "—", Modifier.weight(1f), unit = "°")
        MetricTile("Efficiency", t.efficiency?.let { "${(it * 100).toInt()}" } ?: "—", Modifier.weight(1f), unit = "%")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile(
            "Mirror az / el",
            "${t.pos.getOrNull(0)?.fmt() ?: "—"} / ${t.pos.getOrNull(1)?.fmt() ?: "—"}",
            Modifier.weight(1f),
            unit = "°",
        )
        MetricTile(
            "Servos",
            t.volts.joinToString(" / ") { v -> v?.let { "%.1f".format(it) } ?: "—" },
            Modifier.weight(1f),
            unit = "V",
        )
    }

    SectionCard(title = "Mode") {
        val choices = listOf(
            Triple("track", "Track", Icons.Rounded.WbSunny),
            Triple("idle", "Idle", Icons.Rounded.PauseCircle),
            Triple("manual", "Manual", Icons.Rounded.PanTool),
        )
        val allowed = status?.allowed_modes.orEmpty()
        val current = if (t.intent == "track") "track" else t.mode
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            choices.forEachIndexed { index, (mode, label, icon) ->
                SegmentedButton(
                    selected = current == mode,
                    onClick = { onMode(mode) },
                    enabled = allowed.isEmpty() || mode in allowed || mode == t.mode,
                    shape = SegmentedButtonDefaults.itemShape(index, choices.size),
                    icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                ) { Text(label) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton("Jog", Icons.Rounded.OpenWith, Modifier.weight(1f), onJog)
            ActionButton("Target", Icons.Rounded.GpsFixed, Modifier.weight(1f), onTarget)
        }
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("  $label")
    }
}

@Composable
private fun OverflowMenu(
    onSendTime: () -> Unit,
    onSendLocation: () -> Unit,
    onChangeAddress: () -> Unit,
    onSetupAgain: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More") }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        MenuItem("Send phone time", Icons.Rounded.AccessTime) { open = false; onSendTime() }
        MenuItem("Send phone location", Icons.Rounded.MyLocation) { open = false; onSendLocation() }
        MenuItem("Change address", Icons.Rounded.Edit) { open = false; onChangeAddress() }
        MenuItem("Set up over Bluetooth", Icons.Rounded.Bluetooth) { open = false; onSetupAgain() }
    }
}

@Composable
private fun MenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}

private fun List<Double>.toSkyPoint() = if (size >= 2) SkyPoint(this[0], this[1]) else null

private fun Double.fmt() = "%.1f".format(this)
