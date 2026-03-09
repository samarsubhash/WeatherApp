package com.example.weatherapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.data.RetrofitInstance
import com.example.weatherapp.data.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class WeatherState {
    data object Initial : WeatherState()
    data object Loading : WeatherState()
    data class Success(val data: WeatherResponse) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

class WeatherViewModel : ViewModel() {
    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Initial)
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    // The OpenWeatherMap API key
    private val API_KEY = "ed95deeb066ba1dd17e3cf0c742ea427"

    fun fetchWeather(city: String) {
        if (city.isBlank()) return
        
        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading
            try {
                // Call the API via Retrofit
                val response = RetrofitInstance.api.getWeatherByCity(city = city, apiKey = API_KEY)
                _weatherState.value = WeatherState.Success(response)
            } catch (e: Exception) {
                // Catch network and serialization errors
                val errorMsg = if (e is retrofit2.HttpException && e.code() == 404) {
                    "City not found"
                } else if (e is retrofit2.HttpException && e.code() == 401) {
                    "Invalid API Key. Please add a valid key."
                } else {
                    e.localizedMessage ?: "Failed to fetch weather data"
                }
                _weatherState.value = WeatherState.Error(errorMsg)
            }
        }
    }
}
