package com.assaf.basketclock

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.assaf.basketclock.conditions.ConditionsDialog
import com.assaf.basketclock.ui.theme.CardBackground
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Composable
fun GameCard(gameData: GameData, isExpandedState: MutableState<Boolean>) {

    val showDialogState = remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

            if (gameData.gameStatus != 3){
                val gameConditions = AppDatabase.getDatabase(context).conditionDao().getGameConditions(gameData.gameId).collectAsState(initial = emptyList())

                val deleteCondition = { condition: Condition ->
                    scope.launch{
                        AppDatabase.getDatabase(context).conditionDao().deleteCondition(condition)
                        Toast.makeText(context, "Condition deleted successfully!", Toast.LENGTH_LONG).show()
                    }
                }

                AnimatedVisibility(
                    visible = isExpandedState.value,
                ) {
                    ConditionsSection(gameConditions, deleteCondition)
                }
                ExpandSection(isExpandedState, showDialogState)
            }
        }
    }

    if (showDialogState.value){
        Dialog(
            onDismissRequest = {showDialogState.value = false}
        ) {
            ConditionsDialog(showDialogState, gameData)
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
fun ExpandSection(isExpandedState: MutableState<Boolean>, showDialogState: MutableState<Boolean>){
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
            IconButton(onClick = { showDialogState.value = true }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Condition"
                )
            }
        }
    }
}


@Composable
fun ConditionsSection(conditions: State<List<Condition>>, deleteCondition: (Condition) -> Job){
    Column(
        Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(IntrinsicSize.Min)
        ,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Game Conditions", fontWeight = FontWeight.Bold)
        HorizontalDivider(
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 20.dp)
            ,
            color = Color.LightGray,
            thickness = 0.3.dp
        )
        if (conditions.value.isEmpty()){
            Text("No conditions were found for this game")
        }
        else{
            ConditionsList(conditions.value, deleteCondition)
        }
    }
}


@Composable
fun ConditionsList(conditions: List<Condition>, deleteCondition: (Condition) -> Job){
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        conditions.forEachIndexed { index, condition ->
            ConditionItem(condition, deleteCondition)

            if (index < conditions.size - 1){
                DividerWithText("And")
            }
        }
    }
}

@Composable
fun ConditionItem(condition: Condition, deleteCondition: (Condition) -> Job){
    Row(
        Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(
                id = condition.parsedConditionData.conditionIcon()
            ),
            contentDescription = "Condition Icon",
            modifier = Modifier.size(25.dp),
            colorFilter = ColorFilter.tint(Color.White)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            modifier = Modifier.weight(1f),
            text = condition.parsedConditionData.describeCondition(condition),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = {
                deleteCondition(condition)
            },
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete Condition"
            )
        }

    }
}

@Composable
fun DividerWithText(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f), // Makes the divider take up all space
            color = Color.Gray,
            thickness = 1.dp
        )
        Spacer(modifier = Modifier.width(8.dp)) // Space between the divider and text
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp),
            fontWeight = FontWeight.Thin,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(8.dp)) // Space between the text and divider
        HorizontalDivider(
            modifier = Modifier.weight(1f), // Makes the divider take up all space
            color = Color.Gray,
            thickness = 1.dp
        )
    }
}
