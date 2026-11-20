package com.rachajusto.feature.groupdetail

import com.rachajusto.data.seed.SeedDataFactory
import com.rachajusto.domain.engine.CashFlowMinimizer
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ShareSummaryFormatterTest {

    @Test
    fun formatWhatsAppSummary_includesGroupTotalTransfersAndPixKeys() {
        val group = SeedDataFactory.createSeedGroups(LocalDate.of(2026, 11, 20)).first()
        val summary = CashFlowMinimizer.calculateSummary(group)

        val text = ShareSummaryFormatter.formatWhatsAppSummary(
            group = group,
            summary = summary,
            labels = ShareSummaryLabels(
                totalSpentLabel = "Total gasto",
                pendingTransfersHeader = "Transferências Otimizadas",
                transferLineFormat = "• %1\$s paga *%2\$s* para %3\$s",
                pixKeyPrefix = "Chave PIX",
                pixKeyNotRegistered = "Não cadastrada",
                allSettledMessage = "Tudo quitado!",
                footerLabel = "Gerado pelo RachaJusto"
            )
        )

        assertTrue(text.contains("Fim de Semana em Campos do Jordão"))
        assertTrue(text.contains("Total gasto:"))
        assertTrue(text.contains("Chave PIX:"))
        assertTrue(text.contains("Gerado pelo RachaJusto"))
    }
}
