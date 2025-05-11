package com.example.diplom.UImain

import com.example.diplom.R
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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

@Composable
fun ProfileScreen(context: Context, activeService: ActiveService?, viewModel: MyViewModel) {
    var hasPhoto by remember { mutableStateOf(false) }
    var showReviewsDialog by remember { mutableStateOf(false) }
    var isFullScreenRepost by remember { mutableStateOf(false) }
    val reviews by viewModel.reviewsFlow5.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.sendCommand(
            "SELECT r.rating, r.review, r.review_date, u.username AS reviewer_name " +
                    "FROM ratings r JOIN myusers u ON r.reviewer_myuser_id = u.user_id " +
                    "WHERE r.myuser_id = ${activeService?.idUser} " +
                    "ORDER BY r.review_date DESC LIMIT 5;", "review_five"
        )
        Log.d("ProfileScreen", "send command")
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
            Box(
                modifier = Modifier
                    .size(240.dp)
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
                Row {
                    IconButton(onClick = {
                        isFullScreenRepost = true
                    },
                        modifier = Modifier.size(64.dp)
                    ){
                        Icon(
                            painter = painterResource(id = R.drawable.placemark_icon),
                            contentDescription = "Поделиться",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        showReviewsDialog = true
                    }
                ) {
                    repeat(5) { index ->
                        Icon(
                            painter = painterResource(id = R.drawable.placemark_icon),
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
        if(reviews.isEmpty()){
            Text(
                text = "Отзывов еще нет",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }else{
            LazyColumn {
                items(reviews) { review ->
                    // Ваш UI-элемент для каждого отзыва
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "User: ${review.reviewer_name}")
                        Text(text = "Rating: ${review.rating}")
                        Text(text = review.review?: "No review")
                        Divider()
                    }
                }
            }
        }

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
                containerColor = if (sortType == SortType.DATE_DESC || sortType == SortType.DATE_ASC) Color.Gray else Color.LightGray
            )
        ) {
            Text(dateLabel)
        }

        // Кнопка для сортировки по рейтингу
        val isRatingDesc = sortType == SortType.RATING_DESC
        val ratingLabel = if (isRatingDesc) "Рейтинг ↓" else "Рейтинг ↑"

        Button(
            onClick = {
                val newSort = if (isRatingDesc) SortType.RATING_ASC else SortType.RATING_DESC
                onFilterChange(newSort)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (sortType == SortType.RATING_DESC || sortType == SortType.RATING_ASC) Color.Gray else Color.LightGray
            )
        ) {
            Text(ratingLabel)
        }
    }
}
