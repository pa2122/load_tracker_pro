package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Test

class WearConnectivityTest {

    @Test
    fun test1_WearConnectivityStatus_VerifiesConnectionStatusEnumValues() {
        assertEquals("BLUETOOTH_OFF", WearConnectivityEngine.ConnectionStatus.BLUETOOTH_OFF.name)
        assertEquals("PHONE_DISCONNECTED", WearConnectivityEngine.ConnectionStatus.PHONE_DISCONNECTED.name)
        assertEquals("APP_NOT_INSTALLED_ON_PHONE", WearConnectivityEngine.ConnectionStatus.APP_NOT_INSTALLED_ON_PHONE.name)
        assertEquals("CONNECTED", WearConnectivityEngine.ConnectionStatus.CONNECTED.name)
    }

    @Test
    fun test2_WearCapabilityName_MatchesPhoneCapabilityDeclaration() {
        assertEquals("load_tracker_phone_app", WearConnectivityEngine.CAPABILITY_PHONE_APP)
    }
}
