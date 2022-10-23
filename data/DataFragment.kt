package com.example.android.mobileclient.data

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android.mobileclient.R
import com.example.android.mobileclient.databinding.FragmentDataBinding
import com.example.android.mobileclient.main.MainActivity
import com.example.android.mobileclient.storage.DataTableInfo
import com.example.android.mobileclient.utils.LOADING_DIALOG_HIDE
import com.example.android.mobileclient.utils.LOADING_DIALOG_IDLE
import com.example.android.mobileclient.utils.LOADING_DIALOG_SHOW
import com.example.android.mobileclient.utils.LoadingDialogFragment
import timber.log.Timber
import javax.inject.Inject

class DataFragment : Fragment(), LoadingDialogFragment.LoadingDialogListener {

    @Inject
    lateinit var dataViewModel: DataViewModel

    private var viewModelAdapter: DataPagedListAdapter? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).deviceComponent.inject(this)
        (activity as MainActivity).dataComponent.inject(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dataViewModel.listData.observe(viewLifecycleOwner, Observer<PagedList<DataPagingObject>> {
            it -> it.apply {
                viewModelAdapter?.submitList(it)
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val binding: FragmentDataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_data, container, false)
        binding.setLifecycleOwner(viewLifecycleOwner)
        binding.viewModel = dataViewModel

        // программное управление параметрами шрифта делается через TypeFace
        // ResourcesCompat для совместимости с API level < 23
        val typeFace = ResourcesCompat.getFont(context!!, R.font.cormorant_light)
        val tabSize = resources.getDimensionPixelSize(R.dimen.tab_text_width)
        val padding = resources.getDimensionPixelSize(R.dimen.padding_small)
        viewModelAdapter = DataPagedListAdapter(dataViewModel.dataTableInfo,
                                                typeFace!!,
                                                resources.getDimensionPixelSize(R.dimen.body_font_size), // TODO: разобраться почему дает неправильный расчет
                                                tabSize, // преобразуется из dp в пикесли
                                                padding) // преобразуется из dp в пикесли

        // использовать напрямую через binding а не findViewById
        binding.dataRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = viewModelAdapter
        }

        setupHeader(typeFace, tabSize, padding, dataViewModel.dataTableInfo, binding)

        /**
         * обсерверы для реализации логики данного экрана
         */
        // вызов диалога задания начальной даты
        dataViewModel.triggerBeginDateDialog.observe(this, Observer {
            if (it == true) {
                dataViewModel.beginDate?.let {
                    val datePickerDialog = DatePickerDialog(context, DatePickerDialog.OnDateSetListener {
                        // в конструкторе DatePickerDialog.OnDateSetListener один параметр - лямбда выражение, круглые скобки не нужны
                        view: DatePicker?, year: Int, month: Int, dayOfMonth: Int -> dataViewModel.setBeginDateFromDialog(year, month, dayOfMonth)
                    }, it.year, it.month, it.day) // остальные параметры конструктора DatePickerDialog()
                    datePickerDialog.show()
                }
                dataViewModel.triggerBeginDateDialog.value = false
            }
        })

        // вызов диалога задания начального времени
        dataViewModel.triggerBeginTimeDialog.observe(this, Observer {
            if (it == true) {
                dataViewModel.beginTime?.let {
                    val timePickerDialog = TimePickerDialog(context, TimePickerDialog.OnTimeSetListener {
                        view, hourOfDay, minute -> dataViewModel.setBeginTimeFromDialog(hourOfDay, minute)
                    }, it.hour, it.minute, false)
                    timePickerDialog.show()
                }
                dataViewModel.triggerBeginTimeDialog.value = false
            }
        })

        // вызов диалога задания конечной даты
        dataViewModel.triggerEndDateDialog.observe(this, Observer {
            if (it == true) {
                dataViewModel.endDate?.let {
                    val datePickerDialog = DatePickerDialog(context, DatePickerDialog.OnDateSetListener {
                        view, year, month, dayOfMonth -> dataViewModel.setEndDateFromDialog(year, month, dayOfMonth)
                    }, it.year, it.month, it.day)
                    datePickerDialog.show()
                }
                dataViewModel.triggerEndDateDialog.value = false
            }
        })

        // вызов диалога задания конечного времени
        dataViewModel.triggerEndTimeDialog.observe(this, Observer {
            if (it == true) {
                dataViewModel.endTime?.let {
                    val timePickerDialog = TimePickerDialog(context, TimePickerDialog.OnTimeSetListener {
                        view, hourOfDay, minute -> dataViewModel.setEndTimeFromDialog(hourOfDay, minute)
                    }, it.hour, it.minute, false)
                    timePickerDialog.show()
                }
            }
        })

        // показ диолога о том, что нужно выбрать корректный интервал дат-времен
        dataViewModel.triggerInvalidDateTimeDialog.observe(this, Observer {
            if (it == true) {
                Toast.makeText(context, R.string.invalid_date_time_dialog, Toast.LENGTH_SHORT).show()
                dataViewModel.triggerInvalidDateTimeDialog.value = false
            }
        })

        // показ диалога о том, что отсутствует соединение с сервером
        dataViewModel.triggerConnectionErrorMessage.observe(this, Observer {
            if (it == true) {
                // убрать с экрана диалоговое окно "загрузка..."
                dataViewModel.triggerLoadingDialog.value = LOADING_DIALOG_HIDE
                Toast.makeText(context, R.string.connection_erroe_message, Toast.LENGTH_SHORT).show()
                dataViewModel.triggerConnectionErrorMessage.value = false
            }
        })

        // обновить таблицу после загрузки данных
        dataViewModel.notifyAdapter.observe(this, Observer {
            if (it == true) {
                setupHeader(typeFace, tabSize, padding, dataViewModel.dataTableInfo, binding)
                val adapter = binding.dataRecyclerView.adapter as DataPagedListAdapter
                adapter.dataTableInfo = dataViewModel.dataTableInfo // новое заполнение столбцов
                val layoutManager = binding.dataRecyclerView.layoutManager
                // данный трюк заставляет адаптер полностью пересоздать заново все ViewHolder'ы
                // с учетом новой структуры столбцов
                binding.dataRecyclerView.adapter = null
                binding.dataRecyclerView.layoutManager = null
                binding.dataRecyclerView.adapter = adapter
                binding.dataRecyclerView.layoutManager = layoutManager
                dataViewModel.listData.value?.dataSource?.invalidate()
                //
                dataViewModel.notifyAdapterDone()
            }
        })

        var loadingDialogFragment: LoadingDialogFragment? = null

        // показ диалога при обновлении данных
        dataViewModel.triggerLoadingDialog.observe(this, Observer {
            if (it == LOADING_DIALOG_SHOW) {
                loadingDialogFragment = LoadingDialogFragment()
                loadingDialogFragment?.listener = this // присваиваем ссылку на класс, который реализовал интерфейс
                loadingDialogFragment?.show(getFragmentManager(), LoadingDialogFragment.TAG)
                dataViewModel.triggerLoadingDialog.value = LOADING_DIALOG_IDLE
            }
            if (it == LOADING_DIALOG_HIDE) {
                loadingDialogFragment?.dismiss()
                dataViewModel.triggerLoadingDialog.value = LOADING_DIALOG_IDLE
            }
        })

        return binding.root
    }

    // программное создание шапки таблицы
    private fun setupHeader(typeFace: Typeface?, tabSize: Int, padding: Int, tableInfo: DataTableInfo?, binding: FragmentDataBinding) {
        val headerLayout = binding.tableHeaderLayout

        // сначала удалить все предыдущие столбцы
        val childCount = headerLayout.childCount
        if (childCount > 0) {
            for (ii in 1 until childCount) {
                headerLayout.removeViewAt(1)
            }
        }

        val tabParams = LinearLayout.LayoutParams(tabSize, LinearLayout.LayoutParams.WRAP_CONTENT)
        tableInfo?.let { info ->
            info.info.forEach {
                val tabHeader = TextView(context)
                tabHeader.layoutParams = tabParams
                tabHeader.setTypeface(typeFace)
                tabHeader.setTextSize(14.0f)
                tabHeader.setGravity(Gravity.CENTER)
                tabHeader.setPadding(padding, padding, padding, padding)
                tabHeader.setText(it.value.visibleColumnName)
                headerLayout.addView(tabHeader)
            }
        }
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        // отмена загрузки
        dataViewModel.cancelLoading()
    }
}
