package com.rachajusto.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PixKeyValidatorTest {

    @Test
    fun inspect_identifiesEmailPhoneCpfCnpjAndEvpKeys() {
        val email = PixKeyValidator.inspect("Lucas.Mendes@PixDemo.br")
        assertTrue(email.isValid)
        assertEquals(PixKeyType.EMAIL, email.type)
        assertEquals("lucas.mendes@pixdemo.br", email.normalizedKey)

        val evp = PixKeyValidator.inspect("123e4567-e89b-12d3-a456-426614174000")
        assertTrue(evp.isValid)
        assertEquals(PixKeyType.RANDOM_EVP, evp.type)

        val brMobile = PixKeyValidator.inspect("11987654321")
        assertTrue(brMobile.isValid)
        assertEquals(PixKeyType.PHONE, brMobile.type)
        assertEquals("+5511987654321", brMobile.normalizedKey)

        val cpf = PixKeyValidator.inspect("529.982.247-25")
        assertTrue(cpf.isValid)
        assertEquals(PixKeyType.CPF, cpf.type)

        val cnpj = PixKeyValidator.inspect("11.222.333/0001-81")
        assertTrue(cnpj.isValid)
        assertEquals(PixKeyType.CNPJ, cnpj.type)
    }

    @Test
    fun inspect_rejectsInvalidOrBlankKeys() {
        assertFalse(PixKeyValidator.inspect("").isValid)
        assertFalse(PixKeyValidator.inspect("111.111.111-11").isValid)
        assertFalse(PixKeyValidator.inspect("not-a-valid-key").isValid)
    }
}
