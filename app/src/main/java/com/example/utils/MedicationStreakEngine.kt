package com.example.utils

import com.example.data.local.MedicationAdherenceEntity
import com.example.data.local.MedicineEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class DailyAdherencePoint(
    val dateKey: String,
    val dayShortLabel: String,
    val takenCount: Int,
    val totalCount: Int,
    val ratio: Float // 0f..1f
)

data class PerMedicineAdherenceStat(
    val medicineId: Long,
    val medicineName: String,
    val dosage: String,
    val timeOfDay: String,
    val takenCount: Int,
    val skippedOrMissedCount: Int,
    val adherencePercent: Int,
    val takenToday: Boolean
)

data class MedicationStreakAnalytics(
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val adherenceRate7d: Int,
    val adherenceRate30d: Int,
    val totalTakenAllTime: Int,
    val pendingTodayCount: Int,
    val dailyPoints7d: List<DailyAdherencePoint>,
    val perMedicineStats: List<PerMedicineAdherenceStat>
)

/**
 * Computes medication adherence streaks, 7-day & 30-day compliance percentages,
 * 7-day chart bars, and per-medicine accountability statistics.
 */
object MedicationStreakEngine {

    private const val ONE_DAY_MS = 86_400_000L

    fun analyze(
        medicines: List<MedicineEntity>,
        adherenceLogs: List<MedicationAdherenceEntity>,
        referenceTimeMs: Long = System.currentTimeMillis()
    ): MedicationStreakAnalytics {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayLabelFmt = SimpleDateFormat("EEE", Locale.US)
        val todayKey = dateFmt.format(Date(referenceTimeMs))

        val logsByDate = adherenceLogs.groupBy { it.scheduledDate }

        // Build chronological last 7 days (oldest -> today)
        val dailyPoints7d = (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = referenceTimeMs - (daysAgo * ONE_DAY_MS)
            }
            val key = dateFmt.format(cal.time)
            val label = dayLabelFmt.format(cal.time)
            val dayLogs = logsByDate[key].orEmpty()
            val taken = dayLogs.count { it.status.equals("TAKEN", ignoreCase = true) }
            val expected = maxOf(dayLogs.size, medicines.count { it.isActive })
            val ratio = if (expected <= 0) 0f else (taken.toFloat() / expected.toFloat()).coerceIn(0f, 1f)
            DailyAdherencePoint(
                dateKey = key,
                dayShortLabel = label,
                takenCount = taken,
                totalCount = expected,
                ratio = ratio
            )
        }

        // Compute 7-day and 30-day adherence percentages
        val rate7d = computeWindowRate(adherenceLogs, referenceTimeMs, 7, dateFmt)
        val rate30d = computeWindowRate(adherenceLogs, referenceTimeMs, 30, dateFmt)

        // Compute current & best streak (consecutive days with >= 1 TAKEN and 0 MISSED/SKIPPED)
        val successfulDates = logsByDate.entries
            .filter { (_, logs) ->
                val taken = logs.count { it.status.equals("TAKEN", ignoreCase = true) }
                val missed = logs.count {
                    it.status.equals("MISSED", ignoreCase = true) ||
                        it.status.equals("SKIPPED", ignoreCase = true)
                }
                taken > 0 && missed == 0
            }
            .map { it.key }
            .toSet()

        // Current streak checks today first; if nothing logged yet today, checks starting yesterday
        var currentStreak = 0
        val startOffset = if (successfulDates.contains(todayKey)) 0 else 1
        var offset = startOffset
        while (offset < 365) {
            val key = dateFmt.format(Date(referenceTimeMs - (offset * ONE_DAY_MS)))
            if (successfulDates.contains(key)) {
                currentStreak++
                offset++
            } else {
                break
            }
        }

        // Best streak across sorted successful date keys
        val sortedEpochDays = successfulDates.mapNotNull { key ->
            try {
                val parsed = dateFmt.parse(key) ?: return@mapNotNull null
                (parsed.time / ONE_DAY_MS)
            } catch (_: Exception) {
                null
            }
        }.distinct().sorted()

        var bestStreak = 0
        var run = 0
        var prevDay: Long? = null
        for (day in sortedEpochDays) {
            if (prevDay != null && day == prevDay + 1L) {
                run++
            } else {
                run = 1
            }
            if (run > bestStreak) bestStreak = run
            prevDay = day
        }
        if (currentStreak > bestStreak) bestStreak = currentStreak

        // Per-medicine breakdown
        val activeMeds = medicines.filter { it.isActive }
        val perMedicineStats = activeMeds.map { med ->
            val medLogs = adherenceLogs.filter { it.medicineId == med.id }
            val taken = medLogs.count { it.status.equals("TAKEN", ignoreCase = true) }
            val skipped = medLogs.count {
                it.status.equals("SKIPPED", ignoreCase = true) ||
                    it.status.equals("MISSED", ignoreCase = true)
            }
            val denom = taken + skipped
            val pct = if (denom == 0) 0 else ((taken * 100.0) / denom).roundToInt().coerceIn(0, 100)
            val takenToday = medLogs.any {
                it.scheduledDate == todayKey && it.status.equals("TAKEN", ignoreCase = true)
            }
            PerMedicineAdherenceStat(
                medicineId = med.id,
                medicineName = med.name,
                dosage = med.dosage,
                timeOfDay = med.timeOfDay,
                takenCount = taken,
                skippedOrMissedCount = skipped,
                adherencePercent = pct,
                takenToday = takenToday
            )
        }

        val pendingTodayCount = perMedicineStats.count { !it.takenToday }
        val totalTakenAllTime = adherenceLogs.count { it.status.equals("TAKEN", ignoreCase = true) }

        return MedicationStreakAnalytics(
            currentStreakDays = currentStreak,
            bestStreakDays = bestStreak,
            adherenceRate7d = rate7d,
            adherenceRate30d = rate30d,
            totalTakenAllTime = totalTakenAllTime,
            pendingTodayCount = pendingTodayCount,
            dailyPoints7d = dailyPoints7d,
            perMedicineStats = perMedicineStats
        )
    }

    private fun computeWindowRate(
        logs: List<MedicationAdherenceEntity>,
        referenceTimeMs: Long,
        days: Int,
        dateFmt: SimpleDateFormat
    ): Int {
        val validKeys = (0 until days).map { offset ->
            dateFmt.format(Date(referenceTimeMs - (offset * ONE_DAY_MS)))
        }.toSet()

        val windowLogs = logs.filter { it.scheduledDate in validKeys }
        if (windowLogs.isEmpty()) return 0
        val taken = windowLogs.count { it.status.equals("TAKEN", ignoreCase = true) }
        val counted = windowLogs.count {
            it.status.equals("TAKEN", ignoreCase = true) ||
                it.status.equals("SKIPPED", ignoreCase = true) ||
                it.status.equals("MISSED", ignoreCase = true)
        }
        if (counted == 0) return 0
        return ((taken * 100.0) / counted).roundToInt().coerceIn(0, 100)
    }
}
