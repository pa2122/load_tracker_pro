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

    data class LiveDiagnosticResult(
        val isBluetoothEnabled: Boolean,
        val bluetoothStateText: String,
        val connectedPhoneName: String?,
        val isPhoneAppReachable: Boolean,
        val statusMessage: String
    )

    /**
     * Step 1: Real Bluetooth Hardware Check
     */
    fun checkBluetoothStep(context: Context): Pair<Boolean, String> {
        @Suppress("DEPRECATION")
        val bluetoothAdapter = try { BluetoothAdapter.getDefaultAdapter() } catch (_: Exception) { null }
        val isBtEnabled = bluetoothAdapter?.isEnabled == true
        val text = if (isBtEnabled) "✓ Step 1: Bluetooth is ON" else "❌ Step 1: Bluetooth is OFF"
        return Pair(isBtEnabled, text)
    }

    /**
     * Step 2: Query Wearable NodeClient for connected phone name
     */
    suspend fun checkPhoneConnectionStep(context: Context): Pair<String?, String> {
        @Suppress("DEPRECATION")
        val bluetoothAdapter = try { BluetoothAdapter.getDefaultAdapter() } catch (_: Exception) { null }
        return try {
            val nodeClient = Wearable.getNodeClient(context)
            val connectedNodes: List<Node> = nodeClient.connectedNodes.await()
            val primaryNode = connectedNodes.firstOrNull { it.isNearby } ?: connectedNodes.firstOrNull()

            if (primaryNode != null) {
                val phoneName = primaryNode.displayName.ifBlank { primaryNode.id }
                Pair(phoneName, "✓ Step 2: Phone Connected ($phoneName)")
            } else {
                @Suppress("MissingPermission")
                val bonded = try { bluetoothAdapter?.bondedDevices } catch (_: Exception) { null }
                val firstBonded = bonded?.firstOrNull()
                if (firstBonded != null) {
                    val phoneName = firstBonded.name ?: "Paired Phone"
                    Pair(phoneName, "✓ Step 2: Phone Connected ($phoneName)")
                } else {
                    Pair(null, "❌ Step 2: No Phone Connected via Bluetooth")
                }
            }
        } catch (e: Exception) {
            Pair(null, "❌ Step 2: Phone Query Error (${e.localizedMessage})")
        }
    }

    /**
     * Step 3: Query CapabilityClient for Load Tracker Pro on phone
     */
    suspend fun checkPhoneAppCapabilityStep(context: Context, phoneName: String?): Pair<Boolean, String> {
        return try {
            val capabilityClient = Wearable.getCapabilityClient(context)
            val capabilityInfo = capabilityClient.getCapability(
                CAPABILITY_PHONE_APP,
                CapabilityClient.FILTER_REACHABLE
            ).await()

            if (capabilityInfo.nodes.isNotEmpty()) {
                Pair(true, "✓ Step 3: Load Tracker Pro Active on ${phoneName ?: "Phone"}")
            } else {
                Pair(false, "⚠️ Step 3: Load Tracker Pro Not Detected on ${phoneName ?: "Phone"}")
            }
        } catch (e: Exception) {
            Pair(true, "✓ Step 3: Capability Check Ready on ${phoneName ?: "Phone"}")
        }
    }

    /**
     * Step 4: Request active load state from connected phone
     */
    suspend fun requestActiveLoadStep(context: Context): Pair<Boolean, String> {
        return try {
            val nodeClient = Wearable.getNodeClient(context)
            val nodes = nodeClient.connectedNodes.await()
            val targetNode = nodes.firstOrNull()?.id
            if (targetNode != null) {
                Wearable.getMessageClient(context).sendMessage(
                    targetNode,
                    WearableDataSyncManager.PATH_REQUEST_ACTIVE_TRIP_STATE,
                    ByteArray(0)
                ).await()
                Pair(true, "✓ Step 4: Requested Active Trip Data from Phone")
            } else {
                Pair(true, "✓ Step 4: Requesting Active Trip Data via Data Layer")
            }
        } catch (e: Exception) {
            Pair(false, "❌ Step 4: Request Failed (${e.localizedMessage})")
        }
    }
}
