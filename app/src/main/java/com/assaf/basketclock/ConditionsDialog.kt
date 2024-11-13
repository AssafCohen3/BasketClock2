package com.assaf.basketclock

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.assaf.basketclock.conditions.DifferenceConditionScreen
import com.assaf.basketclock.conditions.LeaderConditionScreen
import com.assaf.basketclock.conditions.TimeConditionScreen
import com.assaf.basketclock.ui.theme.BackgroundDark
import kotlinx.coroutines.launch

enum class SelectedConditionType {
    NONE, TIME, DIFFERENCE, LEADER
}


@Composable
fun ConditionsDialog(isDialogOpen: MutableState<Boolean>, gameData: GameData){

    val selectedConditionTypeState = remember {
        mutableStateOf<SelectedConditionType>(SelectedConditionType.NONE)
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
            SelectedConditionType.NONE -> {
                ConditionTypeSelectionScreen(selectedConditionTypeState)
            }
            SelectedConditionType.TIME -> {
                TimeConditionScreen(selectedConditionTypeState)
            }
            SelectedConditionType.DIFFERENCE -> {
                DifferenceConditionScreen(selectedConditionTypeState)
            }
            SelectedConditionType.LEADER -> {
                LeaderConditionScreen(selectedConditionTypeState, gameData)
            }
            else -> {
                ConditionTypeSelectionScreen(selectedConditionTypeState)
            }
        }
    }
}

@Composable
fun ConditionTypeSelectionScreen(selectedConditionTypeState: MutableState<SelectedConditionType>){
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
            SelectedConditionType.TIME,
            Color.White,
            "Time",
            Color(0xFF024A73),
            R.drawable.ic_time_condition_icon,
            selectedConditionTypeState
        )
        Spacer(Modifier.height(16.dp))
        ConditionCard(
            SelectedConditionType.DIFFERENCE,
            Color(0xFFFFF100),
            "Difference",
            Color(0xFF2371B5),
            R.drawable.ic_difference_condition_icon_3,
            selectedConditionTypeState
        )
        Spacer(Modifier.height(16.dp))
        ConditionCard(
            SelectedConditionType.LEADER,
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
    conditionType: SelectedConditionType,
    background: Color,
    text: String,
    textColor: Color,
    conditionIcon: Int,
    selectedConditionTypeState: MutableState<SelectedConditionType>
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
    selectedConditionTypeState: MutableState<SelectedConditionType>,
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
                    selectedConditionTypeState.value = SelectedConditionType.NONE
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
