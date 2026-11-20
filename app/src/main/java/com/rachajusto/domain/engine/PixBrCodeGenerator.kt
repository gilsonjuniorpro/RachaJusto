package com.rachajusto.domain.engine

import java.text.Normalizer
import java.util.Locale

/**
 * Generates static EMVCo BR Code payloads for Brazilian PIX instant payments
 * with CRC16-CCITT (polynomial 0x1021, initial value 0xFFFF).
 */
object PixBrCodeGenerator {

    const val DEFAULT_DEMO_PIX_KEY = "pix.demo@rachajusto.app"
    private const val PIX_GUI = "br.gov.bcb.pix"
    private const val PAYLOAD_FORMAT_INDICATOR = "01"
    private const val MERCHANT_CATEGORY_CODE = "0000"
    private const val CURRENCY_BRL_ISO_4217 = "986"
    private const val COUNTRY_CODE_BR = "BR"
    private const val DEFAULT_TXID = "***"

    fun generatePayload(
        pixKey: String?,
        merchantName: String,
        merchantCity: String = "SAO PAULO",
        amountCents: Long,
        txId: String = DEFAULT_TXID
    ): String {
        require(amountCents >= 0L) { "amountCents must be non-negative" }

        val effectiveKey = pixKey?.trim().orEmpty().ifEmpty { DEFAULT_DEMO_PIX_KEY }
        val normalizedName = sanitizeField(merchantName, maxLength = 25, fallback = "RECEBEDOR")
        val normalizedCity = sanitizeField(merchantCity, maxLength = 15, fallback = "SAO PAULO")
        val normalizedTxId = sanitizeTxId(txId)

        val merchantAccountInfo = buildString {
            append(formatTlv("00", PIX_GUI))
            append(formatTlv("01", effectiveKey))
        }

        val additionalDataField = formatTlv("05", normalizedTxId)

        val payloadWithoutCrc = buildString {
            append(formatTlv("00", PAYLOAD_FORMAT_INDICATOR))
            append(formatTlv("26", merchantAccountInfo))
            append(formatTlv("52", MERCHANT_CATEGORY_CODE))
            append(formatTlv("53", CURRENCY_BRL_ISO_4217))
            if (amountCents > 0L) {
                append(formatTlv("54", formatAmount(amountCents)))
            }
            append(formatTlv("58", COUNTRY_CODE_BR))
            append(formatTlv("59", normalizedName))
            append(formatTlv("60", normalizedCity))
            append(formatTlv("62", additionalDataField))
            append("6304")
        }

        val crc = computeCrc16Ccitt(payloadWithoutCrc)
        return payloadWithoutCrc + crc
    }

    fun computeCrc16Ccitt(payload: String): String {
        var crc = 0xFFFF
        val polynomial = 0x1021

        for (byte in payload.toByteArray(Charsets.UTF_8)) {
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) {
                    ((crc shl 1) xor polynomial) and 0xFFFF
                } else {
                    (crc shl 1) and 0xFFFF
                }
            }
        }
        return String.format(Locale.US, "%04X", crc and 0xFFFF)
    }

    fun isValidCrc(brCode: String): Boolean {
        if (brCode.length < 8) return false
        val body = brCode.dropLast(4)
        val expectedCrc = brCode.takeLast(4).uppercase(Locale.US)
        if (!body.endsWith("6304")) return false
        return computeCrc16Ccitt(body) == expectedCrc
    }

    internal fun sanitizeField(value: String, maxLength: Int, fallback: String): String {
        val decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        val withoutDiacritics = decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        val asciiClean = withoutDiacritics
            .uppercase(Locale.US)
            .replace(Regex("[^A-Z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(maxLength)
            .trim()
        return asciiClean.ifEmpty { fallback }
    }

    private fun sanitizeTxId(txId: String): String {
        val trimmed = txId.trim()
        if (trimmed == DEFAULT_TXID || trimmed.isEmpty()) return DEFAULT_TXID
        val clean = trimmed.replace(Regex("[^A-Za-z0-9]"), "").take(25)
        return clean.ifEmpty { DEFAULT_TXID }
    }

    private fun formatAmount(amountCents: Long): String {
        val reais = amountCents / 100L
        val cents = amountCents % 100L
        return String.format(Locale.US, "%d.%02d", reais, cents)
    }

    private fun formatTlv(id: String, value: String): String {
        val byteLength = value.toByteArray(Charsets.UTF_8).size
        require(byteLength in 0..99) { "TLV field $id exceeds 99 bytes ($byteLength)" }
        val lengthFormatted = String.format(Locale.US, "%02d", byteLength)
        return "$id$lengthFormatted$value"
    }
}
