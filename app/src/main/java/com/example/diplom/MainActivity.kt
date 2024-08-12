package com.example.diplom

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.content.Context
import android.content.res.Resources
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
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

class MainActivity : ComponentActivity() {
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey("a48271b5-b501-406c-b9b2-98cce9c84a2c")
        MapKitFactory.initialize(this)

//        mapView = MapView(this)
        setContent {
            MyApp()
        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}

@Composable
fun MyApp() {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> ProfileScreen()
                1 -> MapScreen()
                2 -> QRScreen()
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
            label = { Text("QRcod") }
        )
    }
}

@Composable
fun MapScreen() {
    lateinit var mapObjectCollection: MapObjectCollection
    lateinit var placemarkMapObject: PlacemarkMapObject
    var userLocation by remember { mutableStateOf<Point?>(null) }
// Запрашиваем местоположение
    LaunchedEffect(key1 = true) {
        val locationManager = MapKitFactory.getInstance().createLocationManager()
        val locationListener = object : LocationListener {
            override fun onLocationUpdated(location: com.yandex.mapkit.location.Location) {
                userLocation = location.position
            }

            override fun onLocationStatusUpdated(locationStatus: LocationStatus) {
                // Обработка обновления статуса местоположения
            }
        }

        locationManager.requestSingleUpdate(locationListener)
    }

    val targetPoint = Point(58.837566, 35.835812)
    AndroidView(

        factory = { context ->
            val mapView = MapView(context)
            // Дополнительная настройка MapView если нужно
            mapView },
        modifier = Modifier.fillMaxSize(),
        update = {mapView ->
            userLocation?.let {
                mapView.mapWindow.getMap().move(
                    CameraPosition(it, 16.0f, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 1f), // Добавляем анимацию
                    null
                )
                val imageProvider = ImageProvider.fromResource(mapView.context, R.drawable.empty_people2)
                val placemark = mapView.mapWindow.map.mapObjects.addPlacemark().apply {
                    geometry = it
                    setIcon(imageProvider)
                }
                placemark.setIconStyle(IconStyle().apply { scale = 2f })
            }?: run {
                // Если местоположение еще не получено, перемещаем карту к статической точке
                mapView.mapWindow.getMap().move(
                    CameraPosition(targetPoint, 14.0f, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 2f),
                    null
                )
            }

        }
    )
}

@Composable
fun QRScreen() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = "QRScreen")
    }
}

@Composable
fun ProfileScreen() {
    var hasPhoto by remember { mutableStateOf(false) }
    var showReviewsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .weight(1f)
                    .border(
                        BorderStroke(2.dp, Color.Gray),
                        shape = CircleShape
                    )

                    .clip(CircleShape)
                    .clickable {
                        // Загрузка фото
                        hasPhoto = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (hasPhoto) {
                    AsyncImage(
                        model = "https://yandex-images.clstorage.net/100H3bC16/f85172BwRKm5/JPmvU9pjeoYluQ7XILd3e0EZNrsE_PIY98Vz9f4BvdadMwHvPVXfA1FBYtw3rwLo2EclQheCwoAKdbwh8SEtmQCNa2fbto_SBH26F1AnNo_wzba_dTnSqYZ1BmGbhHhjzTWnXQpS7LSyVqDrIGuXaA0S9NPmhrE222L8VgQUaduGmS7yz4CKZYJaP5chhFadkKgmncR5IvhyNHchvNXPxCslRE2a8vwHEZTTGpIZpdtsI7WGF2TgFB_Qv0RH1qnIFKnO058SmlWDeu6Gg6XEPqGI9102m5DKwteGUCy2rOS4RrYOGgAfBqB0kanCuTZo3EOEVVbFsYe_I4xWx5ZLSiWJDSHM4VtmA0p8FvAlx1wxm4cvBgmA2IB0RoOflIw0OGQXnorDraZjx7Bp4gh0-74TpcY3Z7JWTlOM5SeWemgkiW4SzLE4R1BoPicAFaV-g3sm34YpMAhiViWSPLbvBJsmRy4q4ewUo5di-BJbJbtO0Bb0J7bTt6xALCa3JSpZVcnswv_DybUzawy3E8WE3qFaVTz2OlP4ADelIu1GrJZZdRTPmlIdVLJWQ9sgGOY6_xBkVjXl8ha_kj5kRTaay2UrbINuMMtlYjncZdPFpJ3zC6SNhOuTmhBmNgEOJF6UuXZ3rIiiXkSBhyEI4-qnqP-AFDZnFpJ3HABt18R2CHnkK30wvKFYl3ML3bTCBXSMsQh3nMYqoSiDJUWQbQcfFhpGBm1aMp1WwOYhGZHrNfpMUtQHRMcChjzi7OQXxGi6tSvuQX4Q6ZXAyA40goRH7uHpd-2EmhC581RUge2WXDS7lmQOSlL9Z9MmYIlziEQorHOX9Gdn0_d8E55kRIQ6abTZPkIMI5mVMDoMlZN19b2w6Cb9Veky-6CEZNJ_l61UCibm3HsyLjYgdOHqkyiny-xCdwY1JvKVrmPvN7WmyAin293iXYNKpyAq_HaSFmfeM-nEvGdYAirxRfZhM", // URL фото
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Загрузить фото", fontSize = 14.sp, color = Color.Gray)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        showReviewsDialog = true
                    }
                ) {
                    repeat(5) { index ->
                        Icon(
                            painter = painterResource(id = android.R.drawable.btn_star_big_off),
                            contentDescription = "Rating Star",
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { /* TODO: Обработка нажатия */ }) {
                    Text("Друзья")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = { /* TODO: Обработка нажатия */ }) {
                    Text("Рейтинг")
                }
            }
        }

        Text(
            text = "Последние отзывы",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(5) {
                ReviewItem(
                    name = "Имя пользователя",
                    rating = 4,
                    date = "01.01.2024",
                    text = "Это пример отзыва. Очень хороший отзыв."
                )
            }
        }
    }

    if (showReviewsDialog) {
        ReviewsDialog(onDismiss = { showReviewsDialog = false })
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
