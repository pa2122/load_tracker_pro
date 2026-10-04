package com.loadtracker.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

class WearMainActivity : ComponentActivity() {

    private val viewModel: LoadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val allLoadsList by viewModel.allLoads.collectAsState(initial = emptyList())
            val activeWatchLoad = remember(allLoadsList) {
                allLoadsList.firstOrNull { it.tripState != "COMPLETED" }
            }

            WearActiveTripHud(
                activeLoad = activeWatchLoad,
                onWristAction = { actionCommand ->
                    val activePro = activeWatchLoad?.proNumber
                    if (activePro != null) {
                        when (actionCommand) {
                            WearableDataSyncManager.ACTION_ARRIVE_SHIPPER -> viewModel.updateTripState(activePro, "ACTIVE_SHIPPER")
                            WearableDataSyncManager.ACTION_DEPART_SHIPPER -> viewModel.updateTripState(activePro, "ACTIVE_LOADED")
                            WearableDataSyncManager.ACTION_ARRIVE_CONSIGNEE -> viewModel.updateTripState(activePro, "ACTIVE_CONSIGNEE")
                            WearableDataSyncManager.ACTION_COMPLETE_LOAD -> viewModel.updateTripState(activePro, "COMPLETED")
                        }
                    }
                }
            )
        }
    }
}
