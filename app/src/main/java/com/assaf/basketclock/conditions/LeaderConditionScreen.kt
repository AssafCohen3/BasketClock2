package com.assaf.basketclock.conditions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assaf.basketclock.ConditionScreenTitle
import com.assaf.basketclock.GameData
import com.assaf.basketclock.LOGOS_RESOURCES
import com.assaf.basketclock.R
import com.assaf.basketclock.SelectedConditionType
import com.assaf.basketclock.TeamGameData
import com.assaf.basketclock.ui.theme.CardBackground


@Composable
fun LeaderConditionTeamColumn(teamData: TeamGameData, selectedTeam: MutableState<Int?>){
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selectedTeam.value == teamData.teamId) Color(0x343B3CF8) else Color.Transparent)
            .border(BorderStroke(1.dp, Color.Gray), shape = RoundedCornerShape(8.dp))
            .clickable{
                selectedTeam.value = teamData.teamId
            }
    ){
        Column(
            Modifier
                .padding(16.dp)
            ,
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
        }
    }
}

@Composable
fun LeaderConditionContent(gameData: GameData){
    val selectedTeam = remember { mutableStateOf<Int?>(null) }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LeaderConditionTeamColumn(gameData.homeTeam, selectedTeam)
        Spacer(Modifier.width(16.dp))
        Text("VS", fontSize = 30.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(16.dp))
        LeaderConditionTeamColumn(gameData.awayTeam, selectedTeam)
    }
}

@Composable
fun LeaderConditionScreen(
    selectedConditionTypeState: MutableState<SelectedConditionType>,
    gameData: GameData
){
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(16.dp)
        ,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ConditionScreenTitle(
            selectedConditionTypeState,
            "Leader Condition",
            LocalContext.current.getString(R.string.leader_condition_help)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .weight(1f)
            ,
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LeaderConditionContent(gameData)
        }
        Spacer(Modifier.height(12.dp))
        ElevatedButton(
            colors = ButtonDefaults.buttonColors(
                containerColor = CardBackground,
                contentColor = Color.White
            ),
            onClick = {}
        ) {
            Text("Save")
        }
    }
}
