package com.example.android.mobileclient.data

//
// используется для связи пейджинга с SQLite database
//

data class DataPagingObject (
    val id: Int,                   // id строки начиная с нуля
    val dateTime: String,          // дата-время строки в строковом формате
    val isAlarm: Boolean,          // признак тревожности (красная строка, когда от сервера приходит reason == valueTxt)
    val columns: Map<Int, String>  // значения колонок таблицы в виде: "id параметра" - "строковое значение"
)
