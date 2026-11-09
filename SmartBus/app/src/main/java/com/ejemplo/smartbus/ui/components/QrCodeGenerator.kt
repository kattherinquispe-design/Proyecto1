package com.ejemplo.smartbus.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.net.URLEncoder

data class ParsedQrVoucher(
    val code: String,
    val rewardName: String?,
    val points: Int?
)

/**
 * Generador y lector de códigos QR de canje utilizando ZXing.
 * Cada QR codifica el enlace interactivo de verificación con el código único
 * y el nombre del premio canjeado para que al escanearlo muestre el código,
 * debajo el premio canjeado y el botón de Confirmado (1 solo uso).
 */
object QrCodeGenerator {

    private const val VERIFY_BASE_URL = "https://kattherinquispe-design.github.io/Proyecto1/canje.html"

    fun buildVoucherPayload(
        code: String,
        rewardName: String,
        points: Int = 0
    ): String {
        val cleanCode = code.trim()
        val encodedCode = runCatching { URLEncoder.encode(cleanCode, "UTF-8") }.getOrDefault(cleanCode)
        val encodedName = runCatching { URLEncoder.encode(rewardName.trim(), "UTF-8") }.getOrDefault(rewardName.trim())
        return "$VERIFY_BASE_URL?code=$encodedCode&name=$encodedName&pts=$points"
    }

    fun parseVoucherPayload(raw: String): ParsedQrVoucher {
        val trimmed = raw.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return try {
                val uri = Uri.parse(trimmed)
                val code = (uri.getQueryParameter("code") ?: uri.getQueryParameter("c") ?: trimmed).trim()
                val name = uri.getQueryParameter("name") ?: uri.getQueryParameter("n")
                val pts = (uri.getQueryParameter("pts") ?: uri.getQueryParameter("p"))?.toIntOrNull()
                ParsedQrVoucher(code = code, rewardName = name, points = pts)
            } catch (_: Exception) {
                ParsedQrVoucher(code = trimmed, rewardName = null, points = null)
            }
        }
        return ParsedQrVoucher(code = trimmed, rewardName = null, points = null)
    }

    fun generate(content: String, sizePx: Int = 512): ImageBitmap? {
        if (content.isBlank()) return null
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE
                    )
                }
            }
            bitmap.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
