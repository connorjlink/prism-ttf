package org.example

import java.io.File
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.RandomAccessFile
import kotlin.math.sqrt

// prism-ttf App.kt
// (c) Connor J. Link. All Rights Reserved.

const val PT_PER_INCH = 72

// Quadratic bezier curve as defined in the TTF files
class Spline {
    val startPoint: Pair<Float, Float>
    val controlPoint: Pair<Float, Float>
    val endPoint: Pair<Float, Float>

    constructor(startPoint: Pair<Float, Float>, controlPoint: Pair<Float, Float>, endPoint: Pair<Float, Float>) {
        this.startPoint = startPoint
        this.controlPoint = controlPoint
        this.endPoint = endPoint
    }

    fun evaluate(t: Float): Pair<Float, Float> {
        val x = (1 - t) * (1 - t) * startPoint.first + 2 * (1 - t) * t * controlPoint.first + t * t * endPoint.first
        val y = (1 - t) * (1 - t) * startPoint.second + 2 * (1 - t) * t * controlPoint.second + t * t * endPoint.second
        return Pair(x, y)
    }

    fun derivative(t: Float): Pair<Float, Float> {
        val x = 2 * (1 - t) * (controlPoint.first - startPoint.first) + 2 * t * (endPoint.first - controlPoint.first)
        val y = 2 * (1 - t) * (controlPoint.second - startPoint.second) + 2 * t * (endPoint.second - controlPoint.second)
        return Pair(x, y)
    }

    fun length(fontSize: Int, screenDpi: Int): Float {
        var length = 0f
        var previousPoint = startPoint

        // number of discrete steps to take along the curve
        val steps = fontSize * screenDpi / PT_PER_INCH

        for (i in 1..steps) {
            val t = i / steps.toFloat()
            val currentPoint = evaluate(t)
            val dx = currentPoint.first - previousPoint.first
            val dy = currentPoint.second - previousPoint.second
            // Euler's method for the curve
            length += sqrt(dx * dx + dy * dy)
            previousPoint = currentPoint
        }

        return length
    }

    fun distanceToPoint(point: Pair<Float, Float>, fontSize: Int, screenDpi: Int): Float {
        var minDistance = Float.MAX_VALUE

        // number of discrete steps to take along the curve
        val steps = fontSize * screenDpi / PT_PER_INCH

        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val currentPoint = evaluate(t)
            val dx = currentPoint.first - point.first
            val dy = currentPoint.second - point.second
            val distance = sqrt(dx * dx + dy * dy)
            if (distance < minDistance) {
                minDistance = distance
            }
        }

        return minDistance
    }

    fun isClockwise(): Boolean {
        // calculate the signed area of the triangle formed by the start, control, and end points
        val area = 0.5f * (startPoint.first * (controlPoint.second - endPoint.second) + controlPoint.first * (endPoint.second - startPoint.second) + endPoint.first * (startPoint.second - controlPoint.second))
        return area < 0
    }
}

class BoundingBox {
    val xMin: Float
    val yMin: Float
    val xMax: Float
    val yMax: Float

    constructor(xMin: Float, yMin: Float, xMax: Float, yMax: Float) {
        this.xMin = xMin
        this.yMin = yMin
        this.xMax = xMax
        this.yMax = yMax
    }
}

class Glyph {
    val index: Int
    val codePoint: Int
    val boundingBox: BoundingBox
    // TTF glyphs support multiple contours
    val contours: List<List<Spline>> 

    constructor(index: Int, codePoint: Int, boundingBox: BoundingBox, contours: List<List<Spline>>) {
        this.index = index
        this.codePoint = codePoint
        this.boundingBox = boundingBox
        this.contours = contours
    }

    fun renderToSDF(fontSize: Int, screenDpi: Int): ByteArray {
        // figure out how big the SDF needs to be based on the font size and screen DPI and the glyph's bounding box
        val ptPerInch = 72
        
        val boundingWidth = boundingBox.xMax - boundingBox.xMin
        val boundingHeight = boundingBox.yMax - boundingBox.yMin
        // 10% padding relative to median dimension of the bounding box
        val padding = (boundingWidth + boundingHeight) / 2 / 10

        var sdfWidth = ((boundingBox.xMax - boundingBox.xMin + 2 * padding) * fontSize * screenDpi / ptPerInch).toInt()
        var sdfHeight = ((boundingBox.yMax - boundingBox.yMin + 2 * padding) * fontSize * screenDpi / ptPerInch).toInt()

        var sdfData = FloatArray(sdfWidth * sdfHeight)

        // grayscale antialiasing, no hinting, no subpixel rendering

        for (contour in contours) {


            for (spline in contour) {

            }
        }


        return sdfData.toByteArray()
    }
}

class OffsetTable(data: ByteArray) {
    val scalerType: UInt
    val numTables: UShort
    val searchRange: UShort
    val entrySelector: UShort
    val rangeShift: UShort

    init {
        val buffer = ByteBuffer.wrap(data)

        val expectedLength = 12
        if (data.size < expectedLength) {
            throw IllegalArgumentException("TTF offset table offset data must be at least $expectedLength bytes")
        }

        scalerType = buffer.getInt().toUInt()
        numTables = buffer.getShort().toUShort()
        searchRange = buffer.getShort().toUShort()
        entrySelector = buffer.getShort().toUShort()
        rangeShift = buffer.getShort().toUShort()
    }
}

class TableDirectory(data: ByteArray, offsetTable: OffsetTable) {
    data class TableEntry(
        val tag: String,
        val checksum: UInt,
        val offset: UInt,
        val length: UInt
    )

