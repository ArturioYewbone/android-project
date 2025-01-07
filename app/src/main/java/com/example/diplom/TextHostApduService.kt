package com.example.diplom

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import java.nio.charset.Charset

class TextHostApduService : HostApduService() {

    // Текст, который будет отправлен в качестве метки
    private val textToSend = "Пример текста для HCE метки"

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        // Обрабатываем входящий APDU запрос от другого NFC устройства

        // Здесь можно добавить логику обработки APDU-запросов
        // Например, проверка команды SELECT AID или другое

        // Конвертируем текст в байтовый массив и возвращаем его как ответ
        val response = textToSend.toByteArray(Charsets.UTF_8)
        return response
    }

    override fun onDeactivated(reason: Int) {
        // Вызывается, когда соединение с NFC-устройством разрывается
    }
}