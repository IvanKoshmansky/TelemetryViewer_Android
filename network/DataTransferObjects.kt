package com.example.android.mobileclient.network

import com.example.android.mobileclient.database.DatabaseDeviceInfo
import com.example.android.mobileclient.database.DatabaseParameterInfo
import com.example.android.mobileclient.database.DeviceLogObject
import org.json.JSONObject
import java.lang.NumberFormatException
import java.nio.ByteBuffer
import java.util.zip.CRC32

// соответствует JSON одной строчки в таблице на сервере
data class NetworkDeviceInfo (
    val objectId: Int,
    val devtypeId: Int,
    val name: String,
    val uid: String
)

// MutableList<NetworkDeviceInfo> - контейнер, который содержит список строк таблицы по запросу на сервер
// функция расширения использует стандартную функцию Kotlin для создания нового массива
// каждый элемент нового массива создается по коду в лямбда-выражении
fun MutableList<NetworkDeviceInfo>.asDatabaseModel(): Array<DatabaseDeviceInfo> {
    return map {
        DatabaseDeviceInfo(
            objectId = it.objectId,
            devtypeId = it.devtypeId,
            name = it.name,
            uid = it.uid
        )
    }.toTypedArray()
}

data class NetworkParameterInfo (
    val devtypeId: Int,             // идентификация параметра на сервере
    val parametersListId: Int,      // идентификация параметра на сервере
    val parameterTypesId: Int,      // идентификация параметра на сервере
    val parameterTypeClassId: Int,  // идентификация параметра на сервере
    val paramName: String,          // имя параметра
    val mnemoCode: String,          // мнемокод
    val measUnit: String,           // единица измерения
    val paramPositios: String,      // количество знаков до запятой
    val paramPrecision: String      // количество знаков после запятой
) {
    // расчет контрольной суммы для использования в качестве уникального ключа в рамках таблицы Room (для связки таблиц)
    // расчет производится по Id - полям
    fun calcUniqueId(): Long {
        val buffer = ByteBuffer.allocate(64)
        buffer.putInt(devtypeId)
        buffer.putInt(parametersListId)
        buffer.putInt(parameterTypesId)
        buffer.putInt(parameterTypeClassId)
        val crc32 = CRC32()
        crc32.reset()
        crc32.update(buffer.array())
        return crc32.value
    }
}

fun MutableList<NetworkParameterInfo>.asDatabaseModel(): List<DatabaseParameterInfo> {
    return map {
        DatabaseParameterInfo (
            paramUniqueId = it.calcUniqueId(),
            devtypeId = it.devtypeId,
            parametersListId = it.parametersListId,
            parameterTypesId = it.parameterTypesId,
            parameterTypeClassId = it.parameterTypeClassId,
            paramName = it.paramName,
            mnemoCode = it.mnemoCode,
            measUnit = it.measUnit,
            // вычисляемый блок Exception
            // в случае не успешного преобразования возвращается константа 9
            paramPositions = try { it.paramPositios.toInt() } catch (e: NumberFormatException) { 9 },
            paramPrecision = try { it.paramPrecision.toInt() } catch (e: NumberFormatException) { 0 }
        )
    }
}

// для разбора JSON которые приходят с сервера при запросе данных (для таблицы с данными)
data class NetworkDataResponce (
    val parametersListId: Int,  // ID параметра для разбора
    val valueDateTime: String,  // дата-время в специальном строковом формате
    val paramValueTXT: String,  // значение параметра в стороковом формате
    val reason: String          // признак тревожности в строковом формате
)

private object ReadableConsts {
    const val FW_VERSION = "Версия прошивки"
    const val MAX_PACKET_SIZE = "Макс. размер пакета"
    const val REASON = "Причина выхода на связь"
    const val CHANNEL = "Канал связи"
    const val SIGNAL_LEVEL = "Уровень сигнала"
}

