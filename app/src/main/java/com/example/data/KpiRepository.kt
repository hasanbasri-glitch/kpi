package com.example.data

import kotlinx.coroutines.flow.Flow

class KpiRepository(private val dao: PersonnelKpiDao) {
    val allPersonnel: Flow<List<PersonnelKpiEntity>> = dao.getAllPersonnel()

    suspend fun insertAll(list: List<PersonnelKpiEntity>) {
        dao.insertPersonnelList(list)
    }

    suspend fun insert(personnel: PersonnelKpiEntity) {
        dao.insertPersonnel(personnel)
    }

    suspend fun update(personnel: PersonnelKpiEntity) {
        dao.updatePersonnel(personnel)
    }

    suspend fun deleteByNik(nik: String) {
        dao.deletePersonnelByNik(nik)
    }

    suspend fun seedInitialDataIfEmpty(currentList: List<PersonnelKpiEntity>) {
        if (currentList.isEmpty()) {
            dao.insertPersonnelList(SampleData.getInitialPersonnelList())
        }
    }

    suspend fun clearAllAndInsert(list: List<PersonnelKpiEntity>) {
        dao.clearAll()
        dao.insertPersonnelList(list)
    }

    suspend fun clearAllAndSeed() {
        dao.clearAll()
        dao.insertPersonnelList(SampleData.getInitialPersonnelList())
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
