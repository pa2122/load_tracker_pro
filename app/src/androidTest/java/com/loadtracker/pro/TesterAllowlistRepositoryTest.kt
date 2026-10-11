package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TesterAllowlistRepositoryTest {

    @Test
    fun parsesAndNormalizesAllowlistEntries() {
        val ids = TesterAllowlistRepository.parse(
            """[" device-one ", "device-two", "device-one", ""]"""
        )

        assertEquals(setOf("device-one", "device-two"), ids)
    }

    @Test
    fun rejectsNonStringAllowlistEntries() {
        assertThrows(Exception::class.java) {
            TesterAllowlistRepository.parse("""["device-one", 42]""")
        }
    }

    @Test
    fun rejectsMalformedAllowlistJson() {
        assertThrows(Exception::class.java) {
            TesterAllowlistRepository.parse("""{"device":"device-one"}""")
        }
    }
}
