package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.KpiDatabase
import com.example.data.KpiRepository
import com.example.data.PersonnelKpiEntity
import com.example.ui.components.charts.AcSummaryItem
import com.example.ui.components.charts.ProductivityDistribution
import com.example.util.ExcelCsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StoreSummaryItem(
    val storeCode: String,
    val storeName: String,
    val ac: String,
    val am: String,
    val totalPersonil: Int,
    val rataRataBobot: Double,
    val bobotMin: Double,
    val bobotMax: Double
)

class KpiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KpiRepository

    val selectedAMs = MutableStateFlow<Set<String>>(emptySet())
    val selectedACs = MutableStateFlow<Set<String>>(emptySet())
    val selectedStores = MutableStateFlow<Set<String>>(emptySet())
    val searchQuery = MutableStateFlow("")
    val selectedPersonnelNik = MutableStateFlow<String?>(null)

    val isImporting = MutableStateFlow(false)
    val importMessage = MutableStateFlow<String?>(null)

    val googleSheetUrl = MutableStateFlow("https://docs.google.com/spreadsheets/d/1wbfkuUrgLvrTzWAGo2NXytLgRovOrpa6hgzI-p7wTMA/edit?usp=sharing")
    val isSyncingSheet = MutableStateFlow(false)
    val lastSyncTime = MutableStateFlow<String?>(null)

    init {
        val dao = KpiDatabase.getDatabase(application).personnelKpiDao()
        repository = KpiRepository(dao)

        // Initial Live Sync from injected Google Spreadsheet
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.allPersonnel
            repository.seedInitialDataIfEmpty(emptyList())
            syncGoogleSheets()
        }
    }

    val allPersonnel: StateFlow<List<PersonnelKpiEntity>> = repository.allPersonnel
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered Personnel Stream
    val filteredPersonnel: StateFlow<List<PersonnelKpiEntity>> = combine(
        allPersonnel,
        selectedAMs,
        selectedACs,
        selectedStores,
        searchQuery
    ) { all, ams, acs, stores, query ->
        var list = all

        if (ams.isNotEmpty()) {
            list = list.filter { ams.contains(it.am) }
        }
        if (acs.isNotEmpty()) {
            list = list.filter { acs.contains(it.ac) }
        }
        if (stores.isNotEmpty()) {
            list = list.filter { stores.contains(it.storeName) || stores.contains(it.storeCode) }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) || it.nik.lowercase().contains(q) || it.storeName.lowercase().contains(q)
            }
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Available AMs, ACs, Stores for Filter Dropdowns
    val availableAMs: StateFlow<List<String>> = combine(allPersonnel, selectedAMs) { all, _ ->
        all.map { it.am }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableACs: StateFlow<List<String>> = combine(allPersonnel, selectedAMs) { all, ams ->
        var list = all
        if (ams.isNotEmpty()) {
            list = list.filter { ams.contains(it.am) }
        }
        list.map { it.ac }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableStores: StateFlow<List<String>> = combine(allPersonnel, selectedAMs, selectedACs) { all, ams, acs ->
        var list = all
        if (ams.isNotEmpty()) list = list.filter { ams.contains(it.am) }
        if (acs.isNotEmpty()) list = list.filter { acs.contains(it.ac) }
        list.map { it.storeName }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AC Summaries
    val acSummaries: StateFlow<List<AcSummaryItem>> = filteredPersonnel.combine(allPersonnel) { filtered, _ ->
        filtered.groupBy { it.ac }.map { (ac, list) ->
            AcSummaryItem(
                ac = ac,
                totalCrew = list.size,
                avgBobot = list.map { it.grandTotalBobot }.average(),
                sangatProduktif = list.count { it.ket == "SANGAT PRODUKTIF" },
                produktif = list.count { it.ket == "PRODUKTIF" },
                kurangProduktif = list.count { it.ket == "KURANG PRODUKTIF" },
                tidakProduktif = list.count { it.ket == "TIDAK PRODUKTIF" }
            )
        }.sortedByDescending { it.avgBobot }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Store Summaries
    val storeSummaries: StateFlow<List<StoreSummaryItem>> = filteredPersonnel.combine(allPersonnel) { filtered, _ ->
        filtered.groupBy { Triple(it.storeCode, it.storeName, it.ac) }.map { (key, list) ->
            StoreSummaryItem(
                storeCode = key.first,
                storeName = key.second,
                ac = key.third,
                am = list.firstOrNull()?.am ?: "",
                totalPersonil = list.size,
                rataRataBobot = list.map { it.grandTotalBobot }.average(),
                bobotMin = list.minOfOrNull { it.grandTotalBobot } ?: 0.0,
                bobotMax = list.maxOfOrNull { it.grandTotalBobot } ?: 0.0
            )
        }.sortedByDescending { it.rataRataBobot }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Productivity Distribution
    val productivityDistribution: StateFlow<ProductivityDistribution> = filteredPersonnel.combine(allPersonnel) { filtered, _ ->
        ProductivityDistribution(
            sangatProduktif = filtered.count { it.ket == "SANGAT PRODUKTIF" },
            produktif = filtered.count { it.ket == "PRODUKTIF" },
            kurangProduktif = filtered.count { it.ket == "KURANG PRODUKTIF" },
            tidakProduktif = filtered.count { it.ket == "TIDAK PRODUKTIF" }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProductivityDistribution(0, 0, 0, 0))

    // Selected Personnel for Radar
    val selectedPersonnel: StateFlow<PersonnelKpiEntity?> = combine(filteredPersonnel, selectedPersonnelNik) { list, nik ->
        if (nik != null) {
            list.find { it.nik == nik } ?: list.firstOrNull()
        } else {
            list.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleAM(am: String) {
        val current = selectedAMs.value.toMutableSet()
        if (current.contains(am)) current.remove(am) else current.add(am)
        selectedAMs.value = current
    }

    fun toggleAC(ac: String) {
        val current = selectedACs.value.toMutableSet()
        if (current.contains(ac)) current.remove(ac) else current.add(ac)
        selectedACs.value = current
    }

    fun toggleStore(store: String) {
        val current = selectedStores.value.toMutableSet()
        if (current.contains(store)) current.remove(store) else current.add(store)
        selectedStores.value = current
    }

    fun clearFilters() {
        selectedAMs.value = emptySet()
        selectedACs.value = emptySet()
        selectedStores.value = emptySet()
        searchQuery.value = ""
    }

    fun selectPersonnelForRadar(nik: String) {
        selectedPersonnelNik.value = nik
    }

    fun syncGoogleSheets(rawUrl: String = googleSheetUrl.value) {
        viewModelScope.launch(Dispatchers.IO) {
            isSyncingSheet.value = true
            googleSheetUrl.value = rawUrl
            try {
                val parsed = ExcelCsvParser.fetchGoogleSheetsCsv(rawUrl)
                if (parsed.isNotEmpty()) {
                    repository.clearAllAndInsert(parsed)
                    val now = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    lastSyncTime.value = now
                    importMessage.value = "Berhasil sinkronasi live ${parsed.size} data personil dari Google Spreadsheet!"
                } else {
                    importMessage.value = "Gagal mengambil data dari Google Spreadsheet. Pastikan link dapat diakses publik (Anyone with the link can view)."
                }
            } catch (e: Exception) {
                importMessage.value = "Gagal sinkronasi Google Spreadsheet: ${e.localizedMessage}"
            } finally {
                isSyncingSheet.value = false
            }
        }
    }

    fun importFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            isImporting.value = true
            try {
                val parsed = ExcelCsvParser.parseFile(context, uri)
                if (parsed.isNotEmpty()) {
                    repository.insertAll(parsed)
                    importMessage.value = "Berhasil membaca file Excel (Sheet 'GRAND TOTAL BOBOT')! ${parsed.size} data personil diperbarui secara live."
                } else {
                    importMessage.value = "Gagal membaca data Excel/CSV. Pastikan file mempunyai sheet GRAND TOTAL BOBOT dan format kolom sesuai."
                }
            } catch (e: Exception) {
                importMessage.value = "Terjadi kesalahan saat impor: ${e.localizedMessage}"
            } finally {
                isImporting.value = false
            }
        }
    }

    fun addOrUpdatePersonnel(personnel: PersonnelKpiEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(personnel)
        }
    }

    fun deletePersonnel(nik: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteByNik(nik)
        }
    }

    fun resetToSampleData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllAndSeed()
            clearFilters()
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
            clearFilters()
        }
    }
}
