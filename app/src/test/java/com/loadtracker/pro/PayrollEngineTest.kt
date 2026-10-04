package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Test

class PayrollEngineTest {

    @Test
    fun test1_PayrollEngine_CalculatesItemizedDriverPayDownToPenny() {
        val load = CurrentLoad(
            proNumber = "52167364",
            dispatchedBounceMiles = 155.0,
            dispatchedLoadedMiles = 968.0,
            bounceMilesStart = 0.0,
            bounceMilesEnd = 155.0,
            loadedMilesStart = 0.0,
            loadedMilesEnd = 968.0,
            percentageRate = 31.0,
            loadPay = 2335.03,
            tarpType = "S",
            isPreTarped = false,
            pickupTimestamp = 1700000000000L,
            isGoingHome = false,
            tripState = "COMPLETED"
        )

        val breakdown = PayrollEngine.calculateLoadNetPay(load, lumberRate = 50.0, steelRate = 30.0)

        assertEquals(2335.03, breakdown.grossRevenueDollars, 0.01)
        assertEquals(723.86, breakdown.driverCutDollars, 0.01) // 31% of $2335.03 = $723.8593 -> $723.86
        assertEquals(30.00, breakdown.tarpPayDollars, 0.01)     // Steel tarp pay = $30.00
        assertEquals(31.00, breakdown.bounceBonusDollars, 0.01) // 155 mi * $0.20/mi = $31.00
        assertEquals(784.86, breakdown.totalNetPayDollars, 0.01) // $723.86 + $30.00 + $31.00 = $784.86
    }
}