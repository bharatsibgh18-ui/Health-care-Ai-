package com.example.utils

object HealthCardValidator {
    const val PMJAY_LENGTH = 8
    const val ABHA_LENGTH = 14

    /**
     * Sanitizes PMJAY card input to uppercase ASCII letters and digits only, max 8 characters.
     */
    fun sanitizePmjayInput(raw: String): String {
        return raw.uppercase()
            .filter { it in 'A'..'Z' || it in '0'..'9' }
            .take(PMJAY_LENGTH)
    }

    /**
     * Sanitizes ABHA card input to ASCII digits only, max 14 digits.
     */
    fun sanitizeAbhaInput(raw: String): String {
        return raw.filter { it in '0'..'9' }
            .take(ABHA_LENGTH)
    }

    /**
     * Valid for saving: empty (optional) OR exactly 8 uppercase alphanumeric characters.
     */
    fun isValidPmjayForSave(value: String): Boolean {
        val clean = value.trim().uppercase()
        if (clean.isEmpty()) return true
        return clean.length == PMJAY_LENGTH && clean.all { it in 'A'..'Z' || it in '0'..'9' }
    }

    /**
     * Strictly complete 8-character alphanumeric PMJAY card number.
     */
    fun isCompletePmjay(value: String): Boolean {
        val clean = value.trim().uppercase()
        return clean.length == PMJAY_LENGTH && clean.all { it in 'A'..'Z' || it in '0'..'9' }
    }

    /**
     * Valid for saving: empty (optional) OR exactly 14 numeric digits.
     */
    fun isValidAbhaForSave(value: String): Boolean {
        val clean = value.trim()
        if (clean.isEmpty()) return true
        return clean.length == ABHA_LENGTH && clean.all { it in '0'..'9' }
    }

    /**
     * Strictly complete 14-digit numeric ABHA card number.
     */
    fun isCompleteAbha(value: String): Boolean {
        val clean = value.trim()
        return clean.length == ABHA_LENGTH && clean.all { it in '0'..'9' }
    }

    /**
     * Formats a 14-digit ABHA number into standard XX-XXXX-XXXX-XXXX display grouping.
     */
    fun formatAbhaForDisplay(raw: String): String {
        val digits = sanitizeAbhaInput(raw)
        if (digits.length <= 2) return digits
        val sb = StringBuilder()
        sb.append(digits.substring(0, 2))
        var idx = 2
        while (idx < digits.length) {
            sb.append('-')
            val end = (idx + 4).coerceAtMost(digits.length)
            sb.append(digits.substring(idx, end))
            idx = end
        }
        return sb.toString()
    }
}
