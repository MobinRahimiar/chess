package com.chessmasterai

import org.junit.Assert.*
import org.junit.Test

class StockfishUciTest {
    @Test fun parsesCentipawnMultiPvLine() {
        val parsed = parseUciInfoLine("info depth 15 seldepth 22 multipv 2 score cp 38 nodes 12000 nps 800000 pv e2e4 e7e5 g1f3")!!
        assertEquals(2, parsed.rank)
        assertEquals(15, parsed.depth)
        assertEquals("+0.38", parsed.score)
        assertEquals(listOf("e2e4", "e7e5", "g1f3"), parsed.pv)
        assertEquals(12000L, parsed.nodes)
    }

    @Test fun parsesMateScore() {
        val parsed = parseUciInfoLine("info depth 20 multipv 1 score mate -3 nodes 99 pv h2h4")!!
        assertTrue(parsed.scoreIsMate)
        assertEquals("Mate -3", parsed.score)
    }

    @Test fun ignoresIncompleteInfoLines() {
        assertNull(parseUciInfoLine("info depth 8 nodes 20"))
        assertNull(parseUciInfoLine("bestmove e2e4"))
    }
}
