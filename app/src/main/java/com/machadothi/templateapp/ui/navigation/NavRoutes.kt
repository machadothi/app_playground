package com.machadothi.templateapp.ui.navigation

import kotlinx.serialization.Serializable

object NavRoutes {

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