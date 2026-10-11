package com.loadtracker.pro

enum class ProEntitlement {
    PURCHASED,
    TESTER,
    FREE;

    val hasProAccess: Boolean
        get() = this != FREE
}

object ProEntitlementResolver {
    fun resolve(
        hasVerifiedPurchase: Boolean,
        isTesterAllowlisted: Boolean
    ): ProEntitlement = when {
        hasVerifiedPurchase -> ProEntitlement.PURCHASED
        isTesterAllowlisted -> ProEntitlement.TESTER
        else -> ProEntitlement.FREE
    }
}

object DeveloperAccessResolver {
    fun hasAccess(
        isDebugBuild: Boolean,
        isDeveloperDeviceAllowlisted: Boolean
    ): Boolean = isDebugBuild || isDeveloperDeviceAllowlisted
}
