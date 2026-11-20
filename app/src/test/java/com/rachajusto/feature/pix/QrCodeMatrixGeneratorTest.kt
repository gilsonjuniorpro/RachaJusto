package com.rachajusto.feature.pix

import com.rachajusto.domain.engine.PixBrCodeGenerator
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeMatrixGeneratorTest {

    @Test
    fun generate_encodesPixEmvPayloadIntoScannableSquareMatrix() {
        val payload = PixBrCodeGenerator.generatePayload(
            pixKey = "lucas.mendes@pixdemo.br",
            merchantName = "Lucas Mendes",
            merchantCity = "SAO PAULO",
            amountCents = 42550L
        )

        val matrix = QrCodeMatrixGenerator.generate(payload)

        assertTrue("QR matrix must be at least 21x21", matrix.size >= 21)
        val darkCount = matrix.modules.count { it }
        assertTrue("QR matrix must have balanced dark/light modules", darkCount in 100 until (matrix.modules.size - 100))
    }
}
