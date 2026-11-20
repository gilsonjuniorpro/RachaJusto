package com.rachajusto.domain.engine

import java.text.Normalizer
import java.util.Locale

/**
 * Generates EMVCo PIX-compliant alphanumeric Transaction IDs (1..25 ASCII chars)
 * so recipients can identify which group transfer settled each payment.
 */
object PixTxIdGenerator {

    private const val PREFIX = "RJ"
    private const val MAX_LENGTH = 25

    fun generate(
        groupName: String,
        fromName: String,
        amountCents: Long
    ): String {
        val groupSlug = normalizeAlphanumeric(groupName).take(8).ifEmpty { "GRP" }
        val payerSlug = normalizeAlphanumeric(fromName).take(6).ifEmpty { "USER" }
        val amountPart = amountCents.coerceAtLeast(0L).toString().takeLast(9)
        return "$PREFIX$groupSlug$payerSlug$amountPart".take(MAX_LENGTH)
    }

    private fun normalizeAlphanumeric(input: String): String {
        val decomposed = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .uppercase(Locale.US)
            .replace(Regex("[^A-Z0-9]"), "")
    }
}
