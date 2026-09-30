package com.machadothi.templateapp.ui.screen.start

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machadothi.templateapp.repository.heliostat.HeliostatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Decides where the app opens: the dashboard if a heliostat has been set up on
 * this phone before, otherwise the Bluetooth device scan.
 */
@Composable
fun StartScreen(
    onProvisioned: () -> Unit,
    onNotProvisioned: () -> Unit,
    viewModel: StartViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel.state) {
        when (viewModel.state) {
            StartState.Provisioned -> onProvisioned()
            StartState.NotProvisioned -> onNotProvisioned()
            StartState.Checking -> Unit
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@HiltViewModel
class StartViewModel @Inject constructor(
    repository: HeliostatRepository,
) : ViewModel() {

    var state by mutableStateOf<StartState>(StartState.Checking)
        private set

    init {
        viewModelScope.launch {
            state = if (repository.host() != null) StartState.Provisioned else StartState.NotProvisioned
        }
    }
}

sealed class StartState {
    data object Checking : StartState()
    data object Provisioned : StartState()
    data object NotProvisioned : StartState()
}
