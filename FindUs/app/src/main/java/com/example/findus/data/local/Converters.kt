package com.example.findus.data.local

import androidx.room.TypeConverter
import com.example.findus.data.enum.EstadoPortas
import com.example.findus.data.enum.StatusEntrega
import com.example.findus.data.enum.StatusOperacionalVeiculo
import com.example.findus.data.enum.TipoAlvoPerimetro
import com.example.findus.data.enum.TipoVeiculo

class Converters {
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

    @TypeConverter
    fun fromStatusEntrega(value: StatusEntrega): String = value.name

    @TypeConverter
    fun toStatusEntrega(value: String): StatusEntrega = StatusEntrega.valueOf(value)

    @TypeConverter
    fun fromTipoAlvoPerimetro(value: TipoAlvoPerimetro): String = value.name

    @TypeConverter
    fun toTipoAlvoPerimetro(value: String): TipoAlvoPerimetro = TipoAlvoPerimetro.valueOf(value)

    // Lista de ids (ex.: veículos de um grupo). Ids são UUIDs, então a vírgula é um separador seguro.
    @TypeConverter
    fun fromListaIds(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun toListaIds(value: String): List<String> = if (value.isBlank()) emptyList() else value.split(",")
}
