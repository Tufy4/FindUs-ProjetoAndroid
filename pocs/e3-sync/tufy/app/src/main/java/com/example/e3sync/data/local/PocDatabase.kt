package com.example.e3sync.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [VeiculoEntity::class], version = 1, exportSchema = false)
abstract class PocDatabase : RoomDatabase() {

    abstract fun veiculoDao(): VeiculoDao

    companion object {
        @Volatile
        private var instancia: PocDatabase? = null

        fun obter(context: Context): PocDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    PocDatabase::class.java,
                    "findus-poc-sync.db"
                ).build().also { instancia = it }
            }
    }
}