// объект который маппится из JSON при запросе журнала устройств
// причем тип динамического параметра (Int или String) заранее неизвестен
data class NetworkDeviceLogResponce (
    val sessionBeginDateTime: String,
    val sessionDuration: Int,
    val parametersListId: Int,
    val paramValueTxtInt: Int?,
    val paramValueTxtString: String?
) {
    // паттерн "фабрика", статический объект
    object Factory {
        fun create(json: JSONObject, readableParamsMap: Map<Int, String>): NetworkDeviceLogResponce {
            // постоянная "шапка"
            val sessionBeginDateTime = json.getString("SessionBeginDateTime").substring(0, 16)
            val sessionDuration = json.getInt("SessionDuration")
            val parametersListId = json.getInt("ParametersListID")
            // динамическая часть
            var paramValueTxtInt: Int? = null
            var paramValueTxtString: String? = null
            if (readableParamsMap.containsKey(parametersListId)) {
                val paramName = readableParamsMap.get(parametersListId)
                // разбор значения по имени параметра через if-else для улучшения производительности JVM с Kotlin
                // (здесь разбирается полный набор всех возможных полей которые могут быть в JSON)
                if (paramName.equals(ReadableConsts.FW_VERSION)) {
                    paramValueTxtString = json.getString("ParamValueTXT")
                } else if (paramName.equals(ReadableConsts.MAX_PACKET_SIZE)) {
                    paramValueTxtInt = json.getInt("ParamValueTXT")
                } else if (paramName.equals(ReadableConsts.REASON)) {
                    paramValueTxtInt = json.getInt("ParamValueTXT")
                } else if (paramName.equals(ReadableConsts.CHANNEL)) {
                    paramValueTxtInt = json.getInt("ParamValueTXT")
                } else if (paramName.equals(ReadableConsts.SIGNAL_LEVEL)) {
                    paramValueTxtInt = json.getString("ParamValueTXT").toInt()
                }
            }
            // создать объект с помощью фабрики
            return NetworkDeviceLogResponce(sessionBeginDateTime, sessionDuration, parametersListId, paramValueTxtInt, paramValueTxtString)
        }
    }

    // проверить что хотя бы одно из двух динамических значений не равно null (т.е. paramValueTxt разобрался при приеме с сервера)
    fun isNotEmpty(): Boolean = (paramValueTxtInt != null) || (paramValueTxtString != null)
}

// объект который совпадает с DeviceLogObject, но не содержит _id + имеет дополнительную функцию конкатенации-парсинга промежуточных серверных объектов
data class NetworkDeviceLogObject (
    var datetime: String,
    var duration: Int?,
    var fwVersion: String?,
    var channel: Int?,
    var signalLevel: Int?
) {
    fun concatNetworkDeviceLogResponce(networkDeviceLogResponce: NetworkDeviceLogResponce, readableParamsMap: Map<Int, String>) {
        if (readableParamsMap.containsKey(networkDeviceLogResponce.parametersListId)) {
            val paramName = readableParamsMap.get(networkDeviceLogResponce.parametersListId)
            // разбор значения по имени параметра через if-else для улучшения производительности JVM с Kotlin
            // (здесь разбирается полный набор всех возможных полей которые могут быть в JSON)
            if (paramName.equals(ReadableConsts.FW_VERSION)) {
                fwVersion = networkDeviceLogResponce.paramValueTxtString ?: ""
//                эти параметры не требуются
//            } else if (paramName.equals("Максимальная длина пакета")) {
//            } else if (paramName.equals("Причина выхода на связь")) {
            } else if (paramName.equals(ReadableConsts.CHANNEL)) {
                channel = networkDeviceLogResponce.paramValueTxtInt ?: -1
            } else if (paramName.equals(ReadableConsts.SIGNAL_LEVEL)) {
                signalLevel = networkDeviceLogResponce.paramValueTxtInt ?: -1
            }
        }
    }

    fun isNotEpty(): Boolean = (duration != null) && (fwVersion != null) && (channel != null) && (signalLevel != null)
}

fun MutableList<NetworkDeviceLogObject>.asDatabaseModel(): Array<DeviceLogObject> {
    // возвращает список с преобразованными элементами
    return map {
        DeviceLogObject(
            datetime = it.datetime,
            duration = it.duration!!,
            fwVersion = it.fwVersion!!,
            channel = it.channel!!,
            signalLevel = it.signalLevel!!
        )
    }.toTypedArray()
}
