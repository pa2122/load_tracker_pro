package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Test

class IssueLabelsTest {

    @Test
    fun `1x builds map to the v1 label`() {
        assertEquals("version:v1.x", IssueLabels.versionLabel("1.4.2"))
    }

    @Test
    fun `2x builds map to the v2 label`() {
        assertEquals("version:v2.0", IssueLabels.versionLabel("2.0.1"))
    }
}
