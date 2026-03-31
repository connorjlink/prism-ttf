package org.example

// prism-ttf App.kt
// (c) Connor J. Link. All Rights Reserved.

class TTFParser {
    private val ttfFile: String
    private val fontSize: Int

    constructor(ttfFile: String, fontSize: Int) {
        this.ttfFile = ttfFile
        this.fontSize = fontSize
    }

    fun parse(sdfFolder: String) {
        // ensure the folder exists
        val sdfDir = java.io.File(sdfFolder)
        if (!sdfDir.exists()) {
            sdfDir.mkdirs()
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

fun main(arguments: Array<String>) {
    var ttfFile: String? = null
    var fontSize: Int? = null // pt
    var sdfFolder: String? = null

    for (i in arguments.indices) {
        when (arguments[i]) {
            "--ttf" -> ttfFile = arguments.getOrNull(i + 1)
            "--font-size" -> fontSize = arguments.getOrNull(i + 1)?.toIntOrNull()
            "--sdf" -> sdfFolder = arguments.getOrNull(i + 1)
        }
    }

    if (ttfFile == null || fontSize == null) {
        println("Usage: --ttf <path_to_ttf_file> --font-size <font_size_in_pt> --sdf <path_to_output_sdf>")
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

    if (sdfFolder.isNullOrEmpty()) {
        // default to the same name and path as the TTF file but with the .sdf extension
        sdfFolder = ttfFile.substringBeforeLast(".")
    }

    var parser = TTFParser(ttfFile, fontSize)
    parser.parse(sdfFolder)
}
