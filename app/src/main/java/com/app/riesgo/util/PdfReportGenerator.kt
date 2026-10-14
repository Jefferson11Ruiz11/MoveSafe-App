package com.app.riesgo.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.app.riesgo.data.model.EvaluationRecord
import com.app.riesgo.data.model.RiskLevel
import com.app.riesgo.data.repository.AppRepository
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PdfReportGenerator {

    fun generateAndSharePdf(
        context: Context,
        record: EvaluationRecord,
        userName: String,
    ): File? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 Width en puntos (72dpi)
        val pageHeight = 842 // A4 Height en puntos (72dpi)

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            isAntiAlias = true
        }

        var currentY = 115f

        // 1. Encabezado Superior (Banda Principal MoveSafe)
        paint.color = Color.parseColor("#1E293B") // Slate Dark
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, paint)

        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("MoveSafe", 24f, 40f, paint)

        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Sistema de Evaluación Biomecánica y Prevención Lesional", 24f, 62f, paint)

        paint.color = Color.parseColor("#E2E8F0")
        paint.textSize = 10f
        val fechaText = "Fecha: ${record.fecha}"
        val fechaWidth = paint.measureText(fechaText)
        canvas.drawText(fechaText, pageWidth - 24f - fechaWidth, 40f, paint)

        // 2. Tarjeta de Datos del Atleta
        paint.color = Color.parseColor("#F8FAFC")
        val athleteCardRect = RectF(24f, currentY, pageWidth - 24f, currentY + 95f)
        canvas.drawRoundRect(athleteCardRect, 10f, 10f, paint)

        paint.color = Color.parseColor("#334155")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FICHA DEL ATLETA", 38f, currentY + 24f, paint)

        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val displayName = userName.ifBlank { "Atleta Registrado" }
        canvas.drawText("Atleta: $displayName", 38f, currentY + 44f, paint)
        canvas.drawText("Disciplina Evaluada: ${record.deporteEvaluado}", 38f, currentY + 62f, paint)
        canvas.drawText("Prueba Realizada: ${record.tipoPrueba}", 38f, currentY + 80f, paint)

        val activeUser = AppRepository.currentUser
        val displayEdad = if (record.edad > 0) record.edad else (activeUser?.edad ?: 25)
        val displayPeso = if (record.peso > 0f) record.peso else (activeUser?.peso ?: 70f)
        val displayAltura = if (record.altura > 0f) record.altura else (activeUser?.altura ?: 175f)
        val displayImc = if (record.imc > 0f) record.imc else (displayPeso / ((displayAltura / 100f) * (displayAltura / 100f)))

        val rightX = 330f
        val imcCategory = when {
            displayImc < 18.5f -> "Bajo peso"
            displayImc < 25f -> "Peso normal (OMS)"
            displayImc < 30f -> "Sobrepeso (OMS)"
            else -> "Obesidad (OMS)"
        }

        canvas.drawText("Edad: $displayEdad años", rightX, currentY + 44f, paint)
        canvas.drawText("Peso: $displayPeso kg | Estatura: ${displayAltura.toInt()} cm", rightX, currentY + 62f, paint)
        canvas.drawText("Índice IMC: ${String.format(Locale.US, "%.1f", displayImc)} kg/m² ($imcCategory)", rightX, currentY + 80f, paint)

        currentY += 115f

        // 3. Resumen del Nivel de Riesgo (Semáforo Visual)
        val (badgeBgColor, badgeTextColor, labelRiesgo) = when (record.nivelRiesgo) {
            RiskLevel.BAJO -> Triple("#D1FAE5", "#065F46", "RIESGO LESIONAL BAJO")
            RiskLevel.MODERADO -> Triple("#FEF3C7", "#92400E", "RIESGO LESIONAL MODERADO")
            RiskLevel.ALTO -> Triple("#FEE2E2", "#991B1B", "RIESGO LESIONAL ELEVADO")
        }

        paint.color = Color.parseColor(badgeBgColor)
        val riskCardRect = RectF(24f, currentY, pageWidth - 24f, currentY + 90f)
        canvas.drawRoundRect(riskCardRect, 12f, 12f, paint)

        paint.color = Color.parseColor(badgeTextColor)
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(labelRiesgo, 40f, currentY + 36f, paint)

        val probPercent = (record.probabilidad * 100).toInt()
        paint.textSize = 30f
        canvas.drawText("$probPercent%", pageWidth - 100f, currentY + 54f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Probabilidad estimada de sobrecarga/lesión", 40f, currentY + 62f, paint)

        currentY += 110f

        // 4. Diagnóstico en Lenguaje Claro
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DIAGNÓSTICO PREVENTIVO EN LENGUAJE CLARO", 24f, currentY, paint)

        currentY += 18f
        paint.color = Color.parseColor("#334155")
        paint.textSize = 10.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val plainExplanation = when (record.nivelRiesgo) {
            RiskLevel.BAJO -> "Tu técnica de movimiento y patrón de carga articular muestran adecuada amortiguación y estabilidad. Se recomienda mantener la rutina de movilidad y control neuromuscular para prevenir desgaste futuro."
            RiskLevel.MODERADO -> "Se identificaron factores moderados de sobrecarga articular o ligera inestabilidad en los patrones de movimiento. Se aconseja incorporar ejercicios de fortalecimiento estabilizador y calentamiento neuromuscular."
            RiskLevel.ALTO -> "Existe una vulnerabilidad o restricción importante en la cadena cinética durante la prueba física. Se sugiere atención en los protocolos de prevención y consulta especializada en fisioterapia/kinesiología deportiva."
        }

        val lines = splitTextIntoLines(plainExplanation, paint, pageWidth - 48f)
        lines.forEach { line ->
            canvas.drawText(line, 24f, currentY, paint)
            currentY += 15f
        }

        currentY += 15f

        // 5. Hallazgos y Zonas Anatómicas Evaluadas
        if (record.zonasCriticas.isNotEmpty()) {
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("ZONAS ANATÓMICAS EVALUADAS Y PUNTOS CRÍTICOS", 24f, currentY, paint)
            currentY += 18f

            record.zonasCriticas.take(4).forEach { zone ->
                paint.color = Color.parseColor("#F1F5F9")
                val zoneRect = RectF(24f, currentY, pageWidth - 24f, currentY + 36f)
                canvas.drawRoundRect(zoneRect, 8f, 8f, paint)

                val (zoneColor, zoneLabel) = when (zone.alertLevel) {
                    "ALTO" -> Pair("#DC2626", "ALTA ALERTA")
                    "MODERADO" -> Pair("#D97706", "MODERADO")
                    else -> Pair("#16A34A", "NORMAL")
                }

                paint.color = Color.parseColor("#1E293B")
                paint.textSize = 10.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("• ${zone.zoneName} (${zone.category})", 34f, currentY + 16f, paint)

                paint.color = Color.parseColor(zoneColor)
                paint.textSize = 9.5f
                val labelW = paint.measureText(zoneLabel)
                canvas.drawText(zoneLabel, pageWidth - 36f - labelW, currentY + 16f, paint)

                paint.color = Color.parseColor("#64748B")
                paint.textSize = 9f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(zone.description.take(85), 34f, currentY + 29f, paint)

                currentY += 42f
            }
        }

        currentY += 10f

        // 6. Plan de Recomendaciones Preventivas (Generado por IA o Reglas)
        val aiPlan = record.aiCorrectivePlan
        if (aiPlan != null && aiPlan.ejerciciosRecomendados.isNotEmpty()) {
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("PLAN CORRECTIVO PREVENTIVO (Powered by Gemini AI)", 24f, currentY, paint)
            currentY += 18f

            if (aiPlan.resumenEnfoque.isNotBlank()) {
                paint.color = Color.parseColor("#475569")
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                val summaryLines = splitTextIntoLines(aiPlan.resumenEnfoque, paint, pageWidth - 48f)
                summaryLines.forEach { sLine ->
                    canvas.drawText(sLine, 24f, currentY, paint)
                    currentY += 13f
                }
                currentY += 6f
            }

            aiPlan.ejerciciosRecomendados.take(3).forEach { ex ->
                paint.color = Color.parseColor("#0284C7")
                canvas.drawCircle(30f, currentY - 3f, 3f, paint)

                paint.color = Color.parseColor("#0F172A")
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val titleText = "${ex.nombre} [${ex.dosificacion}]"
                canvas.drawText(titleText, 40f, currentY, paint)
                currentY += 14f

                if (ex.explicacionTecnica.isNotBlank()) {
                    paint.color = Color.parseColor("#475569")
                    paint.textSize = 9f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    val expLines = splitTextIntoLines(ex.explicacionTecnica, paint, pageWidth - 64f)
                    expLines.forEach { eLine ->
                        canvas.drawText(eLine, 40f, currentY, paint)
                        currentY += 12f
                    }
                }
                currentY += 4f
            }

            if (aiPlan.pautaFrecuencia.isNotBlank()) {
                paint.color = Color.parseColor("#0F766E")
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Frecuencia sugerida: ${aiPlan.pautaFrecuencia}", 24f, currentY, paint)
                currentY += 16f
            }
        } else if (record.recomendaciones.isNotEmpty()) {
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("PLAN DE RECOMENDACIONES PREVENTIVAS", 24f, currentY, paint)
            currentY += 18f

            record.recomendaciones.take(4).forEach { rec ->
                paint.color = Color.parseColor("#0284C7")
                canvas.drawCircle(30f, currentY - 3f, 3f, paint)

                paint.color = Color.parseColor("#334155")
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                val recLines = splitTextIntoLines(rec, paint, pageWidth - 60f)
                recLines.forEach { rLine ->
                    canvas.drawText(rLine, 40f, currentY, paint)
                    currentY += 14f
                }
                currentY += 3f
            }
        }

        // 7. Pie de Página Formal MoveSafe
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val footerNotice = "Nota: Este informe es un reporte preventivo de orientación biomecánica y no sustituye la evaluación o diagnóstico médico. MoveSafe v1.0."
        canvas.drawText(footerNotice, 24f, pageHeight - 25f, paint)

        pdfDocument.finishPage(page)

        return try {
            val sanitizedUser = userName.replace(" ", "_").replace(Regex("[^a-zA-Z0-9_]"), "")
            val sanitizedDate = record.fecha.replace("/", "-").replace(":", "-").replace(" ", "_")
            val fileName = "Reporte_MoveSafe_${sanitizedUser}_$sanitizedDate.pdf"

            val pdfFile = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Reporte Biomecánico MoveSafe - $userName")
                putExtra(Intent.EXTRA_TEXT, "Adjunto el reporte biomecánico preventivo MoveSafe.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Compartir Reporte PDF MoveSafe"))
            pdfFile
        } catch (_: Exception) {
            pdfDocument.close()
            null
        }
    }

    private fun splitTextIntoLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            currentLine = if (paint.measureText(testLine) <= maxWidth) {
                testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }
}
