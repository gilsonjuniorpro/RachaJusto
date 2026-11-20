package com.rachajusto.domain.engine

import org.junit.Assert.assertTrue
import org.junit.Test

class PixTxIdGeneratorTest {

    @Test
    fun generate_producesValidAlphanumericTxIdUpTo25Chars() {
        val txId = PixTxIdGenerator.generate(
            groupName = "Fim de Semana em Campos do Jordão",
            fromName = "João Pedro",
            amountCents = 42550L
        )

        assertTrue(txId.startsWith("RJ"))
        assertTrue(txId.length in 1..25)
        assertTrue(txId.matches(Regex("^[A-Z0-9]+$")))

        val payload = PixBrCodeGenerator.generatePayload(
            pixKey = "lucas@pixdemo.br",
            merchantName = "Lucas",
            amountCents = 42550L,
            txId = txId
        )
        assertTrue(payload.contains(txId))
        assertTrue(PixBrCodeGenerator.isValidCrc(payload))
    }
}
