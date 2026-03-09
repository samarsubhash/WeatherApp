package com.example.weatherapp.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.weatherapp.data.WeatherResponse
import com.example.weatherapp.ui.theme.*
import com.example.weatherapp.viewmodel.WeatherState
import com.example.weatherapp.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun WeatherScreen(viewModel: WeatherViewModel) {
    val state by viewModel.weatherState.collectAsState()
    var cityInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Determine colors based on weather state
    val (startColor, endColor) = remember(state) {
        when (state) {
            is WeatherState.Success -> {
                val weather = (state as WeatherState.Success).data.weather.firstOrNull()?.main ?: "Clear"
                val isNight = (state as WeatherState.Success).data.weather.firstOrNull()?.icon?.endsWith("n") ?: false
                when {
                    isNight -> NightStart to NightEnd
                    weather.contains("Cloud", ignoreCase = true) -> CloudyStart to CloudyEnd
                    weather.contains("Rain", ignoreCase = true) || weather.contains("Drizzle", ignoreCase = true) -> RainyStart to RainyEnd
                    weather.contains("Clear", ignoreCase = true) -> SunnyStart to SunnyEnd
                    else -> CloudyStart to CloudyEnd
                }
            }
            else -> NightStart to NightEnd
        }
    }

    val animatedStartColor by animateColorAsState(
        targetValue = startColor,
        animationSpec = tween(durationMillis = 1000), label = "startColor"
    )
    val animatedEndColor by animateColorAsState(
        targetValue = endColor,
        animationSpec = tween(durationMillis = 1000), label = "endColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(animatedStartColor, animatedEndColor)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Search Bar
            SearchBar(
                value = cityInput,
                onValueChange = { cityInput = it },
                onSearch = {
                    viewModel.fetchWeather(cityInput)
                    keyboardController?.hide()
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main Content
            when (state) {
                is WeatherState.Initial -> {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TextGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Search for a city\nto view weather",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            color = TextGray
                        )
                    }
                }
                is WeatherState.Loading -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TextWhite)
                    }
                }
                is WeatherState.Success -> {
                    val weatherData = (state as WeatherState.Success).data
                    WeatherContent(data = weatherData, modifier = Modifier.weight(1f))
                }
                is WeatherState.Error -> {
                    val errorMsg = (state as WeatherState.Error).message
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        GlassCard(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = errorMsg,
                                color = Color(0xFFFF6B6B),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
        placeholder = { Text("Search city...", color = TextGray) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Icon",
                tint = TextWhite
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = GlassBackground,
            unfocusedContainerColor = GlassBackground,
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            cursorColor = TextWhite,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        singleLine = true
    )
}

@Composable
fun WeatherContent(data: WeatherResponse, modifier: Modifier = Modifier) {
    val currentTemp = data.main.temp.roundToInt()
    val weatherDesc = data.weather.firstOrNull()?.description?.capitalize(Locale.ROOT) ?: ""
    val iconCode = data.weather.firstOrNull()?.icon ?: "01d"
    val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@4x.png"

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Date and Location
        Text(
            text = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date()),
            style = MaterialTheme.typography.bodyLarge,
            color = TextGray
        )
        Text(
            text = "${data.name}, ${data.sys.country}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main Weather Display
        AsyncImage(
            model = iconUrl,
            contentDescription = "Weather Icon",
            modifier = Modifier.size(200.dp),
            contentScale = ContentScale.Fit
        )

        Text(
            text = "$currentTemp°",
            style = MaterialTheme.typography.displayLarge,
            color = TextWhite,
            modifier = Modifier.offset(x = 12.dp) // Optical alignment for the degree symbol
        )
        
        Text(
            text = weatherDesc,
            style = MaterialTheme.typography.titleLarge,
            color = TextGray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Details Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WeatherDetailCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.WaterDrop,
                label = "Humidity",
                value = "${data.main.humidity}%"
            )
            WeatherDetailCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Air,
                label = "Wind",
                value = "${data.wind.speed.roundToInt()} m/s"
            )
            WeatherDetailCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Thermostat,
                label = "Feels Like",
                value = "${data.main.feelsLike.roundToInt()}°"
            )
        }
    }
}

@Composable
fun WeatherDetailCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextWhite,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextGray,
                maxLines = 1
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(GlassBackground)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
    ) {
        content()
    }
}
