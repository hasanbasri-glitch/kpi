package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.KpiViewModel
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.OverallSummaryCards

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainKpiDashboardScreen(
    viewModel: KpiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }

    val allPersonnel by viewModel.allPersonnel.collectAsStateWithLifecycle()
    val filteredPersonnel by viewModel.filteredPersonnel.collectAsStateWithLifecycle()

    val availableAMs by viewModel.availableAMs.collectAsStateWithLifecycle()
    val selectedAMs by viewModel.selectedAMs.collectAsStateWithLifecycle()

    val availableACs by viewModel.availableACs.collectAsStateWithLifecycle()
    val selectedACs by viewModel.selectedACs.collectAsStateWithLifecycle()

    val availableStores by viewModel.availableStores.collectAsStateWithLifecycle()
    val selectedStores by viewModel.selectedStores.collectAsStateWithLifecycle()

    val acSummaries by viewModel.acSummaries.collectAsStateWithLifecycle()
    val storeSummaries by viewModel.storeSummaries.collectAsStateWithLifecycle()
    val productivityDistribution by viewModel.productivityDistribution.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedPersonnel by viewModel.selectedPersonnel.collectAsStateWithLifecycle()

    val importMessage by viewModel.importMessage.collectAsStateWithLifecycle()
    val googleSheetUrl by viewModel.googleSheetUrl.collectAsStateWithLifecycle()
    val isSyncingSheet by viewModel.isSyncingSheet.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()

    var tempSheetUrl by remember(googleSheetUrl) { mutableStateOf(googleSheetUrl) }

    LaunchedEffect(importMessage) {
        importMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.importMessage.value = null
        }
    }

    val activeFilterCount = selectedAMs.size + selectedACs.size + selectedStores.size

    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            icon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Sinkronkan Google Spreadsheet Live", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Masukkan URL Google Spreadsheet publik. Aplikasi akan membaca sheet 'GRAND TOTAL BOBOT' dan memperbarui seluruh data dashboard secara otomatis.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = tempSheetUrl,
                        onValueChange = { tempSheetUrl = it },
                        label = { Text("URL Google Spreadsheet") },
                        placeholder = { Text("https://docs.google.com/spreadsheets/d/...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                    if (lastSyncTime != null) {
                        Text(
                            "Terakhir sinkron: $lastSyncTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSyncDialog = false
                        viewModel.syncGoogleSheets(tempSheetUrl)
                    },
                    enabled = tempSheetUrl.isNotBlank() && !isSyncingSheet
                ) {
                    if (isSyncingSheet) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Menyinkronkan...")
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sinkronkan Sekarang")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            sheetState = sheetState,
            availableAMs = availableAMs,
            selectedAMs = selectedAMs,
            availableACs = availableACs,
            selectedACs = selectedACs,
            availableStores = availableStores,
            selectedStores = selectedStores,
            onToggleAM = { viewModel.toggleAM(it) },
            onToggleAC = { viewModel.toggleAC(it) },
            onToggleStore = { viewModel.toggleStore(it) },
            onClearFilters = { viewModel.clearFilters() },
            onDismiss = { showFilterSheet = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dashboard Evaluasi KPI",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "Evaluasi & Produktivitas Karyawan Toko",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncGoogleSheets() },
                        enabled = !isSyncingSheet
                    ) {
                        if (isSyncingSheet) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Sinkronkan Google Sheets",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (activeFilterCount > 0) {
                                    Badge { Text("$activeFilterCount") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Google Sheets Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isSyncingSheet) Icons.Default.Sync else Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Google Spreadsheet Live Sync",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (lastSyncTime != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ($lastSyncTime)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = googleSheetUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showSyncDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Link",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.syncGoogleSheets() },
                            enabled = !isSyncingSheet,
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (isSyncingSheet) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Overall KPI Metric Cards
            OverallSummaryCards(
                personnelList = filteredPersonnel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Tabs
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Detail Per Toko & AC", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Store, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Detail Personil & Gap", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.PersonSearch, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Distribusi Produktivitas", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    text = { Text("Data Mentah", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.TableRows, contentDescription = null) }
                )
            }

            // Screen Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTabIndex) {
                    0 -> DetailPerTokoAcScreen(
                        acSummaries = acSummaries,
                        storeSummaries = storeSummaries,
                        personnelList = filteredPersonnel
                    )
                    1 -> DetailPersonilGapScreen(
                        personnelList = filteredPersonnel,
                        selectedPersonnel = selectedPersonnel,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.searchQuery.value = it },
                        onSelectPersonnel = { viewModel.selectPersonnelForRadar(it) }
                    )
                    2 -> DistribusiProduktivitasScreen(
                        distribution = productivityDistribution,
                        personnelList = filteredPersonnel
                    )
                    3 -> DataMentahScreen(
                        personnelList = filteredPersonnel,
                        onAddOrUpdatePersonnel = { viewModel.addOrUpdatePersonnel(it) },
                        onDeletePersonnel = { viewModel.deletePersonnel(it) },
                        onImportFile = { ctx, uri -> viewModel.importFile(ctx, uri) },
                        onResetSampleData = { viewModel.resetToSampleData() },
                        onClearAllData = { viewModel.clearAllData() }
                    )
                }
            }
        }
    }
}

