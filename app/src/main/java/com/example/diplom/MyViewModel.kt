package com.example.diplom

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cache
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.sql.Time
import java.time.LocalDate

data class Review(
    val reviewer_name: String,
    val rating: Int,
    val review: String?,
    val review_date: String
)
data class InfoPeopleOnMap(
    val username: String,
    val latitude: Double,
    val longitude: Double,
    val last_login: String,
    val user_id: Int
)
data class InfoAboutUser(
    val username: String?,
    val last_login: String?
)
class MyViewModel(private val activeService: ActiveService) : ViewModel() {
    private val TAG = "MyViewModel"
    //private val _reviewsFlow = MutableStateFlow<List<Review>>(emptyList())
    //val reviewsFlow: StateFlow<List<Review>> = _reviewsFlow
    private val _reviewsFlow = MutableStateFlow<List<Review>>(emptyList())
    val reviewsFlow: StateFlow<List<Review>> = _reviewsFlow
    private val _reviewsFlow5 = MutableStateFlow<List<Review>>(emptyList())
    val reviewsFlow5: StateFlow<List<Review>> = _reviewsFlow5
    private val _infoPeopleOnMap = MutableStateFlow<List<InfoPeopleOnMap>>(emptyList())
    val infoPeopleOnMap: StateFlow<List<InfoPeopleOnMap>> = _infoPeopleOnMap
    // Состояние загрузки
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingInfo = MutableStateFlow(false)
    val isLoadingInfo: StateFlow<Boolean> = _isLoadingInfo

    private val _infoAboutUser = MutableStateFlow<InfoAboutUser>(InfoAboutUser(null, null))
    val infoAboutUser: StateFlow<InfoAboutUser> = _infoAboutUser

    // Запрашиваем все отзывы (пример)
    fun fetchAllReviews() {
        Log.d(TAG, "запрос на все отзывы")
        _isLoading.value = true
        // Твой SQL-запрос (можно менять как нужно)
        val query = """
            SELECT r.rating, 
                   r.review, 
                   r.review_date, 
                   u.username AS reviewer_name 
            FROM ratings r 
            JOIN myusers u ON r.reviewer_myuser_id = u.user_id
            WHERE r.myuser_id = ${activeService.idUser}
            ORDER BY r.review_date DESC
        """.trimIndent()
        sendCommand(query, "allReviews")
    }

    fun getPeopleOnMap(){
        Log.d(TAG, "запрос на всех пользователей для карты")
        _isLoading.value = true
        val query = """
        WITH user_location AS (
            SELECT latitude, longitude, visibility
            FROM myusers
            WHERE user_id = ${activeService.idUser})
        SELECT DISTINCT u.user_id, u.username, u.latitude, u.longitude, u.last_login
        FROM myusers u
        LEFT JOIN friends f ON (u.user_id = f.friend_id OR u.user_id = f.user_id)
        WHERE 
            u.user_id <> ${activeService.idUser}
            AND (((SELECT visibility FROM user_location) = TRUE
                    AND u.visibility = TRUE
                    AND (6371 * acos(cos(radians((SELECT latitude FROM user_location))) 
                            * cos(radians(u.latitude)) 
                            * cos(radians(u.longitude) - radians((SELECT longitude FROM user_location))) 
                            + sin(radians((SELECT latitude FROM user_location))) 
                            * sin(radians(u.latitude)))) * 1000 <= 1000)         
                OR ((SELECT visibility FROM user_location) = FALSE
                    AND (f.user_id = ${activeService.idUser} OR f.friend_id = ${activeService.idUser})
                ));
        """.trimIndent()
        sendCommand(query, "getPeopleOnMap")
    }

    fun getInfoAboutUser(idUser: Int){
        Log.d(TAG, "запрос на получение инфо по пользавателю")
        _isLoadingInfo.value = true
        val query = """
        select username, last_login
        from myusers 
        where user_id = $idUser;
        """.trimIndent()
        sendCommand(query, "getInfoAboutUser")
    }
    fun sendingReview(review: String, rating:Int, user_id:Int){
        val cmd="""
            INSERT INTO ratings (rating, review, review_date, reviewer_myuser_id, myuser_id)
            VALUES (${rating}, '${review}', CURRENT_TIMESTAMP, ${activeService.idUser}, ${user_id});
        """.trimIndent()
        sendCommand(cmd, "send_review")
    }
    fun sendCommand(command: String, typeSQL: String) {
        activeService.sendCommandFromActivity(command, typeSQL)
    }
    private var isSubscribed = false

    init {
        if (!isSubscribed) {
            subscribeToService()
            isSubscribed = true
        }
    }
    private fun subscribeToService() {
        // Собираем ответы из сервиса
        viewModelScope.launch {
            activeService.responseFlow.collect { serverResponse ->
                try {
                    Log.d(TAG, "Received response: $serverResponse")
                    // Преобразуем серверный ответ в объект
                    val jsonObject = JSONObject(serverResponse)
                    val status = jsonObject.getString("status") // Получаем статус ответа
                    val typeSQL = jsonObject.getString("typeSQL") // Получаем значение typeSQL
                    val data = jsonObject.getJSONArray("data") // Данные в ответе
                    Log.d(TAG, "Parsed data: ${data}")
                    Log.d(TAG, "typeSQL-$typeSQL")
                    when (typeSQL){
                        "review_five"->{
                            val listType = object : TypeToken<List<Review>>() {}.type
                            val reviews: List<Review> = Gson().fromJson(data.toString(), listType)
                            if (reviews.isNotEmpty()) {
                                Log.d(TAG, "No reviews found.")
                                _reviewsFlow5.value = reviews
                            }else{
                                _reviewsFlow5.value = emptyList()
                            }
                        }
                        "allReviews"->{
                            val listType = object : TypeToken<List<Review>>() {}.type
                            val reviews: List<Review> = Gson().fromJson(data.toString(), listType)
                            if (reviews.isEmpty()) {
                                Log.d(TAG, "No reviews found.")
                                _reviewsFlow.value = emptyList()
                            } else {
                                _reviewsFlow.value = reviews
                            }
                        }
                        "getPeopleOnMap"->{
                            val listType = object : TypeToken<List<InfoPeopleOnMap>>() {}.type
                            val reviews: List<InfoPeopleOnMap> = Gson().fromJson(data.toString(), listType)
                            if (reviews.isEmpty()) {
                                Log.d(TAG, "No reviews found.")
                                _infoPeopleOnMap.value = emptyList()
                            } else {
                                _infoPeopleOnMap.value = reviews
                                Log.d(TAG, "получено в виде списка: $reviews")
                            }
                        }
                        "getInfoAboutUser"->{
                            if (data.length() > 0) {
                                val dataObject = data.getJSONObject(0).toString() // Преобразуем объект в строку

                                val user: InfoAboutUser = Gson().fromJson(dataObject, InfoAboutUser::class.java)
                                _infoAboutUser.value = user
                                Log.d(TAG, "получено в виде списка: $user")
                            } else {
                                Log.e(TAG, "Data array is empty")
                            }
//                            val listType = object : TypeToken<InfoAboutUser>() {}.type
//                            val reviews: InfoAboutUser = Gson().fromJson(data, listType)
//
//                            _infoAboutUser.value = reviews
                            //Log.d(TAG, "получено в виде списка: $reviews")

                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing response: ${e.message}")
                    _reviewsFlow.value = emptyList()
                    _isLoading.value = false
                    _isLoadingInfo.value = false
                } finally {
                    _isLoading.value = false
                    _isLoadingInfo.value = false
                    Log.d(TAG, "Loading finished")
                }
            }
        }
    }

}