package com.assaf.basketclock.conditions

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assaf.basketclock.ConditionScreenTitle
import com.assaf.basketclock.R
import com.assaf.basketclock.SelectedConditionType
import com.assaf.basketclock.ui.theme.CardBackground
import com.chargemap.compose.numberpicker.NumberPicker


@Composable
fun <T> SignSelector(
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
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled){expanded = true}
        ,
        contentAlignment = Alignment.Center
    ){
        Text(selectedOptionState.value.toString(), fontSize = 30.sp)

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

@Composable
fun DifferenceConditionContent(){
    val differenceSigns = listOf("≤", ">")
    val selectedDifferenceSign = remember { mutableStateOf(differenceSigns[0]) }
    var selectedDiff by remember { mutableIntStateOf(10) }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SignSelector(listOf("≤", ">"), selectedDifferenceSign, 25.dp, 75.dp)
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .padding(4.dp)
        ){
            NumberPicker(
                value = selectedDiff,
                range = 0..30,
                onValueChange = {
                    selectedDiff = it
                },
                dividersColor = Color.Gray,
                textStyle = TextStyle(color = Color.White)
            )
        }
    }
}

@Composable
fun DifferenceConditionScreen(
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
            "Difference Condition",
            LocalContext.current.getString(R.string.difference_condition_help)
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
            DifferenceConditionContent()
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
