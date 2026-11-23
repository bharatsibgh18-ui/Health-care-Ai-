package com.example.utils

import com.example.data.local.ScannedReportEntity
import com.example.data.local.VitalLogEntity
import kotlin.math.abs
import kotlin.math.roundToInt

enum class BiomarkerAlertLevel {
    NORMAL,
    BORDERLINE,
    HIGH_ALERT
}

enum class BiomarkerTrendDirection {
    IMPROVED,
    WORSENED,
    STABLE,
    SINGLE_READING
}

data class BiomarkerReading(
    val value: Double,
    val timestamp: Long,
    val sourceTitle: String
)

data class BiomarkerTrendCard(
    val key: String,
    val name: String,
    val unit: String,
    val normalRangeLabel: String,
    val normalMin: Double,
    val normalMax: Double,
    val latestValue: Double,
    val previousValue: Double?,
    val delta: Double?,
    val alertLevel: BiomarkerAlertLevel,
    val trendDirection: BiomarkerTrendDirection,
    val alertMessage: String,
    val history: List<BiomarkerReading>
)

private data class BiomarkerSpec(
    val key: String,
    val name: String,
    val unit: String,
    val normalMin: Double,
    val normalMax: Double,
    val normalRangeLabel: String,
    val plausibleMin: Double,
    val plausibleMax: Double,
    val regex: Regex
)

/**
 * Extracts standardized clinical biomarkers from scanned OCR lab reports and logged vitals,
 * tracks longitudinal deltas between reports, and flags out-of-range clinical alerts.
 */
object LabReportTrendEngine {

    private val specs = listOf(
        BiomarkerSpec(
            key = "hb",
            name = "Hemoglobin (Hb)",
            unit = "g/dL",
            normalMin = 12.0,
            normalMax = 17.5,
            normalRangeLabel = "12.0 - 17.5 g/dL",
            plausibleMin = 4.0,
            plausibleMax = 22.0,
            regex = Regex("""(?i)\b(?:hemoglobin|haemoglobin|hb)\b[^0-9\n]{0,18}(\d{1,2}(?:\.\d{1,2})?)""")
        ),
        BiomarkerSpec(
            key = "hba1c",
            name = "HbA1c (3-Mo Sugar)",
            unit = "%",
            normalMin = 4.0,
            normalMax = 5.6,
            normalRangeLabel = "4.0 - 5.6 %",
            plausibleMin = 3.0,
            plausibleMax = 18.0,
            regex = Regex("""(?i)\b(?:hba1c|hb\s*a1c|glycated\s+hemoglobin)\b[^0-9\n]{0,18}(\d{1,2}(?:\.\d{1,2})?)""")
        ),
        BiomarkerSpec(
            key = "fasting_glucose",
            name = "Fasting Blood Glucose",
            unit = "mg/dL",
            normalMin = 70.0,
            normalMax = 99.0,
            normalRangeLabel = "70 - 99 mg/dL",
            plausibleMin = 40.0,
            plausibleMax = 500.0,
            regex = Regex("""(?i)\b(?:fasting\s+(?:blood\s+)?(?:glucose|sugar)|fbs)\b[^0-9\n]{0,18}(\d{2,3}(?:\.\d{1,2})?)""")
        ),
        BiomarkerSpec(
            key = "cholesterol",
            name = "Total Cholesterol",
            unit = "mg/dL",
            normalMin = 100.0,
            normalMax = 200.0,
            normalRangeLabel = "< 200 mg/dL",
            plausibleMin = 70.0,
            plausibleMax = 500.0,
            regex = Regex("""(?i)\b(?:total\s+cholesterol|cholesterol(?:\s+total)?)\b[^0-9\n]{0,18}(\d{2,3}(?:\.\d{1,2})?)""")
        ),
        BiomarkerSpec(
            key = "ldl",
            name = "LDL Cholesterol",
            unit = "mg/dL",
            normalMin = 0.0,
            normalMax = 100.0,
            normalRangeLabel = "< 100 mg/dL",
            plausibleMin = 20.0,
            plausibleMax = 350.0,
            regex = Regex("""(?i)\b(?:ldl(?:\s+cholesterol)?)\b[^0-9\n]{0,18}(\d{2,3}(?:\.\d{1,2})?)""")
        ),
        BiomarkerSpec(
            key = "hdl",
            name = "HDL Cholesterol",
            unit = "mg/dL",
            normalMin = 40.0,
            normalMax = 100.0,
            normalRangeLabel = "≥ 40 mg/dL",
            plausibleMin = 15.0,
            plausibleMax = 140.0,
            regex = Regex("""(?i)\b(?:hdl(?:\s+cholesterol)?)\b[^0-9\n]{0,18}(\d{2,3}(?:\.\d{1,2})?)""")
        ),
        BiomarkerSpec(
            key = "triglycerides",
            name = "Triglycerides",
            unit = "mg/dL",
            normalMin = 0.0,
            normalMax = 150.0,
            normalRangeLabel = "< 150 mg/dL",
            plausibleMin = 30.0,
            plausibleMax = 1000.0,
            regex = Regex("""(?i)\b(?:triglycerides?|tg)\b[^0-9\n]{0,18}(\d{2,4}(?:\.\d{1,2})?)""")
        ),
        BiomarkerSpec(
            key = "tsh",
            name = "Thyroid (TSH)",
            unit = "mIU/L",
            normalMin = 0.4,
            normalMax = 4.2,
            normalRangeLabel = "0.4 - 4.2 mIU/L",
            plausibleMin = 0.01,
            plausibleMax = 100.0,
            regex = Regex("""(?i)\b(?:tsh|thyroid\s+stimulating)\b[^0-9\n]{0,18}(\d{1,2}(?:\.\d{1,2})?)""")
        ),
        BiomarkerSpec(
            key = "creatinine",
            name = "Serum Creatinine",
            unit = "mg/dL",
            normalMin = 0.6,
            normalMax = 1.3,
            normalRangeLabel = "0.6 - 1.3 mg/dL",
            plausibleMin = 0.2,
            plausibleMax = 15.0,
            regex = Regex("""(?i)\b(?:serum\s+creatinine|creatinine)\b[^0-9\n]{0,18}(\d{1,2}(?:\.\d{1,2})?)""")
        )
    )

