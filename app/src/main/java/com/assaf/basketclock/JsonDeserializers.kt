package com.assaf.basketclock

import android.annotation.SuppressLint
import com.assaf.basketclock.conditions.Clock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale


object SimpleDateSerializer: KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)
    private val format = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    override fun deserialize(decoder: Decoder): Date {
        val dateString = decoder.decodeString()
        return format.parse(dateString)
    }

    override fun serialize(encoder: Encoder, value: Date) {
        val dateString = format.format(value)
        encoder.encodeString(dateString)
    }
}

object DateWithTimeSerializer: KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DateWithTime", PrimitiveKind.STRING)
    private val format = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.ENGLISH)

    override fun deserialize(decoder: Decoder): Date {
        val dateString = decoder.decodeString()
        return format.parse(dateString)
    }

    override fun serialize(encoder: Encoder, value: Date) {
        val dateString = format.format(value)
        encoder.encodeString(dateString)
    }
}

object KZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ZonedDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ZonedDateTime) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): ZonedDateTime {
        val string = decoder.decodeString()
        return ZonedDateTime.parse(string)
    }
}

object ClockSerializer : KSerializer<Clock> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Clock", PrimitiveKind.STRING)

    @SuppressLint("DefaultLocale")
    override fun serialize(encoder: Encoder, value: Clock) {
        encoder.encodeString("PT${value.minutes}M${String.format("%05.2f", value.seconds)}S")
    }

    override fun deserialize(decoder: Decoder): Clock {
        val string = decoder.decodeString()
        return Clock(string.substring(2, 4).toInt(), string.substring(5, 10).toDouble())
    }
}
