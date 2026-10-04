package com.loadtracker.pro

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WearableMessageReceiverService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WearableDataSyncManager.PATH_WRIST_ACTION) {
            val actionCommand = String(messageEvent.data, Charsets.UTF_8)
            val activePro = TrackingService.activeProNumber ?: return

            serviceScope.launch {
                val dao = AppDatabase.getDatabase(applicationContext).loadDao()
                when (actionCommand) {
                    WearableDataSyncManager.ACTION_ARRIVE_SHIPPER -> {
                        dao.updateTripState(activePro, "ACTIVE_SHIPPER")
                        TrackingService.activeSegment = "Paused"
                    }
                    WearableDataSyncManager.ACTION_DEPART_SHIPPER -> {
                        dao.updateTripState(activePro, "ACTIVE_LOADED")
                        TrackingService.activeSegment = "Loaded"
                    }
                    WearableDataSyncManager.ACTION_ARRIVE_CONSIGNEE -> {
                        dao.updateTripState(activePro, "ACTIVE_CONSIGNEE")
                        TrackingService.activeSegment = "Paused"
                    }
                    WearableDataSyncManager.ACTION_COMPLETE_LOAD -> {
                        dao.updateTripState(activePro, "COMPLETED")
                        TrackingService.activeSegment = "Paused"
                        TrackingService.activeProNumber = null
                    }
                }
            }
        }
    }
}