    fun analyzeTrends(
        reports: List<ScannedReportEntity>,
        vitalLogs: List<VitalLogEntity> = emptyList()
    ): List<BiomarkerTrendCard> {
        val readingsByKey = linkedMapOf<String, MutableList<BiomarkerReading>>()

        // 1. Parse chronologically from OCR scanned reports
        reports.sortedBy { it.timestamp }.forEach { report ->
            val combinedText = "${report.extractedText}\n${report.aiExplanation}"
            for (spec in specs) {
                val match = spec.regex.find(combinedText) ?: continue
                val value = match.groupValues.getOrNull(1)?.toDoubleOrNull() ?: continue
                if (value in spec.plausibleMin..spec.plausibleMax) {
                    readingsByKey.getOrPut(spec.key) { mutableListOf() }.add(
                        BiomarkerReading(
                            value = round1(value),
                            timestamp = report.timestamp,
                            sourceTitle = report.title.ifBlank { "Lab Report" }
                        )
                    )
                }
            }
        }

        // 2. Also incorporate Blood Sugar & Systolic BP from Vitals logs for continuous trend tracking
        val sugarVitals = vitalLogs
            .filter { it.vitalType.equals("Blood Sugar", ignoreCase = true) && it.primaryValue in 40.0..500.0 }
            .sortedBy { it.timestamp }
        if (sugarVitals.isNotEmpty() && readingsByKey["fasting_glucose"].isNullOrEmpty()) {
            readingsByKey["fasting_glucose"] = sugarVitals.map {
                BiomarkerReading(round1(it.primaryValue), it.timestamp, "Vitals Log")
            }.toMutableList()
        }

        val bpVitals = vitalLogs
            .filter { it.vitalType.equals("Blood Pressure", ignoreCase = true) && it.primaryValue in 70.0..240.0 }
            .sortedBy { it.timestamp }

        val cards = mutableListOf<BiomarkerTrendCard>()

        for (spec in specs) {
            val history = readingsByKey[spec.key].orEmpty().sortedBy { it.timestamp }
            if (history.isEmpty()) continue
            cards += buildCard(spec, history)
        }

        if (bpVitals.isNotEmpty()) {
            val bpSpec = BiomarkerSpec(
                key = "systolic_bp",
                name = "Systolic Blood Pressure",
                unit = "mmHg",
                normalMin = 90.0,
                normalMax = 120.0,
                normalRangeLabel = "90 - 120 mmHg",
                plausibleMin = 70.0,
                plausibleMax = 240.0,
                regex = Regex("$^")
            )
            val bpHistory = bpVitals.map {
                BiomarkerReading(round1(it.primaryValue), it.timestamp, "Vitals Log")
            }
            cards += buildCard(bpSpec, bpHistory)
        }

        // Prioritize High Alert and Borderline biomarkers first
        return cards.sortedBy {
            when (it.alertLevel) {
                BiomarkerAlertLevel.HIGH_ALERT -> 0
                BiomarkerAlertLevel.BORDERLINE -> 1
                BiomarkerAlertLevel.NORMAL -> 2
            }
        }
    }

