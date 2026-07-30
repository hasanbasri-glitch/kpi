package com.example.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PersonnelKpiEntity
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarChart(
    personnel: PersonnelKpiEntity,
    modifier: Modifier = Modifier
) {
    val categories = listOf("PWP", "PSM", "SERBA GRATIS", "MEMBER")
    val actualValues = listOf(personnel.pwp, personnel.psm, personnel.serbaGratis, personnel.member)
    val targetValues = listOf(
        PersonnelKpiEntity.TARGET_PWP,
        PersonnelKpiEntity.TARGET_PSM,
        PersonnelKpiEntity.TARGET_SERBA,
        PersonnelKpiEntity.TARGET_MEMBER
    )
    val maxRanges = listOf(20.0, 20.0, 35.0, 25.0)

    val actualColor = MaterialTheme.colorScheme.primary
    val actualFillColor = actualColor.copy(alpha = 0.35f)
    val targetLineColor = Color(0xFFDC3545) // Red dash for target
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(16.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width.coerceAtMost(size.height) / 2f) * 0.72f
            val numAxes = categories.size
            val angleStep = (2 * Math.PI / numAxes).toFloat()

            // 1. Draw Concentric Grid Levels (0.25, 0.50, 0.75, 1.0)
            val gridLevels = listOf(0.25f, 0.5f, 0.75f, 1.0f)
            for (level in gridLevels) {
                val gridRadius = radius * level
                val gridPath = Path()
                for (i in 0 until numAxes) {
                    val angle = (i * angleStep) - (Math.PI / 2).toFloat()
                    val x = center.x + gridRadius * cos(angle)
                    val y = center.y + gridRadius * sin(angle)
                    if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
                }
                gridPath.close()
                drawPath(gridPath, color = gridColor, style = Stroke(width = 1.dp.toPx()))
            }

            // 2. Draw Axes and Labels
            for (i in 0 until numAxes) {
                val angle = (i * angleStep) - (Math.PI / 2).toFloat()
                val endX = center.x + radius * cos(angle)
                val endY = center.y + radius * sin(angle)
                drawLine(color = gridColor, start = center, end = Offset(endX, endY), strokeWidth = 1.5.dp.toPx())

                // Axis Label Position
                val labelRadius = radius + 22.dp.toPx()
                val labelX = center.x + labelRadius * cos(angle)
                val labelY = center.y + labelRadius * sin(angle)

                val labelText = "${categories[i]}\n${String.format(Locale.US, "%.1f/%.0f", actualValues[i], maxRanges[i])}"
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 11.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }
                drawContext.canvas.nativeCanvas.drawText(
                    categories[i],
                    labelX,
                    labelY - 6.dp.toPx(),
                    paint
                )
                paint.apply {
                    color = android.graphics.Color.GRAY
                    textSize = 10.sp.toPx()
                    isFakeBoldText = false
                }
                drawContext.canvas.nativeCanvas.drawText(
                    String.format(Locale.US, "%.1f / %.0f", actualValues[i], maxRanges[i]),
                    labelX,
                    labelY + 8.dp.toPx(),
                    paint
                )
            }

            // 3. Draw Target Ideal Path (Red Dashed)
            val targetPath = Path()
            for (i in 0 until numAxes) {
                val angle = (i * angleStep) - (Math.PI / 2).toFloat()
                val ratio = (targetValues[i] / maxRanges[i]).toFloat().coerceIn(0f, 1f)
                val pointRadius = radius * ratio
                val x = center.x + pointRadius * cos(angle)
                val y = center.y + pointRadius * sin(angle)
                if (i == 0) targetPath.moveTo(x, y) else targetPath.lineTo(x, y)
            }
            targetPath.close()
            drawPath(
                targetPath,
                color = targetLineColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )

            // 4. Draw Actual Performance Path (Filled Primary)
            val actualPath = Path()
            val points = mutableListOf<Offset>()
            for (i in 0 until numAxes) {
                val angle = (i * angleStep) - (Math.PI / 2).toFloat()
                val ratio = (actualValues[i] / maxRanges[i]).toFloat().coerceIn(0f, 1f)
                val pointRadius = radius * ratio
                val x = center.x + pointRadius * cos(angle)
                val y = center.y + pointRadius * sin(angle)
                val point = Offset(x, y)
                points.add(point)
                if (i == 0) actualPath.moveTo(x, y) else actualPath.lineTo(x, y)
            }
            actualPath.close()
            drawPath(actualPath, color = actualFillColor)
            drawPath(actualPath, color = actualColor, style = Stroke(width = 2.5.dp.toPx()))

            // Draw Point Markers
            for (p in points) {
                drawCircle(color = actualColor, radius = 4.dp.toPx(), center = p)
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = p)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(actualColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Capaian Personil",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(20.dp))

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(targetLineColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Target Ideal (20 / 20 / 35 / 25)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = targetLineColor
            )
        }
    }
}
