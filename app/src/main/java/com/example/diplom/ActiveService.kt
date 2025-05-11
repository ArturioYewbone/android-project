package com.example.diplom

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import kotlinx.coroutines.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.google.android.gms.location.*
import com.yandex.mapkit.geometry.Point
import org.json.JSONArray
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

class ActiveService : Service() {
    private val TAG = "ActiveService"
    private lateinit var locationManager: LocationManager
    private var socket: Socket? = null
    private var outputStreamWriter: OutputStreamWriter? = null
    private var bufferedReader: BufferedReader? = null
    private var inputStreamReader: InputStreamReader? = null
    private val serverIp = "192.168.0.59"  // Замените на ваш IP
    private val serverPort = 8080  // Замените на ваш порт
    private var lastKnownLocation: Point? = null
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    var idUser: Int = 0
//    private val _response = MutableStateFlow("")
//    val rresponse: StateFlow<String> = _response

    private val _responseFlow = MutableSharedFlow<String>(replay = 1)
    val responseFlow: SharedFlow<String> = _responseFlow

    // Свойство для привязки с активити
    private val binder = MyBinder()
    inner class MyBinder : Binder() {
        fun getService(): ActiveService = this@ActiveService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Active service created")
        // Инициализация FusedLocationProviderClient для получения местоположения
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Инициализация LocationCallback для получения обновлений местоположения
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0.let {
                    for (location in it.locations) {
                        Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
                        lastKnownLocation = Point(location.latitude, location.longitude) // Сохраняем последнее местоположение
                        sendLocationToServer() // Отправляем местоположение на сервер
                    }
                }
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Attempting to open socket")
                socket = Socket(serverIp, serverPort)
                Log.d(TAG, "Socket successfully created: ${socket!!.isConnected}")
                outputStreamWriter = OutputStreamWriter(socket!!.getOutputStream())
                inputStreamReader = InputStreamReader(socket!!.getInputStream())
                bufferedReader = BufferedReader(inputStreamReader)
                val intent = Intent("ACTION_CONNECTION_SUCCESS")
                sendBroadcast(intent)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to open socket: ${e.message}", e)
                val intent = Intent("ACTION_CONNECTION_FAILED")
                intent.putExtra("error_message", "Failed to open socket: ${e.message}")
                sendBroadcast(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error: ${e.message}", e)
                val intent = Intent("ACTION_CONNECTION_FAILED")
                intent.putExtra("error_message", "Unexpected error: ${e.message}")
                sendBroadcast(intent)
            }

            // Запускаем задачи на периодическое обновление местоположения и отправку команд
            executor.scheduleAtFixedRate({
                sendLocationToServer()
                // Вы можете добавить другие команды, которые нужно отправить периодически
                //sendCommandFromActivity("Some Command")
            }, 0, 3000, TimeUnit.SECONDS) // Интервал обновления: каждые 10 секунд
        }
    }


    // Публичный метод для получения текущего местоположения
    fun getLocation(): Point? {
        // Проверка разрешений
        Log.d(TAG, "отправка локации в активити")
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "разрешения есть")
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lastKnownLocation = Point(location.latitude, location.longitude)
                    Log.d(TAG, "Last known location: ${location.latitude}, ${location.longitude}")
                } else {
                    Log.d(TAG, "Location not available")
                }
            }
            return lastKnownLocation
        } else {
            Log.e(TAG, "Location permission not granted")
            return null
        }
    }

    // Публичный метод для отправки команд из активити
    fun sendCommandFromActivity(command: String, typeSql: String) {
        coroutineScope.launch {
            val response = sendToServer(command, typeSql)
            _responseFlow.emit(response?:"")
            //_response.value = response ?: ""
            Log.d(TAG, "Response from server: $response")
        }
    }
    private suspend fun sendToServer(command: String, typeSql: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                if(socket == null){Log.d(TAG, "socket null")}
                if(socket!!.isClosed){Log.d(TAG, "socket close")}
                if (socket == null || socket!!.isClosed) {
                    Log.e(TAG, "Socket is closed or null, reopening...")
                    // Переоткрытие сокета
                    socket = Socket(serverIp, serverPort)
                    outputStreamWriter = OutputStreamWriter(socket!!.getOutputStream())
                    inputStreamReader = InputStreamReader(socket!!.getInputStream())
                    bufferedReader = BufferedReader(inputStreamReader)
                }
                var requestData = RequestData("sql", typeSql, command)
                if(command[0] =='1'){
                    val commandLogin = command.substring(1)
                    requestData = RequestData("sql_login", typeSql, commandLogin)
                }else if(command[0] == '2'){
                    val commandLogin = command.substring(1)
                    requestData = RequestData("sql_login", typeSql, commandLogin)
                }


                // Сериализуем объект в JSON с помощью Gson
                val gson = Gson()
                val json = gson.toJson(requestData)

                // Отправляем JSON
                outputStreamWriter?.write("$json\n")
                outputStreamWriter?.flush()
                Log.d(TAG, "JSON sent in sendToServer: $json")
                val response = bufferedReader?.readLine()
                Log.d(TAG, "Server response: $response")
                if(command.startsWith("SELECT user_id" ) && !response.isNullOrEmpty()){
                    try {
                        // Преобразуем строку в JSON-массив
                        val jsonObject = JSONObject(response)
                        val dataArray = jsonObject.getJSONArray("data")
                        // Берем первый объект из массива
                        if (dataArray.length() > 0) {
                            // Берем первый объект из массива
                            val userData = dataArray.getJSONObject(0)

                            // Извлекаем user_id и присваиваем переменной
                            idUser = userData.getInt("user_id")
                            Log.d(TAG, "Parsed user_id: $idUser")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user_id: ${e.message}")
                    }
                }
                response
            } catch (e: Exception) {
                Log.e(TAG, "Error sending command: ${e.message}")
                null
            }
        }
    }
    //CoroutineScope(Dispatchers.IO).launch {
    private fun sendLocationToServer() {

        coroutineScope.launch {
            if (ActivityCompat.checkSelfPermission(
                    this@ActiveService,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "разрешения есть")
                // Получаем последнее известное местоположение
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    // Если местоположение найдено, сохраняем его в переменную
                    if (location != null) {
                        lastKnownLocation = Point(location.latitude, location.longitude)
                    }
                    Log.d(TAG, "Last known location: ${location?.latitude}, ${location?.longitude}")
                    val data = """
                    UPDATE myusers
                    SET latitude = ${location?.latitude},       
                        longitude = ${location?.longitude},
                        last_login = CURRENT_TIMESTAMP 
                    WHERE user_id = $idUser;    
                """.trimIndent()
                    Log.d(TAG, data)
                    sendDataSafely(data)
                }
            }
        }
    }
    data class RequestData(
        val type: String,
        val typeSql:String,
        val command: String
    )
    private fun sendDataSafely(data: String) {
        coroutineScope.launch {
            outputStreamWriter?.let {
                synchronized(it) {
                    try {

                        // Создаём объект с SQL запросом и данными
                        val requestData = RequestData("sql", "", data)
                        Log.d(TAG, requestData.toString())
                        // Сериализуем объект в JSON с помощью Gson
                        val gson = Gson()
                        val json = gson.toJson(requestData)
                        Log.d(TAG, json)
                        // Отправляем JSON
                        it.write("$json\n")
                        it.flush()

                        Log.d(TAG, "Sent JSON in sendDataSafely: $json")
                        val response = bufferedReader?.readLine()
                        Log.d(TAG, "Server response in sendDataSafely: $response")
                    } catch (e: Exception) {
                        Log.d(TAG, "Error in sendDataSafely\n${e.message}")
                        e.printStackTrace()
                        Log.e(TAG, "Error in sendDataSafely: ${e.message}", e)
                    }
                }
            }
        }

    }
    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }
    override fun onDestroy() {
        super.onDestroy()
        try {
            socket?.close()  // Закрытие сокета при уничтожении сервиса
        } catch (e: IOException) {
            Log.e(TAG, "Failed to close socket: ${e.message}")
        }
        executor.shutdown()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d(TAG, "Service destroyed")
    }
}
