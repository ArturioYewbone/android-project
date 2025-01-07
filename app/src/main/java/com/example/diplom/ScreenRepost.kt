package com.example.diplom

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp


class ScreenRepost : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent{
            FullScreenRepost(onClose = {finish()} )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val retIntent = Intent()
        setResult(RESULT_OK, retIntent)
        finish()
    }

}
@Composable
fun FullScreenRepost(onClose: () -> Unit) {
    var currentScreen by remember { mutableStateOf("mainRepost") }

    when (currentScreen) {
        "mainRepost" -> {
            MainScreenRepost(
                onLinkClick = { currentScreen = "link" },
                onQrCodeClick = { currentScreen = "qrCode" },
                onNfcClick = { currentScreen = "nfc" },
                onClose = onClose
            )
        }
        "link" -> {
            LinkScreen(
                onBackClick = { currentScreen = "mainRepost" },
                onClose = onClose
            )
        }
        "qrCode" -> {
            QrCodeScreen(onBackClick = { currentScreen = "mainRepost" }, onClose = onClose)
        }
        "nfc" -> {
            NfcScreen(onBackClick = { currentScreen = "mainRepost" }, onClose = onClose)
        }
    }
}

@Composable
fun MainScreenRepost(onLinkClick: () -> Unit, onQrCodeClick: () -> Unit, onNfcClick: () -> Unit, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.BottomEnd)
        ) {
            Text("Назад")
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Button(onClick = onLinkClick) {
                Text("Ссылка")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onQrCodeClick) {
                Text("QR код")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNfcClick) {
                Text("NFC метка")
            }
        }
    }
}


@Composable
fun LinkScreen(onBackClick: () -> Unit, onClose: () -> Unit) {
    var text by remember { mutableStateOf(TextFieldValue("https://example.com")) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { shareLink(context, text.text) }, modifier = Modifier.fillMaxWidth()) {
            Text("Поделиться в соц сети")
        }
        Button(onClick = onBackClick, modifier = Modifier.align(Alignment.End)) {
            Text("Назад")
        }
    }
}

@Composable
fun QrCodeScreen(onBackClick: () -> Unit, onClose: () -> Unit) {
    var qrBitmap: Bitmap? = generateQRCode("1", 500)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        qrBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.size(500.dp)
            )
        } ?: Text(
            text = "QR код не найден"
        )
        Button(onClick = onBackClick, modifier = Modifier.align(Alignment.End)) {
            Text("Назад")
        }
    }
}

@Composable
fun NfcScreen(onBackClick: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val nfcManager  = remember {NFCManager(context as Activity) }
    var isNfcSupported by remember { mutableStateOf(true) }
    var isNfcEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(nfcManager) {
        isNfcSupported = nfcManager.isNfcSupported()
        isNfcEnabled = nfcManager.isNfcEnabled()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        if (!isNfcSupported) {
            Text(
                text = "NFC не поддерживается на этом устройстве",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }else if (!isNfcEnabled){
            Text(
                text = "NFC выключен. Пожалуйста, включите NFC.",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            Text(
                text = "NFC метка активна",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        Button(onClick = onBackClick, modifier = Modifier.align(Alignment.End)) {
            Text("Назад")
        }
    }
}

fun shareLink(context: Context, link: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, link)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}