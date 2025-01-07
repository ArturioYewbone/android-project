package com.example.diplom

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.location.LocationManagerCompat.getCurrentLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class BackgroundServiceLocation : Service() {
    private val TAG = "ForegroundService"
    private val CHANNEL_ID = "ForegroundServiceChannel"
    private lateinit var locationManager: LocationManager
    private var location: Location? = null
    private var location2: Location? = null
    private var location3: Location? = null
    private val lock = Object()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @Volatile
    private var isThreadRunning = false
    companion object {
        var isServiceRunning = false
    }
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ForegroundService created")
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ForegroundService started")
        // Проверка: если сервис уже запущен, не запускаем его заново
        if (isServiceRunning) {
            Log.d(TAG, "Service already running")
            return START_STICKY
        }

        // Устанавливаем флаг в true, чтобы отметить, что сервис запущен
        isServiceRunning = true
        isThreadRunning = true


        // Создаем уведомление
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("My Foreground Service")
            .setContentText("Service is running in the foreground")
            .setSmallIcon(R.drawable.placemark_icon)
            .setContentIntent(pendingIntent)
            .build()

        // Запуск сервиса как foreground с уведомлением
        startForeground(1, notification)

        // Фоновая задача
        Thread {
            while (isThreadRunning) {
                Log.d(TAG, "launch send")
                if (!sendToServer()){
                    Log.d(TAG, "Send fail")
                }
                Thread.sleep(60000) // Задержка в 1 секунду
            }
        }.start()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ForegroundService destroyed")
        isServiceRunning = false
        isThreadRunning  = false
    }

    private fun createNotificationChannel() {
        // Создание канала уведомлений для Android 8.0 и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
    private fun sendToServer() : Boolean{
        val serverIp = "82.179.140.18"  // IP-адрес сервера
        val serverPort = 44139          // Порт сервера
        return try {
            Log.d(TAG, "open socket")
            val socket = Socket()
            val socketAddress = InetSocketAddress(serverIp, serverPort)
            socket.connect(socketAddress, 20000)

            var message = "offline send"
            Log.d(TAG, message)
            val outputStream: OutputStream = socket.getOutputStream()
            outputStream.write(message.toByteArray())
            location = getCurrentLocation()
            message = "Service"
            Log.d(TAG, message)
            outputStream.write(message.toByteArray())
            if( location != null) {
                message = location.toString()
                Log.d(TAG, message)
                outputStream.write(message.toByteArray())
            }


            getLocationSynchronously()
            message = "GPS"
            outputStream.write(message.toByteArray())
            message = location2.toString()
            outputStream.write(message.toByteArray())
            getLocationSynchronously2()
            message = "Network"
            outputStream.write(message.toByteArray())
            message = location3.toString()
            outputStream.write(message.toByteArray())
            message = ".\n"
            outputStream.write(message.toByteArray())
            Thread.sleep(500)
            outputStream.close()
            socket.close()
            Log.d(TAG, "успешное закрытие сокета")
            true
        }catch (e : Exception){
            Log.e(TAG, "Error in backgroundService: ${e.message}")
            false
        }
    }
    private fun getCurrentLocation(): Location? {
        var currentLocation: Location? = null

        // Проверка разрешений перед запросом местоположения
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLocation = location
                    Log.d(TAG, "Успешно получено местоположение из сервиса")
                }
            }.addOnFailureListener {
                Log.d(TAG, "Ошибка при получении местоположения из сервиса")
            }

            // Ждем получения местоположения
            Thread.sleep(2000)
        } else {
            Log.d(TAG, "Нет разрешений на доступ к местоположению")
        }
        return currentLocation
    }
    private fun getLocationSynchronously() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Нет разрешений на доступ к местоположению (GPS)")
            return
        }
        val handlerThread = HandlerThread("LocationThread").apply { start() }
        val handler = Handler(handlerThread.looper)
        val latch = CountDownLatch(1)

        val locationListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                location2 = loc
                Log.d(TAG, "Получено местоположение: $loc")
                latch.countDown()  // Сигнал о получении местоположения
            }

            override fun onProviderDisabled(provider: String) {
                Log.d(TAG, "Провайдер отключен: $provider")
                latch.countDown()  // Чтобы не ждать дальше, если провайдер отключен
            }

            override fun onProviderEnabled(provider: String) {
                Log.d(TAG, "Провайдер включен: $provider")
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.d(TAG, "Статус провайдера изменен: $provider, статус: $status")
            }
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                locationListener,
                handler.looper
            )

            // Ждем, пока не получим местоположение или не истечет тайм-аут
            if (!latch.await(90, TimeUnit.SECONDS)) {
                Log.w(TAG, "Не удалось получить местоположение за отведенное время GPS")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении местоположения: ${e.message}")
        } finally {
            locationManager.removeUpdates(locationListener)
            handlerThread.quitSafely()
        }
    }
    private fun getLocationSynchronously2() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Нет разрешений на доступ к местоположению (GPS)")
            return
        }
        val handlerThread = HandlerThread("LocationThread").apply { start() }
        val handler = Handler(handlerThread.looper)
        val latch = CountDownLatch(1)

        val locationListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                location3 = loc
                Log.d(TAG, "Получено местоположение network: $loc")
                latch.countDown()  // Сигнал о получении местоположения
            }

            override fun onProviderDisabled(provider: String) {
                Log.d(TAG, "Провайдер отключен: $provider")
                latch.countDown()  // Чтобы не ждать дальше, если провайдер отключен
            }

            override fun onProviderEnabled(provider: String) {
                Log.d(TAG, "Провайдер включен: $provider")
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.d(TAG, "Статус провайдера изменен: $provider, статус: $status")
            }
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0L,
                0f,
                locationListener,
                handler.looper
            )

            // Ждем, пока не получим местоположение или не истечет тайм-аут
            if (!latch.await(90, TimeUnit.SECONDS)) {
                Log.w(TAG, "Не удалось получить местоположение за отведенное время Network")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении местоположения: ${e.message}")
        } finally {
            locationManager.removeUpdates(locationListener)
            handlerThread.quitSafely()
        }
    }
}