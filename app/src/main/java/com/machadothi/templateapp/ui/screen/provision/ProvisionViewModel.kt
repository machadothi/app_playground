package com.machadothi.templateapp.ui.screen.provision

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.machadothi.templateapp.ble.VisibleNetwork
import com.machadothi.templateapp.ble.WifiState
import com.machadothi.templateapp.data.local.HeliostatPrefs
import com.machadothi.templateapp.repository.heliostat.HeliostatRepository
import com.machadothi.templateapp.repository.heliostat.ProvisioningRepository
import com.machadothi.templateapp.ui.navigation.NavRoutes
import com.machadothi.templateapp.wifi.CurrentNetwork
import com.machadothi.templateapp.wifi.PhoneLocation
import com.machadothi.templateapp.wifi.PhoneNetwork
import com.machadothi.templateapp.wifi.WifiBands
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hands the heliostat the credentials of the network the PHONE is on.
 *
 * Connect over BLE -> send the phone's time and location -> read the phone's
 * SSID and band -> ask the board which networks IT can hear -> pre-fill the form,
 * suggesting the 2.4 GHz sibling when the phone is on 5 GHz -> join -> on
 * success remember the IP and switch to HTTP.
 */
@HiltViewModel
class ProvisionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val provisioning: ProvisioningRepository,
    private val heliostat: HeliostatRepository,
    private val prefs: HeliostatPrefs,
    private val currentNetwork: CurrentNetwork,
    private val phoneLocation: PhoneLocation,
) : ViewModel() {

    private val address = savedStateHandle.toRoute<NavRoutes.Provision>().address

    var uiState by mutableStateOf<ProvisionUiState>(ProvisionUiState.Connecting)
        private set

    var ssid by mutableStateOf("")
    var password by mutableStateOf("")

    init {
        connect()
    }

    fun connect() {
        uiState = ProvisionUiState.Connecting
        viewModelScope.launch {
            provisioning.connect(address).onFailure {
                uiState = ProvisionUiState.Error("Could not connect: ${it.message}", canRetry = true)
                return@launch
            }
            provisioning.sendTime() // the board also gets NTP once online; this is the fallback
            val location = phoneLocation.current()
            location?.let { provisioning.sendLocation(it.latitude, it.longitude, it.altitude) }

            val phone = currentNetwork.read()
            val visible = provisioning.scanWifi().getOrDefault(emptyList())
            val suggestion = phone?.takeIf { !it.is24GHz }
                ?.let { WifiBands.suggest24GHzSibling(it.ssid, visible) }

            ssid = suggestion?.ssid ?: phone?.ssid ?: prefs.ssid().orEmpty()
            password = prefs.password(ssid).orEmpty()
            uiState = ProvisionUiState.Ready(
                phone = phone,
                visible = visible,
                suggestion = suggestion,
                locationSent = location != null,
                error = null,
            )
        }
    }

    fun pick(network: VisibleNetwork) {
        ssid = network.ssid
        viewModelScope.launch { password = prefs.password(network.ssid) ?: password }
    }

    fun join() {
        val ready = uiState as? ProvisionUiState.Ready ?: return
        val chosenSsid = ssid.trim()
        val chosenPassword = password
        uiState = ProvisionUiState.Joining(chosenSsid, "Sending credentials…")
        viewModelScope.launch {
            var outcome: WifiState? = null
            runCatching {
                provisioning.join(chosenSsid, chosenPassword).collect { state ->
                    outcome = state
                    if (state.phase == WifiState.Phase.JOINING) {
                        uiState = ProvisionUiState.Joining(chosenSsid, "Joining $chosenSsid…")
                    }
                }
            }.onFailure {
                uiState = ready.copy(error = "Lost contact with the heliostat: ${it.message}")
                return@launch
            }

            val result = outcome
            if (result?.phase == WifiState.Phase.JOINED && result.ip != null) {
                prefs.saveProvisioned(result.ip, address, chosenSsid, chosenPassword)
                heliostat.useHost(result.ip)
                provisioning.disconnect()
                uiState = ProvisionUiState.Joined(chosenSsid, result.ip)
            } else {
                uiState = ready.copy(error = result?.reason?.message ?: "The heliostat did not answer")
            }
        }
    }

    override fun onCleared() {
        provisioning.disconnect()
    }
}

sealed class ProvisionUiState {
    data object Connecting : ProvisionUiState()

    data class Ready(
        val phone: PhoneNetwork?,
        val visible: List<VisibleNetwork>,
        val suggestion: VisibleNetwork?,
        val locationSent: Boolean,
        val error: String?,
    ) : ProvisionUiState()

    data class Joining(val ssid: String, val step: String) : ProvisionUiState()
    data class Joined(val ssid: String, val ip: String) : ProvisionUiState()
    data class Error(val message: String, val canRetry: Boolean) : ProvisionUiState()
}
