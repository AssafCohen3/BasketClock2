package com.assaf.basketclock.conditions

import android.annotation.SuppressLint
import com.assaf.basketclock.data.Condition
import com.assaf.basketclock.R
import com.assaf.basketclock.scheduling.ScheduledGameWithConditions
import com.assaf.basketclock.toDataClass

enum class ConditionType {
    NONE, TIME, DIFFERENCE, LEADER
}

interface AbstractConditionData{
    fun conditionType(): ConditionType

    fun describeCondition(condition: Condition): String

    fun conditionIcon(): Int

    suspend fun calculateMilliSecondsUntilNextRelevant(game: ScheduledGameWithConditions): Long
}

data class DifferenceConditionData(
    val sign: String,
    val difference: Int
): AbstractConditionData{
    override fun conditionType(): ConditionType {
        return ConditionType.DIFFERENCE
    }

    override fun describeCondition(condition: Condition): String {
        return "The difference is $sign than $difference."
    }

    override fun conditionIcon(): Int {
        return R.drawable.ic_difference_condition_icon_3
    }

    override suspend fun calculateMilliSecondsUntilNextRelevant(game: ScheduledGameWithConditions): Long {
        return calculateDifferenceConditionTimeUntilRelevant(game, this)
    }
}

data class LeaderConditionData(
    val leaderTeamId: Int
): AbstractConditionData{
    override fun conditionType(): ConditionType {
        return ConditionType.LEADER
    }

    override fun describeCondition(condition: Condition): String {
        val leaderTeamName = if (condition.homeTeamId == leaderTeamId) condition.homeTeamName else condition.awayTeamName
        return "The leader is $leaderTeamName."
    }

    override fun conditionIcon(): Int {
        return R.drawable.ic_leader_condition_icon
    }

    override suspend fun calculateMilliSecondsUntilNextRelevant(game: ScheduledGameWithConditions): Long {
        return calculateLeaderConditionTimeUntilRelevant(game, this)
    }
}

data class TimeConditionData(
    val startQuarter: Int,
    val startQuarterDisplay: String,
    val startMinute: Int,
    val endQuarter: Int,
    val endQuarterDisplay: String,
    val endMinute: Int
): AbstractConditionData{
    val startMoment
        get() = GameMoment(startQuarter, Clock(startMinute, 0.0))

    val endMoment
        get() = GameMoment(endQuarter, Clock(endMinute, 0.0))

    override fun conditionType(): ConditionType {
        return ConditionType.TIME
    }

    @SuppressLint("DefaultLocale")
    override fun describeCondition(condition: Condition): String {
        val rangeStartMinuteText = if (startQuarter == 5) "" else String.format(" %02d:00", startMoment.getReverseClock().minutes)
        val rangeEndMinuteText = if (endQuarter == 5) "" else String.format(" %02d:00", endMoment.getReverseClock().minutes)
        return "The game is between $startQuarterDisplay$rangeStartMinuteText and $endQuarterDisplay$rangeEndMinuteText."
    }

    override fun conditionIcon(): Int {
        return R.drawable.ic_time_condition_icon
    }

    override suspend fun calculateMilliSecondsUntilNextRelevant(game: ScheduledGameWithConditions): Long {
        return calculateTimeConditionTimeUntilRelevant(game, this)
    }
}


fun decodeConditionData(condition: Condition): AbstractConditionData{
    return when (condition.conditionType){
        ConditionType.DIFFERENCE ->
            condition.conditionData.toDataClass<DifferenceConditionData>()
        ConditionType.LEADER ->
            condition.conditionData.toDataClass<LeaderConditionData>()
        ConditionType.TIME ->
            condition.conditionData.toDataClass<TimeConditionData>()
        else ->
            throw Exception("Unknown condition type: ${condition.conditionType}")
    }
}
