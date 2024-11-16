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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assaf.basketclock.GameData
import com.assaf.basketclock.LOGOS_RESOURCES
import com.assaf.basketclock.R
import com.assaf.basketclock.TeamGameData


@Composable
fun LeaderConditionTeamColumn(teamData: TeamGameData, selectedTeam: MutableState<TeamGameData?>){
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selectedTeam.value != null && selectedTeam.value!!.teamId == teamData.teamId) Color(0x343B3CF8) else Color.Transparent)
            .border(BorderStroke(1.dp, Color.Gray), shape = RoundedCornerShape(8.dp))
            .clickable{
                selectedTeam.value = teamData
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
fun LeaderConditionContent(gameData: GameData, selectedTeam: MutableState<TeamGameData?>){
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
    selectedConditionTypeState: MutableState<ConditionType>,
    gameData: GameData,
    saveCondition: (Map<String, Any>, ConditionType) -> Unit
){
    val selectedTeam = remember { mutableStateOf<TeamGameData?>(null) }

    BaseConditionScreen(
        selectedConditionTypeState = selectedConditionTypeState,
        titleText = "Leader Condition",
        conditionHelpResourceId = R.string.leader_condition_help,
        content = {
            LeaderConditionContent(gameData, selectedTeam)
        },
        generateConditionData = {
            if (selectedTeam.value == null){
                throw ConditionValidationException("You must select a team!")
            }

            mapOf(
                "teamId" to selectedTeam.value!!.teamId
            )
        },
        saveCondition = saveCondition
    )
}
