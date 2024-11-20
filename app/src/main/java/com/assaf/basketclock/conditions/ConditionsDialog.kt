package com.assaf.basketclock.conditions

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.assaf.basketclock.AppDatabase
import com.assaf.basketclock.Condition
import com.assaf.basketclock.GameData
import com.assaf.basketclock.R
import com.assaf.basketclock.ui.theme.BackgroundDark
import com.assaf.basketclock.ui.theme.CardBackground
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class ConditionValidationException(message: String) : Exception(message)


@Composable
fun ConditionsDialog(isDialogOpen: MutableState<Boolean>, gameData: GameData){
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val saveCondition = { conditionData: Map<String, Any>, conditionType: ConditionType ->
        scope.launch {
            AppDatabase.getDatabase(context).getConditionsRepository().insertCondition(Condition(
                gameId = gameData.gameId,
                gameDateTime = gameData.realGameDateTimeUTC,
                homeTeamId = gameData.homeTeam.teamId,
                homeTeamName = gameData.homeTeam.teamName,
                homeTeamTricode = gameData.homeTeam.teamTricode,
                awayTeamId = gameData.awayTeam.teamId,
                awayTeamName = gameData.awayTeam.teamName,
                awayTeamTricode = gameData.awayTeam.teamTricode,
                conditionType = conditionType,
                conditionData = conditionData
            ))
            Toast.makeText(context, "Condition was saved successfully!", Toast.LENGTH_LONG).show()
            isDialogOpen.value = false
        }
    }

    val selectedConditionTypeState = remember {
        mutableStateOf<ConditionType>(ConditionType.NONE)
    }

    Card(
        Modifier
            .padding(16.dp)
            .height(400.dp)
            .fillMaxWidth()
        ,
        colors = CardDefaults.cardColors(
            containerColor = BackgroundDark
        )
    ) {
        when(selectedConditionTypeState.value){
            ConditionType.NONE -> {
                ConditionTypeSelectionScreen(selectedConditionTypeState)
            }
            ConditionType.TIME -> {
                TimeConditionScreen(selectedConditionTypeState, gameData, saveCondition)
            }
            ConditionType.DIFFERENCE -> {
                DifferenceConditionScreen(selectedConditionTypeState, saveCondition)
            }
            ConditionType.LEADER -> {
                LeaderConditionScreen(selectedConditionTypeState, gameData, saveCondition)
            }
            else -> {
                ConditionTypeSelectionScreen(selectedConditionTypeState)
            }
        }
    }
}

@Composable
fun ConditionTypeSelectionScreen(selectedConditionTypeState: MutableState<ConditionType>){
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(16.dp)
        ,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text("Select a Condition Type", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        ConditionCard(
            ConditionType.TIME,
            Color.White,
            "Time",
            Color(0xFF024A73),
            R.drawable.ic_time_condition_icon,
            selectedConditionTypeState
        )
        Spacer(Modifier.height(16.dp))
        ConditionCard(
            ConditionType.DIFFERENCE,
            Color(0xFFFFF100),
            "Difference",
            Color(0xFF2371B5),
            R.drawable.ic_difference_condition_icon_3,
            selectedConditionTypeState
        )
        Spacer(Modifier.height(16.dp))
        ConditionCard(
            ConditionType.LEADER,
            Color(0xFFD90106),
            "Leader",
            Color(0xFFffe600),
            R.drawable.ic_leader_condition_icon,
            selectedConditionTypeState
        )
    }
}


@Composable
fun ColumnScope.ConditionCard(
    conditionType: ConditionType,
    background: Color,
    text: String,
    textColor: Color,
    conditionIcon: Int,
    selectedConditionTypeState: MutableState<ConditionType>
){
    Card(
        Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .weight(1f)
            .clickable{
                selectedConditionTypeState.value = conditionType
            }
        ,
        colors = CardDefaults.cardColors(
            containerColor = background
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
            ,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text, color = textColor, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Image(
                painter = painterResource(
                    id = conditionIcon
                ),
                contentDescription = "$text Condition",
                modifier = Modifier.size(50.dp)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConditionScreenTitle(
    selectedConditionTypeState: MutableState<ConditionType>,
    text: String,
    helpText: String
){
    val tooltipState = rememberTooltipState(isPersistent = false)
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
            ,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    selectedConditionTypeState.value = ConditionType.NONE
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(text, color = Color.White, fontWeight = FontWeight.Bold)

            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    RichTooltip{
                        Text(helpText, textAlign = TextAlign.Center)
                    }
                },
                state = tooltipState
            ) {
                IconButton(onClick = {
                    scope.launch{
                        tooltipState.show()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Help"
                    )
                }

            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(thickness = 0.3.dp, color = Color.DarkGray)
    }
}


@Composable
fun BaseConditionScreen(
    selectedConditionTypeState: MutableState<ConditionType>,
    titleText: String,
    conditionHelpResourceId: Int,
    content: @Composable (() -> Unit),
    generateConditionData: () -> Map<String, Any>,
    saveCondition: (Map<String, Any>, ConditionType) -> Job
){
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
            titleText,
            LocalContext.current.getString(conditionHelpResourceId)
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
            content()
        }
        Spacer(Modifier.height(12.dp))
        ElevatedButton(
            colors = ButtonDefaults.buttonColors(
                containerColor = CardBackground,
                contentColor = Color.White
            ),
            onClick = {
                try{
                    val generatedData = generateConditionData()
                    saveCondition(generatedData, selectedConditionTypeState.value)
                }
                catch (e: ConditionValidationException){
                    scope.launch{
                        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                    }
                }
                catch (e: Exception){
                    scope.launch{
                        Toast.makeText(context, "Got an error generating condition: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        ) {
            Text("Save")
        }
    }
}
