package com.example.utils

import com.example.data.local.UserProfileEntity

data class QrMatrix(
    val size: Int,
    val modules: BooleanArray // row-major: modules[y * size + x] == true means dark module
) {
    fun isDark(x: Int, y: Int): Boolean {
        if (x !in 0 until size || y !in 0 until size) return false
        return modules[y * size + x]
    }
}

/**
 * Pure-Kotlin ISO/IEC 18004 QR Code Encoder (Byte Mode, Error Correction Level L, Versions 1..10)
 * plus Emergency Medical ID payload formatter including PMJAY & ABHA health card numbers.
 * Zero external binary dependencies and works 100% offline during medical emergencies.
 */
object EmergencyQrCodeGenerator {

    fun buildEmergencyPayload(profile: UserProfileEntity?): String {
        val name = profile?.fullName?.ifBlank { "Health Patient" } ?: "Health Patient"
        val age = profile?.age ?: 30
        val blood = profile?.bloodGroup?.ifBlank { "Not Set" } ?: "Not Set"
        val allergies = profile?.allergies?.ifBlank { "None" } ?: "None"
        val conditions = profile?.chronicConditions?.ifBlank { "None" } ?: "None"
        val meds = profile?.currentMedications?.ifBlank { "None" } ?: "None"
        val emergencyName = profile?.emergencyContactName?.ifBlank { "ICE" } ?: "ICE"
        val emergencyPhone = profile?.emergencyContactPhone?.ifBlank { "112" } ?: "112"
        val pmjay = profile?.pmjayCardNumber?.trim().orEmpty()
        val abha = HealthCardValidator.formatAbhaForDisplay(profile?.abhaCardNumber.orEmpty())

        return buildString {
            appendLine("EMERGENCY MEDICAL ID")
            appendLine("Name: $name ($age yrs)")
            appendLine("Blood: $blood")
            appendLine("Allergies: ${allergies.take(45)}")
            appendLine("Conditions: ${conditions.take(45)}")
            appendLine("Meds: ${meds.take(45)}")
            appendLine("ICE: $emergencyName $emergencyPhone")
            if (pmjay.isNotBlank()) appendLine("PMJAY: $pmjay")
            if (abha.isNotBlank()) appendLine("ABHA: $abha")
        }.trim()
    }

    // Version specs for Error Correction Level L (Versions 1..10):
    // (version, totalCodewords, ecCodewordsPerBlock, numBlocksGroup1, dataCodewordsGroup1, numBlocksGroup2, dataCodewordsGroup2)
    private data class VersionSpec(
        val version: Int,
        val totalCodewords: Int,
        val ecPerBlock: Int,
        val blocksG1: Int,
        val dataG1: Int,
        val blocksG2: Int = 0,
        val dataG2: Int = 0,
        val alignmentCenters: IntArray = intArrayOf()
    ) {
        val totalDataCodewords: Int get() = blocksG1 * dataG1 + blocksG2 * dataG2
        val maxByteCapacity: Int get() = totalDataCodewords - if (version <= 9) 2 else 3
    }

    private val versionSpecs = listOf(
        VersionSpec(1, 26, 7, 1, 19, 0, 0, intArrayOf()),
        VersionSpec(2, 44, 10, 1, 34, 0, 0, intArrayOf(6, 18)),
        VersionSpec(3, 70, 15, 1, 55, 0, 0, intArrayOf(6, 22)),
        VersionSpec(4, 100, 20, 1, 80, 0, 0, intArrayOf(6, 26)),
        VersionSpec(5, 134, 26, 1, 108, 0, 0, intArrayOf(6, 30)),
        VersionSpec(6, 172, 18, 2, 68, 0, 0, intArrayOf(6, 34)),
        VersionSpec(7, 196, 20, 2, 78, 0, 0, intArrayOf(6, 22, 38)),
        VersionSpec(8, 242, 24, 2, 97, 0, 0, intArrayOf(6, 24, 42)),
        VersionSpec(9, 292, 30, 2, 116, 0, 0, intArrayOf(6, 26, 46)),
        VersionSpec(10, 346, 18, 2, 68, 2, 69, intArrayOf(6, 28, 50))
    )

