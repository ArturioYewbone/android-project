package com.example.diplom

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.activity.compose.setContent
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

class ActiveService : Service(), LocationListener {
    private val TAG = "ActiveService"
    private lateinit var locationManager: LocationManager
    private var socket: Socket ? = null
    private var outputStreamWriter: OutputStreamWriter? = null
    private var bufferedReader: BufferedReader?= null
    private var inputStreamReader:InputStreamReader?= null
    private var send = false
    private var stringArray: MutableList<String> = mutableListOf()

    private val serverIp = "82.179.140.18"  // Замените на ваш IP
    private val serverPort = 44139  // Замените на ваш порт

    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

    private var lastKnownLocation: Location? = null
    private var isMoving = false
    private var distanceThreshold = 10f // Порог в метрах для определения движения
    private var updateIntervalMoving = 10 * 1000L // 10 секунд
    private var updateIntervalStationary = 60 * 1000L // 60 секунд

    private val binder = MyBinder()

    inner class MyBinder : Binder() {
        fun getService(): ActiveService = this@ActiveService
    }
    companion object {
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("ActiveService", "create active service")
        try {
            Log.d("ActiveService", "Attempting to open socket")
            socket = Socket(serverIp, serverPort)

            Log.d("ActiveService", "Socket successfully created: ${socket!!.isConnected}")
            outputStreamWriter = OutputStreamWriter(socket!!.getOutputStream())
            inputStreamReader = InputStreamReader(socket!!.getInputStream())
            bufferedReader = BufferedReader(inputStreamReader)
        }catch (e: Exception) {
            Log.e("ActiveService", "Failed to open socket: ${e.message}", e)
            val intent = Intent("ACTION_CONNECTION_FAILED")
            intent.putExtra("error_message", "Failed to open socket: ${e.message}")
            sendBroadcast(intent)

        }
        isRunning = true
        executor.execute {
            Thread {
                try {
                    locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

                    try {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            1000L,
                            1f,
                            this,
                            Looper.getMainLooper()
                        )
                    } catch (e: SecurityException) {
                        Log.e("ActiveService", "Permission not granted: ${e.message}", e)
                        stopSelf()
                    }
                    val intent = Intent("ACTION_CONNECTION_SUCCESS")
                    sendBroadcast(intent)

                    // Запускаем планировщик, который будет отправлять местоположение каждую минуту
                    scheduledExecutor.scheduleAtFixedRate({
                        lastKnownLocation?.let {
                            //sendLocationToServer(it)
                            sendDataSafely("latitude=${it.latitude}&longitude=${it.longitude}", false)
                        }
                    }, 0, 10, TimeUnit.SECONDS)
                } catch (e: Exception) {
                    Log.e("ActiveService", "Failed : ${e.message}", e)

                }
            }.start()
        }
    }

    override fun onLocationChanged(location: Location) {
        Log.d("ActiveService", "Location: ${location.latitude}, ${location.longitude}")
        Log.d("ActiveService", "Location: ${location}")

        // Сохраняем последнее известное местоположение
        lastKnownLocation = location

        // Передача местоположения в активити
        val intent = Intent("LOCATION_UPDATE")
        intent.putExtra("latitude", location.latitude)
        intent.putExtra("longitude", location.longitude)
        sendBroadcast(intent)
    }

    public fun sendToServer(array: ArrayList<String>): String?{
        Log.d(TAG, "отправка на сервер из активити")
        var serverResponse: String? = null
        runBlocking {
            // Запускаем корутину
            serverResponse = withContext(Dispatchers.IO) {
                try {
                    val gson = Gson()
                    val json = gson.toJson(array)
                    val printWriter = PrintWriter(outputStreamWriter, true)

                    printWriter.println(json)
                    Log.d(TAG, "JSON отправлен: $json")

                    val response = bufferedReader?.readLine()
                    Log.d(TAG, "Ответ от сервера: $response")

                    printWriter.close()
                    response
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при отправке JSON: ${e.message}", e)
                    null
                }
            }
        }
        return serverResponse
    }
    private fun sendDataSafely(data: String, isJson: Boolean) {
        outputStreamWriter?.let {
            synchronized(it) {
                try {
                    if(isJson){

                    }
                    it.write("$data\n")
                    it.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    private fun sendLocationToServer(location: Location) {
        try {
            send = true
            val data = "latitude=${location.latitude}&longitude=${location.longitude}"
            outputStreamWriter?.write(data)
            outputStreamWriter?.flush()

            Log.d("ActiveService", "Data sent: $data")
            send = false
        } catch (e: IOException) {
            if (e.message?.contains("Broken pipe") == true) {
                Log.e("ActiveService", "Broken pipe error: ${e.message}")
                // Здесь вы можете отправить уведомление или показать сообщение пользователю
                val intent = Intent("ACTION_CHECK_INTERNET_CONNECTION")
                intent.putExtra("message", "Please check your internet connection.")
                sendBroadcast(intent)
            } else {
                Log.e("ActiveService", "Failed to send location: ${e.message}")
            }
            send = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onDestroy() {
        Log.d("ActiveService", "onDestroy вызван в активном сервисе")
        super.onDestroy()
        executor.execute {
            // Закрываем соединение при остановке сервиса
            try {
                outputStreamWriter?.close()
                inputStreamReader?.close()
                socket?.close()
            } catch (e: Exception) {
                Log.e("ActiveService", "Failed to close socket: ${e.message}")
            }
        }
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(this)
        }
        executor.shutdown()
        scheduledExecutor.shutdown()
        isRunning = false
        unregisterReceiver(arrayReceiver)

    }
    private val arrayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "TEST" -> {
                    var l = intent.getStringArrayListExtra("string_array")
                    if (l != null) {
                        Log.d(TAG, "test suc")
                    }
                }
                "GET_API_FOR_MAP" -> {
                    var l = intent.getStringArrayListExtra("string_array")
                    if (l != null) {
                        var res = sendToServer(l)
                    }
                }
            }
        }
    }
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
