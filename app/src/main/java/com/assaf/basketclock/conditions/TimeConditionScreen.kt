package com.assaf.basketclock.conditions

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.assaf.basketclock.GameData
import com.assaf.basketclock.R
import com.assaf.basketclock.serializeToMap
import kotlinx.coroutines.Job

val QUARTERS = listOf(
    Quarter("Q1", 1),
    Quarter("Q2", 2),
    Quarter("Q3", 3),
    Quarter("Q4", 4),
    Quarter("OT", 5)
)

val MINUTES = (0..12).toList()

data class Quarter(
    val displayName: String,
    val quarter: Int
)

data class GameMomentState(
    val quarter: MutableState<Quarter> = mutableStateOf(QUARTERS[0]),
    val minute: MutableIntState = mutableIntStateOf(MINUTES[0]),
){
    fun gameTotalMinute(): Int{
        return if (quarter.value.quarter == 5) 49 else (quarter.value.quarter - 1) * 12 + minute.intValue
    }

    fun effectiveMinute(): Int{
        return if (quarter.value.quarter == 5) 0 else minute.intValue
    }
}

@Composable
fun <T> QuarterRangeSelector(
    options: List<T>,
    selectedOptionState: MutableState<T>,
    width: Dp,
    height: Dp,
    enabled: Boolean = true,
    formatter: ((T) -> String)? = null
){
    var expanded by remember { mutableStateOf(false) }

    Box(
        Modifier
            .width(width)
            .height(height)
            .border(BorderStroke(1.dp, Color.Gray), shape = RoundedCornerShape(8.dp))
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled){expanded = true}
        ,
        contentAlignment = Alignment.Center
    ){
        val selectedOptionText = if(formatter == null) selectedOptionState.value.toString() else formatter(selectedOptionState.value)
        Text(selectedOptionText)

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {expanded = false}
        ) {
            options.forEach{option ->
                val optionText = if(formatter == null) option.toString() else formatter(option)
                DropdownMenuItem(
                    onClick = {
                        selectedOptionState.value = option
                        expanded = false
                    },
                    text = {
                        Text(optionText)
                    }
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun QuarterTimeSelector(selectedGameMoment: GameMomentState){
    val isMinutesEnabled = remember {
        derivedStateOf { selectedGameMoment.quarter.value.quarter != 5 }
    }

    Column(
        Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Row{
                QuarterRangeSelector(
                    QUARTERS,
                    selectedGameMoment.quarter,
                    50.dp,
                    75.dp,
                    true,
                    formatter = {quarter -> quarter.displayName}
                )
                Spacer(Modifier.width(8.dp))
                QuarterRangeSelector(
                    MINUTES,
                    selectedGameMoment.minute,
                    100.dp,
                    75.dp,
                    isMinutesEnabled.value,
                    formatter = { minute -> String.format("%02d:00", minute)}
                )
            }

        }
    }
}

fun validateGameMomentRange(
    gameData: GameData,
    rangeStart: GameMomentState,
    rangeEnd: GameMomentState
): Map<String, Any>{
    if (rangeStart.gameTotalMinute() > rangeEnd.gameTotalMinute()){
        throw ConditionValidationException("The range beginning can't be after the range end.")
    }

    if (gameData.gameStatus == 2 && gameData.gameMoment != null){
        if (gameData.period!! * 12 + gameData.gameMoment!!.getReverseClock().minutes >= rangeEnd.gameTotalMinute()){
            throw ConditionValidationException("The game clock is already after the range end.")
        }
    }

    return TimeConditionData(
        rangeStart.quarter.value.quarter,
        rangeStart.quarter.value.displayName,
        rangeStart.effectiveMinute(),
        rangeEnd.quarter.value.quarter,
        rangeEnd.quarter.value.displayName,
        rangeEnd.effectiveMinute()
    ).serializeToMap()
}

@Composable
fun TimeConditionScreen(
    selectedConditionTypeState: MutableState<ConditionType>,
    gameData: GameData,
    saveCondition: (Map<String, Any>, ConditionType) -> Job
){
    val rangeStart = GameMomentState()
    val rangeEnd = GameMomentState()

    BaseConditionScreen(
        selectedConditionTypeState = selectedConditionTypeState,
        titleText = "Time Condition",
        conditionHelpResourceId = R.string.time_condition_help,
        content = {
            QuarterTimeSelector(rangeStart)
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null
            )
            QuarterTimeSelector(rangeEnd)
        },
        generateConditionData = {
            validateGameMomentRange(gameData, rangeStart, rangeEnd)
        },
        saveCondition = saveCondition
    )
}
