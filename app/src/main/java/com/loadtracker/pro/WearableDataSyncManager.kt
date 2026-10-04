package com.loadtracker.pro

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

object WearableDataSyncManager {

    const val PATH_ACTIVE_TRIP_STATE = "/active_trip_state"
    const val PATH_WRIST_ACTION = "/wrist_action"
    const val PATH_REQUEST_ACTIVE_TRIP_STATE = "/request_active_trip_state"

    // Wrist Action Commands from Smartwatch
    const val ACTION_ARRIVE_SHIPPER = "ARRIVE_SHIPPER"
    const val ACTION_DEPART_SHIPPER = "DEPART_SHIPPER"
    const val ACTION_ARRIVE_CONSIGNEE = "ARRIVE_CONSIGNEE"
    const val ACTION_COMPLETE_LOAD = "COMPLETE_LOAD"

    data class WearTripStatePayload(
        val proNumber: String,
        val tripState: String,
        val bounceMiles: Double,
        val loadedMiles: Double,
        val dockArrivalTime: Long = 0L,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Serializes active load state and syncs to connected Wear OS smartwatches.
     */
    fun syncTripStateToWearable(context: Context, payload: WearTripStatePayload) {
        try {
            val request = PutDataMapRequest.create(PATH_ACTIVE_TRIP_STATE).apply {
                dataMap.putString("proNumber", payload.proNumber)
                dataMap.putString("tripState", payload.tripState)
                dataMap.putDouble("bounceMiles", payload.bounceMiles)
                dataMap.putDouble("loadedMiles", payload.loadedMiles)
                dataMap.putLong("dockArrivalTime", payload.dockArrivalTime)
                dataMap.putLong("timestamp", payload.timestamp)
                dataMap.putLong("nonce", System.nanoTime()) // Force unique hash on every sync
            }
            val putDataReq = request.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(putDataReq)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
