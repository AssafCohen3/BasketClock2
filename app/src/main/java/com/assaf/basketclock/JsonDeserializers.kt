package com.assaf.basketclock

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