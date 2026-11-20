package com.rachajusto.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun formatBrl_formatsPositiveZeroAndNegativeValuesCorrectly() {
        assertEquals("R$ 0,00", CurrencyFormatter.formatBrl(0L))
        assertEquals("R$ 100,00", CurrencyFormatter.formatBrl(10000L))
        assertEquals("R$ 1.234,56", CurrencyFormatter.formatBrl(123456L))
        assertEquals("R$ 0,05", CurrencyFormatter.formatBrl(5L))
        assertEquals("-R$ 86,50", CurrencyFormatter.formatBrl(-8650L))
        assertEquals("+R$ 42,10", CurrencyFormatter.formatBrl(4210L, showPlusSign = true))
        assertEquals("R$ 0,00", CurrencyFormatter.formatBrl(0L, showPlusSign = true))
    }

    @Test
    fun parseDigitsToCents_stripsNonDigitsAndHandlesEmptyInput() {
        assertEquals(0L, CurrencyFormatter.parseDigitsToCents(""))
        assertEquals(0L, CurrencyFormatter.parseDigitsToCents("abc"))
        assertEquals(12345L, CurrencyFormatter.parseDigitsToCents("R$ 123,45"))
        assertEquals(8650L, CurrencyFormatter.parseDigitsToCents("86,50"))
    }

    @Test
    fun formatBrlInput_formatsEditableMonetaryText() {
        assertEquals("0,00", CurrencyFormatter.formatBrlInput(0L))
        assertEquals("2.400,00", CurrencyFormatter.formatBrlInput(240000L))
        assertEquals("0,00", CurrencyFormatter.formatBrlInput(-100L))
    }
}
