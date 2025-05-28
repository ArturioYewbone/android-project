package com.example.diplom.UImain

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.diplom.*
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.StarBorder

private val TAG = "ProfileScreen"
@Composable
fun ProfileScreen(context: Context, activeService: ActiveService?, viewModel: MyViewModel) {
    var hasPhoto by remember { mutableStateOf(false) }
    var showReviewsDialog by remember { mutableStateOf(false) }
    var isFullScreenRepost by remember { mutableStateOf(false) }
    val reviews by viewModel.reviewsFlow5.collectAsState()
    val reviewsFrom by viewModel.reviewsFlow5From.collectAsState()
    val rating by viewModel.avgRating.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.sendCommand(
            "SELECT AVG(rating) AS average_rating FROM ratings WHERE myuser_id = ${activeService?.idUser};",
            "avg_rating"
        )
    }
    LaunchedEffect(rating) {
        // например, guard чтобы не запускать на initial 0f
        if (rating > 0f) {
            viewModel.sendCommand(
                "SELECT r.rating, r.review, r.review_date, u.username AS reviewer_name " +
                        "FROM ratings r JOIN myusers u ON r.reviewer_myuser_id = u.user_id " +
                        "WHERE r.myuser_id = ${activeService?.idUser} " +
                        "ORDER BY r.review_date DESC LIMIT 5;",
                "review_five"
            )
        }
    }

    // 3) Как только reviews обновился (и не пустой), запрашиваем первые 5 «отправленных»
    LaunchedEffect(reviews) {
        if (reviews.isNotEmpty()) {
            viewModel.sendCommand(
                "SELECT r.rating, r.review, r.review_date, u.username AS reviewer_name " +
                        "FROM ratings r JOIN myusers u ON r.myuser_id = u.user_id " +
                        "WHERE r.reviewer_myuser_id = ${activeService?.idUser} " +
                        "ORDER BY r.review_date DESC LIMIT 5;",
                "review_five_from"
            )
        }
    }

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
            ProfilePhotoPicker(context, activeService!!, viewModel, modifier = Modifier)
