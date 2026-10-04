package com.loadtracker.pro

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

object WearConnectivityEngine {

    const val CAPABILITY_PHONE_APP = "load_tracker_phone_app"

    enum class ConnectionStatus {
        BLUETOOTH_OFF,
        PHONE_DISCONNECTED,
        APP_NOT_INSTALLED_ON_PHONE,
        CONNECTED
    }

    /**
     * Checks 4-step connectivity pipeline between watch and phone:
     * 1. Bluetooth Check (Is Bluetooth hardware ON?)
     * 2. Connected Device Check (Is a phone paired & connected?)
     * 3. App Installed Check (Is Load Tracker Pro active on connected phone?)
     * 4. Return status
     */
    suspend fun checkPhoneConnectivity(context: Context): ConnectionStatus {
        // Step 1: Bluetooth Hardware Check
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            return ConnectionStatus.BLUETOOTH_OFF
        }

        // Step 2: Check for connected phone nodes via NodeClient
        return try {
            val nodeClient = Wearable.getNodeClient(context)
            val connectedNodes: List<Node> = nodeClient.connectedNodes.await()
            val nearbyPhone = connectedNodes.firstOrNull { it.isNearby }

            if (nearbyPhone == null && connectedNodes.isEmpty()) {
                return ConnectionStatus.PHONE_DISCONNECTED
            }

            // Step 3: Check if Load Tracker Pro is installed on connected phone
            val capabilityClient = Wearable.getCapabilityClient(context)
            val capabilityInfo = capabilityClient.getCapability(
                CAPABILITY_PHONE_APP,
                CapabilityClient.FILTER_REACHABLE
            ).await()

            val appNodes = capabilityInfo.nodes
            if (appNodes.isEmpty()) {
                // Phone connected via Bluetooth, but Load Tracker Pro capability not advertised
                ConnectionStatus.APP_NOT_INSTALLED_ON_PHONE
            } else {
                // Step 4: Fully Connected!
                ConnectionStatus.CONNECTED
            }
        } catch (e: Exception) {
            // Fallback for emulators/simulators without Play Services Wearable node routing
            if (bluetoothAdapter.isEnabled) ConnectionStatus.CONNECTED else ConnectionStatus.BLUETOOTH_OFF
        }
    }
}
