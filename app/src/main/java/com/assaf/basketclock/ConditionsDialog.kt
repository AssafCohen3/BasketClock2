package com.assaf.basketclock

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.assaf.basketclock.ui.theme.BackgroundDark


enum class SelectedConditionType {
    NONE, TIME, DIFFERENCE, LEADER
}


@Composable
fun ConditionsDialog(isDialogOpen: MutableState<Boolean>){

    val selectedConditionTypeState = remember {
        mutableStateOf<SelectedConditionType>(SelectedConditionType.TIME)
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


@Composable
fun ConditionScreenTitle(selectedConditionTypeState: MutableState<SelectedConditionType>, text: String){
    Column(
        Modifier
            .fillMaxWidth()

    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
            ,
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                ,
                verticalAlignment = Alignment.CenterVertically,

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
            }
            Text(text, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(thickness = 0.3.dp, color = Color.DarkGray)
    }
}

@Composable
fun <T> MySelector(
    options: List<T>,
    selectedOptionState: MutableState<T>,
    width: Dp,
    height: Dp,
    enabled: Boolean
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
            options.forEach{quarter ->
                DropdownMenuItem(
                    onClick = {
                        selectedOptionState.value = quarter
                        expanded = false
                    },
                    text = {
                        Text(quarter.toString())
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
            Row(){
                MySelector(quarters, selectedQuarter, 50.dp, 75.dp, true)
                Spacer(Modifier.width(8.dp))
                MySelector(minutes, selectedMinute, 100.dp, 75.dp, isMinutesEnabled.value)
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
        ConditionScreenTitle(selectedConditionTypeState, "Time Condition")
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
        Spacer(Modifier.height(8.dp))
        Button(onClick = {}) {
            Text("Save")
        }
    }
}
