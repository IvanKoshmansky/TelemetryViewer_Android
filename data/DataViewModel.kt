package com.example.android.mobileclient.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.example.android.mobileclient.device.DeviceManager
import com.example.android.mobileclient.storage.LocalRepository
import com.example.android.mobileclient.utils.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class DataViewModel @Inject constructor (
    application: Application,
    // если параметр объявлен без val или var, то это параметр конструктора,
    // а не свойство, в данном случае он доступен только в теле конструктора {
    // }
    // в функциях-методах класса доступа уже нет
    private val deviceManager: DeviceManager,
    private val localRepository: LocalRepository) : AndroidViewModel(application) { // AndroidViewModel(application) - инициализация суперкласса

    // информация о заголовке таблицы нужна для отрисовки в адаптере
    val dataTableInfo
        get() = localRepository.sharedPreferencesStorage.dataTableInfo

    // кастомная фабрика для работы с sqlite
    val customDataSourceFactory = CustomDataSourceFactory(this, localRepository)
    val config: PagedList.Config = PagedList.Config.Builder()
        .setEnablePlaceholders(false)
        .setPageSize(PAGE_SIZE)
        .setInitialLoadSizeHint(LOAD_COUNT)
        .build()
    val listData: LiveData<PagedList<DataPagingObject>> = LivePagedListBuilder(customDataSourceFactory, config)
        .build()

    // для запуска перерисовки шапки таблицы
    private val _notifyAdapter = MutableLiveData<Boolean>()
    val notifyAdapter
        get() = _notifyAdapter
    fun notifyAdapterDone() {
        _notifyAdapter.value = false
    }

    private val viewModelJob = SupervisorJob()
    val viewModelScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    /**
     * переменные для выбора текущего временного интервала
     */
    var beginDate: SelDate? = null
    var beginTime: SelTime? = null
    var endDate: SelDate? = null
    var endTime: SelTime? = null

    /**
     * LiveData переменные для обновления временных интервалов в текстовых полях
     */
    val beginDateTextView = MutableLiveData<String>()
    val beginTimeTextView = MutableLiveData<String>()
    val endDateTextView = MutableLiveData<String>()
    val endTimeTextView = MutableLiveData<String>()

    //---------------------------------------------------------------------------------------------
    // инициализация контекстной информации данного экрана
    //---------------------------------------------------------------------------------------------
    init {
        if (deviceManager.retrievingDeviceDataBeginDate) {
            beginDate = localRepository.sharedPreferencesStorage.deviceDataBeginDate
        }
        if (deviceManager.retrievingDeviceDataBeginTime) {
            beginTime = localRepository.sharedPreferencesStorage.deviceDataBeginTime
        }
        if (deviceManager.retrievingDeviceDataEndDate) {
            endDate = localRepository.sharedPreferencesStorage.deviceDataEndDate
        }
        if (deviceManager.retrievingDeviceDataEndTime) {
            endTime = localRepository.sharedPreferencesStorage.deviceDataEndTime
        }
        beginDate?.let {
            beginDateTextView.value = String.format(Locale.ENGLISH, "%02d.%02d.%04d", it.day, it.month + 1, it.year)
        }
        beginTime?.let {
            beginTimeTextView.value = String.format(Locale.ENGLISH, "%02d:%02d", it.hour, it.minute)
        }
        endDate?.let {
            endDateTextView.value = String.format(Locale.ENGLISH, "%02d.%02d.%04d", it.day, it.month + 1, it.year)
        }
        endTime?.let {
            endTimeTextView.value = String.format(Locale.ENGLISH, "%02d:%02d", it.hour, it.minute)
        }
    }

    /**
     * переменные-триггеры для показа диалога выбора даты-времени
     */
    val triggerBeginDateDialog = MutableLiveData<Boolean>()
    val triggerBeginTimeDialog = MutableLiveData<Boolean>()
    val triggerEndDateDialog = MutableLiveData<Boolean>()
    val triggerEndTimeDialog = MutableLiveData<Boolean>()
    val triggerInvalidDateTimeDialog = MutableLiveData<Boolean>()

    // управление диалогом при обновлении данных
    val triggerLoadingDialog = MutableLiveData<Int>()

    // показ сообщения о том что нет связи с сервером
    val triggerConnectionErrorMessage = MutableLiveData<Boolean>()

    /**
     * функции задания даты и времени в поля
     */
    fun setBeginDateFromDialog(year: Int, month: Int, dayOfMonth: Int) {
        // String.format для форматированного вывода
        beginDateTextView.value = String.format(Locale.ENGLISH, "%02d.%02d.%04d", dayOfMonth, month + 1, year)
        if (beginDate == null) {
            beginDate = SelDate(year, month, dayOfMonth)
        } else {
            beginDate?.year = year
            beginDate?.month = month
            beginDate?.day = dayOfMonth
        }
        localRepository.sharedPreferencesStorage.deviceDataBeginDate = beginDate
        deviceManager.retrievingDeviceDataBeginDate = true
    }

    fun setBeginTimeFromDialog(hourOfDay: Int, minute: Int) {
        beginTimeTextView.value = String.format(Locale.ENGLISH, "%02d:%02d", hourOfDay, minute)
        if (beginTime == null) {
            beginTime = SelTime(hourOfDay, minute)
        } else {
            beginTime?.hour = hourOfDay
            beginTime?.minute = minute
        }
        localRepository.sharedPreferencesStorage.deviceDataBeginTime = beginTime
        deviceManager.retrievingDeviceDataBeginTime = true
    }

    fun setEndDateFromDialog(year: Int, month: Int, dayOfMonth: Int) {
        endDateTextView.value = String.format(Locale.ENGLISH, "%02d.%02d.%04d", dayOfMonth, month + 1, year)
        if (endDate == null) {
            endDate = SelDate(year, month, dayOfMonth)
        } else {
            endDate?.year = year
            endDate?.month = month
            endDate?.day = dayOfMonth
        }
        localRepository.sharedPreferencesStorage.deviceDataEndDate = endDate
        deviceManager.retrievingDeviceDataEndDate = true
    }

    fun setEndTimeFromDialog(hourOfDay: Int, minute: Int) {
        endTimeTextView.value = String.format(Locale.ENGLISH, "%02d:%02d", hourOfDay, minute)
        if (endTime == null) {
            endTime = SelTime(hourOfDay, minute)
        } else {
            endTime?.hour = hourOfDay
            endTime?.minute = minute
        }
        localRepository.sharedPreferencesStorage.deviceDataEndTime = endTime
        deviceManager.retrievingDeviceDataEndTime = true
    }

    /**
     * обработчики нажатий на кнопки
     */
    fun onBeginDateClick() {
        // подготовить стартовую дату для отображения в компоненте для выбора даты
        if (beginDate == null) {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val day = cal.get(Calendar.DAY_OF_MONTH)
            beginDate = SelDate(year, month, day)
        }
        triggerBeginDateDialog.value = true
    }

    fun onBeginTimeClick() {
        if (beginTime == null) {
            val hour = 0
            val minute = 0
            beginTime = SelTime(hour, minute)
        }
        triggerBeginTimeDialog.value = true
    }

    fun onEndDateClick() {
        if (endDate == null) {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val day = cal.get(Calendar.DAY_OF_MONTH)
            endDate = SelDate(year, month, day)
        }
        triggerEndDateDialog.value = true
    }

    fun onEndTimeClick() {
        if (endTime == null) {
            val hour = 0
            val minute = 0
            endTime = SelTime(hour, minute)
        }
        triggerEndTimeDialog.value = true
    }

    /**
     * основная функция загрузки данных с сервера (по выбранному интервалу)
     */
    fun onLoadDataClick() {
        // подготовить информацию для запроса
        val objectId = deviceManager.currentObjectId
        objectId?.let {
            // есть выбранное устройтсво
            if ((beginDate != null) && (beginTime != null) && (endDate != null) && (endTime != null)) {
                // выбраны дата и время
                val timestamps = DateTimeUtils.dateTimeRangeToTimestampRange(beginDate!!.year, beginDate!!.month, beginDate!!.day, beginTime!!.hour, beginTime!!.minute,
                                                                             endDate!!.year, endDate!!.month, endDate!!.day, endTime!!.hour, endTime!!.minute)
                if (timestamps.end > timestamps.begin) {
                    triggerLoadingDialog.value = LOADING_DIALOG_SHOW // показать диалог "обновление данных"

                    // обработчик внутренних исключений при загрузке (например, нет соединения с сервером или загрузка была остановлена пользователем)
                    val handler = CoroutineExceptionHandler { _, exception ->
                        exception.printStackTrace()
                        if (exception.message.equals("error")) {
                            // показать сообщение "проверьте соединение с сетью" если исключение с сообщением "error"
                            triggerConnectionErrorMessage.value = true
                        }
                    }

                    currentJob = viewModelScope.launch(handler) {
                        // сам запрос к серверу и обновление хранилища
                        localRepository.refreshArchiveData(objectId, timestamps.begin, timestamps.end)
                        // уведомить адаптер о необходимости отобразить новые данные
                        _notifyAdapter.postValue(true) // postValue для работы из других потоков
                        // убрать диалог
                        triggerLoadingDialog.postValue(LOADING_DIALOG_HIDE)
                    }
                } else {
                    // показать диалог о том, что нужно выбрать корректный интервал времени
                    triggerInvalidDateTimeDialog.value = true
                }
            } else {
                // показать диалог о том, что нужно выбрать корректный интервал времени
                triggerInvalidDateTimeDialog.value = true
            }
        }
    }
    /**/

    // остановка текущей загрузки
    // TODO: уточнить почему в этом случае не отрабатывает viewModelJob
    private var currentJob: Job? = null
    fun cancelLoading() {
        currentJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}
