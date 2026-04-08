package com.day.antsschool.server.config

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

// List<String>을 "|" 구분 문자열로 DB에 저장
@Converter
class StringListConverter : AttributeConverter<List<String>, String> {
    override fun convertToDatabaseColumn(attribute: List<String>): String =
        attribute.joinToString("|")

    override fun convertToEntityAttribute(dbData: String): List<String> =
        if (dbData.isBlank()) emptyList() else dbData.split("|")
}
