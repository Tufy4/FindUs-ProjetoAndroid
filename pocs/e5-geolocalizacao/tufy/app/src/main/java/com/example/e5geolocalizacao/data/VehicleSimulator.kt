package com.example.e5geolocalizacao.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VehicleSimulator(
    private val fleet: Map<VehicleInfo, List<RoutePoint>> = VehicleRoutes.fleet,
    private val tickIntervalMs: Long = 2_000L
) {

    private val _vehicles = MutableStateFlow(initialState())
    val vehicles: StateFlow<List<VehicleState>> = _vehicles.asStateFlow()

    private var job: Job? = null

    private fun initialState(): List<VehicleState> =
        fleet.map { (info, route) ->
            val first = route.first()
            VehicleState(info = info, position = first.position, telemetry = first.telemetry)
        }

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return

        val cursors = fleet.keys.associateWith { 0 }.toMutableMap()

        job = scope.launch {
            while (true) {
                delay(tickIntervalMs)

                val updated = fleet.map { (info, route) ->
                    val currentIndex = cursors.getValue(info)
                    val nextIndex = (currentIndex + 1) % route.size
                    cursors[info] = nextIndex

                    val point = route[nextIndex]
                    VehicleState(info = info, position = point.position, telemetry = point.telemetry)
                }

                _vehicles.value = updated
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
