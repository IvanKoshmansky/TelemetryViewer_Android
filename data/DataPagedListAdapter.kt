package com.example.android.mobileclient.data

import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.android.mobileclient.R
import com.example.android.mobileclient.databinding.DataItemBinding
import com.example.android.mobileclient.storage.DataTableInfo
import org.w3c.dom.Text

class DataPagedListAdapter (var dataTableInfo: DataTableInfo?, val typeFace: Typeface, val fontSize: Int, val tabSize: Int, val padding: Int) :
    PagedListAdapter<DataPagingObject, DataPagedListAdapter.ViewHolder> (
        DataDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataPagedListAdapter.ViewHolder {
        return ViewHolder.from(parent, dataTableInfo, typeFace, fontSize, tabSize, padding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, dataTableInfo)
    }

    class ViewHolder private constructor(val binding: DataItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DataPagingObject?, dataTableInfo: DataTableInfo?) {
            binding.dataRow = item
            // загрузить данные в layout контейнер
            dataTableInfo?.let { info ->
                item?.let { item ->
                    val container = binding.dataItemLayout
                    var textViewIndex = 1 // первый TextView занят на дату-время
                    info.info.forEach {
                        // извлечь соответствующее значение по ключу
                        val textValue = item.columns.get(it.key)
                        // присвоить значение текстовому полю в контейнере
                        if (textViewIndex < container.childCount) {
                            val textView = binding.dataItemLayout.getChildAt(textViewIndex) as TextView
                            textView.setText(textValue)
                            textViewIndex++
                        }
                    }
                }
            }
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup, dataTableInfo: DataTableInfo?, typeFace: Typeface, fontSize: Int, tabSize: Int, padding: Int): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = DataItemBinding.inflate(layoutInflater, parent, false)
                val layout = binding.dataItemLayout

                dataTableInfo?.let {
                    // есть описание заголовка таблицы
                    it.info.forEach {
                        val tab = TextView(parent.context)
                        val tabParams = LinearLayout.LayoutParams(tabSize, LinearLayout.LayoutParams.WRAP_CONTENT)
                        tab.layoutParams = tabParams
                        tab.setTypeface(typeFace)
//                        tab.setTextSize(fontSize.toFloat())
                        tab.setTextSize(14.0f) // хардкод
                        tab.setGravity(Gravity.CENTER)
                        tab.setPadding(padding, padding, padding, padding)
                        layout.addView(tab)
                    }
                }

                return ViewHolder(binding)
            }
        }
    }
}

class DataDiffCallback : DiffUtil.ItemCallback<DataPagingObject> () {
    override fun areItemsTheSame(oldItem: DataPagingObject, newItem: DataPagingObject): Boolean {
        // здесь сравнение по id
        return (oldItem.id == newItem.id)
    }

    override fun areContentsTheSame(oldItem: DataPagingObject, newItem: DataPagingObject): Boolean {
        // здесь сравнение по содержимому
        return (oldItem == newItem)
    }
}
