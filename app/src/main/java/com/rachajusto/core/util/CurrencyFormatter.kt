package com.rachajusto.core.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {

    private const val MAX_ALLOWED_CENTS = 999_999_999_99L // R$ 999.999.999,99

    private val ptBrLocale: Locale = Locale.forLanguageTag("pt-BR")

    private val brSymbols = DecimalFormatSymbols(ptBrLocale).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    fun formatBrl(amountCents: Long, showPlusSign: Boolean = false): String {
        val isNegative = amountCents < 0L
        val absCents = abs(amountCents)
        val reais = absCents / 100L
        val cents = absCents % 100L

        val integerFormatter = DecimalFormat("#,##0", brSymbols)
        val formattedInteger = integerFormatter.format(reais)
        val formattedCents = cents.toString().padStart(2, '0')

        val prefix = when {
            isNegative -> "-R$ "
            showPlusSign && amountCents > 0L -> "+R$ "
            else -> "R$ "
        }
        return "$prefix$formattedInteger,$formattedCents"
    }

    fun formatBrlInput(amountCents: Long): String {
        val safeCents = amountCents.coerceAtLeast(0L)
        val reais = safeCents / 100L
        val cents = safeCents % 100L
        val integerFormatter = DecimalFormat("#,##0", brSymbols)
        return "${integerFormatter.format(reais)},${cents.toString().padStart(2, '0')}"
    }

    fun parseDigitsToCents(rawInput: String): Long {
        val digitsOnly = rawInput.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return 0L
        return digitsOnly.take(11).toLongOrNull()?.coerceAtMost(MAX_ALLOWED_CENTS) ?: 0L
    }

    fun formatDate(date: LocalDate, locale: Locale = ptBrLocale): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        return date.format(formatter)
    }

    fun formatShortDate(date: LocalDate, locale: Locale = ptBrLocale): String {
        val pattern = when (locale.language) {
            "en" -> "MMM d"
            else -> "dd MMM"
        }
        return date.format(DateTimeFormatter.ofPattern(pattern, locale))
    }
}
