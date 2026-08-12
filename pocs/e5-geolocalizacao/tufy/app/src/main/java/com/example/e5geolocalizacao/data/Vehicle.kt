package com.example.e5geolocalizacao.data

data class VehiclePosition(
    val latitude: Double,
    val longitude: Double
)

enum class EngineState {
    LIGADO,
    DESLIGADO
}

data class Telemetry(
    val speedKmh: Double,
    val doorsOpen: Boolean,
    val engineState: EngineState
)


data class RoutePoint(
    val position: VehiclePosition,
    val telemetry: Telemetry
)

data class VehicleInfo(
    val id: String,
    val label: String
)

data class VehicleState(
    val info: VehicleInfo,
    val position: VehiclePosition,
    val telemetry: Telemetry
)
