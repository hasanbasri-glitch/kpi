package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonnelKpiDao {
    @Query("SELECT * FROM personnel_kpi ORDER BY grandTotalBobot DESC")
    fun getAllPersonnel(): Flow<List<PersonnelKpiEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonnelList(list: List<PersonnelKpiEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonnel(personnel: PersonnelKpiEntity)

    @Update
    suspend fun updatePersonnel(personnel: PersonnelKpiEntity)

    @Query("DELETE FROM personnel_kpi WHERE nik = :nik")
    suspend fun deletePersonnelByNik(nik: String)

    @Query("DELETE FROM personnel_kpi")
    suspend fun clearAll()
}
