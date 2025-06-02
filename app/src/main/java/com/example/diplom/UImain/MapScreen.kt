package com.example.diplom.UImain

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.diplom.InfoAboutUser
import com.example.diplom.InfoPeopleOnMap
import com.example.diplom.MyViewModel
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.launch
import android.net.Uri
import android.util.TypedValue
import java.io.File

private val TAG = "MapScreen"
@SuppressLint("CommitPrefEdits")
@Composable
fun MapScreen(context:Context, activeService: ActiveService, viewModel: MyViewModel) {
    val isLoading by viewModel.isLoading.collectAsState()

    val sharedPreferences = context.getSharedPreferences("MapScreen", MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    var userId by remember { mutableStateOf<Int>(0)}

    val mapView = remember { MapView(context) }

    val p: Point? = sharedPreferences.getString("latitude", "54.467784")
        ?.let { Point(it.toDouble(), sharedPreferences.getString("longtude", "64.796943")!!.toDouble()) }
    val targetPoint = p
    Log.d(TAG, "Point in shared - ${p?.latitude}:${p?.longitude}")
    var userLocation: Point? = null

    var placemark: PlacemarkMapObject? = null

    val infoOnMap by viewModel.infoPeopleOnMap.collectAsState()
    val avatars by viewModel.avatars.collectAsState(initial = emptyMap())

    var selectedUser by remember { mutableStateOf<Int?>(null)}

    var isPublic by remember { mutableStateOf(sharedPreferences.getBoolean("isPublic", false)) } // Состояние ползунка
    val hasShownWarningKey = "hasShownWarning_${activeService.idUser}"
    val lastEnableTimeKey = "lastEnableTime_${activeService.idUser}"
    // Переменная состояния, контролирующая показ диалога
    var showDialog by remember { mutableStateOf(false) }

    val onMarkerClick = remember {
        { user: InfoPeopleOnMap ->
            selectedUser = user.user_id
            showDialog = true
            Log.d(TAG, "click on placemark: ${user.username} at ${user.latitude}, ${user.longitude}, ${user.user_id}")
            userId = user.user_id
            true
        }
    }
    userLocation = activeService.getLocation()
    // Получаем местоположение пользователя ОДИН раз при запуске
    LaunchedEffect(Unit) {
        Log.d(TAG, "Получение location")
        viewModel.getPeopleOnMap()
    }
    LaunchedEffect(selectedUser) {
        Log.d(TAG, "start LaunchedEffect(selectedUser) with selectedUser $selectedUser")
        selectedUser?.let { userId ->
            viewModel.getInfoAboutUser(userId) // Вызываем метод получения данных
        }
    }
    LaunchedEffect(infoOnMap, avatars) {
        val mapObjects = mapView.mapWindow.map.mapObjects
        mapObjects.clear()

        val ctx = mapView.context
        val prefs = ctx.getSharedPreferences("profile_prefs", MODE_PRIVATE)
        val photoPath = prefs.getString("photo_path", null)

        val iconDp = 48f
        val iconPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            iconDp,
            ctx.resources.displayMetrics
        ).toInt()

        // Выбираем ImageProvider: либо из Bitmap по URI, либо дефолтный ресурс
        val imageProvider = if (!photoPath.isNullOrBlank()) {
            // загружаем Bitmap из файла
            val file = File(photoPath)
            val originalBmp: Bitmap? = if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null

            val scaledBmp: Bitmap? = originalBmp?.let { bmp ->
                Bitmap.createScaledBitmap(bmp, iconPx, iconPx, true)
            }

            if (scaledBmp != null) {
                ImageProvider.fromBitmap(scaledBmp)
            } else {
                ImageProvider.fromResource(ctx, R.drawable.empty_people2)
            }
        } else {
            ImageProvider.fromResource(ctx, R.drawable.empty_people2)
        }

        // Если местоположение уже есть, сразу перемещаем карту
        if (userLocation != null) {
            Log.d(TAG, "userlocation not null")
            userLocation?.let {
                mapView.mapWindow.getMap().move(
                    CameraPosition(it, 16.0f, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 1f), // Smooth animation
                    null
                )
                placemark =
                    mapView.mapWindow.map.mapObjects.addPlacemark().apply {
                        geometry = it
                        setIcon(imageProvider)
                    }
            }
        } else {
            Log.d(TAG, "Местоположение не получено переход к targetPoint")
            targetPoint?.let { CameraPosition(it, 14.0f, 0.0f, 0.0f) }?.let {
                mapView.mapWindow.getMap().move(
                    it,
                    Animation(Animation.Type.SMOOTH, 2f),
                    null
                )
            }
        }
        infoOnMap.forEach { user ->
            val loc = Point(user.latitude, user.longitude)
            Log.d(TAG,"user найден рядом: ${user.username} at ${user.latitude}, ${user.longitude}, ${user.user_id}")

            // 1) Пытаемся достать байты из avatars
            val bmpProvider = avatars[user.user_id]?.let { bytes ->
                // если байты есть — декодируем в Bitmap
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?.let { bmp ->
                        // масштабируем под нужный размер
                        val iconDp = 48f
                        val iconPx = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            iconDp,
                            mapView.context.resources.displayMetrics
                        ).toInt()
                        val scaled = Bitmap.createScaledBitmap(bmp, iconPx, iconPx, true)
                        ImageProvider.fromBitmap(scaled)
                    }
            }
            // 2) либо дефолтная иконка
            val imageProvider = bmpProvider
                ?: ImageProvider.fromResource(mapView.context, R.drawable.empty_people2)

            // 3) добавляем маркер
            mapObjects.addPlacemark(loc, imageProvider).apply {
                isDraggable = true
                setIconStyle(IconStyle().apply { scale = 2f })
                addTapListener { _, _ ->
                    onMarkerClick(user)
                }
            }
        }
    }
    val info by viewModel.infoAboutUser.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    Log.d(TAG, "isLoading=$isLoading")
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Загрузка...")
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )
            // Вызываем `Dialog`, если showDialog == true
            if (showDialog) {
                UserInfoDialog(
                    info = info,
                    onDismiss = { showDialog = false },
                    onSendReview = { reviewText, rating ->
                        Log.d(TAG, "Отзыв отправлен: $rating\n$reviewText")
                        viewModel.sendingReview(reviewText, rating, userId)
                    },
                    activeService
                )
            }

            // Ползунок в правом верхнем углу
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Switch(
                    checked = isPublic,
                    onCheckedChange = { newValue ->
                        if (!newValue) {
                            // Если выключает ползунок
                            val hasShownWarning = sharedPreferences.getBoolean(hasShownWarningKey, false)
                            if (!hasShownWarning) {
                                Toast.makeText(
                                    context,
                                    "Когда включите обратно, вы не сможете ставить отзывы полчаса.",
                                    Toast.LENGTH_LONG
                                ).show()
                                // Отмечаем, что предупреждение уже показывалось
                                editor.putBoolean(hasShownWarningKey, true).apply()
                            }
                        } else {
                            // Если включает обратно — сохраняем текущее время
                            val currentTime = System.currentTimeMillis()
                            editor.putLong(lastEnableTimeKey, currentTime).apply()
                        }

                        // 1. Обновляем локальное состояние
                        isPublic = newValue

                        // 2. Сохраняем в SharedPreferences
                        editor.putBoolean("isPublic", newValue).apply()
                        viewModel.sendCommand("UPDATE myusers " +
                                "SET visibility = ${newValue} " +
                                "WHERE user_id = ${activeService.idUser};", "sql")
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF6200EE), // Цвет при включении
                        uncheckedThumbColor = Color.Gray // Цвет при выключении
                    )
                )
            }
            // Кнопка для перемещения карты на текущие координаты
            FloatingActionButton(
                onClick = {
                    Log.d(TAG, "click on button my location")
                    val t = activeService.getLocation()
                    Log.d(TAG, "get location after click button ${t}")
                    userLocation?.let {
                        mapView.mapWindow?.getMap()?.move(
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

@Composable
fun UserInfoDialog(
    info: InfoAboutUser,
    onDismiss: () -> Unit,
    onSendReview: (String, Int) -> Unit,
    activeService: ActiveService
) {
    var showReviewField by remember { mutableStateOf(false) }
    var reviewText by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("MapScreen", Context.MODE_PRIVATE)
    }
    val lastEnableTimeKey = "lastEnableTime_${activeService.idUser}"
    val lastEnableTime = prefs.getLong(lastEnableTimeKey, 0L)
    // 30 минут в миллисекундах
    val halfHourMillis = 30 * 60 * 1000L
    // сколько прошло с момента последнего включения
    val elapsed = System.currentTimeMillis() - lastEnableTime
    // разрешено ли сейчас оставлять отзыв
    val canLeaveReview = elapsed >= halfHourMillis
    // сколько ещё (в минутах) ждать
    val minutesLeft = ((halfHourMillis - elapsed) / 60000).coerceAtLeast(0)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(Color.White, shape = RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Информация о пользователе", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = info.toString(), modifier = Modifier.padding(vertical = 8.dp))

                if (showReviewField) {
                    var isReviewEmpty by remember { mutableStateOf(false) }
                    var isRatingEmpty by remember { mutableStateOf(false) }
                    // Блок с 5 звездами
                    Row(modifier = Modifier.padding(vertical = 8.dp)) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Оценка $i",
                                tint = if (i <= rating) Color.Yellow else Color.Gray, // Закрашиваем выбранные
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { rating = i
                                        isRatingEmpty = false} // Запоминаем выбранный рейтинг
                            )
                        }
                    }
                    if (isRatingEmpty) {
                        Text("Выберите рейтинг!", color = Color.Red, fontSize = 14.sp)
                    }
                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it
                            isReviewEmpty = false},
                        label = { Text("Введите отзыв") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = isReviewEmpty
                    )
                    if (isReviewEmpty) {
                        Text("Отзыв не должен быть пустым!", color = Color.Red, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        if (reviewText.isBlank()) {
                            isReviewEmpty = true
                        }
                        if (rating == 0) {
                            isRatingEmpty = true
                        }

                        if (!isReviewEmpty && !isRatingEmpty) {
                            onSendReview(reviewText, rating)
                            onDismiss() // Закрыть диалог после отправки
                        }
                    }) {
                        Text("Отправить")
                    }
                } else {
                    Column (horizontalAlignment = Alignment.CenterHorizontally){
                        Button(
                            onClick = { showReviewField = true },
                            enabled = canLeaveReview
                        ) {
                            Text("Оставить отзыв")
                        }
                        if (!canLeaveReview) {
                            Text(
                                text = "Еще не прошло 30 минут с момента включения. Подождите $minutesLeft мин.",
                                color = Color.Red,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDismiss) {
                    Text("Закрыть")
                }
            }
        }
    }
}
@Composable
fun ReviewButton(
    activeService: ActiveService,
    showReviewField: Boolean,
    onShowReviewFieldChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    }
    val lastEnableTimeKey = "lastEnableTime_${activeService.idUser}"
    val lastEnableTime = prefs.getLong(lastEnableTimeKey, 0L)

    // 30 минут в миллисекундах
    val halfHourMillis = 30 * 60 * 1000L
    // сколько прошло с момента включения
    val elapsed = System.currentTimeMillis() - lastEnableTime
    // можно ли оставить отзыв
    val canLeaveReview = elapsed >= halfHourMillis
    // сколько осталось ждать (в минутах)
    val minutesLeft = ((halfHourMillis - elapsed) / 60000).coerceAtLeast(0)

    Column {
        Button(
            onClick = {
                onShowReviewFieldChange(true)
            },
            enabled = canLeaveReview
        ) {
            Text("Оставить отзыв")
        }

        if (!canLeaveReview) {
            Text(
                text = "Еще не прошло 30 минут с момента включения. Подождите $minutesLeft мин.",
                color = Color.Red,
                modifier = Modifier
                    .padding(top = 8.dp)
            )
        }
    }
}
@Composable
fun MarkerInfoSheet(idUser: Int?, viewModel: MyViewModel, data: InfoAboutUser) {
    Log.d(TAG, "open MarkerInfoSheet")

    Spacer(modifier = Modifier.padding(20.dp))
    Text(text = data.toString())


    Spacer(modifier = Modifier.padding(20.dp))
    Spacer(modifier = Modifier.padding(20.dp))

}
