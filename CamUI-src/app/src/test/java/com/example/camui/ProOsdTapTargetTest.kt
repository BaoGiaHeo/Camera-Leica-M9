package com.example.camui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProOsdTapTargetTest {
    @Test
    fun eachProShortcutResolvesAtItsMeasuredOsdCenter() {
        val centers = ProOsdTapTargets.centers(widthPx = 2_400, heightPx = 1_080)
        assertEquals(4, centers.size)
        centers.forEach { center ->
            assertEquals(
                center.target,
                ProOsdTapTargets.resolve(
                    center.xPx,
                    center.yPx,
                    widthPx = 2_400,
                    heightPx = 1_080,
                    minimumTargetPx = 48f
                )
            )
        }
    }

    @Test
    fun touchTargetExpandsAroundCompactPixelGlyph() {
        val center = ProOsdTapTargets.centers(2_400, 1_080)
            .first { it.target == ProOsdTapTarget.AREA }
        assertEquals(
            ProOsdTapTarget.AREA,
            ProOsdTapTargets.resolve(
                center.xPx + 23f,
                center.yPx + 23f,
                widthPx = 2_400,
                heightPx = 1_080,
                minimumTargetPx = 48f
            )
        )
    }

    @Test
    fun overlappingExpandedTargetsSelectTheNearestGlyph() {
        val centers = ProOsdTapTargets.centers(2_400, 1_080)
        val wb = centers.first { it.target == ProOsdTapTarget.WB }
        assertEquals(
            ProOsdTapTarget.WB,
            ProOsdTapTargets.resolve(
                wb.xPx,
                wb.yPx + 2f,
                widthPx = 2_400,
                heightPx = 1_080,
                minimumTargetPx = 48f
            )
        )
    }

    @Test
    fun pointsOutsideAllExpandedTargetsAreIgnored() {
        assertNull(
            ProOsdTapTargets.resolve(
                80f,
                900f,
                widthPx = 2_400,
                heightPx = 1_080,
                minimumTargetPx = 48f
            )
        )
    }

    @Test
    fun invalidDimensionsAndNaNInputAreIgnored() {
        assertNull(ProOsdTapTargets.resolve(1f, 1f, 0, 1))
        assertNull(ProOsdTapTargets.resolve(Float.NaN, 10f, 2_400, 1_080))
    }
}
