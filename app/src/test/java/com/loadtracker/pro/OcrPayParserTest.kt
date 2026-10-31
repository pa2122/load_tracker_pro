package com.loadtracker.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OcrPayParserTest {
    @Test
    fun parsesGrossWithOcrInsertedSpace() {
        assertEquals("1041.13", OcrPayParser.parse("Going Home: Y|Gross: 1 041.13|Load Weight: 45626"))
    }

    @Test
    fun parsesPlainFourDigitGross() {
        assertEquals("1041.13", OcrPayParser.parse("Going Home: Y\nGross: 1041.13\nLoad Weight: 45626"))
    }

    @Test
    fun parsesCommaThousands() {
        assertEquals("1041.13", OcrPayParser.parse("Gross: $1,041.13"))
    }

    @Test
    fun grossWinsOverEarlierKeywordInsideWords() {
        assertEquals("950.00", OcrPayParser.parse("Rate Con 12\nGross: 950.00"))
    }

    @Test
    fun doesNotMergeFollowingNumber() {
        assertEquals("500", OcrPayParser.parse("Gross: 500\nWeight: 45626"))
    }

    @Test
    fun returnsNullWhenNoPay() {
        assertNull(OcrPayParser.parse("Tarp: P"))
    }
}
