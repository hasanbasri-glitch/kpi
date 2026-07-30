package com.example.ui.components.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.PersonnelKpiEntity

data class ScoreBucket(
    val rangeLabel: String,
    val count: Int,
    val color: Color
)

@Composable
fun ScoreDistributionHistogram(
    personnelList: List<PersonnelKpiEntity>,
    modifier: Modifier = Modifier
) {
    if (personnelList.isEmpty()) return

    val buckets = listOf(
        ScoreBucket("< 50.0", personnelList.count { it.grandTotalBobot < 50.0 }, ProductivityColors.TidakProduktif),
        ScoreBucket("50.0 - 64.9", personnelList.count { it.grandTotalBobot in 50.0..64.99 }, ProductivityColors.KurangProduktif),
        ScoreBucket("65.0 - 79.9", personnelList.count { it.grandTotalBobot in 65.0..79.99 }, ProductivityColors.Produktif),
        ScoreBucket("80.0 - 89.9", personnelList.count { it.grandTotalBobot in 80.0..89.99 }, MaterialTheme.colorScheme.primary),
        ScoreBucket("90.0 - 100", personnelList.count { it.grandTotalBobot >= 90.0 }, ProductivityColors.SangatProduktif)
    )

    val maxCount = buckets.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnim = true }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Sebaran (Distribusi) Nilai Total Bobot Personil",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Vertical Bars Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                buckets.forEach { bucket ->
                    val ratio = (bucket.count.toFloat() / maxCount).coerceIn(0f, 1f)
                    val animatedHeightRatio by animateFloatAsState(
                        targetValue = if (startAnim) ratio else 0f,
                        animationSpec = tween(600),
                        label = "heightAnim"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(52.dp)
                    ) {
                        Text(
                            text = "${bucket.count}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = bucket.color
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height((120 * animatedHeightRatio).dp.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(bucket.color)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = bucket.rangeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
