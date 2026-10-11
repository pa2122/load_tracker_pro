package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProEntitlementResolverTest {

    @Test
    fun verifiedPurchaseGrantsProAccess() {
        val entitlement = ProEntitlementResolver.resolve(
            hasVerifiedPurchase = true,
            isTesterAllowlisted = false
        )

        assertEquals(ProEntitlement.PURCHASED, entitlement)
        assertTrue(entitlement.hasProAccess)
    }

    @Test
    fun allowlistedTesterGetsProAccessWithoutPurchase() {
        val entitlement = ProEntitlementResolver.resolve(
            hasVerifiedPurchase = false,
            isTesterAllowlisted = true
        )

        assertEquals(ProEntitlement.TESTER, entitlement)
        assertTrue(entitlement.hasProAccess)
    }

    @Test
    fun userWithoutPurchaseOrTesterAccessIsFree() {
        val entitlement = ProEntitlementResolver.resolve(
            hasVerifiedPurchase = false,
            isTesterAllowlisted = false
        )

        assertEquals(ProEntitlement.FREE, entitlement)
        assertFalse(entitlement.hasProAccess)
    }

    @Test
    fun verifiedPurchaseTakesPrecedenceOverTesterStatus() {
        val entitlement = ProEntitlementResolver.resolve(
            hasVerifiedPurchase = true,
            isTesterAllowlisted = true
        )

        assertEquals(ProEntitlement.PURCHASED, entitlement)
        assertTrue(entitlement.hasProAccess)
    }

    @Test
    fun developerAccessRequiresDebugBuildOrDeveloperDeviceAuthorization() {
        assertTrue(DeveloperAccessResolver.hasAccess(
            isDebugBuild = true,
            isDeveloperDeviceAllowlisted = false
        ))
        assertTrue(DeveloperAccessResolver.hasAccess(
            isDebugBuild = false,
            isDeveloperDeviceAllowlisted = true
        ))
        assertFalse(DeveloperAccessResolver.hasAccess(
            isDebugBuild = false,
            isDeveloperDeviceAllowlisted = false
        ))
    }
}
