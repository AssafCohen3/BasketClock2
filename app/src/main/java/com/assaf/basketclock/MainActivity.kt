package com.assaf.basketclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.TopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.assaf.basketclock.ui.theme.BasketClockTheme
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContent {
            BasketClockTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            backgroundColor = Color(0xFFff8316),
                            elevation = 8.dp,

                        ){
                            Text(
                                text = "Basket Clock",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                ) {
                    innerPadding ->
                    HorizontalCardList(innerPadding)
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}


@Composable
fun CardItem(text: String){
    Card(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth()
    ){
        Text(text = text, modifier = Modifier.padding(16.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalCardList(innerPadding: PaddingValues) {
    var gameCards by remember { mutableStateOf<List<GameData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://cdn.nba.com/static/json/liveData/scoreboard/todaysScoreboard_00.json")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // Handle error
                    isLoading = false
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")

                        val responseBody = response.body?.string()
                        // Parse JSON response and update gameCards
                        val parsedResponse = Json { ignoreUnknownKeys = true }.decodeFromString<ScoreboardResponse>(responseBody as String)
                        gameCards = parsedResponse.scoreboard.games
                        isLoading = false
                    }
                }
            })
        } catch (e: Exception) {
            // Handle error
            isLoading = false
        }
    }


    val pagerState = rememberPagerState(pageCount = {
        1
    })

    if(isLoading){
        LoadingScreen()
    }
    else if(gameCards.size == 0){
        Text("No Games Found :(")
    }
    else{
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFEEEAEA))
        ) { page ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(gameCards.sortedBy { gameData -> gameData.gameTimeUTC }) { gameCard ->
                    GameCard(gameCard)
                }
            }
        }
    }
}