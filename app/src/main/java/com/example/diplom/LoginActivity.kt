package com.example.diplom

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Observer
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
private val TAG = "LoginActivity"
private var activeService: ActiveService? = null
private var isBound = false
class LoginActivity : ComponentActivity() {

    private val connectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive called with action: ${intent.action}")
            when (intent.action) {
                "ACTION_CONNECTION_SUCCESS" -> {
                    // Соединение успешно, выполните нужные действия
                    Log.d(TAG, "Connection successfully established.")
                    // Например, можно обновить UI или перейти на другой экран
                    runOnUiThread{
                        setContent {
                            WelcomeScreen(onLoginClick = {
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            })
                        }
                    }
                }
                "ACTION_CONNECTION_FAILED" -> {
                    // Соединение не удалось, выполните нужные действия
                    val errorMessage = intent.getStringExtra("error_message") ?: "Unknown error"
                    Log.d(TAG, "Connection failed: $errorMessage")
                    // Можно показать диалог или уведомить пользователя другим способом
                    runOnUiThread{
                        setContent{
                            Log.d(TAG, "create dialog menu")
                            RetryDialog(
                                errorMessage = errorMessage,
                                onRetry = {
                                    // Retry logic, such as restarting the service
                                    val intent = Intent(this@LoginActivity, ActiveService::class.java)
                                    //if (ActiveService.isRunning) {
                                     //   Log.d(TAG, "при нажатии на повтор сервис работает")
                                    //    stopService(intent)
                                    //}
                                    startService(intent)
                                },
                                onCancel = {
                                    // Handle cancel action
                                    // For example, finish the activity
                                    //if (ActiveService.isRunning) {
                                        Log.d(TAG, "при нажатии на отмену сервис работает")
                                        val intent = Intent(
                                            this@LoginActivity,
                                            ActiveService::class.java
                                        )
                                        stopService(intent)
                                   // }
                                    finish()
                                }
                            )
                        }
                    }
                }
                "ACTION_CHECK_INTERNET_CONNECTION" -> {
                    val message = intent.getStringExtra("message") ?: "Please check your internet connection."
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }



    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ActiveService.MyBinder
            activeService = binder.getService()
            isBound = true
            // Подписка на LiveData для получения ответа от сервиса
            activeService?.commandResponse?.observe(this@LoginActivity, Observer { response ->
                // Обработка ответа
                Log.d(TAG, "Received response: $response")
                // Можете обновить UI или выполнить другие действия
                //Toast.makeText(this@LoginActivity, "Response: $response", Toast.LENGTH_SHORT).show()
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            activeService = null
            isBound = false
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent{
            RequestLocationPermissionsScreen()
        }
        val serviceIntent = Intent(this, BackgroundServiceLocation::class.java)
        stopService(serviceIntent)
        //WorkManager.getInstance(this).cancelUniqueWork("OfflineWorker")
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction("ACTION_CONNECTION_SUCCESS")
            addAction("ACTION_CONNECTION_FAILED")
            addAction("ACTION_CHECK_INTERNET_CONNECTION")
        }
        registerReceiver(connectionReceiver, filter)
        var serviceIntent = Intent(this, BackgroundServiceLocation::class.java)
        stopService(serviceIntent)
        val intent = Intent(this, ActiveService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        startService(intent)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(connectionReceiver)
        // Отвязываем сервис
        if (isBound) {
            Log.d(TAG, "unbinding in onStop")
            unbindService(serviceConnection)
            isBound = false
        }
        // Останавливаем сервис, если он больше не используется
        val intent = Intent(this, ActiveService::class.java)
        stopService(intent)
    }
    fun sendCommandToService() {
        if (isBound) {
            activeService?.sendCommandFromActivity("Hello from Activity!")
        } else {
            Log.e(TAG, "Service is not bound yet!")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun showLoginScreen() {
        setContent {
            WelcomeScreen(onLoginClick = {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            })
        }
    }

}

@Composable
fun WelcomeScreen(onLoginClick: () -> Unit) {
    var isRegistering by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("df") }
    var password by remember { mutableStateOf("df") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Добро пожаловать",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isRegistering) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            label = { Text("Логин") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (isRegistering) {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Пароль еще раз") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Ошибка, если поля пустые или пароли не совпадают
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = {
                // Проверка на пустые поля
                if (login.isEmpty() || password.isEmpty() || (isRegistering && (name.isEmpty() || confirmPassword.isEmpty()))) {
                    errorMessage = "Пожалуйста, заполните все поля!"
                } else if (isRegistering && password != confirmPassword) {
                    errorMessage = "Пароли не совпадают!"
                } else {
                    errorMessage = "" // Сброс ошибки
                    if(isBound){
                        activeService?.sendCommandFromActivity("login ${login} pass ${password}")
                    }
                    onLoginClick() // логика для кнопки
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = if (isRegistering) "Создать" else "Войти")
        }

        Text(
            text = if (isRegistering) "Уже есть аккаунт?" else "Нет аккаунта?",
            modifier = Modifier
                .clickable { isRegistering = !isRegistering }
                .padding(top = 16.dp)
        )
    }
}

@Composable
fun RetryDialog(
    errorMessage: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {

    val openDialog = remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = {
                // Handle the dialog dismissal
                openDialog.value = false
            },
            title = {
                Text(text = "Connection Error")
            },
            text = {
                Text(text = "Please check your internet connection.")
            },
            confirmButton = {
                TextButton(onClick = {
                    openDialog.value = false
                    onRetry()
                    Log.d(TAG, "click on retry")
                    coroutineScope.launch {
                        delay(1000L) // 1 секунда
                        openDialog.value = true
                    }
                }) {
                    Text("Retry")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    openDialog.value = false
                    onCancel()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
@Composable
fun RequestLocationPermissionsScreen() {
    val context = LocalContext.current
    val permissionGranted = remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val showBackgroundLocationDialog = remember { mutableStateOf(false) }

    // Лаунчер для запроса разрешений
    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Обработка результата разрешений
        permissionGranted.value = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundLocationGranted = permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] == true
            if (!backgroundLocationGranted) {
                // Показываем диалог для запроса фонового разрешения
                showBackgroundLocationDialog.value = true
            }
        }
    }


    if (!permissionGranted.value) {
        // Запрашиваем разрешение на передний план
        LaunchedEffect(Unit) {
            requestPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Показываем диалог объяснения для фонового местоположения
                showBackgroundLocationDialog.value = true
            } else {
                // Запускаем сервис, если все разрешения уже предоставлены
                context.startService(Intent(context, ActiveService::class.java))
            }
        } else {
            // Для устройств ниже Android Q (до 10) просто запускаем сервис
            context.startService(Intent(context, ActiveService::class.java))
        }
    }

    // Отображаем диалог для пояснения необходимости фонового разрешения
    if (showBackgroundLocationDialog.value) {
        BackgroundLocationRationaleDialog(
            onDismiss = { showBackgroundLocationDialog.value = false },
            onConfirm = {
                // Запрашиваем разрешение на доступ к фоновому местоположению
                requestPermissionsLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                )
                showBackgroundLocationDialog.value = false
            }
        )
    }
}

@Composable
fun BackgroundLocationRationaleDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Доступ к фоновому местоположению")
        },
        text = {
            Text(text = "Для корректной работы приложения необходимо разрешение на доступ к местоположению в фоновом режиме. Это позволит приложению отслеживать ваше местоположение, даже если оно не активно.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Предоставить разрешение")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Отмена")
            }
        }
    )
}