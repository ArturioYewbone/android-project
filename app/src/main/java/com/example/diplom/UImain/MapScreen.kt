package com.example.diplom.UImain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.diplom.ActiveService
import com.example.diplom.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

 val TAG = "MapScreen"
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MapScreen(activeService: ActiveService?, userLocation: Point?) {
    var userLocation by remember {mutableStateOf<Point?>(userLocation) }
    Log.d(TAG, "user location in start map ${userLocation}")
    var showInfoSheet by remember { mutableStateOf(false) } // Состояние для управления окном
    var selectedLocation by remember { mutableStateOf<Point?>(null) } // Храним координаты выбранного маркера
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    var mapView: MapView? by remember { mutableStateOf(null) }
    val targetPoint = Point(58.837566, 35.835812)
    val bottomSheetDialog = remember { BottomSheetDialog(context) }
    var placemark: PlacemarkMapObject? = null
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Местоположение", style = MaterialTheme.typography.h6)
                Spacer(modifier = Modifier.height(8.dp))
                userLocation?.let {
                    Text("Широта: ${it.latitude}")
                    Text("Долгота: ${it.longitude}")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { coroutineScope.launch { sheetState.hide() } }) {
                    Text("Закрыть")
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        mapView = this // Сохраняем ссылку на mapView для дальнейшего использования
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mapView ->
                    val scope = CoroutineScope(Dispatchers.Main)
                    scope.launch {
                        val imageProvider =
                            ImageProvider.fromResource(
                                mapView.context,
                                R.drawable.empty_people2
                            )
                        // Если местоположение уже есть, сразу перемещаем карту
                        if (userLocation != null) {
                            Log.d(TAG, "userlocation not null")
                            userLocation?.let {
                                mapView.mapWindow.getMap().move(
                                    CameraPosition(it, 16.0f, 0.0f, 0.0f),
                                    Animation(Animation.Type.SMOOTH, 1f), // Smooth animation
                                    null
                                )
                                placemark = mapView.mapWindow.map.mapObjects.addPlacemark().apply {
                                    geometry = it
                                    setIcon(imageProvider)
                                }
                                placemark?.isDraggable = true
                                placemark?.setIconStyle(IconStyle().apply { scale = 2f })
                                // Добавляем обработчик кликов на маркер
                                placemark?.addTapListener { _, _ ->
                                    Log.d(TAG,"click on placemark point:${userLocation?.latitude}-${userLocation?.longitude}")
                                    coroutineScope.launch { sheetState.show() }
                                    true
                                }
                            }
                        } else {
                            Log.d(TAG, "userLocation null")
                            // Если местоположение ещё не получено, подождём несколько секунд
                            //delay(7000) // Пауза 2 секунды

                            // Параллельно пытаемся получить местоположение
                            val t = activeService?.getLocation()

                            if (t != null) {
                                Log.d(
                                    TAG,
                                    "получено из сервиса при запуске ${t.latitude} : ${t.longitude}"
                                )
                                userLocation = Point(t.latitude, t.longitude)
                                Log.d(TAG, "получены координаты ${userLocation}")
                                // После получения местоположения сразу перемещаем карту
                                userLocation?.let {
                                    mapView.mapWindow.getMap().move(
                                        CameraPosition(it, 16.0f, 0.0f, 0.0f),
                                        Animation(Animation.Type.SMOOTH, 1f), // Smooth animation
                                        null
                                    )
                                    Log.d(TAG, "перемещение к точке при запуске")
                                    placemark =
                                        mapView.mapWindow.map.mapObjects.addPlacemark().apply {
                                            geometry = it
                                            setIcon(imageProvider)
                                        }
                                    placemark?.isDraggable = true
                                    placemark?.setIconStyle(IconStyle().apply { scale = 2f })
                                    // Добавляем обработчик кликов на маркер
                                    placemark?.addTapListener { _, _ ->
                                        Log.d(
                                            TAG,
                                            "click on placemark point:${userLocation?.latitude}-${userLocation?.longitude}"
                                        )
                                        showInfoSheet = true // Открываем BottomSheetDialog
                                        true
                                    }
                                }
                            } else {
                                Log.d(TAG, "Местоположение не получено переход к targetPoint")
                                mapView.mapWindow.getMap().move(
                                    CameraPosition(targetPoint, 14.0f, 0.0f, 0.0f),
                                    Animation(Animation.Type.SMOOTH, 2f),
                                    null
                                )
                            }
                        }

                    }
                }
            )
            // Добавление кнопки для приближения
            FloatingActionButton(
                onClick = {
                    if (activeService == null) {
                        Log.d(TAG, "activeService null")
                    }
                    Log.d(TAG, "click on button my location")
                    val t = activeService?.getLocation()
                    Log.d(TAG, "get location after click button ${t}")
                    if (t != null) {
                        Log.d(TAG, "получены координаты")
                        userLocation = Point(t.latitude, t.longitude)
                    }
                    userLocation?.let {
                        mapView?.mapWindow?.getMap()?.move(
                            CameraPosition(it, 16.0f, 0.0f, 0.0f),
                            Animation(Animation.Type.SMOOTH, 1f), // Smooth animation
                            null
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd) // Размещение в правом нижнем углу
                    .padding(16.dp) // Отступ
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = "Go to My Location")
            }

        }
    }
}
@OptIn(ExperimentalMaterial3Api::class) // Это требуется для BottomSheetScaffold
@Composable
fun BottomSheetContent(location: Point?, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Информация о точке",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Координаты: ${location?.latitude ?: "неизвестно"}, ${location?.longitude ?: "неизвестно"}")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onClose) {
            Text("Закрыть")
        }
    }
}