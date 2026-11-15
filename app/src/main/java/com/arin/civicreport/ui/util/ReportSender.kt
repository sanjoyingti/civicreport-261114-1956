package com.arin.civicreport.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds the complaint text and hands it to the phone's own email app / messaging app.
 * The app itself never talks to a server, so no account or backend is needed.
 */
object ReportSender {

    private const val TAG = "ReportSender"

    fun subject(report: Report): String =
        "Complaint: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }

    fun body(report: Report): String {
        val stamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(report.timestamp))
        val mapLink = "https://www.openstreetmap.org/?mlat=${report.latitude}&mlon=${report.longitude}#map=18/${report.latitude}/${report.longitude}"
        return buildString {
            appendLine("Respected Sir/Madam,")
            appendLine()
            appendLine("I wish to report the following civic issue:")
            appendLine()
            appendLine("Issue type: " + report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) })
            appendLine("Reported on: $stamp")
            appendLine("Address: " + report.address.ifBlank { "not available" })
            if (report.city.isNotBlank() || report.state.isNotBlank()) {
                appendLine("Area: ${report.city} ${report.district} ${report.state} ${report.pincode}".trim())
            }
            appendLine("GPS: ${report.latitude}, ${report.longitude}")
            appendLine("Map: $mapLink")
            if (report.description.isNotBlank()) {
                appendLine()
                appendLine("Details: " + report.description)
            }
            appendLine()
            appendLine("Photograph / video evidence is attached where possible.")
            appendLine("Requesting early inspection and action.")
            appendLine()
            appendLine("Regards,")
            appendLine("Citizen report filed with DuarBagori Reports")
        }
    }

    fun send(context: Context, authorities: List<Authority>, report: Report, method: String) {
        val emails = authorities.map { it.email.trim() }.filter { it.contains("@") }.distinct()
        val phones = authorities.map { it.phone.trim() }.filter { it.isNotBlank() }.distinct()
        val subject = subject(report)
        val body = body(report)

        if (method == "email" || method == "both") {
            if (emails.isNotEmpty()) sendEmail(context, emails, subject, body, report)
        }
        if (method == "sms" || method == "both") {
            if (phones.isNotEmpty()) sendSms(context, phones, body)
        }
    }

    private fun sendEmail(
        context: Context,
        emails: List<String>,
        subject: String,
        body: String,
        report: Report
    ) {
        val media = report.mediaUris.firstOrNull { it.isNotBlank() }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = when {
                media == null -> "text/plain"
                report.mediaType == "video" -> "video/mp4"
                else -> "image/jpeg"
            }
            putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            if (media != null) {
                putExtra(Intent.EXTRA_STREAM, Uri.parse(media))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Send complaint by email"))
        } catch (e: Exception) {
            Log.e(TAG, "Email intent failed", e)
            sendMailTo(context, emails, subject, body)
        }
    }

    private fun sendMailTo(context: Context, emails: List<String>, subject: String, body: String) {
        val uri = Uri.parse("mailto:" + emails.joinToString(",") + "?subject=" +
            Uri.encode(subject) + "&body=" + Uri.encode(body))
        val intent = Intent(Intent.ACTION_SENDTO, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "No email app available", e)
        }
    }

    private fun sendSms(context: Context, phones: List<String>, body: String) {
        val uri = Uri.parse("smsto:" + phones.joinToString(","))
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", body.take(1400))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "SMS intent failed", e)
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("sms:" + phones.joinToString(","))
                    ).putExtra("sms_body", body.take(1400))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (inner: Exception) {
                Log.e(TAG, "No messaging app available", inner)
            }
        }
    }
}
