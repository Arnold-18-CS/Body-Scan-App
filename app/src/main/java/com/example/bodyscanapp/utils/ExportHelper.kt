package com.example.bodyscanapp.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.bodyscanapp.data.entity.Scan
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper class for exporting scan data to various formats
 * 
 * Supports:
 * - JSON export (complete scan data)
 * - CSV export (measurements only)
 * - PDF export (formatted report with measurements and images)
 */
object ExportHelper {
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    /**
     * Export scan to JSON file
     * 
     * @param scan Scan entity to export
     * @param outputFile Output file path
     * @throws Exception if file writing fails
     */
    fun exportToJson(scan: Scan, outputFile: File) {
        try {
            val json = gson.toJson(scan)
            FileWriter(outputFile).use { writer ->
                writer.write(json)
            }
        } catch (e: Exception) {
            android.util.Log.e("ExportHelper", "Error exporting to JSON", e)
            throw e
        }
    }
    
    /**
     * Export measurements to CSV file
     * 
     * @param measurements Map of measurement names to values
     * @param outputFile Output file path
     * @throws Exception if file writing fails
     */
    fun exportToCsv(measurements: Map<String, Float>, outputFile: File) {
        try {
            FileWriter(outputFile).use { writer ->
                // Write header
                writer.append("Measurement,Value (cm)\n")
                
                // Write data rows
                measurements.forEach { (name, value) ->
                    // Escape commas and quotes in measurement names
                    val escapedName = name.replace("\"", "\"\"").let { 
                        if (it.contains(",") || it.contains("\"")) "\"$it\"" else it
                    }
                    writer.append("$escapedName,$value\n")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ExportHelper", "Error exporting to CSV", e)
            throw e
        }
    }
    
    /**
     * Export scan to CSV (convenience method that parses measurements from scan)
     * 
     * @param scan Scan entity to export
     * @param outputFile Output file path
     * @throws Exception if file writing fails
     */
    fun exportScanToCsv(scan: Scan, outputFile: File) {
        val measurements = parseMeasurementsFromJson(scan.measurementsJson)
        exportToCsv(measurements, outputFile)
    }
    
    /**
     * Export scan to PDF file with formatted report
     * 
     * Includes:
     * - Title and date
     * - Measurements table
     * - Embedded images (if provided)
     * 
     * @param scan Scan entity to export
     * @param images List of Bitmap images to include (e.g., captured photos, 3D mesh screenshot)
     * @param outputFile Output file path
     * @throws Exception if PDF creation fails
     */
    fun exportToPdf(scan: Scan, images: List<Bitmap>, outputFile: File) {
        try {
            val writer = PdfWriter(outputFile.absolutePath)
            val pdfDocument = PdfDocument(writer)
            val document = Document(pdfDocument)
            
            // Add title
            val title = Paragraph("Body Scan Report")
                .setFontSize(20f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10f)
            document.add(title)
            
            // Add date
            val dateStr = dateFormat.format(Date(scan.timestamp))
            val dateParagraph = Paragraph("Date: $dateStr")
                .setFontSize(12f)
                .setMarginBottom(20f)
            document.add(dateParagraph)
            
            // Add height information
            val heightParagraph = Paragraph("Height: ${scan.heightCm} cm")
                .setFontSize(12f)
                .setMarginBottom(20f)
            document.add(heightParagraph)
            
            // Add measurements table
            val measurements = parseMeasurementsFromJson(scan.measurementsJson)
            if (measurements.isNotEmpty()) {
                val table = Table(2)
                table.setWidth(500f)
                
                // Header row
                val headerCell1 = Cell()
                    .add(Paragraph("Measurement").setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                val headerCell2 = Cell()
                    .add(Paragraph("Value (cm)").setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                table.addHeaderCell(headerCell1)
                table.addHeaderCell(headerCell2)
                
                // Data rows
                measurements.forEach { (name, value) ->
                    table.addCell(name)
                    table.addCell(String.format("%.1f", value))
                }
                
                document.add(table)
                document.add(Paragraph().setMarginBottom(20f)) // Spacing
            }
            
            // Add images
            images.forEachIndexed { index, bitmap ->
                try {
                    val imageBytes = bitmapToByteArray(bitmap)
                    val imageData = ImageDataFactory.create(imageBytes)
                    val image = Image(imageData)
                    
                    // Scale image to fit page width (max 500 points)
                    val maxWidth = 500f
                    if (image.imageWidth > maxWidth) {
                        val scale = maxWidth / image.imageWidth
                        image.scale(scale, scale)
                    }
                    
                    document.add(Paragraph("Image ${index + 1}").setMarginTop(10f))
                    document.add(image)
                    document.add(Paragraph().setMarginBottom(10f)) // Spacing
                } catch (e: Exception) {
                    android.util.Log.e("ExportHelper", "Error adding image $index to PDF", e)
                    // Continue with next image
                }
            }
            
            document.close()
        } catch (e: Exception) {
            android.util.Log.e("ExportHelper", "Error exporting to PDF", e)
            throw e
        }
    }
    
    /**
     * Parse measurements from JSON string
     */
    private fun parseMeasurementsFromJson(json: String): Map<String, Float> {
        return try {
            val type = object : TypeToken<Map<String, Float>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            android.util.Log.e("ExportHelper", "Error parsing measurements JSON", e)
            emptyMap()
        }
    }
    
    /**
     * Convert Bitmap to byte array (PNG format)
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
        return outputStream.toByteArray()
    }
    
    /**
     * Create a placeholder bitmap if no images are provided
     * Useful for PDF export when images are not available
     */
    fun createPlaceholderBitmap(width: Int = 400, height: Int = 300): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.LTGRAY)
        
        val paint = Paint().apply {
            color = Color.DKGRAY
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        
        val text = "Image not available"
        val x = width / 2f
        val y = height / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, x, y, paint)
        
        return bitmap
    }
    
    // ========== Bulk Export Functions ==========
    
    /**
     * Export all scans as ZIP file (all JSON files)
     * 
     * @param scans List of scans to export
     * @param outputFile Output ZIP file
     * @return Result indicating success or failure
     */
    fun exportAllScansAsZip(scans: List<com.example.bodyscanapp.data.entity.Scan>, outputFile: File): Result<Unit> {
        return try {
            val zipOutputStream = java.util.zip.ZipOutputStream(java.io.FileOutputStream(outputFile))
            
            scans.forEachIndexed { index, scan ->
                val entryName = "scan_${scan.id}_${index + 1}.json"
                val json = gson.toJson(scan)
                
                zipOutputStream.putNextEntry(java.util.zip.ZipEntry(entryName))
                zipOutputStream.write(json.toByteArray())
                zipOutputStream.closeEntry()
            }
            
            zipOutputStream.close()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ExportHelper", "Error exporting scans as ZIP", e)
            Result.failure(e)
        }
    }
    
    /**
     * Export all measurements as CSV file
     * 
     * @param scans List of scans to export
     * @param outputFile Output CSV file
     * @return Result indicating success or failure
     */
    fun exportAllMeasurementsAsCsv(scans: List<com.example.bodyscanapp.data.entity.Scan>, outputFile: File): Result<Unit> {
        return try {
            FileWriter(outputFile).use { writer ->
                // Write header
                writer.append("Scan ID,Date,Height (cm),Waist (cm),Chest (cm),Hips (cm),Thighs (cm),Arms (cm),Neck (cm)\n")
                
                // Write data rows
                scans.forEach { scan ->
                    val measurements = parseMeasurementsFromJson(scan.measurementsJson)
                    val dateStr = dateFormat.format(Date(scan.timestamp))
                    
                    writer.append("${scan.id},$dateStr,${scan.heightCm},")
                    writer.append("${measurements["waist"] ?: ""},")
                    writer.append("${measurements["chest"] ?: ""},")
                    writer.append("${measurements["hips"] ?: ""},")
                    writer.append("${measurements["thighs"] ?: ""},")
                    writer.append("${measurements["arms"] ?: ""},")
                    writer.append("${measurements["neck"] ?: ""}\n")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ExportHelper", "Error exporting measurements as CSV", e)
            Result.failure(e)
        }
    }
    
    /**
     * Export all scans as PDF report (multi-page)
     * 
     * @param scans List of scans to export
     * @param outputFile Output PDF file
     * @return Result indicating success or failure
     */
    fun exportAllScansAsPdf(scans: List<com.example.bodyscanapp.data.entity.Scan>, outputFile: File): Result<Unit> {
        return try {
            val writer = PdfWriter(outputFile.absolutePath)
            val pdfDocument = PdfDocument(writer)
            val document = Document(pdfDocument)
            
            // Add title page
            val title = Paragraph("Body Scan Report - All Scans")
                .setFontSize(24f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20f)
            document.add(title)
            
            val summary = Paragraph("Total Scans: ${scans.size}")
                .setFontSize(14f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(40f)
            document.add(summary)
            
            // Add each scan as a new page
            scans.forEachIndexed { index, scan ->
                if (index > 0) {
                    document.add(com.itextpdf.layout.element.AreaBreak(com.itextpdf.layout.element.AreaBreakType.NEXT_PAGE))
                }
                
                // Scan title
                val scanTitle = Paragraph("Scan #${index + 1} (ID: ${scan.id})")
                    .setFontSize(18f)
                    .setBold()
                    .setMarginBottom(10f)
                document.add(scanTitle)
                
                // Date
                val dateStr = dateFormat.format(Date(scan.timestamp))
                val dateParagraph = Paragraph("Date: $dateStr")
                    .setFontSize(12f)
                    .setMarginBottom(10f)
                document.add(dateParagraph)
                
                // Height
                val heightParagraph = Paragraph("Height: ${scan.heightCm} cm")
                    .setFontSize(12f)
                    .setMarginBottom(20f)
                document.add(heightParagraph)
                
                // Measurements table
                val measurements = parseMeasurementsFromJson(scan.measurementsJson)
                if (measurements.isNotEmpty()) {
                    val table = Table(2)
                    table.setWidth(500f)
                    
                    // Header row
                    val headerCell1 = Cell()
                        .add(Paragraph("Measurement").setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    val headerCell2 = Cell()
                        .add(Paragraph("Value (cm)").setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    table.addHeaderCell(headerCell1)
                    table.addHeaderCell(headerCell2)
                    
                    // Data rows
                    measurements.forEach { (name, value) ->
                        table.addCell(name)
                        table.addCell(String.format("%.1f", value))
                    }
                    
                    document.add(table)
                    document.add(Paragraph().setMarginBottom(20f)) // Spacing
                }
            }
            
            document.close()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ExportHelper", "Error exporting scans as PDF", e)
            Result.failure(e)
        }
    }
}

