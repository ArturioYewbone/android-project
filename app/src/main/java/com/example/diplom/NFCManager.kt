package com.example.diplom

import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import android.os.Parcelable
import android.util.Log
import java.io.IOException
import java.nio.charset.Charset
import java.util.Locale

class NFCManager(private val activity: Activity) {

    val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)


    // Вспомогательный метод для создания NFC сообщения
    private fun createNdefMessage(text: String): NdefMessage {
        val mimeRecord = createTextRecord(text)
        return NdefMessage(mimeRecord)
    }
    // Проверка, поддерживается ли NFC на устройстве
    fun isNfcSupported(): Boolean {
        return nfcAdapter != null
    }

    // Проверка, включен ли NFC
    fun isNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }

    // Метод для чтения NFC метки
    fun readNfcTag(intent: Intent): String? {
        val rawMessages: Array<NdefMessage>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) as? Array<NdefMessage>
        }

        rawMessages?.let {
            for (message in it) {
                for (record in message.records) {
                    val payload = record.payload
                    return String(payload, Charsets.UTF_8)
                }
            }
        }
        return null
    }

    // Метод для записи на NFC метку
    fun writeNfcTag(tag: Tag, text: String): Boolean {
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                val mimeRecord = createTextRecord(text)
                val ndefMessage = NdefMessage(mimeRecord)
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                return true
            } else {
                val format = NdefFormatable.get(tag)
                if (format != null) {
                    format.connect()
                    val mimeRecord = createTextRecord(text)
                    val ndefMessage = NdefMessage(mimeRecord)
                    format.format(ndefMessage)
                    format.close()
                    return true
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("NFCManager","Exception ${e.message}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("NFCManager","Exception ${e.message}")
        }
        return false
    }

    // Вспомогательный метод для создания записи в формате NFC
    private fun createTextRecord(text: String): NdefRecord {
        val langBytes = Locale.getDefault().language.toByteArray(Charset.forName("US-ASCII"))
        val textBytes = text.toByteArray(Charset.forName("UTF-8"))
        val langLength = langBytes.size
        val textLength = textBytes.size
        val payload = ByteArray(1 + langLength + textLength)

        payload[0] = langLength.toByte()
        System.arraycopy(langBytes, 0, payload, 1, langLength)
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength)

        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload)
    }
}