    private fun buildCard(spec: BiomarkerSpec, history: List<BiomarkerReading>): BiomarkerTrendCard {
        val latest = history.last()
        val previous = if (history.size >= 2) history[history.size - 2] else null
        val delta = previous?.let { round1(latest.value - it.value) }

        val alertLevel = classifyAlert(latest.value, spec.normalMin, spec.normalMax)
        val trend = classifyTrend(latest.value, previous?.value, spec.normalMin, spec.normalMax)

        val alertMsg = when {
            latest.value < spec.normalMin ->
                "Below normal (${spec.normalRangeLabel}) — review with physician."
            latest.value > spec.normalMax ->
                "Above normal (${spec.normalRangeLabel}) — follow-up recommended."
            else ->
                "Within healthy reference range (${spec.normalRangeLabel})."
        }

        return BiomarkerTrendCard(
            key = spec.key,
            name = spec.name,
            unit = spec.unit,
            normalRangeLabel = spec.normalRangeLabel,
            normalMin = spec.normalMin,
            normalMax = spec.normalMax,
            latestValue = latest.value,
            previousValue = previous?.value,
            delta = delta,
            alertLevel = alertLevel,
            trendDirection = trend,
            alertMessage = alertMsg,
            history = history
        )
    }

    private fun classifyAlert(value: Double, min: Double, max: Double): BiomarkerAlertLevel {
        if (value in min..max) return BiomarkerAlertLevel.NORMAL
        val span = (max - min).coerceAtLeast(1.0)
        val dist = if (value < min) (min - value) else (value - max)
        return if (dist <= span * 0.20) BiomarkerAlertLevel.BORDERLINE else BiomarkerAlertLevel.HIGH_ALERT
    }

    private fun classifyTrend(
        latest: Double,
        previous: Double?,
        min: Double,
        max: Double
    ): BiomarkerTrendDirection {
        if (previous == null) return BiomarkerTrendDirection.SINGLE_READING
        val diff = abs(latest - previous)
        val threshold = ((max - min) * 0.03).coerceAtLeast(0.1)
        if (diff < threshold) return BiomarkerTrendDirection.STABLE

        val prevDist = distanceToRange(previous, min, max)
        val latestDist = distanceToRange(latest, min, max)
        return when {
            latestDist < prevDist -> BiomarkerTrendDirection.IMPROVED
            latestDist > prevDist -> BiomarkerTrendDirection.WORSENED
            else -> BiomarkerTrendDirection.STABLE
        }
    }

    private fun distanceToRange(v: Double, min: Double, max: Double): Double = when {
        v < min -> min - v
        v > max -> v - max
        else -> 0.0
    }

    private fun round1(v: Double): Double = (v * 10.0).roundToInt() / 10.0
}
