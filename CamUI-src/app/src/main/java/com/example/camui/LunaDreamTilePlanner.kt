package com.example.camui

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

internal data class TileRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

internal data class LunaDreamTile(
    val index: Int,
    val column: Int,
    val row: Int,
    val core: TileRect,
    val expanded: TileRect
)

internal data class LunaDreamTilePlan(
    val outputWidth: Int,
    val outputHeight: Int,
    val coreTileEdge: Int,
    val overlap: Int,
    val tiles: List<LunaDreamTile>
) {
    val columns: Int get() = tiles.maxOfOrNull { it.column + 1 } ?: 0
    val rows: Int get() = tiles.maxOfOrNull { it.row + 1 } ?: 0
    val tileCount: Int get() = tiles.size
    val largestWorkingTilePixels: Long
        get() = tiles.maxOfOrNull { it.expanded.width.toLong() * it.expanded.height } ?: 0L
}

/**
 * Pure tile planner for the future tiled image pipeline.
 * It allocates no Bitmap or pixel buffers and does not itself perform image fusion or JPEG IO.
 */
internal object LunaDreamTilePlanner {
    fun plan(
        outputWidth: Int,
        outputHeight: Int,
        coreTileEdge: Int = 512,
        overlap: Int = 32,
        maximumExpandedEdge: Int = 1_024
    ): LunaDreamTilePlan {
        require(outputWidth > 0 && outputHeight > 0) { "Output dimensions must be positive" }
        require(coreTileEdge > 0) { "Core tile edge must be positive" }
        require(overlap >= 0) { "Overlap cannot be negative" }
        require(maximumExpandedEdge >= coreTileEdge) { "Maximum expanded edge is smaller than core edge" }
        require(coreTileEdge + overlap * 2 <= maximumExpandedEdge) {
            "Core tile plus overlap exceeds maximum expanded tile edge"
        }

        val columns = ceil(outputWidth.toDouble() / coreTileEdge).toInt()
        val rows = ceil(outputHeight.toDouble() / coreTileEdge).toInt()
        val result = ArrayList<LunaDreamTile>(columns * rows)
        var index = 0
        for (row in 0 until rows) {
            val top = row * coreTileEdge
            val bottom = min(outputHeight, top + coreTileEdge)
            for (column in 0 until columns) {
                val left = column * coreTileEdge
                val right = min(outputWidth, left + coreTileEdge)
                val core = TileRect(left, top, right, bottom)
                val expanded = TileRect(
                    left = max(0, left - overlap),
                    top = max(0, top - overlap),
                    right = min(outputWidth, right + overlap),
                    bottom = min(outputHeight, bottom + overlap)
                )
                check(expanded.width <= maximumExpandedEdge && expanded.height <= maximumExpandedEdge) {
                    "Expanded tile ${index + 1} exceeds edge budget"
                }
                result += LunaDreamTile(index++, column, row, core, expanded)
            }
        }
        return LunaDreamTilePlan(outputWidth, outputHeight, coreTileEdge, overlap, result)
    }

    fun recommendedConcurrency(thermalStatus: Int): Int = when {
        thermalStatus >= android.os.PowerManager.THERMAL_STATUS_SEVERE -> 1
        thermalStatus >= android.os.PowerManager.THERMAL_STATUS_MODERATE -> 2
        else -> 4
    }

    fun estimatedWorkingSetBytes(
        plan: LunaDreamTilePlan,
        concurrentTiles: Int,
        bytesPerPixelPerBuffer: Int = 6,
        workingBuffersPerTile: Int = 4
    ): Long {
        require(concurrentTiles > 0)
        require(bytesPerPixelPerBuffer > 0)
        require(workingBuffersPerTile > 0)
        return plan.largestWorkingTilePixels *
            bytesPerPixelPerBuffer.toLong() *
            workingBuffersPerTile *
            concurrentTiles
    }
}

/**
 * Maps a final output pixel to the source crop. Rotation and mirroring are applied
 * in normalized coordinates so the eventual scanline writer need not rotate a full image.
 */
internal class LunaDreamOutputMapper(
    private val sourceWidth: Int,
    private val sourceHeight: Int,
    private val crop: TileRect,
    private val clockwiseRotationDegrees: Int = 0,
    private val mirrorHorizontally: Boolean = false
) {
    init {
        require(sourceWidth > 0 && sourceHeight > 0)
        require(crop.left >= 0 && crop.top >= 0)
        require(crop.right <= sourceWidth && crop.bottom <= sourceHeight)
        require(crop.width > 0 && crop.height > 0)
        require(clockwiseRotationDegrees % 90 == 0) {
            "Output mapper supports 0, 90, 180 and 270 degree rotations"
        }
    }

    fun mapOutputPixel(x: Int, y: Int, outputWidth: Int, outputHeight: Int): Pair<Float, Float> {
        require(outputWidth > 0 && outputHeight > 0)
        require(x in 0 until outputWidth && y in 0 until outputHeight)
        var u = (x + 0.5f) / outputWidth
        val v = (y + 0.5f) / outputHeight
        if (mirrorHorizontally) u = 1f - u
        val (rotatedU, rotatedV) = when (((clockwiseRotationDegrees % 360) + 360) % 360) {
            0 -> u to v
            // Inverse mapping from rotated output canvas back to source crop.
            90 -> (1f - v) to u
            180 -> (1f - u) to (1f - v)
            270 -> v to (1f - u)
            else -> error("Unsupported right-angle rotation")
        }
        return (
            crop.left + rotatedU * crop.width - 0.5f
        ) to (
            crop.top + rotatedV * crop.height - 0.5f
        )
    }
}
