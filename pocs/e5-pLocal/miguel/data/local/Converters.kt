package com.example.findus.data.local

import androidx.room.TypeConverter
import com.example.findus.data.enum.EstadoPortas
import com.example.findus.data.enum.StatusOperacionalVeiculo
import com.example.findus.data.enum.TipoNegociante
import com.example.findus.data.enum.TipoVeiculo

class Converters {
    @TypeConverter
    fun fromTipoNegociante(value: TipoNegociante): String = value.name

    @TypeConverter
    fun toTipoNegociante(value: String): TipoNegociante = TipoNegociante.valueOf(value)

    @TypeConverter
    fun fromTipoVeiculo(value: TipoVeiculo): String = value.name

    @TypeConverter
    fun toTipoVeiculo(value: String): TipoVeiculo = TipoVeiculo.valueOf(value)

    @TypeConverter
    fun fromStatusOperacionalVeiculo(value: StatusOperacionalVeiculo): String = value.name

    @TypeConverter
    fun toStatusOperacionalVeiculo(value: String): StatusOperacionalVeiculo =
        StatusOperacionalVeiculo.valueOf(value)

    @TypeConverter
    fun fromEstadoPortas(value: EstadoPortas): String = value.name

    @TypeConverter
    fun toEstadoPortas(value: String): EstadoPortas = EstadoPortas.valueOf(value)
}