//            Box(
//                modifier = Modifier
//                    .size(240.dp)
//                    .weight(1f)
//                    .border(
//                        BorderStroke(2.dp, Color.Gray),
//                        shape = CircleShape
//                    )
//
//                    .clip(CircleShape)
//                    .clickable {
//                        // Загрузка фото
//                        hasPhoto = true
//                    },
//                contentAlignment = Alignment.Center
//            ) {
//                if (hasPhoto) {
//                    AsyncImage(
//                        model = "https://yandex-images.clstorage.net/100H3bC16/f85172BwRKm5/JPmvU9pjeoYluQ7XILd3e0EZNrsE_PIY98Vz9f4BvdadMwHvPVXfA1FBYtw3rwLo2EclQheCwoAKdbwh8SEtmQCNa2fbto_SBH26F1AnNo_wzba_dTnSqYZ1BmGbhHhjzTWnXQpS7LSyVqDrIGuXaA0S9NPmhrE222L8VgQUaduGmS7yz4CKZYJaP5chhFadkKgmncR5IvhyNHchvNXPxCslRE2a8vwHEZTTGpIZpdtsI7WGF2TgFB_Qv0RH1qnIFKnO058SmlWDeu6Gg6XEPqGI9102m5DKwteGUCy2rOS4RrYOGgAfBqB0kanCuTZo3EOEVVbFsYe_I4xWx5ZLSiWJDSHM4VtmA0p8FvAlx1wxm4cvBgmA2IB0RoOflIw0OGQXnorDraZjx7Bp4gh0-74TpcY3Z7JWTlOM5SeWemgkiW4SzLE4R1BoPicAFaV-g3sm34YpMAhiViWSPLbvBJsmRy4q4ewUo5di-BJbJbtO0Bb0J7bTt6xALCa3JSpZVcnswv_DybUzawy3E8WE3qFaVTz2OlP4ADelIu1GrJZZdRTPmlIdVLJWQ9sgGOY6_xBkVjXl8ha_kj5kRTaay2UrbINuMMtlYjncZdPFpJ3zC6SNhOuTmhBmNgEOJF6UuXZ3rIiiXkSBhyEI4-qnqP-AFDZnFpJ3HABt18R2CHnkK30wvKFYl3ML3bTCBXSMsQh3nMYqoSiDJUWQbQcfFhpGBm1aMp1WwOYhGZHrNfpMUtQHRMcChjzi7OQXxGi6tSvuQX4Q6ZXAyA40goRH7uHpd-2EmhC581RUge2WXDS7lmQOSlL9Z9MmYIlziEQorHOX9Gdn0_d8E55kRIQ6abTZPkIMI5mVMDoMlZN19b2w6Cb9Veky-6CEZNJ_l61UCibm3HsyLjYgdOHqkyiny-xCdwY1JvKVrmPvN7WmyAin293iXYNKpyAq_HaSFmfeM-nEvGdYAirxRfZhM", // URL фото
//                        contentDescription = "Profile Photo",
//                        modifier = Modifier.fillMaxSize(),
//                        contentScale = ContentScale.Crop
//                    )
//                } else {
//                    Text("Загрузить фото", fontSize = 14.sp, color = Color.Gray)
//                }
//            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                TextButton(
                    onClick = { isFullScreenRepost = true },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .wrapContentHeight(),
                    colors = ButtonDefaults.textButtonColors(
                        backgroundColor = Color(0xFF6200EE), // фиолетовый фон
                        contentColor = Color.White          // белый текст
                    )
                ) {
                    Text(
                        text = "Поделиться профилем",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Card(
                    modifier = Modifier,
                    shape = MaterialTheme.shapes.medium,
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.onSurface,
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Моя средняя оценка",
                            style = MaterialTheme.typography.body1,
                            color = MaterialTheme.colors.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        RatingStars(
                            rating = rating,
                            modifier = Modifier
                                .clickable { showReviewsDialog = true }
                                .padding(4.dp)
                        )
                    }
                }

            }
        }
        ReviewsScreen(reviews, reviewsFrom)

    }
    if (showReviewsDialog) {
        ReviewsDialog(
            onDismiss = { showReviewsDialog = false },
            viewModel = viewModel  // <-- передаём тот же инстанс
        )
    }
    if (isFullScreenRepost) {
        val intent = Intent(context, ScreenRepost::class.java)
        context.startActivity(intent)
        isFullScreenRepost = false

    }

}
@Composable
fun ProfilePhotoPicker(
    context: Context,
    activeService: ActiveService,
    viewModel: MyViewModel,
    modifier: Modifier = Modifier
) {
    // SharedPrefs для хранения URI загруженной фотографии
    val prefs = remember { context.getSharedPreferences("profile_prefs", MODE_PRIVATE) }
    // Сохраняем строку URI
    var photoUriString by remember { mutableStateOf(prefs.getString("photo_uri", "") ?: "") }
    val photoUri: Uri? = photoUriString.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }

    // Диалог «Заменить фото»
    var showReplaceDialog by remember { mutableStateOf(false) }

    // Лаунчер для выбора изображения
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Читаем байты
            val photoBytes: ByteArray? = context.contentResolver.openInputStream(it)
                ?.use { it.readBytes() }
            photoBytes?.let { bytes ->
                // Отправляем "сырые" байты в ваш сервис
                //activeService.sendPhoto(bytes)
                Log.d(TAG, "Send photo")
                val previewDec = bytes.take(5).joinToString(", ")
                Log.d(TAG, "First 5 bytes (dec): [$previewDec]")
                activeService.sendCommandFromActivity("", "send_avatar", bytes)
//            val bytes = context.contentResolver.openInputStream(it)?.use { stream ->
//                stream.readBytes()
//            }
//            if (bytes != null) {
//                // Кодируем в base64
//                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
//                // Отправляем SQL-запрос на сервер: обновляем поле avatar_data
//                activeService.sendCommandFromActivity(base64, "send_avatar")
//            }
            }
        }
    }

    // Сам бокс
    Box(
        modifier = modifier
            .size(200.dp)  // квадрат
            .border(BorderStroke(2.dp, Color.Gray), shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                if (photoUri == null) {
                    // первый раз — сразу открыть проводник
                    launcher.launch("image/*")
                } else {
                    // уже есть фото — предложить заменить
                    showReplaceDialog = true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Profile Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text("Загрузить фото", fontSize = 14.sp, color = Color.Gray)
        }
    }

    // Диалог замены
    if (showReplaceDialog) {
        AlertDialog(
            onDismissRequest = { showReplaceDialog = false },
            title = { Text("Фото профиля") },
            text = { Text("Хотите заменить текущее фото?") },
            confirmButton = {
                TextButton(onClick = {
                    showReplaceDialog = false
                    launcher.launch("image/*")
                }) {
                    Text("Заменить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
data class UploadAvatarRequest(
    val command: String = "upload_avatar",
    val userId: Int,
    val avatarBase64: String
)

@Composable
fun RatingStars(rating: Float, modifier: Modifier = Modifier) {
    // Подсчитываем, сколько целых звёзд и нужен ли «полузвук»:
    val fullStars = rating.toInt().coerceIn(0, 5)
    val hasPartialStar = ((rating - fullStars) >= 0.01f) && (fullStars < 5)
    val emptyStars = 5 - fullStars - if (hasPartialStar) 1 else 0

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Полные звёзды
        repeat(fullStars) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700), // жёлтый
                modifier = Modifier.size(24.dp)
            )
        }
        // Одна «не целая» звезда (половинка)
        if (hasPartialStar) {
            Icon(
                imageVector = Icons.Default.StarHalf,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(24.dp)
            )
        }
        // Пустые звёзды
        repeat(emptyStars) {
            Icon(
                imageVector = Icons.Default.StarBorder,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ReviewsScreen(reviews: List<Review>, reviewsFrom: List<Review>) {
    var isReceivedSelected by remember { mutableStateOf(true) }

    Column(modifier = Modifier.padding(16.dp)) {
        // Заголовок
        Text(
            text = "Отзывы",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Кнопки переключения
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                onClick = { isReceivedSelected = true },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isReceivedSelected) MaterialTheme.colors.primary else Color.LightGray
                )
            ) {
                Text("Полученные")
            }

            Button(
                onClick = { isReceivedSelected = false },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (!isReceivedSelected) MaterialTheme.colors.primary else Color.LightGray
                )
            ) {
                Text("Отправленные")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val currentReviews = if (isReceivedSelected) reviews else reviewsFrom

        if (currentReviews.isEmpty()) {
            Text(
                text = "Отзывов еще нет",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            LazyColumn {
                items(currentReviews) { review ->
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "Пользователь: ${review.reviewer_name}")
                        Text(text = "Оценка: ${review.rating}")
                        Text(text = review.review ?: "Нет текста отзыва")
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewsDialog(
    onDismiss: () -> Unit,
    viewModel: MyViewModel
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val allReviews by viewModel.reviewsFlow.collectAsState()

    var sortType by remember { mutableStateOf(SortType.DATE_DESC) }

    Log.d("ReviewsDialog", "isLoading - $isLoading")
    // Запуск загрузки только если данные ещё не загружены
    LaunchedEffect(Unit) {
        if (!isLoading) {
            Log.d("ReviewsDialog", "Fetching reviews...")
            viewModel.fetchAllReviews()
        }
    }

    val sortedReviews = remember(allReviews, sortType) {
        sortReviews(allReviews, sortType)
    }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Все отзывы",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color.Gray
                        )
                    }
                }

                FiltersRow(sortType = sortType, onFilterChange = { newSortType ->
                    sortType = newSortType
                })

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Загрузка...")
                    }
                } else {
                    if(sortedReviews.isEmpty()){
                        Text(
                            text = "Отзывов еще нет",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }else{
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(sortedReviews) { review ->
                                ReviewItem(
                                    name = review.reviewer_name,
                                    rating = review.rating,
                                    date = review.review_date,
                                    text = review.review ?: "No review"
                                )
                                Divider()
                            }
                        }
                    }

                }
            }
        }
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
                androidx.compose.material3.Text(text = name, fontWeight = FontWeight.Bold)
                androidx.compose.material3.Text(text = date, fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            repeat(rating) {
                androidx.compose.material3.Icon(
                    painter = painterResource(id = android.R.drawable.btn_star_big_on),
                    contentDescription = "Star",
                    tint = Color.Yellow,
                    modifier = Modifier.size(16.dp)
                )
            }
            repeat(5 - rating) {
                androidx.compose.material3.Icon(
                    painter = painterResource(id = android.R.drawable.btn_star_big_off),
                    contentDescription = "Star",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.material3.Text(text = text)
    }
}

// Твой enum для разных типов сортировки
enum class SortType {
    DATE_DESC,  // Сначала новые
    DATE_ASC,   // Сначала старые
    RATING_DESC,// По убыванию рейтинга
    RATING_ASC  // По возрастанию рейтинга
}

fun sortReviews(reviews: List<Review>, sortType: SortType): List<Review> {
    return when (sortType) {
        SortType.DATE_DESC -> reviews.sortedByDescending { it.review_date }
        SortType.DATE_ASC -> reviews.sortedBy { it.review_date }
        SortType.RATING_DESC -> reviews.sortedByDescending { it.rating }
        SortType.RATING_ASC -> reviews.sortedBy { it.rating }
    }
}
@Composable
fun FiltersRow(
    sortType: SortType,
    onFilterChange: (SortType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Кнопка для сортировки по дате
        val isDateDesc = sortType == SortType.DATE_DESC
        val dateLabel = if (isDateDesc) "Дата ↓" else "Дата ↑"

        Button(
            onClick = {
                val newSort = if (isDateDesc) SortType.DATE_ASC else SortType.DATE_DESC
                onFilterChange(newSort)
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (sortType == SortType.DATE_DESC || sortType == SortType.DATE_ASC) Color.Gray else Color.LightGray
            )
        ) {
            Text(dateLabel)
        }

        // Кнопка для сортировки по рейтингу
        val isRatingDesc = sortType == SortType.RATING_DESC
        val ratingLabel = if (isRatingDesc) "Оценка ↓" else "Оценка ↑"

        Button(
            onClick = {
                val newSort = if (isRatingDesc) SortType.RATING_ASC else SortType.RATING_DESC
                onFilterChange(newSort)
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (sortType == SortType.RATING_DESC || sortType == SortType.RATING_ASC) Color.Gray else Color.LightGray
            )
        ) {
            Text(ratingLabel)
        }
    }
}
