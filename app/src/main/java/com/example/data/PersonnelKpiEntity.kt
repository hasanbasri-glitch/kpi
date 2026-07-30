package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "personnel_kpi")
data class PersonnelKpiEntity(
    @PrimaryKey val nik: String,
    val name: String,
    val position: String,
    val storeCode: String,
    val storeName: String,
    val ac: String,
    val am: String,
    val pwp: Double,
    val psm: Double,
    val serbaGratis: Double,
    val member: Double,
    val grandTotalBobot: Double = pwp + psm + serbaGratis + member,
    val ket: String = computeKet(pwp + psm + serbaGratis + member)
) {
    companion object {
        const val TARGET_PWP = 20.0
        const val TARGET_PSM = 20.0
        const val TARGET_SERBA = 35.0
        const val TARGET_MEMBER = 25.0
        const val TARGET_TOTAL = 100.0

        fun computeKet(totalScore: Double): String {
            return when {
                totalScore >= 85.0 -> "SANGAT PRODUKTIF"
                totalScore >= 70.0 -> "PRODUKTIF"
                totalScore >= 50.0 -> "KURANG PRODUKTIF"
                else -> "TIDAK PRODUKTIF"
            }
        }
    }

    val gapPwp: Double
        get() = (TARGET_PWP - pwp).coerceAtLeast(0.0)

    val gapPsm: Double
        get() = (TARGET_PSM - psm).coerceAtLeast(0.0)

    val gapSerba: Double
        get() = (TARGET_SERBA - serbaGratis).coerceAtLeast(0.0)

    val gapMember: Double
        get() = (TARGET_MEMBER - member).coerceAtLeast(0.0)

    val maxGapCategory: Pair<String, Double>
        get() {
            val gaps = mapOf(
                "PWP" to gapPwp,
                "PSM" to gapPsm,
                "SERBA GRATIS" to gapSerba,
                "MEMBER" to gapMember
            )
            val maxEntry = gaps.maxByOrNull { it.value }
            return if (maxEntry != null) Pair(maxEntry.key, maxEntry.value) else Pair("PWP", 0.0)
        }

    val analisisKekurangan: String
        get() {
            val (category, gap) = maxGapCategory
            return if (gap <= 0.0) {
                "Memenuhi Target Ideal Semua Indikator"
            } else {
                String.format(Locale.US, "Kekurangan Utama di: %s (Gap: %.2f)", category, gap)
            }
        }
}
