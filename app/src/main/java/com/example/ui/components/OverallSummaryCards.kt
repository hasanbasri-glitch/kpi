package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.PersonnelKpiEntity
import java.util.Locale

@Composable
fun OverallSummaryCards(
    personnelList: List<PersonnelKpiEntity>,
    modifier: Modifier = Modifier
) {
    val totalCrew = personnelList.size
    val minBobot = if (totalCrew > 0) personnelList.minOf { it.grandTotalBobot } else 0.0
    val maxBobot = if (totalCrew > 0) personnelList.maxOf { it.grandTotalBobot } else 0.0

    val sangatProd = personnelList.count { it.ket == "SANGAT PRODUKTIF" }
    val prod = personnelList.count { it.ket == "PRODUKTIF" }
    val kurangProd = personnelList.count { it.ket == "KURANG PRODUKTIF" }
    val tidakProd = personnelList.count { it.ket == "TIDAK PRODUKTIF" }

    val totalProd = sangatProd + prod
    val totalKurang = kurangProd + tidakProd

    val prodPct = if (totalCrew > 0) (totalProd.toFloat() / totalCrew) * 100f else 0f
    val kurangPct = if (totalCrew > 0) (totalKurang.toFloat() / totalCrew) * 100f else 0f

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MetricCard(
                title = "Total Personil",
                value = "$totalCrew Orang",
                subtitle = "Personil Terdaftar",
                icon = Icons.Default.Groups,
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            MetricCard(
                title = "Bobot Tertinggi",
                value = String.format(Locale.US, "%.2f", maxBobot),
                subtitle = "Skor Maksimal",
                icon = Icons.Default.ArrowUpward,
                color = Color(0xFF10B981)
            )
        }
        item {
            MetricCard(
                title = "Bobot Terendah",
                value = String.format(Locale.US, "%.2f", minBobot),
                subtitle = "Skor Minimal",
                icon = Icons.Default.ArrowDownward,
                color = Color(0xFFEF4444)
            )
        }
        item {
            MetricCard(
                title = "Potensi / Produktif",
                value = "$totalProd Orang",
                subtitle = String.format(Locale.US, "%.1f%% dari Total", prodPct),
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF06B6D4)
            )
        }
        item {
            MetricCard(
                title = "Tidak/Kurang Produktif",
                value = "$totalKurang Orang",
                subtitle = String.format(Locale.US, "%.1f%% dari Total", kurangPct),
                icon = Icons.Default.Warning,
                color = Color(0xFFF59E0B)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.height(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
