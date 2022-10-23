package com.example.android.mobileclient.network

import android.os.StrictMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.security.InvalidParameterException
import java.sql.*

// <T> объект сопоставления с одной записью JSON (одна строка в таблице)
suspend fun <T: Any?> serverRequest(serverRequestInfo: ServerRequestInfo, channel: Channel<MutableList<T>>, fromJson: (JSONObject) -> T) {

    var con: Connection? = null
    var st: Statement? = null
    var rs: ResultSet? = null

    // флаг о том, что в списке исключений есть исключение при остановке корутины
    // нужен для дальнейшей логики обработки поскольку при отмене корутины генерируется сразу несколько исключений
    // например если процесс прервется на моменте парсинга JSON то возникнет еще и JSONException
    var isCanceled = false

    try {

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()
            .build())

        Class.forName("net.sourceforge.jtds.jdbc.Driver")

        try {
            // тестовая задержка
//            delay(10000)

            con = DriverManager.getConnection(serverRequestInfo.connectString)

            con?.let {
                // если con != null

                // количество запросов в сессии
                val iterCount =
                    if (serverRequestInfo.isStoredProc) serverRequestInfo.storedProcCalls?.size
                    else serverRequestInfo.queryStrings?.size

                for (step in 0 until iterCount!!) {
                    try {
                        if (!serverRequestInfo.isStoredProc) {
                            // не хранимая процедура
                            st = it.createStatement()
                            rs = st?.executeQuery(serverRequestInfo.queryStrings!![step])
                        } else {
                            // хранимая процедура
                            // применяем вычисляемое выражение ${}
                            var callRequest =
                                "{call ${serverRequestInfo.storedProcCalls!![step].name} ("
                            val paramsCount = serverRequestInfo.storedProcCalls[step].params.size
                            for (jj in 0 until paramsCount) {
                                callRequest += if (jj < paramsCount - 1) "?, " else "?)}"
                            }
                            val call = it.prepareCall(callRequest)
                            for (ii in 0 until paramsCount) {
                                // используем оператор "when" вместо цепочки "if-else" для более компактного кода
                                when (serverRequestInfo.storedProcCalls[step].params[ii].parameterType) {
                                    Types.NVARCHAR -> call.setString(
                                        ii + 1,
                                        serverRequestInfo.storedProcCalls[step].params[ii].stringValue
                                    )
                                    Types.INTEGER -> {
                                        // здесь через локаьную переменную и элвис оператор
                                        val intValue: Int =
                                            serverRequestInfo.storedProcCalls[step].params[ii].intValue
                                                ?: 0
                                        call.setInt(ii + 1, intValue)
                                    }
                                    Types.TIMESTAMP -> call.setTimestamp(
                                        ii + 1,
                                        serverRequestInfo.storedProcCalls[step].params[ii].timestamp
                                    )
                                    else -> throw InvalidParameterException("Stored procedure parameter not supported!")
                                }
                            }
                            rs = call?.executeQuery()
                        }

                        rs?.let {
                            val container = mutableListOf<T>()
                            val columnCount = rs!!.metaData.columnCount
                            while (it.next()) {
                                // !! не только вызывает exception если rs == null
                                // но и преобразует его к не null-совместимому типу
                                val jsonObject = JSONObject()
                                for (ii in 1..columnCount) {
                                    // разница между ".." и "until" : until исключает последнее значение
                                    jsonObject.put(
                                        it.metaData.getColumnName(ii),
                                        it.getString(ii) ?: ""
                                    )
                                    // ?: (элвис оператор)
                                    // если getString(ii) не равно null то вернуть результат getString(ii)
                                    // иначе вернуть то что стоит после elvis оператора ("")
                                }
                                val element = fromJson(jsonObject)
                                container.add(element)
                            }

                            if (!container.isEmpty()) {
                                channel.send(container)
                            }

                            // все запросы выполнены
                            if (step == iterCount - 1) channel.close()
                        }

                    } catch (e: CancellationException) {
                        isCanceled = true
                        e.printStackTrace()
                        throw IOException("cancel")
                    } catch (e: SQLException) {
                        e.printStackTrace()
                        if (!isCanceled)
                            throw IOException("error")
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        if (!isCanceled)
                            throw IOException("error")
                    } finally {
                        rs?.close()
                        st?.close()
                    }
                }
            }

        } catch (e: CancellationException) {
            isCanceled = true
            e.printStackTrace()
            throw IOException("cancel")
        } catch (e: SQLException) {
            e.printStackTrace()
            if (!isCanceled)
                throw IOException("error")
        } catch (e: Exception) {
            e.printStackTrace()
            if (!isCanceled)
                throw IOException("error")
        } finally {
            try {
                con?.close()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }

    } catch (e: CancellationException) {
        isCanceled = true
        e.printStackTrace()
        throw IOException("cancel")
    } catch (e: ClassNotFoundException) {
        e.printStackTrace()
        try {
            rs?.close()
            st?.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }
}
