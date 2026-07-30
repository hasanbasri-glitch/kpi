package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.PersonnelKpiEntity
import com.example.ui.components.charts.ProductivityDistribution
import com.example.ui.components.charts.ProductivityPieChart
import com.example.ui.components.charts.ScoreDistributionHistogram
import java.util.Locale

@Composable
fun DistribusiProduktivitasScreen(
    distribution: ProductivityDistribution,
    personnelList: List<PersonnelKpiEntity>,
    modifier: Modifier = Modifier
) {
    val total = distribution.total
    val prodPct = if (total > 0) ((distribution.sangatProduktif + distribution.produktif).toFloat() / total) * 100f else 0f
    val kurangPct = if (total > 0) ((distribution.kurangProduktif + distribution.tidakProduktif).toFloat() / total) * 100f else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "📊 Distribusi Kategori Produktivitas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sebaran rasio tingkat produktivitas dan frekuensi nilai bobot personil toko secara komprehensif.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            ProductivityPieChart(distribution = distribution)
        }

        item {
            ScoreDistributionHistogram(personnelList = personnelList)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 Insight Evaluasi Produktivitas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = String.format(
                            Locale.US,
                            "• Sebanyak %.1f%% (%d orang) personil berada pada kategori Produktif dan Sangat Produktif.\n" +
                                    "• Sebanyak %.1f%% (%d orang) personil memerlukan intervensi pembinaan (Kurang & Tidak Produktif).\n" +
                                    "• Rata-rata bobot KPI secara keseluruhan berada pada angka %.2f.",
                            prodPct,
                            distribution.sangatProduktif + distribution.produktif,
                            kurangPct,
                            distribution.kurangProduktif + distribution.tidakProduktif,
                            if (personnelList.isNotEmpty()) personnelList.map { it.grandTotalBobot }.average() else 0.0
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}
