package com.example.diplom.UImain

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.diplom.ActiveService
import com.example.diplom.InfoAboutUser
import com.example.diplom.MyViewModel
import com.example.diplom.NFCManager
import com.example.diplom.ScannigProfile
import com.example.diplom.activeService
import com.example.diplom.activityResultLauncher
import com.example.diplom.generateQRCode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

private val TAG = "Scanner"
@Composable
fun Scanner(nfcIntent: Intent? = null, context: Context, activeService: ActiveService, myViewModel: MyViewModel) {
    var selectedTab by remember { mutableStateOf("showQR") }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            selectedTab = "scan"
        } else {
            // Разрешение не предоставлено
        }
    }
    val scanQRCodeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Обработка результата сканирования QR-кода
    }

    val nfcManager = remember { NFCManager(context as Activity) }
    var nfcText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedTab, nfcIntent) {
        if (selectedTab == "ScanNfc" && nfcIntent != null) {
            nfcText = nfcManager.readNfcTag(nfcIntent)
            if (nfcText == null) {
                nfcText = "Не удалось прочитать текст с метки."
            }
        }
    }
    Column {
        // Верхняя панель с кнопками
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { selectedTab = "ScanNFC" },
                enabled = selectedTab != "ScanNFC"
            ) {
                Text("Отсканировать через NFC")
            }
            Button(
                onClick = {
                    val cameraPermission = Manifest.permission.CAMERA
                    when {
                        ContextCompat.checkSelfPermission(
                            context,
                            cameraPermission
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            selectedTab = "scanQR"
                        }

                        else -> {
                            cameraPermissionLauncher.launch(cameraPermission)
                        }
                    }
                },
                enabled = selectedTab != "scanQR"
            ) {
                Text("Отсканировать QR код")
            }
        }

        // Содержимое в зависимости от выбранной вкладки
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                "ScanNFC" -> {
                    Text(
                        text = nfcText ?: "Ожидание сканирования NFC метки...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                "scanQR" -> {
                    QRCodeScannerView(context, lifecycleOwner = lifecycleOwner, myViewModel)
                }
            }
        }
    }
}
private var id = 0
@Composable
fun QRCodeScannerView(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    viewModel: MyViewModel
) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }

    val userInfo by viewModel.infoAboutUser
        .collectAsState(initial = InfoAboutUser("", "",0f,))
    val infoPeopleOnMap by viewModel.infoPeopleOnMap.collectAsState()
// Флаг, показываем ли диалог
    val showDialog = remember(userInfo, infoPeopleOnMap) {
        // диалог — если у нас есть отсканированный ID и он встречается в списке
        id != 0 && infoPeopleOnMap.any { it.user_id == id }
    }
    // как только showDialog стал true — прекратим сканирование
    LaunchedEffect(showDialog) {
        if (showDialog) {
            // realtime: отцепляем камеру
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
            isScanningActive = false
        }
    }
    // Как только пришли реальные данные — показываем диалог
//    LaunchedEffect(userInfo, infoPeopleOnMap) {
//        if (id != 0 &&
//            infoPeopleOnMap.any { it.user_id == id }
//        ) {
//            showDialog = true
//        }
//    }

    LaunchedEffect(cameraProviderFuture, isScanningActive) {
        if (!isScanningActive) return@LaunchedEffect
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()

        val imageAnalysis = ImageAnalysis.Builder()
            .build()
            .also { analysis ->
                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        if (!isScanningActive) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstOrNull()?.let { barcode ->
                                    val text = barcode.displayValue?.toIntOrNull()
                                    if (text != null) {
                                        id = text
                                        viewModel.getPeopleOnMap()
                                        viewModel.getInfoAboutUser(text)
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageAnalysis
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(Modifier.fillMaxSize()) {
        // рисуем превью только если мы ещё сканируем
        if (isScanningActive) {
            AndroidView({ previewView }, Modifier.matchParentSize())
        }
    }
    if (showDialog) {
        UserInfoDialog(
            info          = userInfo,
            activeService = activeService!!/* ваш activeService объект */,
            onDismiss     = {
                id       = 0 // опционально — очистить Flow в VM
                isScanningActive = true
                // можно сбросить userInfo в viewModel, если нужно
            },
            onSendReview = { reviewText, rating ->
                viewModel.sendingReview(reviewText, rating, id)
            },
            viewModel = viewModel,
            selectedUser = 0
        )
    }
}
public var isScanningActive = true


private fun handleBarcode(context: Context, barcode: Barcode, viewModel: MyViewModel) {
    if (!isScanningActive) return
    when (barcode.valueType) {
        Barcode.TYPE_URL -> {
            Log.d(TAG, "URL scanning")
        }
        Barcode.TYPE_TEXT -> {
            val text = barcode.displayValue
            Log.d(TAG, "$text")
            text?.let {
                viewModel.getPeopleOnMap()
                viewModel.getInfoAboutUser(it.toInt())

                id = it.toInt()
            }

            stopScanning()
            val intent = Intent(context, ScannigProfile::class.java).apply {
                putExtra("text", text)
            }
            activityResultLauncher.launch(intent)
        }
    }
}

private fun stopScanning() {
    isScanningActive = false // Остановить анализ кадров
}