package com.machadothi.templateapp.ui.navigation

import kotlinx.serialization.Serializable

object NavRoutes {

    // --- heliostat -------------------------------------------------------------------

    /** Decides: dashboard if a heliostat is set up on this phone, else device scan. */
    @Serializable
    data object Start

    @Serializable
    data object DeviceScan

    /** BLE provisioning of the device at [address]. */
    @Serializable
    data class Provision(val address: String)

    @Serializable
    data object Address

    @Serializable
    data object Dashboard

    @Serializable
    data object Jog

    @Serializable
    data object Target

    // --- the original sensor demo, kept as a reference for the conventions -------------

    @Serializable
    data object Sensors

    @Serializable
    data object Filter

    @Serializable
    data object Graph {

        @Serializable
        data object Temperature

        @Serializable
        data object Humidity

    }
}
