package com.example.android.mobileclient.data

import com.example.android.mobileclient.data.devicelog.DeviceLogFragment
import com.example.android.mobileclient.di.ActivityScope
import com.example.android.mobileclient.main.MainActivity
import dagger.Subcomponent

@ActivityScope
@Subcomponent
interface DataComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(): DataComponent
    }

    fun inject(fragment: DataFragment)
    fun inject(fragment: DeviceLogFragment)
}
