package com.rachajusto.data.service

import com.rachajusto.domain.model.DemoReceiptPreset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class MockReceiptScannerServiceTest {

    @Test
    fun scanPreset_returnsRealisticItemsForEveryPreset() = runTest {
        val service = MockReceiptScannerService(scanDelayMs = 0L)

        for (preset in DemoReceiptPreset.entries) {
            val result = service.scanPreset(preset)
            assertTrue(result.title.isNotBlank())
            assertTrue(result.items.isNotEmpty())
            assertTrue(result.items.all { it.amountCents > 0L })
        }
    }
}
