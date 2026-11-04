package com.example.findus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.findus.data.local.dao.CentroDistribuicaoDao
import com.example.findus.data.local.dao.EntregaDao
import com.example.findus.data.local.dao.GrupoVeiculoDao
import com.example.findus.data.local.dao.MotoristaDao
import com.example.findus.data.local.dao.PerimetroDao
import com.example.findus.data.local.dao.RegistroTelemetriaDao
import com.example.findus.data.local.dao.VeiculoDao
import com.example.findus.data.local.entity.CentroDistribuicaoEntity
import com.example.findus.data.local.entity.EntregaEntity
import com.example.findus.data.local.entity.GrupoVeiculoEntity
import com.example.findus.data.local.entity.MotoristaEntity
import com.example.findus.data.local.entity.PerimetroEntity
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import com.example.findus.data.local.entity.VeiculoEntity

@Database(
    entities = [
        VeiculoEntity::class,
        RegistroTelemetriaEntity::class,
        MotoristaEntity::class,
        CentroDistribuicaoEntity::class,
        EntregaEntity::class,
        GrupoVeiculoEntity::class,
        PerimetroEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun veiculoDao(): VeiculoDao
    abstract fun registroTelemetriaDao(): RegistroTelemetriaDao
    abstract fun motoristaDao(): MotoristaDao
    abstract fun centroDistribuicaoDao(): CentroDistribuicaoDao
    abstract fun entregaDao(): EntregaDao
    abstract fun grupoVeiculoDao(): GrupoVeiculoDao
    abstract fun perimetroDao(): PerimetroDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "findus.db"
                )
                    // PoC: schema ainda em mudança: recria o banco em vez de exigir migrations manuais.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { instancia = it }
            }
    }
}
