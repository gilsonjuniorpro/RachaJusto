package com.rachajusto.data.service

import com.rachajusto.domain.model.DemoReceiptPreset
import com.rachajusto.domain.model.ExpenseCategory
import com.rachajusto.domain.model.ScannedReceiptItem
import com.rachajusto.domain.model.ScannedReceiptResult
import com.rachajusto.domain.service.ReceiptScannerService
import kotlinx.coroutines.delay

class MockReceiptScannerService(
    private val scanDelayMs: Long = 1200L
) : ReceiptScannerService {

    override suspend fun scanPreset(preset: DemoReceiptPreset): ScannedReceiptResult {
        if (scanDelayMs > 0L) {
            delay(scanDelayMs)
        }
        return when (preset) {
            DemoReceiptPreset.BAR_COMANDA -> ScannedReceiptResult(
                title = "Comanda Bar da Esquina",
                category = ExpenseCategory.FOOD,
                serviceFeePercent = 10,
                items = listOf(
                    ScannedReceiptItem("Picanha na Chapa", 18000L),
                    ScannedReceiptItem("Balde Cerveja Artesanal (6x)", 12000L),
                    ScannedReceiptItem("Porção Batata com Cheddar", 4800L),
                    ScannedReceiptItem("Sucos e Refrigerantes", 3600L)
                )
            )

            DemoReceiptPreset.CHURRASCO_MERCADO -> ScannedReceiptResult(
                title = "Churrasco & Bebidas Empório",
                category = ExpenseCategory.MARKET,
                serviceFeePercent = 0,
                items = listOf(
                    ScannedReceiptItem("Picanha + Fraldinha", 24500L),
                    ScannedReceiptItem("Linguiça + Pão de Alho + Carvão", 8200L),
                    ScannedReceiptItem("Caixa Cerveja Puro Malte", 11800L),
                    ScannedReceiptItem("Refrigerantes e Água com Gás", 4400L)
                )
            )

            DemoReceiptPreset.RESTAURANTE_FONDUE -> ScannedReceiptResult(
                title = "Noite do Fondue na Serra",
                category = ExpenseCategory.FOOD,
                serviceFeePercent = 10,
                items = listOf(
                    ScannedReceiptItem("Sequência Fondue Queijo", 19000L),
                    ScannedReceiptItem("Sequência Fondue Carne", 21500L),
                    ScannedReceiptItem("Fondue Chocolate com Frutas", 11500L),
                    ScannedReceiptItem("Garrafa Vinho Cabernet", 14500L)
                )
            )
        }
    }
}
