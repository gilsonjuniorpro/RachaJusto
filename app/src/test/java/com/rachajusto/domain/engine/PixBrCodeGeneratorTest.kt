package com.rachajusto.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PixBrCodeGeneratorTest {

    @Test
    fun crc16Ccitt_matchesStandardCheckVector() {
        // Standard CRC-16/CCITT-FALSE check value for ASCII "123456789" is 0x29B1
        assertEquals("29B1", PixBrCodeGenerator.computeCrc16Ccitt("123456789"))
    }

    @Test
    fun generatePayload_containsValidEmvStructureAndChecksum() {
        val payload = PixBrCodeGenerator.generatePayload(
            pixKey = "lucas.silva@email.com",
            merchantName = "Lucas João Conceição de Oliveira Neto",
            merchantCity = "São Paulo - SP",
            amountCents = 12345L
        )

        assertTrue(payload.startsWith("000201"))
        assertTrue(payload.contains("0014br.gov.bcb.pix"))
        assertTrue(payload.contains("0121lucas.silva@email.com"))
        assertTrue(payload.contains("52040000"))
        assertTrue(payload.contains("5303986"))
        assertTrue(payload.contains("5406123.45"))
        assertTrue(payload.contains("5802BR"))
        // Name stripped of accents, uppercase, max 25 chars
        assertTrue(payload.contains("5925LUCAS JOAO CONCEICAO DE O"))
        // City stripped of accents, uppercase, max 15 chars
        assertTrue(payload.contains("6012SAO PAULO SP"))
        assertTrue(payload.contains("62070503***"))
        assertTrue(PixBrCodeGenerator.isValidCrc(payload))

        // Corrupted payload must fail CRC verification
        val tampered = payload.dropLast(1) + if (payload.last() == '0') '1' else '0'
        assertFalse(PixBrCodeGenerator.isValidCrc(tampered))
    }

    @Test
    fun generatePayload_usesDemoFallbackKeyWhenMissing() {
        val payload = PixBrCodeGenerator.generatePayload(
            pixKey = null,
            merchantName = "Ana",
            amountCents = 5000L
        )

        assertTrue(payload.contains(PixBrCodeGenerator.DEFAULT_DEMO_PIX_KEY))
        assertTrue(payload.contains("540550.00"))
        assertTrue(PixBrCodeGenerator.isValidCrc(payload))
    }

    @Test
    fun sanitizeField_normalizesPortugueseAccentsAndSpecialChars() {
        assertEquals("JOAO ACAO", PixBrCodeGenerator.sanitizeField("João Ação!@#", 25, "FALLBACK"))
        assertEquals("FALLBACK", PixBrCodeGenerator.sanitizeField("!!!", 15, "FALLBACK"))
    }
}
