package com.example.diplom

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Path
import kotlinx.coroutines.delay
import kotlin.random.Random

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplashScreen()
        }
    }
}

@Composable
fun SplashScreen() {
    val context = LocalContext.current
    // Задержка перед переходом к другой активности
    LaunchedEffect(Unit) {
        delay(1) // Задержка в 2 секунды
        context.startActivity(Intent(context, LoginActivity::class.java))
        // Завершить текущую активность, чтобы пользователь не мог вернуться к SplashScreen
        (context as? ComponentActivity)?.finish()
    }
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val rotationAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black, Color(0xFF001F3F)) // Темный градиент
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Далекие звезды
        Canvas(modifier = Modifier.fillMaxSize()) {
            val starCount = 100
            repeat(starCount) {
                val x = Random.nextFloat() * size.width // Генерация случайного числа для x
                val y = Random.nextFloat() * size.height // Генерация случайного числа для y
                drawCircle(
                    color = Color.White.copy(alpha = Random.nextFloat() * 0.05f + 0.9f), // Генерация случайной прозрачности от 0.1 до 0.3
                    radius = Random.nextFloat() * 1.5f + 1.5f, // Генерация случайного радиуса от 1 до 3
                    center = Offset(x, y)
                )
            }
        }

        // Пульсирующая звезда
        Canvas(
            modifier = Modifier
                .size(200.dp * pulseAnimation) // Вытянутая звезда
                .offset(y = (-200).dp)
        ) {
            val path = Path().apply {
                val outerRadius = size.minDimension / 2
                val innerRadius = outerRadius / 2.5f
                val centerX = size.width / 2
                val centerY = size.height / 2
                val angle = (Math.PI * 2) / 5

                moveTo(
                    centerX + outerRadius * kotlin.math.cos(0.0).toFloat(),
                    centerY - outerRadius * kotlin.math.sin(0.0).toFloat()
                )

                for (i in 1 until 10) {
                    val radius = if (i % 2 == 0) outerRadius else innerRadius
                    lineTo(
                        centerX + radius * kotlin.math.cos(angle * i).toFloat(),
                        centerY - radius * kotlin.math.sin(angle * i).toFloat()
                    )
                }
                close()
            }
            rotate(degrees = -18f, pivot = Offset(size.width / 2, size.height / 2)) {


                drawPath(
                    path = path,
                    color = Color(0xFFFFD700) // Теплый золотистый цвет
                )
            }

            drawIntoCanvas { canvas ->
                rotate(rotationAnimation -18f) { // Поворачиваем звезду на 180 градусов
                    drawPath(
                        path = path,
                        color = Color(0xFFFFD700).copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Название приложения
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 50.dp)
        ) {
            Text(
                text = "Звездный Пульс",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.background(Color.Transparent)
            )

            Text(
                text = "Ваш социальный ритм среди звезд",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.background(Color.Transparent).padding(top = 8.dp)
            )
        }
    }
}
