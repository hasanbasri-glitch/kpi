package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.PersonnelKpiEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object ExcelCsvParser {

    fun parseFile(context: Context, uri: Uri): List<PersonnelKpiEntity> {
        val fileName = getFileName(context, uri).lowercase()
        return if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            parseXlsx(context, uri)
        } else {
            parseCsv(context, uri)
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "file.csv"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    fun fetchGoogleSheetsCsv(rawUrl: String): List<PersonnelKpiEntity> {
        val docId = extractSpreadsheetId(rawUrl) ?: return emptyList()
        val candidateUrls = listOf(
            "https://docs.google.com/spreadsheets/d/$docId/gviz/tq?tqx=out:csv&sheet=GRAND%20TOTAL%20BOBOT",
            "https://docs.google.com/spreadsheets/d/$docId/export?format=csv&sheet=GRAND%20TOTAL%20BOBOT",
            "https://docs.google.com/spreadsheets/d/$docId/gviz/tq?tqx=out:csv",
            "https://docs.google.com/spreadsheets/d/$docId/export?format=csv"
        )

        for (urlStr in candidateUrls) {
            try {
                var connection = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 12000
                connection.readTimeout = 12000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

                var status = connection.responseCode
                if (status == java.net.HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == java.net.HttpURLConnection.HTTP_MOVED_PERM ||
                    status == 307
                ) {
                    val newUrl = connection.getHeaderField("Location")
                    if (!newUrl.isNullOrEmpty()) {
                        connection = java.net.URL(newUrl).openConnection() as java.net.HttpURLConnection
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        status = connection.responseCode
                    }
                }

                if (status == java.net.HttpURLConnection.HTTP_OK) {
                    val csvText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    if (csvText.isNotBlank() && (csvText.contains(",") || csvText.contains(";"))) {
                        val result = parseCsvString(csvText)
                        if (result.isNotEmpty()) {
                            return result
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return emptyList()
    }

    fun extractSpreadsheetId(url: String): String? {
        if (url.length == 44 && !url.contains("/")) return url
        val regex = "/spreadsheets/d/([a-zA-Z0-9-_]+)".toRegex()
        val match = regex.find(url)
        return match?.groupValues?.get(1)
    }

    fun parseCsvString(csvText: String): List<PersonnelKpiEntity> {
        val list = mutableListOf<PersonnelKpiEntity>()
        try {
            val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.size <= 1) return emptyList()

            var headerRowIdx = 0
            var maxScore = 0
            val keyTerms = listOf("nik", "nama", "name", "pwp", "psm", "serba", "member", "toko", "store", "ac", "am", "jabatan", "position", "bobot")

            for (i in 0 until minOf(lines.size, 15)) {
                val tokens = parseCsvLine(lines[i])
                var score = 0
                for (t in tokens) {
                    val clean = cleanHeader(t)
                    if (keyTerms.any { clean.contains(it) }) score++
                }
                if (score > maxScore) {
                    maxScore = score
                    headerRowIdx = i
                }
            }

            val headers = parseCsvLine(lines[headerRowIdx]).map { cleanHeader(it) }

            val amIdx = headers.indexOfFirst { it.contains("am") }
            val acIdx = headers.indexOfFirst { it.contains("ac") }
            val storeCodeIdx = headers.indexOfFirst { it.contains("store code") || it.contains("kode toko") || it.contains("kode") }
            val storeNameIdx = headers.indexOfFirst { it.contains("store name") || it.contains("nama toko") || it.contains("toko") }
            val nikIdx = headers.indexOfFirst { it.contains("nik") }
            val nameIdx = headers.indexOfFirst { it.contains("name") || it.contains("nama") }
            val posIdx = headers.indexOfFirst { it.contains("position") || it.contains("jabatan") }
            val pwpIdx = headers.indexOfFirst { it.contains("pwp") }
            val psmIdx = headers.indexOfFirst { it.contains("psm") }
            val serbaIdx = headers.indexOfFirst { it.contains("serba") || it.contains("gratis") }
            val memberIdx = headers.indexOfFirst { it.contains("member") }

            for (i in (headerRowIdx + 1) until lines.size) {
                val row = parseCsvLine(lines[i])
                if (row.isEmpty() || row.all { it.isEmpty() }) continue

                val nik = getSafeCell(row, nikIdx, "NIK-$i")
                val name = getSafeCell(row, nameIdx, "Personil $i")
                val pos = getSafeCell(row, posIdx, "CREW")
                val storeCode = getSafeCell(row, storeCodeIdx, "T001")
                val storeName = getSafeCell(row, storeNameIdx, "Toko $storeCode")
                val ac = getSafeCell(row, acIdx, "AC Area")
                val am = getSafeCell(row, amIdx, "AM Region")

                val pwp = getSafeDouble(row, pwpIdx, 15.0)
                val psm = getSafeDouble(row, psmIdx, 15.0)
                val serba = getSafeDouble(row, serbaIdx, 25.0)
                val member = getSafeDouble(row, memberIdx, 18.0)

                val nikLower = nik.lowercase()
                val nameLower = name.lowercase()
                if (nikLower.contains("total") || nameLower.contains("total") || nikLower.contains("grand")) {
                    continue
                }

                list.add(
                    PersonnelKpiEntity(
                        nik = nik,
                        name = name,
                        position = pos,
                        storeCode = storeCode,
                        storeName = storeName,
                        ac = ac,
                        am = am,
                        pwp = pwp,
                        psm = psm,
                        serbaGratis = serba,
                        member = member
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var delimiter = ','
        if (!line.contains(",") && line.contains(";")) {
            delimiter = ';'
        }

        for (i in 0 until line.length) {
            val c = line[i]
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (c == delimiter && !inQuotes) {
                result.add(current.toString().trim().removeSurrounding("\""))
                current.clear()
            } else {
                current.append(c)
            }
        }
        result.add(current.toString().trim().removeSurrounding("\""))
        return result
    }

    fun parseCsv(context: Context, uri: Uri): List<PersonnelKpiEntity> {
        val list = mutableListOf<PersonnelKpiEntity>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()
            if (lines.size <= 1) return emptyList()

            // Detect delimiter (, or ;)
            val headerLine = lines[0]
            val delimiter = if (headerLine.contains(";")) ";" else ","

            val headers = headerLine.split(delimiter).map { cleanHeader(it) }

            val amIdx = headers.indexOfFirst { it.contains("am") }
            val acIdx = headers.indexOfFirst { it.contains("ac") }
            val storeCodeIdx = headers.indexOfFirst { it.contains("store code") || it.contains("kode toko") || it.contains("kode") }
            val storeNameIdx = headers.indexOfFirst { it.contains("store name") || it.contains("nama toko") || it.contains("toko") }
            val nikIdx = headers.indexOfFirst { it.contains("nik") }
            val nameIdx = headers.indexOfFirst { it.contains("name") || it.contains("nama") }
            val posIdx = headers.indexOfFirst { it.contains("position") || it.contains("jabatan") }
            val pwpIdx = headers.indexOfFirst { it.contains("pwp") }
            val psmIdx = headers.indexOfFirst { it.contains("psm") }
            val serbaIdx = headers.indexOfFirst { it.contains("serba") || it.contains("gratis") }
            val memberIdx = headers.indexOfFirst { it.contains("member") }

            for (i in 1 until lines.size) {
                val row = lines[i].split(delimiter).map { it.trim().removeSurrounding("\"") }
                if (row.isEmpty() || row.all { it.isEmpty() }) continue

                val nik = getSafeCell(row, nikIdx, "NIK-$i")
                val name = getSafeCell(row, nameIdx, "Personil $i")
                val pos = getSafeCell(row, posIdx, "CREW")
                val storeCode = getSafeCell(row, storeCodeIdx, "T001")
                val storeName = getSafeCell(row, storeNameIdx, "Toko $storeCode")
                val ac = getSafeCell(row, acIdx, "AC Area")
                val am = getSafeCell(row, amIdx, "AM Region")

                val pwp = getSafeDouble(row, pwpIdx, 15.0)
                val psm = getSafeDouble(row, psmIdx, 15.0)
                val serba = getSafeDouble(row, serbaIdx, 25.0)
                val member = getSafeDouble(row, memberIdx, 18.0)

                list.add(
                    PersonnelKpiEntity(
                        nik = nik,
                        name = name,
                        position = pos,
                        storeCode = storeCode,
                        storeName = storeName,
                        ac = ac,
                        am = am,
                        pwp = pwp,
                        psm = psm,
                        serbaGratis = serba,
                        member = member
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun parseXlsx(context: Context, uri: Uri): List<PersonnelKpiEntity> {
        val list = mutableListOf<PersonnelKpiEntity>()
        try {
            val zipEntries = mutableMapOf<String, ByteArray>()
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase()
                    if (name.startsWith("xl/")) {
                        zipEntries[name] = zip.readBytes()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            if (zipEntries.isEmpty()) return emptyList()

            // Step 1: Parse Shared Strings
            val sharedStrings = mutableListOf<String>()
            zipEntries["xl/sharedstrings.xml"]?.let { bytes ->
                parseSharedStrings(bytes.inputStream(), sharedStrings)
            }

            // Step 2: Parse Workbook Sheet Names and RIDs
            val sheetNameToRid = mutableMapOf<String, String>()
            zipEntries["xl/workbook.xml"]?.let { bytes ->
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(bytes.inputStream(), "UTF-8")
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name.equals("sheet", ignoreCase = true)) {
                        val name = parser.getAttributeValue(null, "name") ?: ""
                        var rId = parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
                        if (rId.isNullOrBlank()) {
                            rId = parser.getAttributeValue(null, "r:id") ?: ""
                        }
                        if (name.isNotBlank() && !rId.isNullOrBlank()) {
                            sheetNameToRid[name] = rId
                        }
                    }
                    eventType = parser.next()
                }
            }

            // Step 3: Parse Relationships
            val ridToTarget = mutableMapOf<String, String>()
            zipEntries["xl/_rels/workbook.xml.rels"]?.let { bytes ->
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(bytes.inputStream(), "UTF-8")
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name.equals("Relationship", ignoreCase = true)) {
                        val id = parser.getAttributeValue(null, "Id") ?: ""
                        val target = parser.getAttributeValue(null, "Target") ?: ""
                        if (id.isNotBlank() && target.isNotBlank()) {
                            var cleanTarget = target.removePrefix("/").removePrefix("xl/")
                            if (!cleanTarget.startsWith("xl/")) cleanTarget = "xl/$cleanTarget"
                            ridToTarget[id] = cleanTarget.lowercase()
                        }
                    }
                    eventType = parser.next()
                }
            }

            // Step 4: Find sheet path targeting "GRAND TOTAL BOBOT"
            var targetSheetPath: String? = null
            val grandTotalEntry = sheetNameToRid.entries.find { it.key.uppercase().contains("GRAND TOTAL BOBOT") }
                ?: sheetNameToRid.entries.find { it.key.uppercase().contains("GRAND TOTAL") }
                ?: sheetNameToRid.entries.find { it.key.uppercase().contains("BOBOT") }

            if (grandTotalEntry != null) {
                val rId = grandTotalEntry.value
                targetSheetPath = ridToTarget[rId]
            }

            if (targetSheetPath == null || !zipEntries.containsKey(targetSheetPath)) {
                targetSheetPath = zipEntries.keys.find { it.startsWith("xl/worksheets/sheet") }
            }

            if (targetSheetPath == null) return emptyList()

            val sheetBytes = zipEntries[targetSheetPath] ?: return emptyList()
            val rowsMap = mutableMapOf<Int, MutableMap<Int, String>>()
            parseSheetXml(sheetBytes.inputStream(), sharedStrings, rowsMap)

            if (rowsMap.isEmpty()) return emptyList()

            // Step 5: Automatically detect header row
            var bestHeaderRowIdx = rowsMap.keys.minOrNull() ?: 1
            var maxHeaderScore = 0

            val keyTerms = listOf("nik", "nama", "name", "pwp", "psm", "serba", "member", "toko", "store", "ac", "am", "jabatan", "position", "bobot")

            for (r in rowsMap.keys.sorted().take(20)) {
                val rowCells = rowsMap[r] ?: continue
                var score = 0
                for (cellVal in rowCells.values) {
                    val cleanVal = cleanHeader(cellVal)
                    if (keyTerms.any { cleanVal.contains(it) }) {
                        score++
                    }
                }
                if (score > maxHeaderScore) {
                    maxHeaderScore = score
                    bestHeaderRowIdx = r
                }
            }

            val headerMap = rowsMap[bestHeaderRowIdx] ?: emptyMap()
            val headers = headerMap.mapValues { cleanHeader(it.value) }

            val amCol = headers.entries.find { it.value.contains("am") }?.key ?: -1
            val acCol = headers.entries.find { it.value.contains("ac") }?.key ?: -1
            val storeCodeCol = headers.entries.find { it.value.contains("store code") || it.value.contains("kode toko") || it.value.contains("kode") }?.key ?: -1
            val storeNameCol = headers.entries.find { it.value.contains("store name") || it.value.contains("nama toko") || it.value.contains("toko") }?.key ?: -1
            val nikCol = headers.entries.find { it.value.contains("nik") }?.key ?: -1
            val nameCol = headers.entries.find { it.value.contains("name") || it.value.contains("nama") }?.key ?: -1
            val posCol = headers.entries.find { it.value.contains("position") || it.value.contains("jabatan") }?.key ?: -1
            val pwpCol = headers.entries.find { it.value.contains("pwp") }?.key ?: -1
            val psmCol = headers.entries.find { it.value.contains("psm") }?.key ?: -1
            val serbaCol = headers.entries.find { it.value.contains("serba") || it.value.contains("gratis") }?.key ?: -1
            val memberCol = headers.entries.find { it.value.contains("member") }?.key ?: -1

            val sortedRowKeys = rowsMap.keys.sorted().filter { it > bestHeaderRowIdx }
            for (r in sortedRowKeys) {
                val rowCells = rowsMap[r] ?: continue
                if (rowCells.values.all { it.isBlank() }) continue

                val nik = getSafeMapString(rowCells, nikCol, "NIK-$r")
                val name = getSafeMapString(rowCells, nameCol, "Personil $r")
                val pos = getSafeMapString(rowCells, posCol, "CREW")
                val storeCode = getSafeMapString(rowCells, storeCodeCol, "T001")
                val storeName = getSafeMapString(rowCells, storeNameCol, "Toko $storeCode")
                val ac = getSafeMapString(rowCells, acCol, "AC Area")
                val am = getSafeMapString(rowCells, amCol, "AM Region")

                val pwp = getSafeMapDouble(rowCells, pwpCol, 15.0)
                val psm = getSafeMapDouble(rowCells, psmCol, 15.0)
                val serba = getSafeMapDouble(rowCells, serbaCol, 25.0)
                val member = getSafeMapDouble(rowCells, memberCol, 18.0)

                list.add(
                    PersonnelKpiEntity(
                        nik = nik,
                        name = name,
                        position = pos,
                        storeCode = storeCode,
                        storeName = storeName,
                        ac = ac,
                        am = am,
                        pwp = pwp,
                        psm = psm,
                        serbaGratis = serba,
                        member = member
                    )
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseSharedStrings(inputStream: InputStream, strings: MutableList<String>) {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        var eventType = parser.eventType
        var currentText = StringBuilder()
        var inT = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "t") {
                        inT = true
                        currentText.clear()
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inT) {
                        currentText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") {
                        inT = false
                        strings.add(currentText.toString())
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseSheetXml(
        inputStream: InputStream,
        sharedStrings: List<String>,
        rowsMap: MutableMap<Int, MutableMap<Int, String>>
    ) {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        var eventType = parser.eventType

        var currentRowIdx = -1
        var currentColIdx = -1
        var cellType = ""
        var cellVal = StringBuilder()
        var inV = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            val rVal = parser.getAttributeValue(null, "r")
                            currentRowIdx = rVal?.toIntOrNull() ?: (currentRowIdx + 1)
                        }
                        "c" -> {
                            cellType = parser.getAttributeValue(null, "t") ?: ""
                            val cellRef = parser.getAttributeValue(null, "r") ?: ""
                            currentColIdx = parseColIndexFromRef(cellRef)
                        }
                        "v" -> {
                            inV = true
                            cellVal.clear()
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inV) {
                        cellVal.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> {
                            inV = false
                            val raw = cellVal.toString().trim()
                            val parsedValue = if (cellType == "s") {
                                val sIdx = raw.toIntOrNull() ?: -1
                                if (sIdx in sharedStrings.indices) sharedStrings[sIdx] else raw
                            } else {
                                raw
                            }
                            if (currentRowIdx >= 0 && currentColIdx >= 0) {
                                val rowMap = rowsMap.getOrPut(currentRowIdx) { mutableMapOf() }
                                rowMap[currentColIdx] = parsedValue
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseColIndexFromRef(ref: String): Int {
        val colLetters = ref.takeWhile { it.isLetter() }.uppercase()
        if (colLetters.isEmpty()) return 0
        var col = 0
        for (char in colLetters) {
            col = col * 26 + (char - 'A' + 1)
        }
        return col - 1
    }

    private fun cleanHeader(str: String): String {
        return str.lowercase().trim().replace("_", " ").replace("-", " ")
    }

    private fun getSafeCell(row: List<String>, idx: Int, default: String): String {
        if (idx in row.indices && row[idx].isNotBlank()) {
            return row[idx].trim()
        }
        return default
    }

    private fun getSafeDouble(row: List<String>, idx: Int, default: Double): Double {
        if (idx in row.indices) {
            val v = row[idx].replace(",", ".").toDoubleOrNull()
            if (v != null) return v
        }
        return default
    }

    private fun getSafeMapString(map: Map<Int, String>, col: Int, default: String): String {
        val v = map[col]
        return if (!v.isNullByBlank()) v!!.trim() else default
    }

    private fun getSafeMapDouble(map: Map<Int, String>, col: Int, default: Double): Double {
        val v = map[col]
        if (v != null) {
            val d = v.replace(",", ".").toDoubleOrNull()
            if (d != null) return d
        }
        return default
    }

    private fun String?.isNullByBlank(): Boolean = this == null || this.isBlank()

    fun generateCsvExport(list: List<PersonnelKpiEntity>): String {
        val sb = StringBuilder()
        sb.append("NIK;NAME;POSITION;STORE CODE;STORE NAME;AC;AM;PWP;PSM;SERBA GRATIS;MEMBER;GRAND TOTAL BOBOT;KET;ANALISIS KEKURANGAN\n")
        for (item in list) {
            sb.append("${item.nik};${item.name};${item.position};${item.storeCode};${item.storeName};${item.ac};${item.am};${item.pwp};${item.psm};${item.serbaGratis};${item.member};${item.grandTotalBobot};${item.ket};${item.analisisKekurangan}\n")
        }
        return sb.toString()
    }
}
