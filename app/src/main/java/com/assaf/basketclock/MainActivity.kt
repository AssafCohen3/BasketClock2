package com.assaf.basketclock

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.assaf.basketclock.scheduling.fireDailyReceiver
import com.assaf.basketclock.ui.theme.BackgroundDark
import com.assaf.basketclock.ui.theme.BasketClockTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var hasAlarmSchedulingPermissionsState: MutableState<Boolean>? = null
    private var hasNotificationsPermissionsState: MutableState<Boolean>? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContent {
            hasAlarmSchedulingPermissionsState = remember { mutableStateOf(canScheduleExactAlarms(this)) }
            hasNotificationsPermissionsState = remember { mutableStateOf(canPostNotifications(this)) }

            BasketClockTheme {
                Scaffold(
                    topBar = {
                        MyAppBar()
                    }
                ) {
                    innerPadding ->
                    MainActivityContent(innerPadding)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasAlarmSchedulingPermissionsState?.value = canScheduleExactAlarms(this)
        hasNotificationsPermissionsState?.value = canPostNotifications(this)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainActivityContent(innerPadding: PaddingValues) {

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

        if (!hasAlarmSchedulingPermissionsState!!.value){
            SchedulingPermissionDialog()
        }
        else if (!hasNotificationsPermissionsState!!.value){
            AllowNotificationsDialog()
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
    fun AllowNotificationsDialog(){
        ShowPermissionsDialog(
            "For the app to work properly, you need to grant it the permission to show notifications.",
            "Allow notifications"
        ) {
            openNotificationsSettingsActivity()
        }
    }

    fun openNotificationsSettingsActivity(){
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101);
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppBar(){
    val currentContext = LocalContext.current
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
                            .size(30.dp)
                            .clip(CircleShape)
                    )
                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )
                    Text(
                        text = "Basket Clock",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        fireDailyReceiver(currentContext)
                    }) {
                        Image(
                            painter = painterResource(
                                id = R.drawable.bug_svgrepo_com
                            ),
                            modifier = Modifier.size(25.dp),
                            colorFilter = ColorFilter.tint(Color.White),
                            contentDescription = "Debug Alarm Manager"
                        )
                    }
                    Spacer(Modifier.width(8.dp))

                }
            }
        )
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
                        var currentDateString = SimpleDateFormat("dd/MM", Locale.getDefault()).format(currentGameDate.gameDate)
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
            val statusOrderMap = mapOf<Int, Int>(
                // First show active games.
                2 to 1,
                // Then show future games.
                1 to 2,
                // Then show finished games.
                3 to 3
            )
            val sortedGames = gamesForDate.sortedWith(
                compareBy<GameData> { statusOrderMap[it.gameStatus] }.thenBy { it.realGameDateTimeUTC }
            )
            val gamesWithExpandStates = sortedGames.map { game -> game to remember { mutableStateOf(false) } }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(gamesWithExpandStates) { (gameData, gameCardState) ->
                    GameCard(gameData, gameCardState)
                }
            }
        }
    }
}

@Composable
fun SchedulingPermissionDialog(){
    val context = LocalContext.current

    ShowPermissionsDialog(
        "For the app to work properly, you need to grant it the permission to schedule exact alarms.",
        "Grant Permissions"
    ) {
        openScheduleAlarmsPermissionActivity(context)
    }
}

fun openScheduleAlarmsPermissionActivity(context: Context){
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        context.startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowPermissionsDialog(
    prompt: String,
    confirmText: String,
    onGrantPermissionsClick: () -> Unit
){
    BasicAlertDialog(
        onDismissRequest = {},
    ){
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    prompt,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        onGrantPermissionsClick()
                    },
                ) {
                    Text(confirmText)
                }
            }
        }
    }
}
