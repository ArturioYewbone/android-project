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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.yandex.mapkit.geometry.Point

class ActiveService : Service(), LocationListener {
    private val TAG = "ActiveService"
    private lateinit var locationManager: LocationManager
    private var socket: Socket? = null
    private var outputStreamWriter: OutputStreamWriter? = null
    private var bufferedReader: BufferedReader? = null
    private var inputStreamReader: InputStreamReader? = null
    private val serverIp = "192.168.0.59"  // Замените на ваш IP
    private val serverPort = 8080  // Замените на ваш порт
    private var lastKnownLocation: Location? = null
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // LiveData для отправки ответа в активити
    private val _commandResponse = MutableLiveData<String>()
    val commandResponse: LiveData<String> get() = _commandResponse

    // Свойство для привязки с активити
    private val binder = MyBinder()

    inner class MyBinder : Binder() {
        fun getService(): ActiveService = this@ActiveService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Active service created")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
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
            }, 0, 30, TimeUnit.SECONDS) // Интервал обновления: каждые 10 секунд
        }
    }

    // Публичный метод для отправки команд из активити
    fun sendCommandFromActivity(command: String) {
        coroutineScope.launch {
            val response = sendToServer(command)
            _commandResponse.postValue(response ?: "No response from server") // Отправляем ответ через LiveData
            Log.d(TAG, "Response from server: $response")
        }
    }
    // Публичный метод для получения текущего местоположения
    fun getLocation(): Location? {
        // Проверка разрешений
        Log.d(TAG, "отправка локации в активити")
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "разрешения есть")
            if(lastKnownLocation == null) {

                // Получаем последнее известное местоположение
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    // Если местоположение найдено, сохраняем его в переменную
                    lastKnownLocation = location
                    Log.d(TAG, "Last known location: ${location?.latitude}, ${location?.longitude}")
                }
            }
            return lastKnownLocation
        } else {
            Log.e(TAG, "Location permission not granted")
            return null
        }
    }

    private suspend fun sendToServer(command: String): String? {
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
                val gson = Gson()
                val json = gson.toJson(command)
                val printWriter = PrintWriter(outputStreamWriter, true)
                printWriter.println(json)
                Log.d(TAG, "JSON sent: $json")

                val response = bufferedReader?.readLine()
                Log.d(TAG, "Server response: $response")


                response
            } catch (e: Exception) {
                Log.e(TAG, "Error sending command: ${e.message}")
                null
            }
        }
    }

    private fun sendLocationToServer() {
        if (lastKnownLocation == null) {
            // Если местоположение пустое, запросим его
            try {
                val locationProvider = LocationManager.GPS_PROVIDER
                locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    // Получаем последнее известное местоположение
                    lastKnownLocation = locationManager.getLastKnownLocation(locationProvider)

                    // Запрашиваем обновление местоположения
                    locationManager.requestSingleUpdate(locationProvider, object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            lastKnownLocation = location
                            val data = "latitude=${location.latitude}&longitude=${location.longitude}"
                            sendDataSafely(data, false)
                            Log.d(TAG, "Location updated and sent: $data")
                        }

                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

                        override fun onProviderEnabled(provider: String) {}

                        override fun onProviderDisabled(provider: String) {}
                    }, Looper.getMainLooper())
                } else {
                    Log.e(TAG, "Location permission not granted")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request location: ${e.message}")
            }
        } else {
            Log.d(TAG, "lastKnownLocation not null")
            // Если местоположение уже известно, отправляем его
            lastKnownLocation?.let {
                val data = "latitude=${it.latitude}&longitude=${it.longitude}"
                sendDataSafely(data, false)
                Log.d(TAG, "Location sent: $data")
            }
        }
    }


    private fun sendDataSafely(data: String, isJson: Boolean) {
        outputStreamWriter?.let {
            synchronized(it) {
                try {
                    it.write("$data\n")
                    it.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
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
        Log.d(TAG, "Service destroyed")
    }
    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "Location changed: ${location.latitude}, ${location.longitude}")
        lastKnownLocation = location
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {    }

    override fun onProviderEnabled(provider: String) {    }

    override fun onProviderDisabled(provider: String) {}
}
