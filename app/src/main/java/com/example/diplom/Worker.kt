package com.example.diplom

import android.Manifest
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Duration
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class Worker (appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private lateinit var locationManager: LocationManager
    private var location: Location? = null
    private var location2: Location? = null
    private var location3: Location? = null
    private val lock = Object()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun doWork(): Result {
        Log.d("WorkerBackGround", "launch Worker")
        locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        // Получаем текущее время
        val currentTime = System.currentTimeMillis()


        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Запросите разрешения у пользователя
            Log.d("WorkerBackGround", "Нет разрешений на местоположение")
        }
        val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGPSEnabled && !isNetworkEnabled) {
            Log.w("WorkerBackGround", "Ни один из провайдеров местоположения не доступен")
        }
        // Логика отправки времени на сервер
        val isSuccess = sendToServer(currentTime)
        return if (isSuccess) {
            Log.d("WorkerBackGround", "Worker result.success")
            scheduleNextWork()
            Result.success()
        } else {
            // В случае неудачи, пробуем снова
            Log.d("WorkerBackGround", "Worker result.retry")
            Result.retry()
        }
    }
    private fun scheduleNextWork() {
        val constraints = Constraints.Builder()
            .setRequiresDeviceIdle(false) // Не требовать простоя устройства
            .setRequiresBatteryNotLow(true) // Не запускать, если батарея разряжена
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<Worker>()
            .setConstraints(constraints)
            .setInitialDelay(Duration.ofSeconds(30))  // Задержка в 1 минуту
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "Worker", // Уникальное имя для работы
            ExistingWorkPolicy.APPEND, // Заменить старую работу новой
            workRequest
        )
    }

    private fun sendToServer(time: Long): Boolean {
        // Ваша логика отправки времени на сервер
        val serverIp = "82.179.140.18"  // IP-адрес сервера
        val serverPort = 44139          // Порт сервера
        Log.d("WorkerBackGround", "offline send")
        return try{

//            if (location == null) {
//                Log.e("WorkerBackGround", "Не удалось получить местоположение")
//                return false
//            }



//                val location = getCurrentLocation()
//                if (location == null) {
//                    Log.e("WorkerBackGround", "Ошибка: не удалось получить местоположение")
//                    return@withContext false
//                }
            val socket = Socket()
            val socketAddress = InetSocketAddress(serverIp, serverPort)
            socket.connect(socketAddress, 20000)

            var message = "offline send"
            val outputStream: OutputStream = socket.getOutputStream()
            outputStream.write(message.toByteArray())
//            location = getCurrentLocation() ?: return false
//            message = "Service"
//            outputStream.write(message.toByteArray())
//            message = location.toString()
//            outputStream.write(message.toByteArray())
//
//            getLocationSynchronously()
//            message = "GPS"
//            outputStream.write(message.toByteArray())
//            message = location2.toString()
//            outputStream.write(message.toByteArray())
//            getLocationSynchronously2()
//            message = "Network"
//            outputStream.write(message.toByteArray())
//            message = location3.toString()
//            outputStream.write(message.toByteArray())
//            message = "\n"
//            outputStream.write(message.toByteArray())
            outputStream.close()
            socket.close()
            Log.d("WorkerBackGround", "успешное закрытие сокета")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("WorkerBackGround", "Ошибка при отправке времени на сервер: ${e.message}", e)
            false
        }

    }
    private fun getCurrentLocation(): Location? {
        var currentLocation: Location? = null
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLocation = location
                Log.d("WorkerBackGround", "успешно получено местоположение из сервиса")
            }
        }.addOnFailureListener {
            Log.d("WorkerBackGround", "ошибка при получении местоположения из сервиса")
            // Handle the error
        }
        // Ожидание получения местоположения
        Thread.sleep(2000)
        return currentLocation
    }
    private fun getLocationSynchronously() {
        val handlerThread = HandlerThread("LocationThread").apply { start() }
        val handler = Handler(handlerThread.looper)
        val latch = CountDownLatch(1)

        val locationListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                location2 = loc
                Log.d("WorkerBackGround", "Получено местоположение: $loc")
                latch.countDown()  // Сигнал о получении местоположения
            }

            override fun onProviderDisabled(provider: String) {
                Log.d("WorkerBackGround", "Провайдер отключен: $provider")
                latch.countDown()  // Чтобы не ждать дальше, если провайдер отключен
            }

            override fun onProviderEnabled(provider: String) {
                Log.d("WorkerBackGround", "Провайдер включен: $provider")
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.d("WorkerBackGround", "Статус провайдера изменен: $provider, статус: $status")
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
            if (!latch.await(60, TimeUnit.SECONDS)) {
                Log.w("WorkerBackGround", "Не удалось получить местоположение за отведенное время GPS")
            }

        } catch (e: Exception) {
            Log.e("WorkerBackGround", "Ошибка при получении местоположения: ${e.message}")
        } finally {
            locationManager.removeUpdates(locationListener)
            handlerThread.quitSafely()
        }
    }
    private fun getLocationSynchronously2() {
        val handlerThread = HandlerThread("LocationThread").apply { start() }
        val handler = Handler(handlerThread.looper)
        val latch = CountDownLatch(1)

        val locationListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                location3 = loc
                Log.d("WorkerBackGround", "Получено местоположение network: $loc")
                latch.countDown()  // Сигнал о получении местоположения
            }

            override fun onProviderDisabled(provider: String) {
                Log.d("WorkerBackGround", "Провайдер отключен: $provider")
                latch.countDown()  // Чтобы не ждать дальше, если провайдер отключен
            }

            override fun onProviderEnabled(provider: String) {
                Log.d("WorkerBackGround", "Провайдер включен: $provider")
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.d("WorkerBackGround", "Статус провайдера изменен: $provider, статус: $status")
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
            if (!latch.await(60, TimeUnit.SECONDS)) {
                Log.w("WorkerBackGround", "Не удалось получить местоположение за отведенное время Network")
            }

        } catch (e: Exception) {
            Log.e("WorkerBackGround", "Ошибка при получении местоположения: ${e.message}")
        } finally {
            locationManager.removeUpdates(locationListener)
            handlerThread.quitSafely()
        }
    }
}