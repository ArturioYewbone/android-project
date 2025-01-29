package com.example.diplom

import android.os.Bundle
import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.nfc.NfcAdapter
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.diplom.UImain.*

lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
var userLocation: Point? = null
private val TAG = "MainActivity"
private var aService: ActiveService? = null
class MainActivity : ComponentActivity() {
    private val internetCheckReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action){
                "ACTION_CHECK_INTERNET_CONNECTION" -> {
                    val message = intent.getStringExtra("message") ?: "Please check your internet connection."
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d(TAG,"service connect")
            val binder = service as ActiveService.MyBinder
            aService = binder.getService()
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d(TAG,"service disconnect")
            aService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Intent(this, ActiveService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
//        val intent = Intent("TEST")
        //intent.putExtra("string_array", "getApiForMap")
        //var t = aService?.sendToServer(arrayListOf("getApiForMap"))
        //var t = ""
        //Log.d(TAG, "ответ от серера в мэйне $t")

        if (!isMapKitInitialized) {
            MapKitFactory.setApiKey("a48271b5-b501-406c-b9b2-98cce9c84a2c")
            MapKitFactory.initialize(this)
            isMapKitInitialized = true

        }
        val t = aService?.getLocation()
        t?.let { userLocation = Point(it.latitude, it.longitude)
            Log.d(TAG, "location received")
        }
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK || result.resultCode == Activity.RESULT_CANCELED) {
                // Активность завершена, можно снова включить сканирование
                isScanningActive = true
            }
        }
        nfcManager = NFCManager(this)
//        mapView = MapView(this)
        setContent {
            MyApp(this)
        }
    }
    companion object {
        private var isMapKitInitialized = false
    }
    private lateinit var nfcManager: NFCManager

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("ACTION_CHECK_INTERNET_CONNECTION")
        registerReceiver(internetCheckReceiver, filter)
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        nfcManager.nfcAdapter?.enableForegroundDispatch(
            this,
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT),
            null,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(internetCheckReceiver)
        nfcManager.nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onStop() {
        super.onStop()
        MapKitFactory.getInstance().onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        Log.d(TAG, "onStop main activity")
        val serviceIntent = Intent(this, BackgroundServiceLocation::class.java)
        // Проверяем версию Android перед запуском сервиса
        if (!BackgroundServiceLocation.isServiceRunning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "destroy main activity")
        stopService(Intent(this, ActiveService::class.java))
        MapKitFactory.getInstance().onStop()
        val serviceIntent = Intent(this, BackgroundServiceLocation::class.java)
        // Проверяем версию Android перед запуском сервиса
        if (!BackgroundServiceLocation.isServiceRunning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
        super.onDestroy()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Убедитесь, что Intent относится к NFC
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            // Обновляем Intent в Compose
            setContent {
                Scanner(nfcIntent = intent, context = this)
            }
        }
    }
}

@Composable
fun MyApp(context: Context) {
    var selectedTab by remember { mutableStateOf(0) }
    if(aService == null){Log.d(TAG, "in method MyApp active service null")}
    Scaffold(
        bottomBar = {
            BottomNavigationBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> ProfileScreen(context)
                1 -> MapScreen(aService, userLocation)
                2 -> Scanner(null, context)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(ImageVector.vectorResource(id = androidx.core.R.drawable.ic_call_answer), contentDescription = "Home") },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            icon = { Icon(ImageVector.vectorResource(id = androidx.core.R.drawable.ic_call_answer), contentDescription = "Search") },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            label = { Text("Map") }
        )
        NavigationBarItem(
            icon = { Icon(ImageVector.vectorResource(id = androidx.core.R.drawable.ic_call_decline), contentDescription = "Profile") },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            label = { Text("Сканер") }
        )
    }
}

@Composable
fun ReviewItem(name: String, rating: Int, date: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = android.R.drawable.ic_menu_camera),
                contentDescription = "User Photo",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(text = name, fontWeight = FontWeight.Bold)
                Text(text = date, fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            repeat(rating) {
                Icon(
                    painter = painterResource(id = android.R.drawable.btn_star_big_on),
                    contentDescription = "Star",
                    tint = Color.Yellow,
                    modifier = Modifier.size(16.dp)
                )
            }
            repeat(5 - rating) {
                Icon(
                    painter = painterResource(id = android.R.drawable.btn_star_big_off),
                    contentDescription = "Star",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = text)
    }
}
@Composable
fun ReviewsDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Все отзывы",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Здесь можно добавить фильтры

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(20) {
                        ReviewItem(
                            name = "Имя пользователя",
                            rating = 4,
                            date = "01.01.2024",
                            text = "Это пример отзыва. Очень хороший отзыв."
                        )
                    }
                }
            }
        }
    }
}

