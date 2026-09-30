package com.machadothi.templateapp.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machadothi.templateapp.repository.heliostat.HeliostatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * DEFOCUS, STOW and E-STOP, reachable from every heliostat screen.
 *
 * These are the controls someone reaches for when the beam is somewhere it should
 * not be, so they are never more than one tap away, whatever screen is open.
 * The software E-stop latches and releases torque; the physical E-stop on the
 * machine additionally cuts servo power in hardware.
 */
@Composable
fun SafetyBar(viewModel: SafetyViewModel = hiltViewModel()) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            viewModel.message?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = { viewModel.set("defocus") }, modifier = Modifier.weight(1f)) {
                    Text("Defocus")
                }
                OutlinedButton(onClick = { viewModel.set("stow") }, modifier = Modifier.weight(1f)) {
                    Text("Stow")
                }
                Button(
                    onClick = { viewModel.set("estop") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("E-STOP", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@HiltViewModel
class SafetyViewModel @Inject constructor(
    private val repository: HeliostatRepository,
) : ViewModel() {

    var message by mutableStateOf<String?>(null)
        private set

    fun set(mode: String) {
        viewModelScope.launch {
            message = repository.setMode(mode).exceptionOrNull()?.message
        }
    }
}
