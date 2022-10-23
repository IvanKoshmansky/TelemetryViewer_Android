package com.example.android.mobileclient.data

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.PositionalDataSource
import com.example.android.mobileclient.storage.LocalRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

//
// кастомный дата сорс для пейджинга + его фабрика
//

const val LOAD_COUNT = 10
const val PAGE_SIZE = 10

class CustomDataSource (val dataViewModel: DataViewModel, val localRepository: LocalRepository) : PositionalDataSource<DataPagingObject>() {

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<DataPagingObject>) {
        dataViewModel.viewModelScope.launch {
            val totalCount = localRepository.getDataTableRowsCount()
            val position = computeInitialLoadPosition(params, totalCount)
            val loadSize = computeInitialLoadSize(params, position, totalCount)
            val rows = localRepository.getDataTableRows(position, loadSize)
            callback.onResult(rows, position, totalCount)
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<DataPagingObject>) {
        dataViewModel.viewModelScope.launch {
            // количество строк для загрузки всегда кратно PAGE_SIZE
            // и может грузануть за пределами, т.е. если было передано totalCount = 21
            // то в завершающую итерацию он запросит startPosition = 21 и loadSize = 10
            // в этом случае надо передавать пустой список - на этом успокоится
            val rows = localRepository.getDataTableRows(params.startPosition, params.loadSize)
            callback.onResult(rows)
        }
    }
}

class CustomDataSourceFactory (val dataViewModel: DataViewModel, val localRepository: LocalRepository) : DataSource.Factory<Int, DataPagingObject>() {
    val sourceLiveData = MutableLiveData<CustomDataSource>()
    var latestSource: CustomDataSource? = null
    override fun create(): DataSource<Int, DataPagingObject> {
        latestSource = CustomDataSource(dataViewModel, localRepository)
        sourceLiveData.postValue(latestSource)
        return latestSource as CustomDataSource
    }
}
