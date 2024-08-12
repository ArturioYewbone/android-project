package com.example.diplom
import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.*

fun generateQRCode(content: String, size: Int): Bitmap? {
    return try {
        // Настройки для кодировки (например, UTF-8)
        val hints = Hashtable<EncodeHintType, String>()
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

        // Создание битовой матрицы QR-кода из данных
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height

        // Создание пустого Bitmap для QR-кода
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        // Заполнение Bitmap на основе данных из битовой матрицы
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) -0x1000000 else -0x1)
            }
        }

        bmp
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
