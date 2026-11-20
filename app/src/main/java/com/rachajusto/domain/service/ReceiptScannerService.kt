package com.rachajusto.domain.service

import com.rachajusto.domain.model.DemoReceiptPreset
import com.rachajusto.domain.model.ScannedReceiptResult

interface ReceiptScannerService {
    suspend fun scanPreset(preset: DemoReceiptPreset): ScannedReceiptResult
}
