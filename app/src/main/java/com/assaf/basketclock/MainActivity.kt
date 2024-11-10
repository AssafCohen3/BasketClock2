package com.assaf.basketclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assaf.basketclock.ui.theme.BackgroundDark
import com.assaf.basketclock.ui.theme.BasketClockTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContent {
            BasketClockTheme {
                Scaffold(
                    topBar = {
                        Surface(shadowElevation = 8.dp) {
                            TopAppBar(
                                modifier = Modifier
                                    .background(BackgroundDark),
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ){
                                        Image(
                                            painter = painterResource(id = R.drawable.app_logo),
                                            contentDescription = "App Icon",
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                        )
                                        Spacer(
                                            modifier = Modifier.width(8.dp)
                                        )
                                        Text(
                                            text = "Basket Clock",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorizontalCardList(innerPadding: PaddingValues) {
    var calendarResponseWithTodayDate by remember { mutableStateOf<CalendarResponseWithTodayDate?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val refreshData = {
        coroutineScope.launch {
            try {
                isLoading = true
                val calendarResponse = fetchCompleteGamesData()
                calendarResponseWithTodayDate = calendarResponse
                isLoading = false
                hasError = false
            } catch (_: Exception) {
                hasError = true
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    if(isLoading){
        LoadingScreen()
    }
    else{
        @Suppress("KotlinConstantConditions")
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = {
                refreshData()
            },
            modifier = Modifier.padding(innerPadding)
        ) {
            if(hasError){
                Box(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Got an error while fetching the data.\nTry Reload.",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            else{
                calendarResponseWithTodayDate?.let { response ->
                    GamesPager(response, coroutineScope)
                }
            }
        }
    }
}


@Composable
fun DateIndicator(pagerState: PagerState, calendar: CalendarResponseWithTodayDate,
                  coroutineScope: CoroutineScope, todayIndex: Int){
    TabRow(
        selectedTabIndex = pagerState.currentPage,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(60.dp)
        ,
        indicator = {},
        divider = {}
    ) {
        // Add tabs for each date
        calendar.leagueSchedule.gameDates.forEachIndexed { index, currentGameDate ->
            if(index >= pagerState.currentPage - 1 && index <= pagerState.currentPage + 1){
                Tab(
                    modifier = if (index == pagerState.currentPage)
                        Modifier.background(Color(0x2C8192E5)) else Modifier,
                    selected = index == pagerState.currentPage,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        var currentDateString = SimpleDateFormat("MM/dd", Locale.getDefault()).format(currentGameDate.gameDate)
                        if (index == todayIndex){
                            currentDateString = "${currentDateString}\nToday"
                        }
                        Text(currentDateString, color = Color.White)
                    }
                )
            }
        }
    }
}

@Composable
fun GamesPager(response: CalendarResponseWithTodayDate, coroutineScope: CoroutineScope){
    val gamesByDate = response.leagueSchedule.gameDates.associateBy { gameDate ->
        gameDate.gameDate
    }
    val initialPageIndex = gamesByDate.keys.indexOf(response.todayDate)

    val pagerState = rememberPagerState(
        pageCount = { gamesByDate.size },
        initialPage = initialPageIndex
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
    ){
        DateIndicator(
            pagerState,
            response,
            coroutineScope,
            initialPageIndex
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
        ) { page ->

            val gamesForDate = response.leagueSchedule.gameDates[page].games
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(gamesForDate.sortedBy { gameData -> gameData.realGameDateTimeUTC }) { gameCard ->
                    GameCard(gameCard)
                }
            }
        }
    }
}
