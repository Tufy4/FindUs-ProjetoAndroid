package com.example.e5geolocalizacao.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.e5geolocalizacao.data.VehicleSimulator
import com.example.e5geolocalizacao.data.VehicleState
import kotlinx.coroutines.flow.StateFlow


class MapViewModel(
    private val simulator: VehicleSimulator = VehicleSimulator()
) : ViewModel() {

    val vehicles: StateFlow<List<VehicleState>> = simulator.vehicles

    init {
        simulator.start(viewModelScope)
    }

    override fun onCleared() {
        simulator.stop()
        super.onCleared()
    }
}
