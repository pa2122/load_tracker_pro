package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeAddressResolverTest {

    @Test
    fun createQueryCombinesAddressLinesAndUsesFirstLineAsDisplayName() {
        val query = HomeAddressResolver.createQuery(
            "\nHome Base\n15381 TX-198\nMabank, TX 75147\n"
        )

        assertEquals(
            HomeAddressQuery(
                address = "Home Base, 15381 TX-198, Mabank, TX 75147",
                displayName = "Home Base"
            ),
            query
        )
    }

    @Test
    fun createQueryUsesSingleLineAsAddressWithoutOverridingDisplayName() {
        val query = HomeAddressResolver.createQuery("15381 TX-198, Mabank, TX 75147")

        assertEquals("15381 TX-198, Mabank, TX 75147", query?.address)
        assertNull(query?.displayName)
    }

    @Test
    fun createQueryReturnsNullForBlankInput() {
        assertNull(HomeAddressResolver.createQuery(" \n\t "))
    }
}
