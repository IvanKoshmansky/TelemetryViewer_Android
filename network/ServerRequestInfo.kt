package com.example.android.mobileclient.network

import java.sql.Timestamp

//
// описание параметров хранимой процедуры
//

class StoredProcParams (
    val parameterType: Int?,
    val stringValue: String?,
    val intValue: Int?,
    val timestamp: Timestamp?
)

//
// вызов хранимой процедуры
//

class StoredProcCall (
    val name: String,
    val params: Array<StoredProcParams>
)

//
// класс с запросом данных от сервера
// первичный конструктор приватный, его в коде вызвать нельзя
// два вторичных с разными наборами параметров
//

class ServerRequestInfo private constructor (
    val connectString: String,
    val queryStrings: Array<String>?,
    val isStoredProc: Boolean,
    val storedProcCalls: Array<StoredProcCall>?
) {
    constructor(connectString: String, queryStrings: Array<String>): this(connectString, queryStrings, false, null)
    constructor(connectString: String, storedProcCalls: Array<StoredProcCall>): this(connectString, null, true, storedProcCalls)
}
