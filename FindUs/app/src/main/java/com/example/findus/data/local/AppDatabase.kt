package com.example.findus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.findus.data.local.dao.MotoristaDao
import com.example.findus.data.local.dao.RegistroTelemetriaDao
import com.example.findus.data.local.dao.VeiculoDao
import com.example.findus.data.local.entity.MotoristaEntity
import com.example.findus.data.local.entity.RegistroTelemetriaEntity
import com.example.findus.data.local.entity.VeiculoEntity

@Database(
    entities = [
        VeiculoEntity::class,
        RegistroTelemetriaEntity::class,
        MotoristaEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun veiculoDao(): VeiculoDao
    abstract fun registroTelemetriaDao(): RegistroTelemetriaDao
    abstract fun motoristaDao(): MotoristaDao

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
