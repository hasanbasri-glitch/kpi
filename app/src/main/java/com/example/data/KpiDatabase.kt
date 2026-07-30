package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PersonnelKpiEntity::class], version = 1, exportSchema = false)
abstract class KpiDatabase : RoomDatabase() {
    abstract fun personnelKpiDao(): PersonnelKpiDao

    companion object {
        @Volatile
        private var INSTANCE: KpiDatabase? = null

        fun getDatabase(context: Context): KpiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KpiDatabase::class.java,
                    "kpi_store_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
