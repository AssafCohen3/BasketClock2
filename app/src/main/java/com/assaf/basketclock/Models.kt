package com.assaf.basketclock

import com.assaf.basketclock.conditions.ConditionType
import java.time.ZonedDateTime


data class ConditionData(
    val gameId: String,
    val gameDateTime: ZonedDateTime,
    val homeTeamId: Int,
    val homeTeamName: String,
    val homeTeamTricode: String,
    val awayTeamId: Int,
    val awayTeamName: String,
    val awayTeamTricode: String,
    val conditionType: ConditionType,
    val conditionData: Map<String, Any>
)
