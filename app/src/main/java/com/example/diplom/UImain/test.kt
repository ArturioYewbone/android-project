package com.example.diplom.UImain

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import androidx.collection.intFloatMapOf
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.diplom.InfoAboutUser
import com.example.diplom.InfoPeopleOnMap
import com.example.diplom.MyViewModel
import kotlinx.coroutines.launch

 private val TAG = "MapScreen"
@SuppressLint("CommitPrefEdits")
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen2(context:Context, activeService: ActiveService, viewModel: MyViewModel) {
    val mapView = remember { MapView(context) }
    val mapViewState = rememberUpdatedState(mapView)
    val targetPoint = Point(54.467784, 64.796943)
    var userLocation: Point? = targetPoint
    var selectedUser by remember { mutableStateOf<Int?>(null)}
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val info by viewModel.infoAboutUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        userLocation = activeService.getLocation()
        viewModel.getPeopleOnMap()
    }
    LaunchedEffect(selectedUser) {
        selectedUser?.let { userId ->
            viewModel.getInfoAboutUser(userId) // Вызываем метод получения данных
        }
    }
    Scaffold { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AndroidView(
                factory = { mapViewState.value },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    val imageProvider = ImageProvider.fromResource(view.context, R.drawable.empty_people2)
                    if (userLocation != null) {
                        view.mapWindow.getMap().move(
                            CameraPosition(userLocation!!, 16.0f, 0.0f, 0.0f),
                            Animation(Animation.Type.SMOOTH, 1f),
                            null
                        )
                        view.mapWindow.map.mapObjects.addPlacemark().apply {
                            geometry = userLocation!!
                            setIcon(imageProvider)
                            addTapListener { _, _ ->
                                selectedUser = 12
                                showSheet = true
                                Log.d(TAG, "Нажата метка пользователя: 12")

                                coroutineScope.launch {
                                    sheetState.show()
                                }
                                true
                            }

                        } // Загружаем пользователей на карте
                        viewModel.infoPeopleOnMap.value.forEach { user ->
                            val usersLocation = Point(user.latitude, user.longitude)

                            val placemarkUsers =
                                view.mapWindow.map.mapObjects.addPlacemark().apply {
                                    geometry = usersLocation
                                    setIcon(imageProvider)
                                }

                            // Обработчик клика на метку
                            placemarkUsers.addTapListener { _, _ ->
                                selectedUser = user.user_id
                                showSheet = true
                                Log.d(TAG, "Нажата метка пользователя: ${user.username}")

                                coroutineScope.launch {
                                    sheetState.show()
                                }
                                true
                            }
                        }
                    }

                }
            )
        }
    }

// Вне Scaffold
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            MarkerInfoSheet2(
                idUser = selectedUser,
                viewModel = viewModel,
                data = info
            )
        }
    }

}
@Composable
fun MarkerInfoSheet2(idUser: Int?, viewModel: MyViewModel, data: InfoAboutUser) {
    Log.d(TAG, "open MarkerInfoSheet")

    Spacer(modifier = Modifier.padding(20.dp))
    Text(text = data.toString())


    Spacer(modifier = Modifier.padding(20.dp))
    Spacer(modifier = Modifier.padding(20.dp))

}