    // Precomputed 15-bit BCH format strings for EC Level L (01) and masks 0..7
    private val formatBitsLevelL = intArrayOf(
        0x77C4, 0x72F3, 0x7DAA, 0x789D,
        0x662F, 0x6318, 0x6C41, 0x6976
    )

    // Version info strings for versions 7..10 (18-bit BCH)
    private val versionInfoBits = mapOf(
        7 to 0x07C94,
        8 to 0x085BC,
        9 to 0x09A99,
        10 to 0x0A4D3
    )

    fun encode(text: String): QrMatrix {
        val rawBytes = text.toByteArray(Charsets.UTF_8)
        val spec = versionSpecs.firstOrNull { rawBytes.size <= it.maxByteCapacity }
            ?: versionSpecs.last()
        val dataBytes = if (rawBytes.size > spec.maxByteCapacity) {
            rawBytes.copyOf(spec.maxByteCapacity)
        } else {
            rawBytes
        }

        // 1. Build bit stream (Mode 0100 = Byte, Char count, Data, Terminator, Pad)
        val bits = ArrayList<Boolean>(spec.totalDataCodewords * 8)
        appendBits(bits, 0b0100, 4)
        val countBits = if (spec.version <= 9) 8 else 16
        appendBits(bits, dataBytes.size, countBits)
        for (b in dataBytes) {
            appendBits(bits, b.toInt() and 0xFF, 8)
        }
        val capacityBits = spec.totalDataCodewords * 8
        val terminatorLen = minOf(4, capacityBits - bits.size)
        appendBits(bits, 0, terminatorLen)
        while (bits.size % 8 != 0) bits.add(false)
        var padToggle = true
        while (bits.size < capacityBits) {
            appendBits(bits, if (padToggle) 0xEC else 0x11, 8)
            padToggle = !padToggle
        }

        // Pack into data codewords
        val dataCodewords = IntArray(spec.totalDataCodewords) { i ->
            var v = 0
            for (b in 0 until 8) {
                v = (v shl 1) or if (bits[i * 8 + b]) 1 else 0
            }
            v
        }

        // 2. Split into blocks and compute Reed-Solomon EC codewords
        val dataBlocks = mutableListOf<IntArray>()
        val ecBlocks = mutableListOf<IntArray>()
        var offset = 0
        repeat(spec.blocksG1) {
            val block = dataCodewords.copyOfRange(offset, offset + spec.dataG1)
            dataBlocks += block
            ecBlocks += reedSolomonEc(block, spec.ecPerBlock)
            offset += spec.dataG1
        }
        repeat(spec.blocksG2) {
            val block = dataCodewords.copyOfRange(offset, offset + spec.dataG2)
            dataBlocks += block
            ecBlocks += reedSolomonEc(block, spec.ecPerBlock)
            offset += spec.dataG2
        }

        // 3. Interleave data and EC codewords
        val finalCodewords = ArrayList<Int>(spec.totalCodewords)
        val maxDataLen = maxOf(spec.dataG1, spec.dataG2)
        for (i in 0 until maxDataLen) {
            for (block in dataBlocks) {
                if (i < block.size) finalCodewords += block[i]
            }
        }
        for (i in 0 until spec.ecPerBlock) {
            for (ec in ecBlocks) {
                if (i < ec.size) finalCodewords += ec[i]
            }
        }

        // 4. Construct function-pattern matrix
        val size = spec.version * 4 + 17
        val baseModules = BooleanArray(size * size)
        val isFunction = BooleanArray(size * size)

        drawFinderPattern(baseModules, isFunction, size, 0, 0)
        drawFinderPattern(baseModules, isFunction, size, size - 7, 0)
        drawFinderPattern(baseModules, isFunction, size, 0, size - 7)

        // Timing patterns
        for (i in 8 until size - 8) {
            setFunc(baseModules, isFunction, size, i, 6, i % 2 == 0)
            setFunc(baseModules, isFunction, size, 6, i, i % 2 == 0)
        }

        // Alignment patterns
        val centers = spec.alignmentCenters
        for (cy in centers) {
            for (cx in centers) {
                val overlapsFinder =
                    (cx <= 8 && cy <= 8) ||
                        (cx >= size - 9 && cy <= 8) ||
                        (cx <= 8 && cy >= size - 9)
                if (!overlapsFinder) {
                    drawAlignmentPattern(baseModules, isFunction, size, cx, cy)
                }
            }
        }

        // Dark module & reserve format info areas
        setFunc(baseModules, isFunction, size, 8, size - 8, true)
        reserveFormatAreas(isFunction, size)
        if (spec.version >= 7) {
            drawVersionInfo(baseModules, isFunction, size, versionInfoBits[spec.version] ?: 0)
        }

        // 5. Place interleaved codewords in upward/downward 2-column zig-zag
        val codewordBits = BooleanArray(finalCodewords.size * 8)
        for (i in finalCodewords.indices) {
            val cw = finalCodewords[i]
            for (b in 0 until 8) {
                codewordBits[i * 8 + b] = ((cw ushr (7 - b)) and 1) != 0
            }
        }
        var bitIdx = 0
        var upward = true
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            for (vert in 0 until size) {
                val y = if (upward) size - 1 - vert else vert
                for (j in 0..1) {
                    val x = right - j
                    if (!isFunction[y * size + x]) {
                        baseModules[y * size + x] =
                            if (bitIdx < codewordBits.size) codewordBits[bitIdx] else false
                        bitIdx++
                    }
                }
            }
            upward = !upward
            right -= 2
        }

