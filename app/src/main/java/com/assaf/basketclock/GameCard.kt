package com.assaf.basketclock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.assaf.basketclock.ui.theme.CardBackground
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Composable
fun GameCard(gameData: GameData, isExpandedState: MutableState<Boolean>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .animateContentSize()
        ,
        colors = CardDefaults.cardColors(
            containerColor = CardBackground,
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ){
            GameDataSection(gameData)
            AnimatedVisibility(
                visible = isExpandedState.value,
            ) {
                ConditionsSection()
            }
            ExpandSection(isExpandedState)
        }
    }
}


@Composable
fun GameDataSection(gameData: GameData){
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

@Composable
fun TeamColumn(teamData: TeamGameData, gameStatus: Int, scoreBefore: Boolean){
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        if(scoreBefore){
            TeamColumnScore(teamData, gameStatus)
        }

        Column(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(
                    id = LOGOS_RESOURCES.getOrDefault(teamData.teamId, R.drawable.placeholder_logo)
                ),
                contentDescription = "Team Logo",
                modifier = Modifier.size(50.dp)
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


@Composable
fun ExpandSection(isExpandedState: MutableState<Boolean>){
    Row(
        Modifier
            .height(IntrinsicSize.Min)
            .fillMaxWidth()
            .padding(bottom = 5.dp)
        ,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ){
        IconButton(onClick = {isExpandedState.value = !isExpandedState.value}) {
            Icon(
                imageVector = if (isExpandedState.value) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpandedState.value) "Collapse" else "Expand"
            )
        }

        if (isExpandedState.value){
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Condition"
                )
            }
        }
    }
}


@Composable
fun ConditionsSection(){
    Column(
        Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(IntrinsicSize.Min)
        ,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalDivider(
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 20.dp)
            ,
            color = Color.LightGray,
            thickness = 0.3.dp
        )

        Text("This game has no conditions.", color = Color.White, fontWeight = FontWeight.Bold)
    }
}