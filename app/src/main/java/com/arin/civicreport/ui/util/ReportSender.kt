package com.arin.civicreport.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Opens the phone's mail and/or messaging app with the complaint already written,
 * addressed to the chosen officials and, where possible, with the photo or video attached.
 */
object ReportSender {

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val emails = authorities
            .map { it.email.trim() }
            .filter { it.isNotBlank() && it.contains("@") }
            .distinct()
        val phones = authorities
            .map { it.phone.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val body = buildBody(report)
        val subject = buildSubject(report)
        val media = report.mediaUris.firstOrNull { it.isNotBlank() }

        when (method) {
            "email" -> sendEmail(context, emails, subject, body, report.mediaType, media)
            "sms" -> sendSms(context, phones, body)
            else -> {
                sendEmail(context, emails, subject, body, report.mediaType, media)
                sendSms(context, phones, body)
            }
        }
    }

    private fun buildSubject(report: Report): String {
        val types = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        val where = report.city.ifBlank { report.district.ifBlank { report.state } }
        return "Complaint: $types${if (where.isNotBlank()) " at $where" else ""}"
    }

    private fun buildBody(report: Report): String {
        val types = report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
        val stamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(report.timestamp))
        val builder = StringBuilder()
        builder.appendLine("Respected Sir/Madam,")
        builder.appendLine()
        builder.appendLine("I would like to report the following issue:")
        builder.appendLine("Incident: $types")
        builder.appendLine("Filed on: $stamp")
        builder.appendLine()
        builder.appendLine("Exact location (GPS): ${String.format(Locale.US, "%.6f", report.latitude)}, ${String.format(Locale.US, "%.6f", report.longitude)}")
        builder.appendLine("Address: ${report.address.ifBlank { "not available" }}")
        if (report.pincode.isNotBlank()) builder.appendLine("PIN code: ${report.pincode}")
        if (report.description.isNotBlank()) {
            builder.appendLine()
            builder.appendLine("Notes: ${report.description}")
        }
        builder.appendLine()
        if (!report.mediaUris.firstOrNull().isNullOrBlank()) {
            builder.appendLine("Photograph / video of the spot: ${report.mediaUris.first()}")
        }
        builder.appendLine()
        builder.appendLine("Map link: https://www.google.com/maps/search/?api=1&query=${report.latitude},${report.longitude}")
        builder.appendLine()
        builder.appendLine("Kindly inspect the spot and take the necessary action.")
        builder.appendLine()
        builder.appendLine("Sent through DuarBagori Reports.")
        return builder.toString()
    }

    private fun mimeFor(mediaType: String): String =
        if (mediaType.equals("video", ignoreCase = true)) "video/*" else "image/*"

    private fun sendEmail(
        context: Context,
        emails: List<String>,
        subject: String,
        body: String,
        mediaType: String,
        media: String?
    ) {
        if (emails.isEmpty()) return
        val recipients = emails.toTypedArray()
        try {
            if (media != null) {
                val uri = Uri.parse(media)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeFor(mediaType)
                    putExtra(Intent.EXTRA_EMAIL, recipients)
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Send complaint by email"))
            } else {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:" + emails.joinToString(","))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                context.startActivity(intent)
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No email app found on this phone.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Email could not be opened: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendSms(context: Context, phones: List<String>, body: String) {
        if (phones.isEmpty()) return
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:" + phones.joinToString(","))
                putExtra("sms_body", body)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No messaging app found on this phone.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Messaging could not be opened: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
