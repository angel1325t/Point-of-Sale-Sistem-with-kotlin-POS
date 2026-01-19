package com.dev.point_of_sale_sistem_with_kotlin_pos.core.network

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.sync.SyncScheduler
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class NetworkObserver(
    context: Context,
    owner: LifecycleOwner,
    networkMonitor: NetworkMonitor
) {

    private val scheduler = SyncScheduler(context)

    init {
        owner.lifecycleScope.launch {
            networkMonitor.isOnline()
                .distinctUntilChanged()
                .collect { isOnline ->
                    if (isOnline) {
                        scheduler.schedule()
                    }
                }
        }
    }
}
