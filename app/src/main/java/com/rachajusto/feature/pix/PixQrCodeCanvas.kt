package com.rachajusto.feature.pix

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PixQrCodeCanvas(
    payload: String,
    modifier: Modifier = Modifier,
    qrSize: Dp = 216.dp
) {
    val qrMatrix = remember(payload) {
        QrCodeMatrixGenerator.generate(payload, quietZoneModules = 1)
    }

    // Always render high-contrast black modules on pure white surface so cameras scan reliably in Dark Mode
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .background(Color.White)
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(qrSize)) {
                val cellWidth = size.width / qrMatrix.size
                val cellHeight = size.height / qrMatrix.size

                for (row in 0 until qrMatrix.size) {
                    for (col in 0 until qrMatrix.size) {
                        if (qrMatrix.isDark(row, col)) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(col * cellWidth, row * cellHeight),
                                size = Size(cellWidth + 0.5f, cellHeight + 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
