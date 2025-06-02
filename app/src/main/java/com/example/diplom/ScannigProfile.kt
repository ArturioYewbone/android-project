package com.example.diplom

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import org.w3c.dom.Text
private val TAG = "ScannigProfile"
class ScannigProfile : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Open Scanning activity")
        val scannedData = intent.getStringExtra("text") ?: ""
        Log.d(TAG, scannedData)
        setContent {
            openProfile(scannedData)
        }
    }
}

@Composable
fun openProfile(text: String){
    val context = LocalContext.current as ComponentActivity
    Box(modifier = Modifier.fillMaxSize()) {
        // Центрированный текст
        Text(
            text = text,
            modifier = Modifier.align(Alignment.Center)
        )

        // Кнопка назад внизу справа
        Button(
            onClick = {
                // Создаем Intent для передачи данных обратно
                val resultIntent = Intent().apply {
                    putExtra("isScanningActive", true) // Передаем флаг true
                }
                // Устанавливаем результат для этой активности
                context.setResult(Activity.RESULT_OK, resultIntent)
                // Завершаем активность
                context.finish()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp) // Отступы от краев экрана
        ) {
            Text("Назад")
        }
    }
}