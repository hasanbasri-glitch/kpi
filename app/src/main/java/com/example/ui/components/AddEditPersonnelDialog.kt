package com.example.ui.components

import com.example.ui.screens.StatusBadge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.PersonnelKpiEntity
import java.util.Locale

@Composable
fun AddEditPersonnelDialog(
    initialPersonnel: PersonnelKpiEntity? = null,
    onDismiss: () -> Unit,
    onSave: (PersonnelKpiEntity) -> Unit
) {
    var nik by remember { mutableStateOf(initialPersonnel?.nik ?: "") }
    var name by remember { mutableStateOf(initialPersonnel?.name ?: "") }
    var position by remember { mutableStateOf(initialPersonnel?.position ?: "CREW") }
    var storeCode by remember { mutableStateOf(initialPersonnel?.storeCode ?: "") }
    var storeName by remember { mutableStateOf(initialPersonnel?.storeName ?: "") }
    var ac by remember { mutableStateOf(initialPersonnel?.ac ?: "") }
    var am by remember { mutableStateOf(initialPersonnel?.am ?: "") }

    var pwpStr by remember { mutableStateOf(initialPersonnel?.pwp?.toString() ?: "15.0") }
    var psmStr by remember { mutableStateOf(initialPersonnel?.psm?.toString() ?: "15.0") }
    var serbaStr by remember { mutableStateOf(initialPersonnel?.serbaGratis?.toString() ?: "25.0") }
    var memberStr by remember { mutableStateOf(initialPersonnel?.member?.toString() ?: "18.0") }

    val computedPwp by remember { derivedStateOf { pwpStr.toDoubleOrNull() ?: 0.0 } }
    val computedPsm by remember { derivedStateOf { psmStr.toDoubleOrNull() ?: 0.0 } }
    val computedSerba by remember { derivedStateOf { serbaStr.toDoubleOrNull() ?: 0.0 } }
    val computedMember by remember { derivedStateOf { memberStr.toDoubleOrNull() ?: 0.0 } }

    val totalBobot by remember { derivedStateOf { computedPwp + computedPsm + computedSerba + computedMember } }
    val computedKet by remember { derivedStateOf { PersonnelKpiEntity.computeKet(totalBobot) } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialPersonnel == null) "➕ Tambah Personil Toko Baru" else "✏️ Edit Data Personil Toko",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nik,
                    onValueChange = { nik = it },
                    label = { Text("NIK (Nomor Induk Karyawan)") },
                    enabled = initialPersonnel == null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap Karyawan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = position,
                        onValueChange = { position = it },
                        label = { Text("Jabatan (CREW/ACOS/COS)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = storeCode,
                        onValueChange = { storeCode = it },
                        label = { Text("Kode Toko") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Nama Toko") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = ac,
                        onValueChange = { ac = it },
                        label = { Text("Area Coordinator (AC)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = am,
                        onValueChange = { am = it },
                        label = { Text("Area Manager (AM)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Capaian Bobot KPI (Max: PWP 20, PSM 20, SERBA 35, MEMBER 25)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pwpStr,
                        onValueChange = { pwpStr = it },
                        label = { Text("PWP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = psmStr,
                        onValueChange = { psmStr = it },
                        label = { Text("PSM") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = serbaStr,
                        onValueChange = { serbaStr = it },
                        label = { Text("SERBA") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = memberStr,
                        onValueChange = { memberStr = it },
                        label = { Text("MEMBER") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Grand Total Bobot", style = MaterialTheme.typography.labelMedium)
                            Text(String.format(Locale.US, "%.2f / 100.0", totalBobot), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        StatusBadge(ket = computedKet)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            if (nik.isNotBlank() && name.isNotBlank()) {
                                onSave(
                                    PersonnelKpiEntity(
                                        nik = nik.trim(),
                                        name = name.trim(),
                                        position = position.trim(),
                                        storeCode = storeCode.trim(),
                                        storeName = storeName.trim(),
                                        ac = ac.trim(),
                                        am = am.trim(),
                                        pwp = computedPwp,
                                        psm = computedPsm,
                                        serbaGratis = computedSerba,
                                        member = computedMember
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = nik.isNotBlank() && name.isNotBlank()
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}
