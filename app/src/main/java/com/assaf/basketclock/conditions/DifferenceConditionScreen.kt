package com.assaf.basketclock.conditions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assaf.basketclock.R
import com.chargemap.compose.numberpicker.NumberPicker


val DIFFERENCE_SIGNS = listOf("≤", ">")

data class DifferenceConditionData(
    val sign: String,
    val difference: Int
){
    fun toMap(): Map<String, Any>{
        return mapOf(
            "sign" to sign,
            "difference" to difference
        )
    }

    companion object{
        fun fromMap(map: Map<String, Any>): DifferenceConditionData {
            return DifferenceConditionData(
                map["sign"] as String,
                map["difference"] as Int
            )
        }
    }
}


@Composable
fun SignSelector(
    selectedOptionState: MutableState<String>,
){
    var expanded by remember { mutableStateOf(false) }

    Box(
        Modifier
            .width(25.dp)
            .height(75.dp)
            .clickable{expanded = true}
        ,
        contentAlignment = Alignment.Center
    ){
        Text(selectedOptionState.value.toString(), fontSize = 30.sp)

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {expanded = false}
        ) {
            DIFFERENCE_SIGNS.forEach{option ->
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
fun DifferenceConditionContent(
    selectedDifferenceSign: MutableState<String>,
    selectedDiff: MutableState<Int>
){
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SignSelector(selectedDifferenceSign)
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .padding(4.dp)
        ){
            NumberPicker(
                value = selectedDiff.value,
                range = 0..30,
                onValueChange = {
                    selectedDiff.value = it
                },
                dividersColor = Color.Gray,
                textStyle = TextStyle(color = Color.White)
            )
        }
    }
}

@Composable
fun DifferenceConditionScreen(
    selectedConditionTypeState: MutableState<ConditionType>,
    saveCondition: (Map<String, Any>, ConditionType) -> Unit
) {
    val selectedDifferenceSign = remember { mutableStateOf(DIFFERENCE_SIGNS[0]) }
    val selectedDiff  = remember { mutableIntStateOf(10) }
    val conditionData = remember {
        derivedStateOf { DifferenceConditionData(
            selectedDifferenceSign.value,
            selectedDiff.intValue
        ) }
    }

    BaseConditionScreen(
        selectedConditionTypeState = selectedConditionTypeState,
        titleText = "Difference Condition",
        conditionHelpResourceId = R.string.difference_condition_help,
        content = {
            DifferenceConditionContent(
                selectedDifferenceSign,
                selectedDiff
            )
        },
        generateConditionData = {
            conditionData.value.toMap()
        },
        saveCondition = saveCondition
    )
}
