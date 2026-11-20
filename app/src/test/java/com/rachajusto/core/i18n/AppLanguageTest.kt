package com.rachajusto.core.i18n

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun fromTag_resolvesAllFourSupportedLanguagesAndDefaultsToPtBr() {
        assertEquals(AppLanguage.PT_BR, AppLanguage.fromTag("pt-BR"))
        assertEquals(AppLanguage.EN, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.ES, AppLanguage.fromTag("es"))
        assertEquals(AppLanguage.FR, AppLanguage.fromTag("fr"))
        assertEquals(AppLanguage.PT_BR, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.PT_BR, AppLanguage.fromTag("de"))
    }
}
