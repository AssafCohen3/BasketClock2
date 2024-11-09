package com.assaf.basketclock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.assaf.basketclock.ui.theme.CardBackground
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Composable
fun GameCard(gameData: GameData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground,
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(16.dp)
            ,
            horizontalArrangement = Arrangement.SpaceBetween // Distribute space evenly
        ) {
            // Home Team Column
            Column {
                TeamColumn(gameData.homeTeam, gameData.gameStatus, false)
            }

            // Game Details Column
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (gameData.gameStatus) {
                    3 -> {
                        Text(text = "FINAL", fontWeight = FontWeight.Bold)
                    }
                    2 -> {
                        Text(text = gameData.gameStatusText.trim(), fontWeight = FontWeight.Bold)
                    }
                    else -> {
                        Column {
                            val zonedDatetime = gameData.realGameDateTimeUTC.withZoneSameInstant(ZoneId.systemDefault())
                            Text(text = DateTimeFormatter.ofPattern("HH:mm").format(zonedDatetime), fontWeight = FontWeight.Bold)
                            Text(text = DateTimeFormatter.ofPattern("dd/MM").format(zonedDatetime))
                        }
                    }
                }
            }

            // Away Team Column (Similar to Home Team)
            Column {
                TeamColumn(gameData.awayTeam, gameData.gameStatus, true)
            }
        }
    }
}


@Composable
fun TeamColumn(teamData: TeamGameData, gameStatus: Int, scoreBefore: Boolean){
    Row(
//        modifier = Modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if(scoreBefore){
            TeamColumnScore(teamData, gameStatus)
        }

        // Team Details Column
        Column(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
//            Image(
//                painter = painterResource(id = R.drawable.logo_1610612737),
//                contentDescription = "Team Logo",
//                modifier = Modifier.size(50.dp)
//            )
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("https://cdn.nba.com/logos/nba/${teamData.teamId}/global/L/logo.svg")
                    .decoderFactory(SvgDecoder.Factory())
                    .build(),
                contentDescription = "Team Logo",
                modifier = Modifier.size(50.dp),
                placeholder = painterResource(id = R.drawable.placeholder_logo)
            )
            Text(text = teamData.teamTricode, fontWeight = FontWeight.Bold)
            Text(text = "${teamData.wins} - ${teamData.losses}")
        }

        if(!scoreBefore){
            TeamColumnScore(teamData, gameStatus)
        }
    }
}


@Composable
fun TeamColumnScore(teamData: TeamGameData, gameStatus: Int){
    if (gameStatus != 1) {
        Text(
            modifier = Modifier.padding(10.dp),
            text = teamData.score.toString(),
            fontWeight = FontWeight.Bold
        )
    }
}