    val entries: List<TableEntry>

    init {
        val buffer = ByteBuffer.wrap(data)
        buffer.position(12) // table directory starts immediately after the offset table

        val expectedLength = 16 * offsetTable.numTables.toInt()
        if (data.size < 12 + expectedLength) {
            throw IllegalArgumentException("TTF table directory data must be at least ${12 + expectedLength} bytes")
        }

        entries = List(offsetTable.numTables.toInt()) { i ->
            val tagBytes = ByteArray(4)
            buffer.get(tagBytes)
            val tag = String(tagBytes, Charsets.US_ASCII)

            val checksum = buffer.getInt().toUInt()
            val offset = buffer.getInt().toUInt()
            val length = buffer.getInt().toUInt()

            TableEntry(tag, checksum, offset, length)
        }
    }
}

class TTFParser {
    private val ttfFile: String
    private val fontSize: Int
    private val screenDpi: Int

    constructor(ttfFile: String, fontSize: Int, screenDpi: Int) {
        this.ttfFile = ttfFile
        this.fontSize = fontSize
        this.screenDpi = screenDpi
    }

    fun parse(sdfFolder: String) {
        // ensure the folder exists
        val sdfDir = java.io.File(sdfFolder)
        if (!sdfDir.exists()) {
            sdfDir.mkdirs()
            println("Note: SDF output folder did not exist, created it at $sdfFolder")
        }

        val ttfFile = java.io.File(ttfFile)
        if (!ttfFile.exists()) {
            println("Error: TTF file does not exist.")
            return
        }

        try {
            var ttfData = ttfFile.readBytes()

            // first, read the offset table and table directory
            var offsetTable = OffsetTable(ttfData)
            var tableDirectory = TableDirectory(ttfData, offsetTable)

            // second, verify that the required tables are present and read their data
            val requiredTables = setOf("head", "maxp", "loca", "glyf", "cmap")
            val missingTables = requiredTables - tableDirectory.entries.map { it.tag }.toSet()
            if (missingTables.isNotEmpty()) {
                println("Error: TTF file is missing required tables: ${missingTables.joinToString(", ")}")
                return
            }

            val headTable = tableDirectory.entries.first { it.tag == "head" }
            val maxpTable = tableDirectory.entries.first { it.tag == "maxp" }
            val locaTable = tableDirectory.entries.first { it.tag == "loca" }
            val glyfTable = tableDirectory.entries.first { it.tag == "glyf" }
            val cmapTable = tableDirectory.entries.first { it.tag == "cmap" }

            // third, begin parsing tables to load glyphs
            
            
            // fourth, render glyphs to SDFs at the appropriate size and resolution


            // fifth, export the SDFs to the output folder



        } catch (e: Exception) {
            println("Error: unexpected parse of TTF file: ${e.message}")
            return
        }

        /*
        TODO
            1. File parsing
            - Offset table
            - Table directory
            - Required tables:
                - head
                - maxp
                - loca
                - glyf
                - cmap

            2. Glyph loading
            - Map character → glyph index (cmap)
            - Read glyph outline from glyf
        */
    }
}

const val BUFFER_SIZE: Long = 1024 * 1024 * 10 // 10 MB

class MemoryMappedFile(sharedMemoryFile: String) {
    val memoryFile: RandomAccessFile
    val fileChannel: FileChannel
    val buffer: MappedByteBuffer

    init {
        memoryFile = RandomAccessFile(sharedMemoryFile, "rw")
        fileChannel = memoryFile.channel
        buffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, BUFFER_SIZE)
    }

    fun close() {
        fileChannel.close()
        memoryFile.close()
    }
}

fun main(arguments: Array<String>) {
    var ttfFile: String? = null
    var fontSize: Int? = null // pt
    var screenDpi: Int? = null // ppi
    var sdfFolder: String? = null

    // TODO: accept another argument for a singular codepoint, default to a standard set of ASCII characters if not provided
    var asciiCodepoints = (32..126).toList() // space to tilde

    for (i in arguments.indices) {
        when (arguments[i]) {
            "--codepoint" -> asciiCodepoints = listOf(arguments.getOrNull(i + 1)?.toIntOrNull() ?: 32)
            "--ttf" -> ttfFile = arguments.getOrNull(i + 1)
            "--font-size" -> fontSize = arguments.getOrNull(i + 1)?.toIntOrNull()
            "--dpi" -> screenDpi = arguments.getOrNull(i + 1)?.toIntOrNull()
            "--sdf" -> sdfFolder = arguments.getOrNull(i + 1)
        }
    }

    if (ttfFile == null || fontSize == null || screenDpi == null) {
        println("Usage: --ttf <path_to_ttf_file> --font-size <font_size_in_pt> --dpi <screen_dpi> --sdf <path_to_output_sdf>")
        return
    }

    if (ttfFile.isEmpty()) {
        println("Error: TTF file path cannot be empty.")
        return
    }

    if (!ttfFile.endsWith(".ttf", ignoreCase = true)) {
        println("Error: TTF file must have a .ttf extension.")
        return
    }

    if (fontSize <= 0) {
        println("Error: Font size must be a positive integer.")
        return
    }

    if (screenDpi <= 0) {
        println("Error: Screen DPI must be a positive integer.")
        return
    }

    if (sdfFolder.isNullOrEmpty()) {
        // default to the same name and path as the TTF file but with the .sdf extension
        sdfFolder = ttfFile.substringBeforeLast(".")
    }

    var parser = TTFParser(ttfFile, fontSize, screenDpi)
    parser.parse(sdfFolder)
}
