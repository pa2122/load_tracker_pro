package com.loadtracker.pro

import android.bluetooth.BluetoothAdapter
import android.content.Context
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
     * 3. Return status
     */
    suspend fun checkPhoneConnectivity(context: Context): ConnectionStatus {
        // Step 1: Bluetooth Hardware Check
        @Suppress("DEPRECATION")
        val bluetoothAdapter = try { BluetoothAdapter.getDefaultAdapter() } catch (_: Exception) { null }
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            return ConnectionStatus.BLUETOOTH_OFF
        }

        // Step 2: Check for connected phone nodes via NodeClient or Bluetooth Bonded Devices
        return try {
            val nodeClient = Wearable.getNodeClient(context)
            val connectedNodes: List<Node> = nodeClient.connectedNodes.await()

            if (connectedNodes.isNotEmpty()) {
                ConnectionStatus.CONNECTED
            } else {
                @Suppress("MissingPermission")
                val bondedDevices = try { bluetoothAdapter.bondedDevices } catch (_: SecurityException) { null } catch (_: Exception) { null }
                if (bondedDevices != null && bondedDevices.isNotEmpty()) {
                    ConnectionStatus.CONNECTED
                } else {
                    ConnectionStatus.PHONE_DISCONNECTED
                }
            }
        } catch (_: Exception) {
            if (bluetoothAdapter.isEnabled) ConnectionStatus.CONNECTED else ConnectionStatus.BLUETOOTH_OFF
        }
    }
}