        // 6. Pick optimal mask (0..7) minimizing QR penalty score
        var bestMask = 0
        var bestPenalty = Int.MAX_VALUE
        var bestModules = baseModules
        for (mask in 0..7) {
            val candidate = baseModules.copyOf()
            applyMask(candidate, isFunction, size, mask)
            drawFormatInfo(candidate, size, formatBitsLevelL[mask])
            val penalty = computePenalty(candidate, size)
            if (penalty < bestPenalty) {
                bestPenalty = penalty
                bestMask = mask
                bestModules = candidate
            }
        }
        drawFormatInfo(bestModules, size, formatBitsLevelL[bestMask])

        return QrMatrix(size = size, modules = bestModules)
    }

    private fun appendBits(out: MutableList<Boolean>, value: Int, bitCount: Int) {
        for (i in bitCount - 1 downTo 0) {
            out.add(((value ushr i) and 1) != 0)
        }
    }

    private fun setFunc(
        modules: BooleanArray,
        isFunc: BooleanArray,
        size: Int,
        x: Int,
        y: Int,
        dark: Boolean
    ) {
        if (x in 0 until size && y in 0 until size) {
            modules[y * size + x] = dark
            isFunc[y * size + x] = true
        }
    }

    private fun drawFinderPattern(
        modules: BooleanArray,
        isFunc: BooleanArray,
        size: Int,
        left: Int,
        top: Int
    ) {
        for (dy in -1..7) {
            for (dx in -1..7) {
                val x = left + dx
                val y = top + dy
                if (x !in 0 until size || y !in 0 until size) continue
                val dark = dx in 0..6 && dy in 0..6 && (
                    dx == 0 || dx == 6 || dy == 0 || dy == 6 || (dx in 2..4 && dy in 2..4)
                    )
                setFunc(modules, isFunc, size, x, y, dark)
            }
        }
    }

    private fun drawAlignmentPattern(
        modules: BooleanArray,
        isFunc: BooleanArray,
        size: Int,
        cx: Int,
        cy: Int
    ) {
        for (dy in -2..2) {
            for (dx in -2..2) {
                val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                setFunc(modules, isFunc, size, cx + dx, cy + dy, dist != 1)
            }
        }
    }

    private fun reserveFormatAreas(isFunc: BooleanArray, size: Int) {
        for (i in 0..8) {
            if (i != 6) {
                isFunc[8 * size + i] = true
                isFunc[i * size + 8] = true
            }
        }
        for (i in 0..7) {
            isFunc[8 * size + (size - 1 - i)] = true
        }
        for (i in 0..6) {
            isFunc[(size - 1 - i) * size + 8] = true
        }
    }

    private fun drawFormatInfo(modules: BooleanArray, size: Int, bits: Int) {
        for (i in 0..5) modules[8 * size + i] = ((bits ushr (14 - i)) and 1) != 0
        modules[8 * size + 7] = ((bits ushr 8) and 1) != 0
        modules[8 * size + 8] = ((bits ushr 7) and 1) != 0
        modules[7 * size + 8] = ((bits ushr 6) and 1) != 0
        for (i in 0..5) modules[(5 - i) * size + 8] = ((bits ushr (5 - i)) and 1) != 0

        for (i in 0..6) modules[(size - 1 - i) * size + 8] = ((bits ushr (14 - i)) and 1) != 0
        for (i in 0..7) modules[8 * size + (size - 8 + i)] = ((bits ushr (7 - i)) and 1) != 0
    }

    private fun drawVersionInfo(modules: BooleanArray, isFunc: BooleanArray, size: Int, bits: Int) {
        for (i in 0 until 18) {
            val dark = ((bits ushr i) and 1) != 0
            val a = size - 11 + (i % 3)
            val b = i / 3
            setFunc(modules, isFunc, size, a, b, dark)
            setFunc(modules, isFunc, size, b, a, dark)
        }
    }

    private fun applyMask(modules: BooleanArray, isFunc: BooleanArray, size: Int, mask: Int) {
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (isFunc[y * size + x]) continue
                val invert = when (mask) {
                    0 -> (x + y) % 2 == 0
                    1 -> y % 2 == 0
                    2 -> x % 3 == 0
                    3 -> (x + y) % 3 == 0
                    4 -> ((y / 2) + (x / 3)) % 2 == 0
                    5 -> ((x * y) % 2 + (x * y) % 3) == 0
                    6 -> (((x * y) % 2 + (x * y) % 3) % 2) == 0
                    else -> (((x + y) % 2 + (x * y) % 3) % 2) == 0
                }
                if (invert) modules[y * size + x] = !modules[y * size + x]
            }
        }
    }

    private fun computePenalty(modules: BooleanArray, size: Int): Int {
        var penalty = 0
        // Horizontal and vertical runs
        for (y in 0 until size) {
            var runColor = modules[y * size]
            var runLen = 1
            for (x in 1 until size) {
                if (modules[y * size + x] == runColor) {
                    runLen++
                } else {
                    if (runLen >= 5) penalty += 3 + (runLen - 5)
                    runColor = modules[y * size + x]
                    runLen = 1
                }
            }
            if (runLen >= 5) penalty += 3 + (runLen - 5)
        }
        for (x in 0 until size) {
            var runColor = modules[x]
            var runLen = 1
            for (y in 1 until size) {
                if (modules[y * size + x] == runColor) {
                    runLen++
                } else {
                    if (runLen >= 5) penalty += 3 + (runLen - 5)
                    runColor = modules[y * size + x]
                    runLen = 1
                }
            }
            if (runLen >= 5) penalty += 3 + (runLen - 5)
        }
        // 2x2 blocks
        for (y in 0 until size - 1) {
            for (x in 0 until size - 1) {
                val c = modules[y * size + x]
                if (c == modules[y * size + x + 1] &&
                    c == modules[(y + 1) * size + x] &&
                    c == modules[(y + 1) * size + x + 1]
                ) {
                    penalty += 3
                }
            }
        }
        return penalty
    }

    private fun reedSolomonEc(data: IntArray, degree: Int): IntArray {
        val gen = rsGeneratorPoly(degree)
        val result = IntArray(degree)
        for (b in data) {
            val factor = b xor result[0]
            System.arraycopy(result, 1, result, 0, degree - 1)
            result[degree - 1] = 0
            for (i in 0 until degree) {
                result[i] = result[i] xor gfMul(gen[i], factor)
            }
        }
        return result
    }

    private fun rsGeneratorPoly(degree: Int): IntArray {
        var poly = IntArray(degree)
        poly[degree - 1] = 1
        var root = 1
        for (i in 0 until degree) {
            for (j in 0 until degree) {
                poly[j] = gfMul(poly[j], root)
                if (j + 1 < degree) {
                    poly[j] = poly[j] xor poly[j + 1]
                }
            }
            root = gfMul(root, 2)
        }
        return poly
    }

    private fun gfMul(x: Int, y: Int): Int {
        var z = 0
        for (i in 7 downTo 0) {
            z = (z shl 1) xor ((z ushr 7) * 0x11D)
            z = z xor (((y ushr i) and 1) * x)
        }
        return z and 0xFF
    }
}
