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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.assaf.basketclock.ConditionScreenTitle
import com.assaf.basketclock.R
import com.assaf.basketclock.SelectedConditionType
import com.assaf.basketclock.ui.theme.CardBackground

@Composable
fun <T> QuarterRangeSelector(
    options: List<T>,
    selectedOptionState: MutableState<T>,
    width: Dp,
    height: Dp,
    enabled: Boolean = true
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
        Text(selectedOptionState.value.toString())

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {expanded = false}
        ) {
            options.forEach{option ->
                DropdownMenuItem(
                    onClick = {
                        selectedOptionState.value = option
                        expanded = false
                    },
                    text = {
                        Text(option.toString())
                    }
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun QuarterTimeSelector(){
    val quarters = listOf("Q1", "Q2", "Q3", "Q4", "OT")
    val selectedQuarter = remember { mutableStateOf(quarters[0]) }

    val isMinutesEnabled = remember {
        derivedStateOf { selectedQuarter.value != "OT" }
    }

    val minutes = (0..12).map { minute -> String.format("%02d:00", minute) }
    val selectedMinute = remember { mutableStateOf(minutes[0]) }

    Column(
        Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Row{
                QuarterRangeSelector(quarters, selectedQuarter, 50.dp, 75.dp, true)
                Spacer(Modifier.width(8.dp))
                QuarterRangeSelector(minutes, selectedMinute, 100.dp, 75.dp, isMinutesEnabled.value)
            }

        }
    }
}

@Composable
fun TimeConditionScreen(
    selectedConditionTypeState: MutableState<SelectedConditionType>,
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
            "Time Condition",
            LocalContext.current.getString(R.string.time_condition_help)
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
            QuarterTimeSelector()
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null
            )
            QuarterTimeSelector()
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
